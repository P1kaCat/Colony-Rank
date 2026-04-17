package com.colonyrank.mod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.colonyrank.mod.command.CommandColonyAdmin;
import com.colonyrank.mod.command.CommandColonyRank;
import com.colonyrank.mod.command.CommandColonyScore;
import com.colonyrank.mod.config.ColonyRankGameConfig;
import com.colonyrank.mod.data.ColonyDataCollector;
import com.colonyrank.mod.data.ColonyScoreCalculator;

import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry;

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

    public ColonyRankMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initialisation du mod ColonyRank...");

        modEventBus.addListener(this::commonSetup);
        ColonyRankGameConfig.init();
        LOGGER.info("Config en jeu Fzzy initialisee (langue={}, fichier={})", ColonyRankGameConfig.getLanguageCode(), ColonyRankGameConfig.getExpectedConfigPath());

        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (IConfigScreenFactory) (container, modListScreen) -> {
                var configScreen = ClientConfigRegistry.INSTANCE.provideScreen$fzzy_config(ColonyRankGameConfig.CONFIG_SCREEN_SCOPE);
                return configScreen != null ? configScreen : modListScreen;
            }
        );

        IEventBus forgeEventBus = NeoForge.EVENT_BUS;
        forgeEventBus.addListener(this::onServerStarting);
        forgeEventBus.addListener(this::onServerStopping);
        forgeEventBus.addListener(this::onServerTick);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initialisation commune terminee");
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Demarrage du serveur - initialisation des collecteurs ColonyRank");

        dataCollector = new ColonyDataCollector();
        scoreCalculator = new ColonyScoreCalculator();

        var commandDispatcher = event.getServer().getCommands().getDispatcher();
        CommandColonyRank.register(commandDispatcher);
        CommandColonyScore.register(commandDispatcher);
        CommandColonyAdmin.register(commandDispatcher);

        LOGGER.info("Commandes ColonyRank enregistrees");
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        LOGGER.info("Arret du serveur - nettoyage des ressources ColonyRank");
        if (dataCollector != null) {
            dataCollector.shutdown();
        }
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        long tick = event.getServer().getTickCount();
        if (tick - lastUpdateTick >= UPDATE_INTERVAL_TICKS) {
            lastUpdateTick = tick;

            if (dataCollector != null) {
                dataCollector.updateAllColonies(event.getServer());
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