package com.roll_54.roll_mod.minestar.debug;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.HueriStatueEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * {@code /rollmod admin debug fill [player]} and {@code /rollmod admin debug clear [player]}: test content for
 * every part of the hub, and the exact undo of it. Operators only — see {@link DebugFill}.
 *
 * <p>{@code /rollmod admin debug hueristatue [targets]}: forgets that they ever opened a Hueri statue.
 *
 * <p>{@code /rollmod admin debug orepalette [all_possible_variants]}: every ore material (or every ore block) in a flat
 * square under you — see {@link OrePalette}.
 *
 * <p>The level-2 gate sits on {@code debug}, not on {@code admin}: other classes register {@code rollmod admin}
 * ungated, and a gate on a literal several registrations share is kept or dropped by registration order.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DebugFillCommand {

    private DebugFillCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod").then(Commands.literal("admin")
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("fill")
                                .executes(ctx -> run(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                        "fill", DebugFill::fill))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> run(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"), "fill", DebugFill::fill))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> run(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                        "clear", DebugFill::clear))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> run(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"), "clear", DebugFill::clear))))
                        .then(Commands.literal("hueristatue")
                                .executes(ctx -> resetStatue(ctx.getSource(),
                                        List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetStatue(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("orepalette")
                                .executes(ctx -> orePalette(ctx.getSource(), false))
                                .then(Commands.literal("all_possible_variants")
                                        .executes(ctx -> orePalette(ctx.getSource(), true)))))));
    }

    private static int orePalette(CommandSourceStack source, boolean allVariants) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int[] placed = OrePalette.place(player, allVariants);
        BlockPos at = player.blockPosition().below();
        source.sendSuccess(() -> Component.literal("[Debug] Placed " + placed[0]
                + (allVariants ? " ore blocks, every variant (" : " ore materials (")
                + placed[1] + "x" + placed[1] + ") under " + at.getX() + ", " + at.getY() + ", " + at.getZ()
                + " — the blocks that were there are gone").withStyle(ChatFormatting.GOLD), true);
        return placed[0];
    }

    /** Back to "never opened a Hueri statue", so the particles, "!" and hint come back at once. */
    private static int resetStatue(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            HueriStatueEvents.reset(target);
        }
        source.sendSuccess(() -> Component.literal("[Debug] Hueri statue reset for " + targets.size()
                + " player(s)").withStyle(ChatFormatting.GOLD), true);
        return targets.size();
    }

    private static int run(CommandSourceStack source, ServerPlayer target, String action,
                           BiFunction<CommandSourceStack, ServerPlayer, List<String>> body)
            throws CommandSyntaxException {
        List<String> lines = body.apply(source, target);
        source.sendSuccess(() -> Component.literal("[Debug] " + action + " for "
                + target.getGameProfile().getName() + ":").withStyle(ChatFormatting.GOLD), true);
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal("  " + line).withStyle(ChatFormatting.GRAY), false);
        }
        return lines.size();
    }
}
