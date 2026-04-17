package com.colonyrank.mod.util;

import com.colonyrank.mod.data.ColonyData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhookPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");
    private static final LocalizationManager I18N = LocalizationManager.get();
    private static final String CONFIG_PATH = "config/colonyrank-discord.properties";
    private static final String MESSAGE_ID_PATH = "data/colonyrank/discord_message_id.txt";
    private static final String LAST_SENT_PATH = "data/colonyrank/discord_last_sent.txt";
    private static final int MAX_COLONIES = 10;
    private static final long MIN_PUBLISH_INTERVAL_MS = 5000L;
    private static final long DUPLICATE_WINDOW_MS = 15000L;
    private static final LocalTime DEFAULT_DAILY_TIME = LocalTime.of(20, 0);

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String webhookUrl;
    private final boolean dailyMode;
    private final LocalTime dailyTime;
    private final ZoneId zoneId;
    private volatile long lastPublishMillis = 0L;
    private volatile String lastPayloadHash = null;
    private volatile long lastPayloadMillis = 0L;

    public DiscordWebhookPublisher() {
        ensureDefaultConfig();
        Properties properties = loadConfigProperties();
        this.webhookUrl = resolveWebhookUrl(properties);
        this.dailyMode = isDailyMode(properties);
        this.dailyTime = readDailyTime(properties);
        this.zoneId = readZoneId(properties);
        Path configPath = Paths.get(CONFIG_PATH).toAbsolutePath();
        LOGGER.info("Config Discord chargee: mode={}, dailyTime={}, timezone={}, path={}", dailyMode ? "daily" : "update", dailyTime, zoneId, configPath);
        if (!isWebhookConfigured()) {
            LOGGER.warn("Webhook Discord non configure. Definir COLONYRANK_DISCORD_WEBHOOK ou config/colonyrank-discord.properties.");
        } else {
            LOGGER.info("Webhook Discord configure.");
        }
    }

    public void publishRankingAsync(List<ColonyData> colonies) {
        if (!canPublish(false)) {
            return;
        }
        CompletableFuture.runAsync(() -> publishRanking(colonies, false, dailyMode));
    }

    public void publishRankingNowAsync(List<ColonyData> colonies) {
        if (!canPublish(true)) {
            return;
        }
        CompletableFuture.runAsync(() -> publishRanking(colonies, true, false));
    }

    public void publishDailyNowAsync(List<ColonyData> colonies) {
        if (!canPublish(true)) {
            return;
        }
        CompletableFuture.runAsync(() -> publishRanking(colonies, true, true));
    }

    public boolean isDailyDue() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }
        if (!dailyMode) {
            return false;
        }
        LocalDate today = LocalDate.now(zoneId);
        LocalDate lastSent = readLastSentDate();
        if (today.equals(lastSent)) {
            return false;
        }
        LocalTime nowTime = LocalTime.now(zoneId);
        return !nowTime.isBefore(dailyTime);
    }

    public boolean shouldPublishOnSave() {
        return webhookUrl != null && !webhookUrl.isBlank() && !dailyMode;
    }

    public String getDailyDebugSummary() {
        LocalDate today = LocalDate.now(zoneId);
        LocalTime nowTime = LocalTime.now(zoneId);
        LocalDate lastSent = readLastSentDate();
        boolean due = isDailyDue();
        return String.format(
            Locale.ROOT,
            "dailyMode=%s, now=%s, dailyTime=%s, timezone=%s, lastSent=%s, today=%s, due=%s",
            dailyMode,
            nowTime,
            dailyTime,
            zoneId,
            lastSent,
            today,
            due
        );
    }

    public boolean isWebhookConfigured() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    public String getConfigPath() {
        return Paths.get(CONFIG_PATH).toAbsolutePath().toString();
    }

    public String getDailyStatusLine() {
        return String.format(
            Locale.ROOT,
            "mode=%s, dailyTime=%s, timezone=%s, now=%s, lastSent=%s, due=%s",
            dailyMode ? "daily" : "update",
            dailyTime,
            zoneId,
            LocalTime.now(zoneId),
            readLastSentDate(),
            isDailyDue()
        );
    }

    private void publishRanking(List<ColonyData> colonies, boolean force, boolean markDaily) {
        try {
            String payload = buildPayload(colonies);
            if (!force && isDuplicatePayload(payload)) {
                return;
            }
            String messageId = readMessageId();
            LOGGER.info("Envoi Discord (force={}, daily={}) - messageId={}", force, markDaily, messageId != null ? "oui" : "non");

            if (messageId != null) {
                int status = sendPatch(messageId, payload);
                if (status == 404) {
                    deleteMessageId();
                    int postStatus = sendPost(payload);
                    if (postStatus >= 200 && postStatus < 300) {
                        LOGGER.debug("Message Discord cree");
                        updateLastSentDateIfNeeded(markDaily);
                    }
                } else if (status >= 200 && status < 300) {
                    updateLastSentDateIfNeeded(markDaily);
                }
                return;
            }

            int postStatus = sendPost(payload);
            if (postStatus >= 200 && postStatus < 300) {
                LOGGER.debug("Message Discord cree");
                updateLastSentDateIfNeeded(markDaily);
            }
        } catch (Exception e) {
            LOGGER.warn("Echec envoi Discord", e);
        }
    }

    private synchronized boolean isDuplicatePayload(String payload) {
        long now = System.currentTimeMillis();
        String hash = Integer.toHexString(payload.hashCode());
        if (hash.equals(lastPayloadHash) && (now - lastPayloadMillis) < DUPLICATE_WINDOW_MS) {
            return true;
        }
        lastPayloadHash = hash;
        lastPayloadMillis = now;
        return false;
    }

    private int sendPost(String payload) throws IOException, InterruptedException {
        URI uri = URI.create(webhookUrl + "?wait=true");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            storeMessageIdFromResponse(response.body());
            LOGGER.info("Webhook Discord POST OK (statut {})", response.statusCode());
        } else {
            LOGGER.warn("Webhook Discord POST en echec (statut {}). Reponse: {}", response.statusCode(), response.body());
        }
        return response.statusCode();
    }

    private int sendPatch(String messageId, String payload) throws IOException, InterruptedException {
        URI uri = URI.create(webhookUrl + "/messages/" + messageId);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            LOGGER.info("Webhook Discord PATCH OK (statut {})", response.statusCode());
            return response.statusCode();
        }
        LOGGER.warn("Webhook Discord PATCH en echec (statut {}). Reponse: {}", response.statusCode(), response.body());
        return response.statusCode();
    }

    private String buildPayload(List<ColonyData> colonies) {
        I18N.reload();
        StringBuilder description = new StringBuilder();
        int limit = Math.min(colonies.size(), MAX_COLONIES);
        for (int i = 0; i < limit; i++) {
            ColonyData colony = colonies.get(i);
            description.append(I18N.t("discord.entry.header", i + 1, colony.getName(), Math.round(colony.getScore())))
                .append("\n");
            description.append(I18N.t(
                "discord.entry.details",
                colony.getPopulation(),
                colony.getBuildingCount(),
                colony.getAverageBuildingLevel(),
                colony.getColonyAgeDisplay()
            )).append("\n\n");
        }

        if (description.length() == 0) {
            description.append(I18N.t("discord.none"));
        }

        Instant now = Instant.now();

        JsonObject embed = new JsonObject();
        embed.addProperty("title", I18N.t("discord.title"));
        embed.addProperty("description", description.toString());
        embed.addProperty("color", 0xFFD700);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", I18N.t("discord.footer", now, colonies.size()));
        embed.add("footer", footer);
        embed.addProperty("timestamp", now.toString());

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        JsonObject payload = new JsonObject();
        payload.add("embeds", embeds);

        return gson.toJson(payload);
    }

    private String resolveWebhookUrl(Properties properties) {
        String env = System.getenv("COLONYRANK_DISCORD_WEBHOOK");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = System.getProperty("colonyrank.discord.webhook");
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }

        String fileValue = properties.getProperty("webhookUrl");
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }
        return null;
    }

    private void ensureDefaultConfig() {
        Path configPath = Paths.get(CONFIG_PATH);
        if (Files.exists(configPath)) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            String template = "# Integration Discord ColonyRank\n"
                + "# Remplace l URL ci-dessous par ton webhook Discord\n"
                + "webhookUrl=https://discord.com/api/webhooks/PASTE_WEBHOOK_HERE\n"
                + "\n"
                + "# mode: 'update' (a chaque sauvegarde) ou 'daily'\n"
                + "mode=daily\n"
                + "# heure quotidienne au format 24h HH:mm\n"
                + "dailyTime=20:00\n"
                + "# fuseau horaire (vide = fuseau du serveur)\n"
                + "timezone=\n";
            Files.writeString(configPath, template);
            LOGGER.info("Fichier de config Discord cree dans {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.warn("Impossible de creer le fichier de config Discord dans {}", CONFIG_PATH, e);
        }
    }

    private Properties loadConfigProperties() {
        Properties properties = new Properties();
        Path configPath = Paths.get(CONFIG_PATH);
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.warn("Impossible de lire {}", CONFIG_PATH, e);
            }
        }
        return properties;
    }

    private boolean isDailyMode(Properties properties) {
        String mode = properties.getProperty("mode", "update").trim().toLowerCase(Locale.ROOT);
        return "daily".equals(mode);
    }

    private LocalTime readDailyTime(Properties properties) {
        String value = properties.getProperty("dailyTime", "20:00").trim();
        if (value.isBlank()) {
            return DEFAULT_DAILY_TIME;
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            LOGGER.warn("dailyTime invalide '{}', utilisation de {}", value, DEFAULT_DAILY_TIME);
            return DEFAULT_DAILY_TIME;
        }
    }

    private ZoneId readZoneId(Properties properties) {
        String value = properties.getProperty("timezone", "").trim();
        if (value.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(value);
        } catch (Exception e) {
            LOGGER.warn("Timezone invalide '{}', utilisation du fuseau systeme", value);
            return ZoneId.systemDefault();
        }
    }

    private synchronized boolean canPublish(boolean force) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastPublishMillis < MIN_PUBLISH_INTERVAL_MS) {
            return false;
        }

        if (!force && dailyMode) {
            LocalDate today = LocalDate.now(zoneId);
            LocalDate lastSent = readLastSentDate();
            if (today.equals(lastSent)) {
                return false;
            }
            LocalTime nowTime = LocalTime.now(zoneId);
            if (nowTime.isBefore(dailyTime)) {
                return false;
            }
        }

        lastPublishMillis = now;
        return true;
    }

    private LocalDate readLastSentDate() {
        Path path = Paths.get(LAST_SENT_PATH);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String value = Files.readString(path).trim();
            return value.isBlank() ? null : LocalDate.parse(value);
        } catch (Exception e) {
            LOGGER.debug("Impossible de lire la derniere date d envoi", e);
            return null;
        }
    }

    private void updateLastSentDateIfNeeded(boolean markDaily) {
        if (!markDaily) {
            return;
        }
        Path path = Paths.get(LAST_SENT_PATH);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, LocalDate.now(zoneId).toString());
        } catch (IOException e) {
            LOGGER.debug("Impossible d enregistrer la derniere date d envoi", e);
        }
    }

    private String readMessageId() {
        Path path = Paths.get(MESSAGE_ID_PATH);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String value = Files.readString(path).trim();
            return value.isBlank() ? null : value;
        } catch (IOException e) {
            LOGGER.debug("Impossible de lire l ID du message Discord", e);
            return null;
        }
    }

    private void deleteMessageId() {
        Path path = Paths.get(MESSAGE_ID_PATH);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.debug("Impossible de supprimer l ID du message Discord", e);
        }
    }

    private void storeMessageIdFromResponse(String responseBody) {
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            if (response != null && response.has("id")) {
                String id = response.get("id").getAsString();
                Path path = Paths.get(MESSAGE_ID_PATH);
                Files.createDirectories(path.getParent());
                Files.writeString(path, id);
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible d enregistrer l ID du message Discord", e);
        }
    }
}
