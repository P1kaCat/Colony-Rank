package com.colonyrank.mod.command;

import com.colonyrank.mod.ColonyRankMod;
import com.colonyrank.mod.util.LocalizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Commande administrative /colonyadmin pour la gestion du mod
 */
public class CommandColonyAdmin {

    private static final LocalizationManager I18N = LocalizationManager.get();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("colonyadmin")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("reload")
                    .executes(CommandColonyAdmin::executeReload)
                )
                .then(Commands.literal("refresh")
                    .executes(CommandColonyAdmin::executeRefresh)
                )
                .then(Commands.literal("clear")
                    .executes(CommandColonyAdmin::executeClear)
                )
                .then(Commands.literal("status")
                    .executes(CommandColonyAdmin::executeStatus)
                )
                .then(Commands.literal("export")
                    .executes(CommandColonyAdmin::executeExport)
                )
                .then(Commands.literal("sendleaderboard")
                    .executes(CommandColonyAdmin::executeSendLeaderboard)
                )
                .then(Commands.literal("discordstatus")
                    .executes(CommandColonyAdmin::executeDiscordStatus)
                )
                .then(Commands.literal("senddaily")
                    .executes(CommandColonyAdmin::executeSendDaily)
                )
                .then(Commands.literal("help")
                    .executes(CommandColonyAdmin::executeHelp)
                )
        );
    }

    private static int executeSendLeaderboard(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        try {
            if (ColonyRankMod.getDataCollector() == null) {
                source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.sendleaderboard.start")), true);

            ColonyRankMod.getDataCollector().updateAllColonies(source.getServer(), false);
            if (ColonyRankMod.getScoreCalculator() != null) {
                ColonyRankMod.getScoreCalculator().recalculateAllScores(ColonyRankMod.getDataCollector());
            }
            ColonyRankMod.getDataCollector().saveColoniesToJson(false);
            ColonyRankMod.getDataCollector().publishDiscordNow();

            source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.sendleaderboard.done")), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00A7c" + I18N.t("admin.sendleaderboard.error", e.getMessage())));
            return 0;
        }
    }

    private static int executeDiscordStatus(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
            return 0;
        }

        boolean configured = ColonyRankMod.getDataCollector().isDiscordWebhookConfigured();

        source.sendSuccess(() -> Component.literal("\u00A76" + I18N.t("admin.discordstatus.header")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.discordstatus.webhook", configured ? I18N.t("admin.discordstatus.webhook_yes") : I18N.t("admin.discordstatus.webhook_no"))), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.discordstatus.config", ColonyRankMod.getDataCollector().getDiscordConfigPath())), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.discordstatus.state", ColonyRankMod.getDataCollector().getDiscordStatusLine())), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.discordstatus.language", I18N.getLanguageCode())), false);
        source.sendSuccess(() -> Component.literal("\u00A76" + I18N.t("admin.discordstatus.footer")), false);

        return 1;
    }

    private static int executeSendDaily(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.senddaily.start")), true);

        ColonyRankMod.getDataCollector().updateAllColonies(source.getServer(), false);
        if (ColonyRankMod.getScoreCalculator() != null) {
            ColonyRankMod.getScoreCalculator().recalculateAllScores(ColonyRankMod.getDataCollector());
        }
        ColonyRankMod.getDataCollector().saveColoniesToJson(false);
        ColonyRankMod.getDataCollector().publishDailyNow(source.getServer());

        source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.senddaily.done")), true);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        try {
            if (ColonyRankMod.getDataCollector() == null) {
                source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.reload.start")), true);
            ColonyRankMod.getDataCollector().updateAllColonies(source.getServer());
            source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.reload.done", ColonyRankMod.getDataCollector().getColonyCount())), true);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00A7c" + I18N.t("admin.reload.error", e.getMessage())));
            return 0;
        }
    }

    private static int executeRefresh(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        try {
            if (ColonyRankMod.getDataCollector() == null) {
                source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
                return 0;
            }

            if (ColonyRankMod.getDataCollector().getColonyCount() == 0) {
                source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.refresh.empty_cache")), true);
                ColonyRankMod.getDataCollector().updateAllColonies(source.getServer());
            } else {
                source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.refresh.start")), true);
            }

            ColonyRankMod.getScoreCalculator().recalculateAllScores(ColonyRankMod.getDataCollector());
            ColonyRankMod.getDataCollector().saveColoniesToJson();

            source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.refresh.done")), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00A7c" + I18N.t("admin.refresh.error", e.getMessage())));
            return 0;
        }
    }

    private static int executeClear(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        try {
            if (ColonyRankMod.getDataCollector() == null) {
                source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.clear.start")), true);

            int count = ColonyRankMod.getDataCollector().getColonyCount();
            ColonyRankMod.getDataCollector().clearCache();

            source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.clear.done", count)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00A7c" + I18N.t("admin.clear.error", e.getMessage())));
            return 0;
        }
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("\u00A76" + I18N.t("admin.status.header")), false);

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("admin.status.collector_not_initialized")));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.status.collector_running")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.status.colonies_cache", ColonyRankMod.getDataCollector().getColonyCount())), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.status.json_file")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.status.language", I18N.getLanguageCode())), false);
        source.sendSuccess(() -> Component.literal("\u00A76" + I18N.t("admin.status.footer")), false);

        return 1;
    }

    private static int executeExport(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        try {
            if (ColonyRankMod.getDataCollector() == null) {
                source.sendFailure(Component.literal(I18N.t("admin.collector_not_initialized")));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.export.start")), true);
            ColonyRankMod.getDataCollector().saveColoniesToJson();
            source.sendSuccess(() -> Component.literal("\u00A7a" + I18N.t("admin.export.done")), true);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00A7c" + I18N.t("admin.export.error", e.getMessage())));
            return 0;
        }
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("\u00A76" + I18N.t("admin.help.header")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.reload")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.refresh")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.clear")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.status")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.export")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.discordstatus")), false);
        source.sendSuccess(() -> Component.literal("\u00A7e" + I18N.t("admin.help.senddaily")), false);
        source.sendSuccess(() -> Component.literal("\u00A76" + I18N.t("admin.help.footer")), false);

        return 1;
    }
}