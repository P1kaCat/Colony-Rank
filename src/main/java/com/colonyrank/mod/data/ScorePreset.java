package com.colonyrank.mod.data;

import com.colonyrank.mod.util.LocalizationManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public enum ScorePreset {
    OLD_ORIGINAL(
        "old_original",
        ScoreMode.OLD,
        "score.preset.old.original",
        "score.preset.old.original.desc",
        5.0,
        5.0,
        10.0,
        100.0,
        10.0,
        1.0
    ),
    OLD_POPULATION(
        "old_population",
        ScoreMode.OLD,
        "score.preset.old.population",
        "score.preset.old.population.desc",
        100.0,
        10.0,
        10.0,
        5.0,
        5.0,
        1.0
    ),
    OLD_EXPANSION(
        "old_expansion",
        ScoreMode.OLD,
        "score.preset.old.expansion",
        "score.preset.old.expansion.desc",
        10.0,
        5.0,
        10.0,
        5.0,
        100.0,
        1.0
    ),
    OLD_MANAGEMENT(
        "old_management",
        ScoreMode.OLD,
        "score.preset.old.management",
        "score.preset.old.management.desc",
        10.0,
        100.0,
        5.0,
        10.0,
        5.0,
        1.0
    ),
    OLD_METROPOLIS(
        "old_metropolis",
        ScoreMode.OLD,
        "score.preset.old.metropolis",
        "score.preset.old.metropolis.desc",
        10.0,
        5.0,
        100.0,
        5.0,
        10.0,
        1.0
    ),
    NEW_DEVELOPMENT(
        "new_development",
        ScoreMode.NEW,
        "score.preset.new.development",
        "score.preset.new.development.desc",
        5.0,
        0.4,
        5.0,
        2.5,
        0.5,
        10.0
    ),
    NEW_POPULATION(
        "new_population",
        ScoreMode.NEW,
        "score.preset.new.population",
        "score.preset.new.population.desc",
        10.0,
        0.6,
        5.0,
        1.5,
        0.5,
        10.0
    ),
    NEW_EXPANSION(
        "new_expansion",
        ScoreMode.NEW,
        "score.preset.new.expansion",
        "score.preset.new.expansion.desc",
        5.0,
        0.4,
        8.0,
        1.5,
        2.0,
        10.0
    ),
    NEW_MANAGEMENT(
        "new_management",
        ScoreMode.NEW,
        "score.preset.new.management",
        "score.preset.new.management.desc",
        5.0,
        1.0,
        6.0,
        1.5,
        0.5,
        10.0
    ),
    NEW_METROPOLIS(
        "new_metropolis",
        ScoreMode.NEW,
        "score.preset.new.metropolis",
        "score.preset.new.metropolis.desc",
        5.0,
        0.4,
        12.0,
        1.0,
        1.0,
        10.0
    );

    private static final LocalizationManager I18N = LocalizationManager.get();

    private final String id;
    private final ScoreMode mode;
    private final String labelKey;
    private final String descriptionKey;
    private final double populationWeight;
    private final double happinessWeight;
    private final double buildingWeight;
    private final double levelWeight;
    private final double claimsWeight;
    private final double finalScale;

    ScorePreset(
        String id,
        ScoreMode mode,
        String labelKey,
        String descriptionKey,
        double populationWeight,
        double happinessWeight,
        double buildingWeight,
        double levelWeight,
        double claimsWeight,
        double finalScale
    ) {
        this.id = id;
        this.mode = mode;
        this.labelKey = labelKey;
        this.descriptionKey = descriptionKey;
        this.populationWeight = populationWeight;
        this.happinessWeight = happinessWeight;
        this.buildingWeight = buildingWeight;
        this.levelWeight = levelWeight;
        this.claimsWeight = claimsWeight;
        this.finalScale = finalScale;
    }

    public String getId() {
        return id;
    }

    public ScoreMode getMode() {
        return mode;
    }

    public String getDisplayName() {
        return I18N.t(labelKey);
    }

    public String getDescription() {
        return I18N.t(descriptionKey);
    }

    public double getPopulationWeight() {
        return populationWeight;
    }

    public double getHappinessWeight() {
        return happinessWeight;
    }

    public double getBuildingWeight() {
        return buildingWeight;
    }

    public double getLevelWeight() {
        return levelWeight;
    }

    public double getClaimsWeight() {
        return claimsWeight;
    }

    public double getFinalScale() {
        return finalScale;
    }

    public boolean isOldMode() {
        return mode.isOld();
    }

    public boolean isNewMode() {
        return mode.isNew();
    }

    public ScoreBreakdown calculate(ColonyData colony) {
        double population = Math.max(0, colony.getPopulation());
        double buildings = Math.max(0, colony.getBuildingCount());
        double averageLevel = Math.max(0.0, colony.getAverageBuildingLevel());
        double claims = Math.max(0, colony.getClaimedChunks());
        double happiness = Math.max(0.0, colony.getOverallHappiness());

        double populationComponent = population * populationWeight;
        double happinessComponent = isOldMode()
            ? happiness * happinessWeight
            : population * happiness * happinessWeight;
        double buildingComponent = buildings * buildingWeight;
        double levelComponent = isOldMode()
            ? averageLevel * levelWeight
            : averageLevel * buildings * levelWeight;
        double claimsComponent = claims * claimsWeight;
        double total = (populationComponent + happinessComponent + buildingComponent + levelComponent + claimsComponent) * finalScale;

        return new ScoreBreakdown(
            populationComponent,
            happinessComponent,
            buildingComponent,
            levelComponent,
            claimsComponent,
            total,
            finalScale
        );
    }

    public String formatBreakdown(ScoreBreakdown breakdown) {
        if (isOldMode()) {
            return I18N.t(
                "score.breakdown.old",
                breakdown.populationComponent(),
                populationWeight,
                breakdown.buildingComponent(),
                buildingWeight,
                breakdown.levelComponent(),
                levelWeight,
                breakdown.claimsComponent(),
                claimsWeight,
                breakdown.happinessComponent(),
                happinessWeight
            );
        }

        return I18N.t(
            "score.breakdown.new",
            breakdown.populationComponent(),
            populationWeight,
            breakdown.happinessComponent(),
            happinessWeight,
            breakdown.buildingComponent(),
            buildingWeight,
            breakdown.levelComponent(),
            levelWeight,
            breakdown.claimsComponent(),
            claimsWeight,
            finalScale
        );
    }

    public static ScorePreset fromId(String value) {
        if (value == null) {
            return OLD_ORIGINAL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ScorePreset preset : values()) {
            if (preset.id.equals(normalized)) {
                return preset;
            }
        }
        return OLD_ORIGINAL;
    }

    public static List<String> idsForMode(ScoreMode mode) {
        return Arrays.stream(values())
            .filter(preset -> preset.mode == mode)
            .map(ScorePreset::getId)
            .collect(Collectors.toList());
    }

    public static List<String> allIds() {
        return Arrays.stream(values())
            .map(ScorePreset::getId)
            .collect(Collectors.toList());
    }

    public static ScorePreset defaultForMode(ScoreMode mode) {
        Objects.requireNonNull(mode, "mode");
        return mode.isOld() ? OLD_ORIGINAL : NEW_DEVELOPMENT;
    }
}
