package com.roll_54.roll_mod.minestar.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /tpa}, {@code /tpahere}, {@code /tpaccept}, {@code /tpdeny}, {@code /tpcancel} — and the
 * same five under {@code /rollmod tpa …}.
 *
 * <p>Answering takes an optional name so a player with two requests waiting can say which one they
 * mean; with no name the oldest is answered, which is what a player with one request wants.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class TpaCommands {

    private TpaCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("rollmod").then(Commands.literal("tpa")
                .then(ask("to", TpaRequest.Kind.TO))
                .then(ask("here", TpaRequest.Kind.HERE))
                .then(answer("accept", true))
                .then(answer("deny", false))
                .then(cancel("cancel"))
                .then(mode("mode"))));

        dispatcher.register(ask("tpa", TpaRequest.Kind.TO));
        dispatcher.register(ask("tpahere", TpaRequest.Kind.HERE));
        dispatcher.register(answer("tpaccept", true));
        dispatcher.register(answer("tpdeny", false));
        dispatcher.register(cancel("tpcancel"));
        dispatcher.register(mode("tpamode"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ask(String literal, TpaRequest.Kind kind) {
        return Commands.literal(literal)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer from = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            return TpaService.request(from, target, kind) ? 1 : 0;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> answer(String literal, boolean accept) {
        return Commands.literal(literal)
                .executes(ctx -> resolve(ctx.getSource(), null, accept))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> resolve(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player").getGameProfile().getName(),
                                accept)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> cancel(String literal) {
        return Commands.literal(literal)
                .executes(ctx -> TpaService.cancel(ctx.getSource().getPlayerOrException(), null) ? 1 : 0)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> TpaService.cancel(ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player").getGameProfile().getName())
                                ? 1 : 0));
    }

    /**
     * {@code /tpamode} on its own reports the setting; with a mode it changes it.
     *
     * <p>A literal per constant rather than a string argument, so tab-completion lists the modes and
     * adding one needs no edit here.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> mode(String literal) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(literal)
                .executes(ctx -> show(ctx.getSource().getPlayerOrException()));
        for (TpaMode value : TpaMode.values()) {
            node.then(Commands.literal(value.id())
                    .executes(ctx -> set(ctx.getSource().getPlayerOrException(), value)));
        }
        return node;
    }

    private static int show(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.modeIs",
                TpaSettings.mode(player.getUUID()).title()));
        return 1;
    }

    private static int set(ServerPlayer player, TpaMode mode) {
        TpaSettings.set(player, mode);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.modeSet", mode.title()));
        // So a command typed with the hub open moves the mark in the settings window too.
        TpaViewers.syncTo(player);
        return 1;
    }

    private static int resolve(CommandSourceStack source, String name, boolean accept)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return (accept ? TpaService.accept(player, name) : TpaService.deny(player, name)) ? 1 : 0;
    }
}
