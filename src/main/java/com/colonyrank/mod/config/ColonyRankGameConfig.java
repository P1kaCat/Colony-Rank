package com.colonyrank.mod.config;

import com.colonyrank.mod.ColonyRankMod;
import com.colonyrank.mod.util.LocalizationManager;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.Locale;

public class ColonyRankGameConfig extends Config {
    public static final ResourceLocation CONFIG_ID = ResourceLocation.fromNamespaceAndPath(ColonyRankMod.MODID, "settings");
    public static final String CONFIG_SCREEN_SCOPE = ColonyRankMod.MODID;

    private static ColonyRankGameConfig INSTANCE;

    // Must be non-final for Fzzy Config reflection/serialization.
    public ValidatedString language = ValidatedString.fromValues("en", "fr");

    public ColonyRankGameConfig() {
        super(CONFIG_ID);
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            return;
        }
        // BOTH ensures the config exists client-side (screen/file) and server-side (synced).
        INSTANCE = ConfigApiJava.registerAndLoadConfig(ColonyRankGameConfig::new, RegisterType.BOTH);
    }

    @Override
    public void onSyncClient() {
        LocalizationManager.get().reload();
    }

    @Override
    public void onSyncServer() {
        LocalizationManager.get().reload();
    }

    @Override
    public void onUpdateClient() {
        LocalizationManager.get().reload();
    }

    @Override
    public void onUpdateServer(ServerUpdateContext context) {
        LocalizationManager.get().reload();
        super.onUpdateServer(context);
    }

    public static String getLanguageCode() {
        if (INSTANCE == null) {
            return null;
        }

        String raw = INSTANCE.language.get();
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "en" -> "en";
            case "fr" -> "fr";
            default -> "en";
        };
    }

    public static String getExpectedConfigPath() {
        return Path.of("config", ColonyRankMod.MODID, "settings.toml").toAbsolutePath().toString();
    }
}