package com.colonyrank.mod.util;

import com.colonyrank.mod.config.ColonyRankGameConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

public final class LocalizationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");
    private static final String CONFIG_PATH = "config/colonyrank.properties";

    private enum Language {
        FR,
        EN
    }

    private static final LocalizationManager INSTANCE = new LocalizationManager();

    private volatile Language currentLanguage = Language.EN;

    private LocalizationManager() {
        ensureDefaultConfig();
        reload();
    }

    public static LocalizationManager get() {
        return INSTANCE;
    }

    public synchronized void reload() {
        String fzzyLanguage = ColonyRankGameConfig.getLanguageCode();
        if (fzzyLanguage != null && !fzzyLanguage.isBlank()) {
            currentLanguage = "fr".equals(fzzyLanguage) ? Language.FR : Language.EN;
            return;
        }

        Properties properties = new Properties();
        Path configPath = Paths.get(CONFIG_PATH);

        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.warn("Impossible de lire la config de langue {}", configPath.toAbsolutePath(), e);
            }
        }

        String lang = properties.getProperty("language", "en").trim().toLowerCase(Locale.ROOT);
        currentLanguage = "en".equals(lang) ? Language.EN : Language.FR;
    }

    public String getLanguageCode() {
        return currentLanguage == Language.EN ? "en" : "fr";
    }

    public String getConfigPath() {
        return Paths.get(CONFIG_PATH).toAbsolutePath().toString();
    }

    public String t(String key, Object... args) {
        String template = getTemplate(key);
        if (args == null || args.length == 0) {
            return template;
        }
        return String.format(Locale.ROOT, template, args);
    }

    private String getTemplate(String key) {
        boolean en = currentLanguage == Language.EN;

        switch (key) {
            case "rank.collector_not_initialized":
                return en ? "ColonyRank collector is not initialized" : "Collecteur ColonyRank non initialise";
            case "rank.no_colonies":
                return en ? "No colonies found on this server" : "Aucune colonie trouvee sur ce serveur";
            case "rank.header":
                return en ? "========== COLONY RANKING ==========" : "========== CLASSEMENT COLONIES ==========";
            case "rank.line":
                return en
                    ? "#%d %s [Pop: %d | Buildings: %d | AvgLvl(5): %.2f | Age: %s | Chunks: %d | Happiness: %.1f | Score: %.0f]"
                    : "#%d %s [Pop: %d | Batiments: %d | Niv(5): %.2f | Anciennete: %s | Chunks: %d | Bonheur: %.1f | Score: %.0f]";
            case "rank.footer":
                return en ? "=======================================" : "=======================================";
            case "rank.total":
                return en ? "Total colonies: %d" : "Colonies totales: %d";

            case "score.collector_not_initialized":
                return en ? "ColonyRank collector is not initialized" : "Collecteur ColonyRank non initialise";
            case "score.colony_not_found_name":
                return en ? "Colony '%s' not found" : "Colonie '%s' introuvable";
            case "score.colony_not_found_id":
                return en ? "Colony ID %d not found" : "Colonie ID %d introuvable";
            case "score.list.header":
                return en ? "========== COLONY LIST ==========" : "========== LISTE COLONIES ==========";
            case "score.list.item":
                return en ? "ID %d - %s" : "ID %d - %s";
            case "score.list.footer":
                return en ? "================================" : "====================================";
            case "score.details.header":
                return en ? "========== COLONY DETAILS ==========" : "========== DETAILS COLONIE ==========";
            case "score.details.colony":
                return en ? "Colony: %s" : "Colonie: %s";
            case "score.details.population":
                return en ? "Population: %d citizens" : "Population: %d habitants";
            case "score.details.buildings":
                return en ? "Buildings: %d" : "Batiments: %d";
            case "score.details.avg_level":
                return en ? "Average building level (out of 5): %.2f" : "Niveau moyen des batiments (sur 5): %.2f";
            case "score.details.age":
                return en ? "Age: %s" : "Anciennete: %s";
            case "score.details.chunks":
                return en ? "Claimed chunks: %d" : "Chunks claims: %d";
            case "score.details.happiness":
                return en ? "Overall happiness: %.2f" : "Bonheur global: %.2f";
            case "score.details.breakdown":
                return en ? "Score breakdown: %s" : "Detail du score: %s";
            case "score.details.total":
                return en ? "TOTAL SCORE: %.0f" : "SCORE TOTAL: %.0f";
            case "score.details.footer":
                return en ? "=====================================" : "=====================================";

            case "score.breakdown":
                return en
                    ? "Population: %.1f | Buildings: %.1f | Levels: %.1f | Chunks: %.1f | Happiness: %.1f"
                    : "Population: %.1f | Batiments: %.1f | Niveaux: %.1f | Chunks: %.1f | Bonheur: %.1f";

            case "admin.collector_not_initialized":
                return en ? "Collector is not initialized" : "Collecteur non initialise";
            case "admin.sendleaderboard.start":
                return en ? "Sending leaderboard to Discord..." : "Envoi du classement sur Discord...";
            case "admin.sendleaderboard.done":
                return en ? "Leaderboard sent (if webhook is configured)." : "Classement envoye (si webhook configure).";
            case "admin.sendleaderboard.error":
                return en ? "Error while sending: %s" : "Erreur pendant l envoi: %s";
            case "admin.discordstatus.header":
                return en ? "========== DISCORD STATUS ==========" : "========== DISCORD STATUS ==========";
            case "admin.discordstatus.webhook":
                return en ? "Webhook: %s" : "Webhook: %s";
            case "admin.discordstatus.webhook_yes":
                return en ? "YES" : "OUI";
            case "admin.discordstatus.webhook_no":
                return en ? "NO" : "NON";
            case "admin.discordstatus.config":
                return en ? "Config: %s" : "Config: %s";
            case "admin.discordstatus.state":
                return en ? "State: %s" : "Etat: %s";
            case "admin.discordstatus.language":
                return en ? "Language: %s" : "Langue: %s";
            case "admin.discordstatus.footer":
                return en ? "===================================" : "===================================";
            case "admin.senddaily.start":
                return en ? "Forced daily Discord send..." : "Envoi quotidien force sur Discord...";
            case "admin.senddaily.done":
                return en ? "Daily send attempted (check logs)." : "Envoi quotidien tente (voir logs).";
            case "admin.reload.start":
                return en ? "Reloading colony data..." : "Rechargement des donnees des colonies...";
            case "admin.reload.done":
                return en ? "Reload complete! %d colonies found." : "Recharge terminee ! %d colonies trouvees.";
            case "admin.reload.error":
                return en ? "Error during reload: %s" : "Erreur pendant le rechargement: %s";
            case "admin.refresh.empty_cache":
                return en ? "Empty cache - reloading colonies..." : "Cache vide - rechargement des colonies...";
            case "admin.refresh.start":
                return en ? "Refreshing scores..." : "Rafraichissement des scores...";
            case "admin.refresh.done":
                return en ? "Refresh complete!" : "Rafraichissement termine !";
            case "admin.refresh.error":
                return en ? "Error during refresh: %s" : "Erreur pendant le rafraichissement: %s";
            case "admin.clear.start":
                return en ? "Clearing colony cache..." : "Vidage du cache des colonies...";
            case "admin.clear.done":
                return en ? "Cache cleared! (%d colonies removed)" : "Cache vide ! (%d colonies supprimees)";
            case "admin.clear.error":
                return en ? "Error during clear: %s" : "Erreur pendant le vidage: %s";
            case "admin.status.header":
                return en ? "========== COLONYRANK STATUS ==========" : "========== STATUT COLONYRANK ==========";
            case "admin.status.collector_running":
                return en ? "Collector: RUNNING" : "Collecteur: EN COURS";
            case "admin.status.collector_not_initialized":
                return en ? "Collector: NOT INITIALIZED" : "Collecteur: NON INITIALISE";
            case "admin.status.colonies_cache":
                return en ? "Colonies in cache: %d" : "Colonies en cache: %d";
            case "admin.status.json_file":
                return en ? "JSON file: data/colonyrank/colonies.json" : "Fichier JSON: data/colonyrank/colonies.json";
            case "admin.status.language":
                return en ? "Language: %s" : "Langue: %s";
            case "admin.status.footer":
                return en ? "=====================================" : "=====================================";
            case "admin.export.start":
                return en ? "Exporting colonies to JSON..." : "Export des colonies en JSON...";
            case "admin.export.done":
                return en ? "Export complete! File: data/colonyrank/colonies.json" : "Export termine ! Fichier: data/colonyrank/colonies.json";
            case "admin.export.error":
                return en ? "Error during export: %s" : "Erreur pendant l export: %s";
            case "admin.help.header":
                return en ? "========== COLONYADMIN HELP ==========" : "========== AIDE COLONYADMIN ==========";
            case "admin.help.reload":
                return en ? "/colonyadmin reload - Reload all colonies" : "/colonyadmin reload - Recharger toutes les colonies";
            case "admin.help.refresh":
                return en ? "/colonyadmin refresh - Recalculate scores and save" : "/colonyadmin refresh - Recalculer les scores et sauvegarder";
            case "admin.help.clear":
                return en ? "/colonyadmin clear - Clear cache" : "/colonyadmin clear - Vider le cache";
            case "admin.help.status":
                return en ? "/colonyadmin status - Show status" : "/colonyadmin status - Afficher le statut";
            case "admin.help.export":
                return en ? "/colonyadmin export - Export to JSON" : "/colonyadmin export - Exporter en JSON";
            case "admin.help.discordstatus":
                return en ? "/colonyadmin discordstatus - Discord config status" : "/colonyadmin discordstatus - Etat config Discord";
            case "admin.help.senddaily":
                return en ? "/colonyadmin senddaily - Force daily send" : "/colonyadmin senddaily - Forcer l envoi quotidien";
            case "admin.help.footer":
                return en ? "=====================================" : "=====================================";

            case "discord.title":
                return en ? "MineColonies Leaderboard" : "Classement MineColonies";
            case "discord.entry.header":
                return en ? "#%d %s - Score: %d" : "#%d %s - Score: %d";
            case "discord.entry.details":
                return en
                    ? "Population: %d | Buildings: %d | Avg level (out of 5): %.2f | Age: %s"
                    : "Population: %d | Batiments: %d | Niveau moyen (sur 5): %.2f | Anciennete: %s";
            case "discord.none":
                return en ? "No colonies found." : "Aucune colonie trouvee.";
            case "discord.footer":
                return en ? "Last update: %s | Total colonies: %d" : "Derniere maj: %s | Colonies totales: %d";

            default:
                return key;
        }
    }

    private void ensureDefaultConfig() {
        Path configPath = Paths.get(CONFIG_PATH);
        if (Files.exists(configPath)) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            String template = "# ColonyRank language settings\n"
                + "# language: fr or en\n"
                + "language=en\n";
            Files.writeString(configPath, template);
            LOGGER.info("Fichier de langue cree: {}", configPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.warn("Impossible de creer la config de langue {}", configPath.toAbsolutePath(), e);
        }
    }
}