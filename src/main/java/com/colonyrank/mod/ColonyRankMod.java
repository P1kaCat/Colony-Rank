package com.colonyrank.mod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.colonyrank.mod.command.CommandColonyAdmin;
import com.colonyrank.mod.command.CommandColonyRank;
import com.colonyrank.mod.command.CommandColonyScore;
import com.colonyrank.mod.config.ColonyRankGameConfig;
import com.colonyrank.mod.data.ColonyDataCollector;
import com.colonyrank.mod.data.ColonyScoreCalculator;

@Mod(ColonyRankMod.MODID)
public class ColonyRankMod {
    public static final String MODID = "colonyrank";
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");

    private static ColonyDataCollector dataCollector;
    private static ColonyScoreCalculator scoreCalculator;
    private static long lastUpdateTick = 0;
    private static final long UPDATE_INTERVAL_TICKS = 6000;
    private static long lastDailyCheckTick = 0;
    private static final long DAILY_CHECK_INTERVAL_TICKS = 200;

    public ColonyRankMod() {
        LOGGER.info("Initialisation du mod ColonyRank 2.0.0...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ColonyRankGameConfig.init();
        LOGGER.info("Config Fzzy initialisee (langue={}, mode={}, preset={}, fichier={})",
            ColonyRankGameConfig.getLanguageCode(),
            ColonyRankGameConfig.getScoringMode(),
            ColonyRankGameConfig.getNewPreset(),
            ColonyRankGameConfig.getExpectedConfigPath());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientConfigScreen();
        }

        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addListener(this::onRegisterCommands);
        forgeEventBus.addListener(this::onServerStarting);
        forgeEventBus.addListener(this::onServerStopping);
        forgeEventBus.addListener(this::onServerTick);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initialisation commune terminee");
    }

    private static void registerClientConfigScreen() {
        try {
            Class.forName("com.colonyrank.mod.client.ColonyRankClientConfigScreen")
                .getMethod("register")
                .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible d'enregistrer l'ecran de config ColonyRank", e);
        }
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Demarrage du serveur - initialisation des collecteurs ColonyRank");

        dataCollector = new ColonyDataCollector();
        scoreCalculator = new ColonyScoreCalculator();
    }

    /**
     * Commands must be registered during this event so dedicated-server clients receive
     * the command tree when they join. Registering them during ServerStarting is too late.
     */
    private void onRegisterCommands(final RegisterCommandsEvent event) {
        CommandColonyRank.register(event.getDispatcher());
        CommandColonyScore.register(event.getDispatcher());
        CommandColonyAdmin.register(event.getDispatcher());

        LOGGER.info("Commandes ColonyRank enregistrees");
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        LOGGER.info("Arret du serveur - nettoyage des ressources ColonyRank");
        if (dataCollector != null) {
            dataCollector.shutdown();
        }
    }

    private void onServerTick(final TickEvent.ServerTickEvent event) {
        // Only run on END phase to avoid running twice per tick
        if (event.phase != TickEvent.Phase.END) return;

        long tick = event.getServer().getTickCount();
        if (tick - lastUpdateTick >= UPDATE_INTERVAL_TICKS) {
            lastUpdateTick = tick;

            if (dataCollector != null) {
                dataCollector.updateAllColonies(event.getServer());
                if (scoreCalculator != null) {
                    scoreCalculator.recalculateAllScores(dataCollector);
                }
                dataCollector.saveColoniesToJson();
            }
        }

        if (dataCollector != null && tick - lastDailyCheckTick >= DAILY_CHECK_INTERVAL_TICKS) {
            lastDailyCheckTick = tick;
            dataCollector.publishDailyIfDue(event.getServer());
        }
    }

    public static ColonyDataCollector getDataCollector() {
        return dataCollector;
    }

    public static ColonyScoreCalculator getScoreCalculator() {
        return scoreCalculator;
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
