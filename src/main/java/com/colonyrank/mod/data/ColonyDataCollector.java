package com.colonyrank.mod.data;

import com.colonyrank.mod.util.DiscordWebhookPublisher;
import com.colonyrank.mod.ColonyRankMod;
import com.colonyrank.mod.util.MineColoniesAPIHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects and caches colony data from MineColonies.
 * Handles data refresh, JSON export, Discord publishing, and ignored colonies.
 */
public class ColonyDataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");
    private static final String JSON_FILE_PATH = "data/colonyrank/colonies.json";

    private final MineColoniesAPIHelper apiHelper = new MineColoniesAPIHelper();
    private final DiscordWebhookPublisher discordPublisher = new DiscordWebhookPublisher();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final IgnoredColoniesManager ignoredManager = new IgnoredColoniesManager();

    private final Map<Integer, ColonyData> colonyCache = new ConcurrentHashMap<>();

    public ColonyDataCollector() {
        LOGGER.info("ColonyDataCollector initialise ({} colonies ignorees)", ignoredManager.getIgnoredCount());
    }

    /**
     * Update all colonies from all server levels.
     * @param server the Minecraft server
     */
    public void updateAllColonies(MinecraftServer server) {
        updateAllColonies(server, true);
    }

    /**
     * Update all colonies from all server levels.
     * @param server the Minecraft server
     * @param silent if true, does not publish to Discord on update
     */
    public void updateAllColonies(MinecraftServer server, boolean silent) {
        Set<Integer> allIds = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            allIds.addAll(apiHelper.getAllColonyIds(level));
        }

        LOGGER.debug("Mise a jour de {} colonies", allIds.size());

        // Remove colonies that no longer exist
        Set<Integer> staleIds = new HashSet<>(colonyCache.keySet());
        staleIds.removeAll(allIds);
        for (int id : staleIds) {
            colonyCache.remove(id);
        }

        // Update or add colonies
        for (int colonyId : allIds) {
            ColonyData data = findColonyInLevels(colonyId, server);
            if (data != null) {
                colonyCache.put(colonyId, data);
            }
        }

        LOGGER.info("Cache des colonies mis a jour: {} colonies ({} ignorees)", colonyCache.size(), ignoredManager.getIgnoredCount());

        if (!silent && discordPublisher.shouldPublishOnSave()) {
            discordPublisher.publishRankingNowAsync(getColoniesRanked());
        }
    }

    private ColonyData findColonyInLevels(int colonyId, MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            ColonyData data = apiHelper.getColonyData(colonyId, level);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    /**
     * Check if daily Discord publish is due, and publish if so.
     */
    public void publishDailyIfDue(MinecraftServer server) {
        if (discordPublisher.isDailyDue()) {
            updateAllColonies(server, true);
            if (ColonyRankMod.getScoreCalculator() != null) {
                ColonyRankMod.getScoreCalculator().recalculateAllScores(this);
            }
            saveColoniesToJson(true);
            discordPublisher.publishDailyNowAsync(getColoniesRanked());
        }
    }

    /**
     * Force publish the leaderboard to Discord immediately.
     */
    public void publishDiscordNow() {
        discordPublisher.publishRankingNowAsync(getColoniesRanked());
    }

    /**
     * Force a daily publish to Discord.
     */
    public void publishDailyNow(MinecraftServer server) {
        updateAllColonies(server, true);
        if (ColonyRankMod.getScoreCalculator() != null) {
            ColonyRankMod.getScoreCalculator().recalculateAllScores(this);
        }
        saveColoniesToJson(true);
        discordPublisher.publishDailyNowAsync(getColoniesRanked());
    }

    /**
     * Get colonies ranked by score (descending).
     * Ignored colonies are excluded from the ranking.
     */
    public List<ColonyData> getColoniesRanked() {
        List<ColonyData> ranked = new ArrayList<>();
        for (ColonyData colony : colonyCache.values()) {
            if (!ignoredManager.isIgnored(colony.getColonyId())) {
                ranked.add(colony);
            }
        }
        ranked.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return ranked;
    }

    /**
     * Get a colony by its ID.
     * Returns null for ignored colonies.
     */
    public ColonyData getColonyData(int colonyId) {
        if (ignoredManager.isIgnored(colonyId)) return null;
        return colonyCache.get(colonyId);
    }

    /**
     * Get a colony by its name (case-insensitive).
     * Returns null for ignored colonies.
     */
    public ColonyData getColonyDataByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lowerName = name.trim().toLowerCase(Locale.ROOT);
        for (ColonyData colony : colonyCache.values()) {
            if (ignoredManager.isIgnored(colony.getColonyId())) continue;
            if (colony.getName() != null && colony.getName().trim().toLowerCase(Locale.ROOT).equals(lowerName)) {
                return colony;
            }
        }
        return null;
    }

    /**
     * Get all cached colonies (including ignored ones).
     */
    public Map<Integer, ColonyData> getAllColonies() {
        return Collections.unmodifiableMap(colonyCache);
    }

    /**
     * Get all non-ignored cached colonies.
     */
    public Map<Integer, ColonyData> getActiveColonies() {
        Map<Integer, ColonyData> active = new LinkedHashMap<>();
        for (Map.Entry<Integer, ColonyData> entry : colonyCache.entrySet()) {
            if (!ignoredManager.isIgnored(entry.getKey())) {
                active.put(entry.getKey(), entry.getValue());
            }
        }
        return active;
    }

    /**
     * Get the number of cached colonies (including ignored).
     */
    public int getColonyCount() {
        return colonyCache.size();
    }

    /**
     * Get the number of active (non-ignored) colonies.
     */
    public int getActiveColonyCount() {
        int count = 0;
        for (int id : colonyCache.keySet()) {
            if (!ignoredManager.isIgnored(id)) count++;
        }
        return count;
    }

    /**
     * Clear the colony cache.
     */
    public void clearCache() {
        colonyCache.clear();
        LOGGER.info("Cache des colonies vide");
    }

    /**
     * Save colony data to JSON file.
     * Only active (non-ignored) colonies are included in the ranking.
     */
    public void saveColoniesToJson() {
        saveColoniesToJson(true);
    }

    /**
     * Save colony data to JSON file.
     * @param silent if true, does not trigger Discord publish
     */
    public void saveColoniesToJson(boolean silent) {
        Path path = Paths.get(JSON_FILE_PATH);
        try {
            Files.createDirectories(path.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("generatedAt", java.time.Instant.now().toString());
            root.addProperty("colonyCount", getActiveColonyCount());
            root.addProperty("ignoredCount", ignoredManager.getIgnoredCount());

            JsonArray coloniesArray = new JsonArray();
            for (ColonyData colony : getColoniesRanked()) {
                JsonObject colJson = new JsonObject();
                colJson.addProperty("id", colony.getColonyId());
                colJson.addProperty("name", colony.getName());
                colJson.addProperty("population", colony.getPopulation());
                colJson.addProperty("buildingCount", colony.getBuildingCount());
                colJson.addProperty("averageBuildingLevel", colony.getAverageBuildingLevel());
                colJson.addProperty("claimedChunks", colony.getClaimedChunks());
                colJson.addProperty("overallHappiness", colony.getOverallHappiness());
                colJson.addProperty("colonyAgeDays", colony.getColonyAgeDays());
                colJson.addProperty("colonyAgeHours", colony.getColonyAgeHours());
                colJson.addProperty("colonyAgeDisplay", colony.getColonyAgeDisplay());
                colJson.addProperty("score", colony.getScore());
                coloniesArray.add(colJson);
            }
            root.add("colonies", coloniesArray);

            Files.writeString(path, gson.toJson(root));
            LOGGER.debug("Donnees des colonies sauvegardees dans {}", path.toAbsolutePath());

            if (!silent && discordPublisher.shouldPublishOnSave()) {
                discordPublisher.publishRankingAsync(getColoniesRanked());
            }
        } catch (IOException e) {
            LOGGER.warn("Impossible de sauvegarder les donnees des colonies dans {}", path.toAbsolutePath(), e);
        }
    }

    // --- Ignored colonies management ---

    public IgnoredColoniesManager getIgnoredManager() {
        return ignoredManager;
    }

    public boolean ignoreColony(int colonyId) {
        return ignoredManager.ignore(colonyId);
    }

    public boolean unignoreColony(int colonyId) {
        return ignoredManager.unignore(colonyId);
    }

    public boolean isColonyIgnored(int colonyId) {
        return ignoredManager.isIgnored(colonyId);
    }

    // --- Discord ---

    public boolean isDiscordWebhookConfigured() {
        return discordPublisher.isWebhookConfigured();
    }

    public String getDiscordConfigPath() {
        return discordPublisher.getConfigPath();
    }

    public String getDiscordStatusLine() {
        return discordPublisher.getDailyStatusLine();
    }

    /**
     * Shutdown the collector and clean up resources.
     */
    public void shutdown() {
        saveColoniesToJson(true);
        LOGGER.info("ColonyDataCollector arrete");
    }
}
