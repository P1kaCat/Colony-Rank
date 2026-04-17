package com.colonyrank.mod.command;

import com.colonyrank.mod.ColonyRankMod;
import com.colonyrank.mod.data.ColonyData;
import com.colonyrank.mod.util.LocalizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /colonyscore - Affiche les details d'une colonie specifique
 */
public class CommandColonyScore {

    private static final LocalizationManager I18N = LocalizationManager.get();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("colonyscore")
                .then(Commands.literal("list")
                    .executes(CommandColonyScore::executeList)
                )
                .then(Commands.literal("id")
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(0))
                        .executes(CommandColonyScore::executeById)
                    )
                )
                .then(Commands.argument("colonyName", StringArgumentType.greedyString())
                    .executes(CommandColonyScore::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();
        String colonyName = StringArgumentType.getString(context, "colonyName");

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("score.collector_not_initialized")));
            return 0;
        }

        ColonyData colony = ColonyRankMod.getDataCollector().getColonyDataByName(colonyName);

        if (colony == null) {
            source.sendFailure(Component.literal(I18N.t("score.colony_not_found_name", colonyName)));
            return 0;
        }

        return displayColonyDetails(source, colony);
    }

    private static int executeById(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();
        int colonyId = IntegerArgumentType.getInteger(context, "colonyId");

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("score.collector_not_initialized")));
            return 0;
        }

        ColonyData colony = ColonyRankMod.getDataCollector().getColonyData(colonyId);
        if (colony == null) {
            source.sendFailure(Component.literal(I18N.t("score.colony_not_found_id", colonyId)));
            return 0;
        }

        return displayColonyDetails(source, colony);
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        I18N.reload();
        CommandSourceStack source = context.getSource();

        if (ColonyRankMod.getDataCollector() == null) {
            source.sendFailure(Component.literal(I18N.t("score.collector_not_initialized")));
            return 0;
        }

        List<ColonyData> colonies = ColonyRankMod.getDataCollector().getAllColonies().values().stream()
            .sorted(Comparator.comparingInt(ColonyData::getColonyId))
            .collect(Collectors.toList());

        if (colonies.isEmpty()) {
            source.sendSuccess(
                () -> Component.literal(I18N.t("rank.no_colonies")),
                false
            );
            return 1;
        }

        source.sendSuccess(
            () -> Component.literal("\u00A76" + I18N.t("score.list.header")),
            false
        );

        for (ColonyData colony : colonies) {
            source.sendSuccess(
                () -> Component.literal("\u00A7e" + I18N.t("score.list.item", colony.getColonyId(), colony.getName())),
                false
            );
        }

        source.sendSuccess(
            () -> Component.literal("\u00A76" + I18N.t("score.list.footer")),
            false
        );

        return 1;
    }

    private static int displayColonyDetails(CommandSourceStack source, ColonyData colony) {
        I18N.reload();

        source.sendSuccess(
            () -> Component.literal("\u00A76" + I18N.t("score.details.header")),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.colony", colony.getName())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.population", colony.getPopulation())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.buildings", colony.getBuildingCount())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.avg_level", colony.getAverageBuildingLevel())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.age", colony.getColonyAgeDisplay())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.chunks", colony.getClaimedChunks())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.happiness", colony.getOverallHappiness())),
            false
        );

        String breakdown = ColonyRankMod.getScoreCalculator().getScoreBreakdown(colony);
        source.sendSuccess(
            () -> Component.literal("\u00A7e" + I18N.t("score.details.breakdown", breakdown)),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A7a\u00A7l" + I18N.t("score.details.total", colony.getScore())),
            false
        );

        source.sendSuccess(
            () -> Component.literal("\u00A76" + I18N.t("score.details.footer")),
            false
        );

        return 1;
    }
}