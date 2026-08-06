package com.colonyrank.mod.config;

import com.colonyrank.mod.ColonyRankMod;
import com.colonyrank.mod.util.LocalizationManager;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

public class ColonyRankGameConfig extends Config {
    public static final ResourceLocation CONFIG_ID = ResourceLocation.fromNamespaceAndPath(ColonyRankMod.MODID, "settings");
    public static final String CONFIG_SCREEN_SCOPE = ColonyRankMod.MODID;

    private static final int MULTIPLIER_5 = 5;
    private static final int MULTIPLIER_10 = 10;
    private static final int MULTIPLIER_100 = 100;
    private static final int[] MULTIPLIER_VALUES = {MULTIPLIER_5, MULTIPLIER_10, MULTIPLIER_100};
    private static final int[] MULTIPLIER_COUNTS = {2, 2, 1};

    private static final BiFunction<Integer, String, MutableComponent> MULTIPLIER_LABEL_PROVIDER =
        (value, key) -> Component.literal("x" + value);
    private static final BiFunction<Integer, String, Component> MULTIPLIER_DESCRIPTION_PROVIDER =
        (value, key) -> Component.literal("Multiplier x" + value);

    private static ColonyRankGameConfig INSTANCE;

    // --- Scoring mode (OLD or NEW) ---
    public ValidatedString scoringMode = ValidatedString.fromValues("new", "old", "new");

    // --- NEW mode preset (includes "custom" for user-defined coefficients) ---
    public ValidatedString newPreset = ValidatedString.fromValues("developpement",
        "developpement", "population", "expansion", "gestion", "metropole", "custom");

    // --- Custom preset coefficients (used when newPreset = "custom") ---
    // NEW mode formula per component:
    //   Population = PNJ × popCoef
    //   Bonheur    = PNJ × Bonheur × happinessCoef
    //   Bâtiments  = Nombre de bâtiments × buildingCoef
    //   Niveau     = Niveau moyen × Nombre de bâtiments × levelCoef
    //   Claims     = Claims × claimsCoef
    //   Final: (Pop + Bonheur + Bât + Niveau + Claims) × 5
    public ValidatedDouble customPopCoef = new ValidatedDouble(5.0);
    public ValidatedDouble customHappinessCoef = new ValidatedDouble(0.4);
    public ValidatedDouble customBuildingCoef = new ValidatedDouble(5.0);
    public ValidatedDouble customLevelCoef = new ValidatedDouble(2.5);
    public ValidatedDouble customClaimsCoef = new ValidatedDouble(0.5);

    // --- OLD mode multiplier fields (with quota: 2x x5, 2x x10, 1x x100) ---
    public ValidatedChoice<Integer> populationMultiplier = createMultiplierChoice(MULTIPLIER_5);
    public ValidatedChoice<Integer> buildingMultiplier = createMultiplierChoice(MULTIPLIER_10);
    public ValidatedChoice<Integer> averageBuildingLevelMultiplier = createMultiplierChoice(MULTIPLIER_100);
    public ValidatedChoice<Integer> claimedChunksMultiplier = createMultiplierChoice(MULTIPLIER_10);
    public ValidatedChoice<Integer> overallHappinessMultiplier = createMultiplierChoice(MULTIPLIER_5);

    // --- Language ---
    public ValidatedString language = ValidatedString.fromValues("en", "fr");

    public ColonyRankGameConfig() {
        super(CONFIG_ID);
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            return;
        }
        INSTANCE = ConfigApiJava.registerAndLoadConfig(ColonyRankGameConfig::new, RegisterType.BOTH);
        INSTANCE.normalizeMultiplierSettings();
    }

    @Override
    public void onSyncClient() {
        normalizeMultiplierSettings();
        LocalizationManager.get().reload();
    }

    @Override
    public void onSyncServer() {
        normalizeMultiplierSettings();
        LocalizationManager.get().reload();
    }

    @Override
    public void onUpdateClient() {
        normalizeMultiplierSettings();
        LocalizationManager.get().reload();
    }

    @Override
    public void onUpdateServer(ServerUpdateContext context) {
        normalizeMultiplierSettings();
        LocalizationManager.get().reload();

        if (ColonyRankMod.getDataCollector() != null && ColonyRankMod.getScoreCalculator() != null) {
            ColonyRankMod.getScoreCalculator().recalculateAllScores(ColonyRankMod.getDataCollector());
            ColonyRankMod.getDataCollector().saveColoniesToJson();
        }

        super.onUpdateServer(context);
    }

    // --- Scoring mode ---
    public static String getScoringMode() {
        if (INSTANCE == null) return "new";
        String raw = INSTANCE.scoringMode.get();
        if (raw == null) return "new";
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "old".equals(normalized) ? "old" : "new";
    }

    // --- NEW preset ---
    public static String getNewPreset() {
        if (INSTANCE == null) return "developpement";
        String raw = INSTANCE.newPreset.get();
        if (raw == null) return "developpement";
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "population" -> "population";
            case "expansion" -> "expansion";
            case "gestion" -> "gestion";
            case "metropole" -> "metropole";
            case "custom" -> "custom";
            default -> "developpement";
        };
    }

    public static void setScoringMode(String mode) {
        if (INSTANCE == null) return;
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!"old".equals(normalized) && !"new".equals(normalized)) return;
        INSTANCE.scoringMode.trySetQuiet(normalized);
        INSTANCE.save();
    }

    public static void setNewPreset(String preset) {
        if (INSTANCE == null) return;
        String normalized = preset.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("developpement") && !normalized.equals("population") &&
            !normalized.equals("expansion") && !normalized.equals("gestion") &&
            !normalized.equals("metropole") && !normalized.equals("custom")) return;
        INSTANCE.newPreset.trySetQuiet(normalized);
        INSTANCE.save();
    }

    // --- Custom preset coefficients ---
    public static double getCustomPopCoef() {
        if (INSTANCE == null) return 5.0;
        return INSTANCE.customPopCoef.get();
    }

    public static double getCustomHappinessCoef() {
        if (INSTANCE == null) return 0.4;
        return INSTANCE.customHappinessCoef.get();
    }

    public static double getCustomBuildingCoef() {
        if (INSTANCE == null) return 5.0;
        return INSTANCE.customBuildingCoef.get();
    }

    public static double getCustomLevelCoef() {
        if (INSTANCE == null) return 2.5;
        return INSTANCE.customLevelCoef.get();
    }

    public static double getCustomClaimsCoef() {
        if (INSTANCE == null) return 0.5;
        return INSTANCE.customClaimsCoef.get();
    }

    // --- Language ---
    public static String getLanguageCode() {
        if (INSTANCE == null) return null;
        String raw = INSTANCE.language.get();
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "en" -> "en";
            case "fr" -> "fr";
            default -> "en";
        };
    }

    // --- OLD mode multipliers ---
    public static int getPopulationMultiplier() {
        return getMultiplierValue(INSTANCE == null ? null : INSTANCE.populationMultiplier, MULTIPLIER_5);
    }

    public static int getBuildingMultiplier() {
        return getMultiplierValue(INSTANCE == null ? null : INSTANCE.buildingMultiplier, MULTIPLIER_10);
    }

    public static int getAverageBuildingLevelMultiplier() {
        return getMultiplierValue(INSTANCE == null ? null : INSTANCE.averageBuildingLevelMultiplier, MULTIPLIER_100);
    }

    public static int getClaimedChunksMultiplier() {
        return getMultiplierValue(INSTANCE == null ? null : INSTANCE.claimedChunksMultiplier, MULTIPLIER_10);
    }

    public static int getOverallHappinessMultiplier() {
        return getMultiplierValue(INSTANCE == null ? null : INSTANCE.overallHappinessMultiplier, MULTIPLIER_5);
    }

    public static String getExpectedConfigPath() {
        return Path.of("config", ColonyRankMod.MODID, "settings.toml").toAbsolutePath().toString();
    }

    private static ValidatedChoice<Integer> createMultiplierChoice(int defaultValue) {
        return new ValidatedChoice<>(
            defaultValue,
            List.of(MULTIPLIER_5, MULTIPLIER_10, MULTIPLIER_100),
            new ValidatedInt(),
            MULTIPLIER_LABEL_PROVIDER,
            MULTIPLIER_DESCRIPTION_PROVIDER,
            ValidatedChoice.WidgetType.CYCLING
        );
    }

    private static int getMultiplierValue(ValidatedChoice<Integer> choice, int fallback) {
        if (choice == null) return fallback;
        Integer value = choice.get();
        return value != null ? value : fallback;
    }

    private void normalizeMultiplierSettings() {
        List<ValidatedChoice<Integer>> fields = List.of(
            populationMultiplier,
            buildingMultiplier,
            averageBuildingLevelMultiplier,
            claimedChunksMultiplier,
            overallHappinessMultiplier
        );

        int[] remaining = MULTIPLIER_COUNTS.clone();
        Integer[] resolved = new Integer[fields.size()];

        for (int i = 0; i < fields.size(); i++) {
            Integer requested = fields.get(i).get();
            int multiplierIndex = indexOfMultiplier(requested);
            if (multiplierIndex >= 0 && remaining[multiplierIndex] > 0) {
                resolved[i] = requested;
                remaining[multiplierIndex]--;
            }
        }

        for (int i = 0; i < fields.size(); i++) {
            if (resolved[i] != null) continue;
            for (int j = 0; j < MULTIPLIER_VALUES.length; j++) {
                if (remaining[j] > 0) {
                    resolved[i] = MULTIPLIER_VALUES[j];
                    remaining[j]--;
                    break;
                }
            }
        }

        boolean changed = false;
        for (int i = 0; i < fields.size(); i++) {
            int newValue = resolved[i] != null ? resolved[i] : MULTIPLIER_VALUES[0];
            Integer current = fields.get(i).get();
            if (current == null || current.intValue() != newValue) {
                fields.get(i).trySetQuiet(newValue);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    private static int indexOfMultiplier(Integer value) {
        if (value == null) return -1;
        for (int i = 0; i < MULTIPLIER_VALUES.length; i++) {
            if (MULTIPLIER_VALUES[i] == value) return i;
        }
        return -1;
    }
}
