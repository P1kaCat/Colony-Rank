package com.colonyrank.mod.data;

import java.util.Locale;

/**
 * Holds all statistics for a single colony.
 * Populated by ColonyDataCollector via MineColoniesAPIHelper.
 * Score is calculated by ColonyScoreCalculator.
 */
public class ColonyData {
    private final int colonyId;
    private final String name;
    private int population;
    private int buildingCount;
    private double averageBuildingLevel;
    private int claimedChunks;
    private double overallHappiness;
    private int colonyAgeDays;
    private int colonyAgeHours;
    private double score;

    public ColonyData(int colonyId, String name) {
        this.colonyId = colonyId;
        this.name = name;
    }

    public int getColonyId() {
        return colonyId;
    }

    public String getName() {
        return name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public int getBuildingCount() {
        return buildingCount;
    }

    public void setBuildingCount(int buildingCount) {
        this.buildingCount = buildingCount;
    }

    public double getAverageBuildingLevel() {
        return averageBuildingLevel;
    }

    public void setAverageBuildingLevel(double averageBuildingLevel) {
        this.averageBuildingLevel = averageBuildingLevel;
    }

    public int getClaimedChunks() {
        return claimedChunks;
    }

    public void setClaimedChunks(int claimedChunks) {
        this.claimedChunks = claimedChunks;
    }

    public double getOverallHappiness() {
        return overallHappiness;
    }

    public void setOverallHappiness(double overallHappiness) {
        this.overallHappiness = overallHappiness;
    }

    public int getColonyAgeDays() {
        return colonyAgeDays;
    }

    public void setColonyAgeDays(int colonyAgeDays) {
        this.colonyAgeDays = colonyAgeDays;
    }

    public int getColonyAgeHours() {
        return colonyAgeHours;
    }

    public void setColonyAgeHours(int colonyAgeHours) {
        this.colonyAgeHours = colonyAgeHours;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Returns a human-readable age string.
     * Format: "Xd Yh" if days > 0, otherwise "Yh".
     */
    public String getColonyAgeDisplay() {
        if (colonyAgeDays > 0) {
            int remainingHours = colonyAgeHours % 24;
            if (remainingHours > 0) {
                return String.format(Locale.ROOT, "%dd %dh", colonyAgeDays, remainingHours);
            }
            return String.format(Locale.ROOT, "%dd", colonyAgeDays);
        }
        if (colonyAgeHours > 0) {
            return String.format(Locale.ROOT, "%dh", colonyAgeHours);
        }
        return "0h";
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
            "ColonyData{id=%d, name='%s', pop=%d, buildings=%d, avgLvl=%.2f, chunks=%d, happiness=%.2f, age=%s, score=%.0f}",
            colonyId, name, population, buildingCount, averageBuildingLevel, claimedChunks,
            overallHappiness, getColonyAgeDisplay(), score);
    }
}
