package com.roll_54.roll_mod.minestar.dailytasks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskQuota;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /rollmod admin dailytasks …} — operator tooling for the daily-task system.
 *
 * <p>Registered here in {@code common} rather than in the server module's {@code CommandRegistry}
 * so it also exists in the dev client run, where the daily tasks actually need testing.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DailyTasksCommand {

    private DailyTasksCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                .then(Commands.literal("dailytasks")
                        // Gate on "dailytasks", not "admin": several classes register "rollmod admin",
                        // and Brigadier keeps only the first-registered node's requirement when it
                        // merges them. Nobody else registers this node, so this gate always holds.
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                        .then(Commands.literal("reroll")
                                .executes(ctx -> rerollAll(ctx.getSource()))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> rerollPlayers(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetPlayers(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("progress")
                                .then(Commands.argument("index", IntegerArgumentType.integer(
                                                0, DailyTasksState.MAX_TASK_COUNT - 1))
                                        .then(Commands.argument("amount",
                                                        IntegerArgumentType.integer(1))
                                                .executes(ctx -> progress(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "index"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))));
    }

    private static int info(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);

        source.sendSuccess(() -> Component.literal(
                "Group %s — %d member(s), multiplier x%d, resets in %s"
                        .formatted(group.id(), group.memberCount(), group.memberCount() + 1,
                                formatDuration(DailyTaskManager.secondsUntilNextRoll()))), false);

        // Walks the cap, not the quota: a slot past what this group drew has no view at all, so
        // the listing is as long as the group's own set rather than always MAX_TASK_COUNT lines.
        int listed = 0;
        for (int i = 0; i < DailyTasksState.MAX_TASK_COUNT; i++) {
            DailyTaskManager.TaskView v = DailyTaskManager.view(player, i);
            if (v == null) continue;
            listed++;
            final int index = i;
            source.sendSuccess(() -> Component.literal("  [%d] %s  %d/%d%s%s".formatted(
                    index, v.task().id(), v.progress(), v.required(),
                    v.completed() ? "  DONE" : "",
                    v.claimed() ? "  CLAIMED" : "")), false);
        }
        if (listed == 0) {
            source.sendSuccess(() -> Component.literal("  <not rolled>"), false);
        }

        DailyTaskManager.BonusView bonus = DailyTaskManager.bonusView(player);
        source.sendSuccess(() -> bonus == null
                ? Component.literal("  bonus: <none>")
                : Component.literal("  bonus: %s  %d/%d%s".formatted(
                        bonus.reward().id(), bonus.completed(), bonus.total(),
                        bonus.claimed() ? "  CLAIMED" : "")), false);
        return 1;
    }

    /**
     * Server-wide: every group is cleared, so each draws its own brand new set (and bonus reward)
     * the next time it is touched. There is no shared server set to reroll any more.
     */
    private static int rerollAll(CommandSourceStack source) {
        DailyTaskManager.forceRoll(source.getServer());
        source.sendSuccess(() -> Component.literal(
                "Cleared every group; each will draw a new set of daily tasks. All progress cleared."),
                true);
        return 1;
    }

    /**
     * Per-player: gives each target's group its own fresh set. The group is the unit — rerolling
     * someone in an FTB party rerolls the whole party, since a party shares one set of tasks and
     * one progress record — so the feedback spells out how many players were actually affected.
     */
    private static int rerollPlayers(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int rerolled = 0;
        for (ServerPlayer target : targets) {
            List<String> tasks = DailyTaskManager.rerollFor(target);
            if (tasks == null) {
                source.sendFailure(Component.literal(
                        "Could not draw a set — fewer than %d daily tasks are registered."
                                .formatted(DailyTaskQuota.forPlayer(target))));
                return 0;
            }
            rerolled++;
            int party = DailyTaskGroups.of(target).memberCount();
            source.sendSuccess(() -> Component.literal(
                    "Rerolled %s%s: %s".formatted(
                            target.getGameProfile().getName(),
                            party > 1 ? " (and their party of %d)".formatted(party) : "",
                            String.join(", ", tasks))), true);
        }
        return rerolled;
    }

    /** Per-player: same tasks, progress and claims wiped. */
    private static int resetPlayers(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int reset = 0;
        for (ServerPlayer target : targets) {
            if (!DailyTaskManager.resetFor(target)) {
                source.sendFailure(Component.literal(
                        "Could not draw a set — fewer than %d daily tasks are registered."
                                .formatted(DailyTaskQuota.forPlayer(target))));
                return 0;
            }
            reset++;
            int party = DailyTaskGroups.of(target).memberCount();
            source.sendSuccess(() -> Component.literal(
                    "Cleared daily-task progress for %s%s.".formatted(
                            target.getGameProfile().getName(),
                            party > 1 ? " (and their party of %d)".formatted(party) : "")), true);
        }
        return reset;
    }

    private static int progress(CommandSourceStack source, int index, int amount)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DailyTaskManager.addRawProgress(player, index, amount);
        source.sendSuccess(() -> Component.literal(
                "Added %d progress to task %d.".formatted(amount, index)), false);
        return 1;
    }

    private static String formatDuration(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}
