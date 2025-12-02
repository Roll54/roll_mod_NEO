package com.roll_54.roll_mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AutoGiveCommands {

    private static final String TAG = "roll_mod:autogive";   // зберігаємо як STRING

    public static void register(CommandDispatcher<CommandSourceStack> d) {

        d.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("autogive")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                                            // Записуємо як STRING
                                            player.getPersistentData().putString(TAG, Boolean.toString(value));

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Autogive set to " + TAG),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )
        );

        d.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("admin")
                                .requires(src -> src.hasPermission(2))

                                .then(Commands.literal("autogive")

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

                                                            String stored = target.getPersistentData().getString(TAG);
                                                            if (stored.isEmpty()) stored = "unset";

                                                            String finalStored = stored;
                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal("Player " + name + " autogive = " + finalStored),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                                )
                                        )



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

                                                                    // записуємо як STRING
                                                                    target.getPersistentData().putString(TAG, Boolean.toString(value));

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
