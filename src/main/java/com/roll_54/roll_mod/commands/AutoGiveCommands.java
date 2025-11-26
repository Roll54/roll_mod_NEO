package com.roll_54.roll_mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AutoGiveCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {


        // /rollmod autogive <bool>   — for normal players

        d.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("autogive")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                                            player.getPersistentData().putBoolean("roll_mod:autogive", value);

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Autogive set to " + value),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )
        );


        // /rollmod admin autogive ...

        d.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("admin")
                                .requires(src -> src.hasPermission(2))

                                .then(Commands.literal("autogive")

                                        // /rollmod admin autogive get <player>

                                        .then(Commands.literal("get")
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                                                builder.suggest(p.getName().getString());
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "player");
                                                            ServerPlayer target = ctx.getSource().getServer()
                                                                    .getPlayerList().getPlayerByName(name);

                                                            if (target == null) {
                                                                ctx.getSource().sendFailure(
                                                                        Component.literal("Player '" + name + "' not found.")
                                                                );
                                                                return 0;
                                                            }

                                                            boolean val = target.getPersistentData().getBoolean("roll_mod:autogive");

                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal("Player " + name + " autogive = " + val),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                                )
                                        )


                                        // /rollmod admin autogive set <player> <value>

                                        .then(Commands.literal("set")
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                                                builder.suggest(p.getName().getString());
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                                .executes(ctx -> {
                                                                    String name = StringArgumentType.getString(ctx, "player");
                                                                    boolean value = BoolArgumentType.getBool(ctx, "value");

                                                                    ServerPlayer target = ctx.getSource().getServer()
                                                                            .getPlayerList().getPlayerByName(name);

                                                                    if (target == null) {
                                                                        ctx.getSource().sendFailure(
                                                                                Component.literal("Player '" + name + "' not found.")
                                                                        );
                                                                        return 0;
                                                                    }

                                                                    target.getPersistentData().putBoolean("roll_mod:autogive", value);

                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal("Set autogive for " + name + " to " + value),
                                                                            true
                                                                    );

                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }
}
