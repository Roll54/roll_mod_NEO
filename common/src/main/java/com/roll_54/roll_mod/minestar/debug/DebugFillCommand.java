package com.roll_54.roll_mod.minestar.debug;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.function.Function;

/**
 * {@code /rollmod debug fill [player]} and {@code /rollmod debug clear [player]}: test content for
 * every part of the hub, and the exact undo of it. Operators only — see {@link DebugFill}.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DebugFillCommand {

    private DebugFillCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod")
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
                                                EntityArgument.getPlayer(ctx, "player"), "clear", DebugFill::clear))))));
    }

    private static int run(CommandSourceStack source, ServerPlayer target, String action,
                           Function<ServerPlayer, List<String>> body) throws CommandSyntaxException {
        List<String> lines = body.apply(target);
        source.sendSuccess(() -> Component.literal("[Debug] " + action + " for "
                + target.getGameProfile().getName() + ":").withStyle(ChatFormatting.GOLD), true);
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal("  " + line).withStyle(ChatFormatting.GRAY), false);
        }
        return lines.size();
    }
}
