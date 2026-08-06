package com.colonyrank.mod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the list of ignored colonies.
 * Ignored colonies are excluded from the ranking, score calculation, and Discord publishing.
 * Stored in config/colonyrank/ignored_colonies.json
 */
public class IgnoredColoniesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");
    private static final Path IGNORED_FILE = Paths.get("config", "colonyrank", "ignored_colonies.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<Integer> ignoredIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public IgnoredColoniesManager() {
        load();
    }

    /**
     * Load ignored colonies from the JSON file.
     */
    public void load() {
        try {
            if (Files.exists(IGNORED_FILE)) {
                String content = Files.readString(IGNORED_FILE);
                JsonArray array = JsonParser.parseString(content).getAsJsonArray();
                ignoredIds.clear();
                for (JsonElement element : array) {
                    ignoredIds.add(element.getAsInt());
                }
                LOGGER.info("Charge {} colonie(s) ignoree(s)", ignoredIds.size());
            }
        } catch (Exception e) {
            LOGGER.warn("Impossible de charger les colonies ignorees: {}", e.getMessage());
        }
    }

    /**
     * Save ignored colonies to the JSON file.
     */
    public void save() {
        try {
            Files.createDirectories(IGNORED_FILE.getParent());
            JsonArray array = new JsonArray();
            for (int id : new TreeSet<>(ignoredIds)) {
                array.add(id);
            }
            Files.writeString(IGNORED_FILE, GSON.toJson(array));
        } catch (IOException e) {
            LOGGER.warn("Impossible de sauvegarder les colonies ignorees: {}", e.getMessage());
        }
    }

    /**
     * Add a colony to the ignored list.
     * @return true if the colony was added (was not already ignored)
     */
    public boolean ignore(int colonyId) {
        boolean added = ignoredIds.add(colonyId);
        if (added) save();
        return added;
    }

    /**
     * Remove a colony from the ignored list.
     * @return true if the colony was removed (was ignored)
     */
    public boolean unignore(int colonyId) {
        boolean removed = ignoredIds.remove(colonyId);
        if (removed) save();
        return removed;
    }

    /**
     * Check if a colony is ignored.
     */
    public boolean isIgnored(int colonyId) {
        return ignoredIds.contains(colonyId);
    }

    /**
     * Get the set of ignored colony IDs.
     */
    public Set<Integer> getIgnoredIds() {
        return Collections.unmodifiableSet(ignoredIds);
    }

    /**
     * Get the count of ignored colonies.
     */
    public int getIgnoredCount() {
        return ignoredIds.size();
    }

    /**
     * Clear all ignored colonies.
     */
    public void clear() {
        ignoredIds.clear();
        save();
    }
}
