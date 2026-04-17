package com.colonyrank.mod.command;

import com.colonyrank.mod.ColonyRankMod;
import com.colonyrank.mod.data.ColonyData;
import com.colonyrank.mod.util.LocalizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Commande /colonyrank - Affiche le classement des colonies par score
 */
public class CommandColonyRank {

    private static final LocalizationManager I18N = LocalizationManager.get();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("colonyrank")
                .executes(CommandColonyRank::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("rank.collector_not_initialized")));
            return 0;
        }

        // Silent refresh to keep the ranking up to date.
        ColonyRankMod.getDataCollector().updateAllColonies(source.getServer());
        if (ColonyRankMod.getScoreCalculator() != null) {
            ColonyRankMod.getScoreCalculator()
                .recalculateAllScores(ColonyRankMod.getDataCollector());
        }
        ColonyRankMod.getDataCollector().saveColoniesToJson();

        List<ColonyData> rankedColonies = ColonyRankMod.getDataCollector().getColoniesRanked();

        if (rankedColonies.isEmpty()) {
            source.sendSuccess(
                () -> Component.literal(I18N.t("rank.no_colonies")),
                false
            );
            return 1;
        }

        source.sendSuccess(
            () -> Component.literal("\u00A76" + I18N.t("rank.header")),
            false
        );

        int rank = 1;
        for (ColonyData colony : rankedColonies) {
            String rankingLine = I18N.t(
                "rank.line",
                rank,
                colony.getName(),
                colony.getPopulation(),
                colony.getBuildingCount(),
                colony.getAverageBuildingLevel(),
                colony.getColonyAgeDisplay(),
                colony.getClaimedChunks(),
                colony.getOverallHappiness(),
                colony.getScore()
            );

            source.sendSuccess(
                () -> Component.literal("\u00A7e" + rankingLine),
                false
            );

            rank++;
        }

        source.sendSuccess(
            () -> Component.literal("\u00A76" + I18N.t("rank.footer")),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A77" + I18N.t("rank.total", rankedColonies.size())),
            false
        );

        return 1;
    }
}