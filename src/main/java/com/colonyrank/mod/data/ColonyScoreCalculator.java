package com.colonyrank.mod.data;

import com.colonyrank.mod.config.ColonyRankGameConfig;
import com.colonyrank.mod.util.LocalizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

/**
 * Calculates colony scores based on the configured scoring mode and preset.
 *
 * Two modes:
 * - OLD: historical multiplier-based system (stat × multiplier, sum)
 * - NEW: cross-product system linking stats (PNJ × Bonheur, Niveau × Bâtiments)
 *
 * NEW mode formula:
 *   Score = (Population + Bonheur + Bâtiments + Niveau + Claims) × 5
 *
 * NEW presets define coefficients for each component.
 */
public class ColonyScoreCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");
    private static final LocalizationManager I18N = LocalizationManager.get();

    public static final String MODE_OLD = "old";
    public static final String MODE_NEW = "new";

    /**
     * NEW mode presets with their coefficients.
     * Order: popCoef, happinessCoef, buildingCoef, levelCoef, claimsCoef
     *
     * NEW mode formula per component:
     *   Population = PNJ × popCoef
     *   Bonheur    = PNJ × Bonheur × happinessCoef
     *   Bâtiments  = Nombre de bâtiments × buildingCoef
     *   Niveau     = Niveau moyen × Nombre de bâtiments × levelCoef
     *   Claims     = Claims × claimsCoef
     *
     * Final: (Pop + Bonheur + Bât + Niveau + Claims) × 5
     */
    public enum NewPreset {
        DEVELOPPEMENT(5, 0.4, 5, 2.5, 0.5),
        POPULATION(10, 0.6, 5, 1.5, 0.5),
        EXPANSION(5, 0.4, 8, 1.5, 2),
        GESTION(5, 1.0, 6, 1.5, 0.5),
        METROPOLE(5, 0.4, 12, 1.0, 1);

        public final double popCoef;
        public final double happinessCoef;
        public final double buildingCoef;
        public final double levelCoef;
        public final double claimsCoef;

        NewPreset(double popCoef, double happinessCoef, double buildingCoef, double levelCoef, double claimsCoef) {
            this.popCoef = popCoef;
            this.happinessCoef = happinessCoef;
            this.buildingCoef = buildingCoef;
            this.levelCoef = levelCoef;
            this.claimsCoef = claimsCoef;
        }

        public static NewPreset fromName(String name) {
            if (name == null) return DEVELOPPEMENT;
            return switch (name.trim().toLowerCase(Locale.ROOT)) {
                case "population" -> POPULATION;
                case "expansion" -> EXPANSION;
                case "gestion" -> GESTION;
                case "metropole" -> METROPOLE;
                default -> DEVELOPPEMENT;
            };
        }

        public String getDisplayName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public ColonyScoreCalculator() {
        LOGGER.info("ColonyScoreCalculator initialise");
    }

    /**
     * Recalculate scores for all colonies in the collector.
     */
    public void recalculateAllScores(ColonyDataCollector collector) {
        String mode = ColonyRankGameConfig.getScoringMode();
        if (MODE_NEW.equalsIgnoreCase(mode)) {
            calculateNewScores(collector);
        } else {
            calculateOldScores(collector);
        }
    }

    /**
     * OLD mode: sum of (stat × multiplier) for each stat.
     * Multipliers come from ColonyRankGameConfig (with quota: 2x x5, 2x x10, 1x x100).
     */
    private void calculateOldScores(ColonyDataCollector collector) {
        int popM = ColonyRankGameConfig.getPopulationMultiplier();
        int bonM = ColonyRankGameConfig.getOverallHappinessMultiplier();
        int batM = ColonyRankGameConfig.getBuildingMultiplier();
        int nivM = ColonyRankGameConfig.getAverageBuildingLevelMultiplier();
        int claimM = ColonyRankGameConfig.getClaimedChunksMultiplier();

        for (ColonyData colony : collector.getAllColonies().values()) {
            double pop = colony.getPopulation() * popM;
            double bon = colony.getOverallHappiness() * bonM;
            double bat = colony.getBuildingCount() * batM;
            double niv = colony.getAverageBuildingLevel() * nivM;
            double claims = colony.getClaimedChunks() * claimM;
            double score = pop + bon + bat + niv + claims;
            colony.setScore(score);
        }

        LOGGER.debug("Scores OLD recalculés pour {} colonies", collector.getColonyCount());
    }

    /**
     * NEW mode: cross-product scoring with preset coefficients.
     * Score = (Pop + Bonheur + Bât + Niveau + Claims) × 5
     */
    private void calculateNewScores(ColonyDataCollector collector) {
        NewPreset preset = NewPreset.fromName(ColonyRankGameConfig.getNewPreset());

        for (ColonyData colony : collector.getAllColonies().values()) {
            double pop = colony.getPopulation() * preset.popCoef;
            double bon = colony.getPopulation() * colony.getOverallHappiness() * preset.happinessCoef;
            double bat = colony.getBuildingCount() * preset.buildingCoef;
            double niv = colony.getAverageBuildingLevel() * colony.getBuildingCount() * preset.levelCoef;
            double claims = colony.getClaimedChunks() * preset.claimsCoef;
            double subtotal = pop + bon + bat + niv + claims;
            double score = subtotal * 5;
            colony.setScore(score);
        }

        LOGGER.debug("Scores NEW (preset={}) recalculés pour {} colonies", preset.getDisplayName(), collector.getColonyCount());
    }

    /**
     * Returns a human-readable breakdown of the score for a colony.
     * Format depends on the current scoring mode.
     */
    public String getScoreBreakdown(ColonyData colony) {
        String mode = ColonyRankGameConfig.getScoringMode();
        if (MODE_NEW.equalsIgnoreCase(mode)) {
            return getNewBreakdown(colony);
        } else {
            return getOldBreakdown(colony);
        }
    }

    private String getOldBreakdown(ColonyData colony) {
        int popM = ColonyRankGameConfig.getPopulationMultiplier();
        int bonM = ColonyRankGameConfig.getOverallHappinessMultiplier();
        int batM = ColonyRankGameConfig.getBuildingMultiplier();
        int nivM = ColonyRankGameConfig.getAverageBuildingLevelMultiplier();
        int claimM = ColonyRankGameConfig.getClaimedChunksMultiplier();

        double pop = colony.getPopulation() * popM;
        double bon = colony.getOverallHappiness() * bonM;
        double bat = colony.getBuildingCount() * batM;
        double niv = colony.getAverageBuildingLevel() * nivM;
        double claims = colony.getClaimedChunks() * claimM;

        return I18N.t("score.breakdown",
            pop, popM,
            bat, batM,
            niv, nivM,
            claims, claimM,
            bon, bonM);
    }

    private String getNewBreakdown(ColonyData colony) {
        NewPreset preset = NewPreset.fromName(ColonyRankGameConfig.getNewPreset());

        double pop = colony.getPopulation() * preset.popCoef;
        double bon = colony.getPopulation() * colony.getOverallHappiness() * preset.happinessCoef;
        double bat = colony.getBuildingCount() * preset.buildingCoef;
        double niv = colony.getAverageBuildingLevel() * colony.getBuildingCount() * preset.levelCoef;
        double claims = colony.getClaimedChunks() * preset.claimsCoef;
        double subtotal = pop + bon + bat + niv + claims;
        double total = subtotal * 5;

        return I18N.t("score.breakdown.new",
            pop, preset.popCoef,
            bon, colony.getPopulation(), colony.getOverallHappiness(), preset.happinessCoef,
            bat, preset.buildingCoef,
            niv, colony.getAverageBuildingLevel(), colony.getBuildingCount(), preset.levelCoef,
            claims, preset.claimsCoef,
            subtotal, total);
    }
}
