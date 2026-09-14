package com.colonyrank.mod.data;

import com.colonyrank.mod.util.LocalizationManager;

import java.util.Locale;

public enum ScoreMode {
    OLD("old", "score.mode.old", "score.mode.old.desc"),
    NEW("new", "score.mode.new", "score.mode.new.desc");

    private final String id;
    private final String labelKey;
    private final String descriptionKey;

    ScoreMode(String id, String labelKey, String descriptionKey) {
        this.id = id;
        this.labelKey = labelKey;
        this.descriptionKey = descriptionKey;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return LocalizationManager.get().t(labelKey);
    }

    public String getDescription() {
        return LocalizationManager.get().t(descriptionKey);
    }

    public boolean isOld() {
        return this == OLD;
    }

    public boolean isNew() {
        return this == NEW;
    }

    public static ScoreMode fromId(String value) {
        if (value == null) {
            return OLD;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ScoreMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return OLD;
    }
}
