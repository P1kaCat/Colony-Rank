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
 * Handles data refresh, JSON export, and Discord publishing.
 */
public class ColonyDataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");
    private static final String JSON_FILE_PATH = "data/colonyrank/colonies.json";

    private final MineColoniesAPIHelper apiHelper = new MineColoniesAPIHelper();
    private final DiscordWebhookPublisher discordPublisher = new DiscordWebhookPublisher();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<Integer, ColonyData> colonyCache = new ConcurrentHashMap<>();

    public ColonyDataCollector() {
        LOGGER.info("ColonyDataCollector initialise");
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

        LOGGER.info("Cache des colonies mis a jour: {} colonies", colonyCache.size());

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
     */
    public List<ColonyData> getColoniesRanked() {
        List<ColonyData> ranked = new ArrayList<>(colonyCache.values());
        ranked.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return ranked;
    }

    /**
     * Get a colony by its ID.
     */
    public ColonyData getColonyData(int colonyId) {
        return colonyCache.get(colonyId);
    }

    /**
     * Get a colony by its name (case-insensitive).
     */
    public ColonyData getColonyDataByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lowerName = name.trim().toLowerCase(Locale.ROOT);
        for (ColonyData colony : colonyCache.values()) {
            if (colony.getName() != null && colony.getName().trim().toLowerCase(Locale.ROOT).equals(lowerName)) {
                return colony;
            }
        }
        return null;
    }

    /**
     * Get all cached colonies.
     */
    public Map<Integer, ColonyData> getAllColonies() {
        return Collections.unmodifiableMap(colonyCache);
    }

    /**
     * Get the number of cached colonies.
     */
    public int getColonyCount() {
        return colonyCache.size();
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
            root.addProperty("colonyCount", colonyCache.size());

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

    /**
     * Check if Discord webhook is configured.
     */
    public boolean isDiscordWebhookConfigured() {
        return discordPublisher.isWebhookConfigured();
    }

    /**
     * Get the Discord config file path.
     */
    public String getDiscordConfigPath() {
        return discordPublisher.getConfigPath();
    }

    /**
     * Get the Discord status line for display.
     */
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
