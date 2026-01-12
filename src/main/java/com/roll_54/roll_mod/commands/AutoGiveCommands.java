package com.roll_54.roll_mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.roll_54.roll_mod.data.RMMAttachment.AUTO_GIVE;

public class AutoGiveCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {

        // /rollmod autogive <true|false>  (для себе)
        d.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("autogive")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ServerPlayer player =
                                                    ctx.getSource().getPlayerOrException();

                                            boolean value =
                                                    BoolArgumentType.getBool(ctx, "value");

                                            player.setData(
                                                   AUTO_GIVE.get(),
                                                    value
                                            );

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Autogive set to " + value
                                                    ),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )
        );

        // /rollmod admin autogive get/set <player>
        d.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("admin")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("autogive")

                                        // GET
                                        .then(Commands.literal("get")
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            for (ServerPlayer p :
                                                                    ctx.getSource().getServer()
                                                                            .getPlayerList().getPlayers()) {
                                                                builder.suggest(
                                                                        p.getName().getString()
                                                                );
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> {
                                                            String name =
                                                                    StringArgumentType.getString(ctx, "player");

                                                            ServerPlayer target =
                                                                    ctx.getSource().getServer()
                                                                            .getPlayerList()
                                                                            .getPlayerByName(name);

                                                            if (target == null) {
                                                                ctx.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "Player '" + name + "' not found."
                                                                        )
                                                                );
                                                                return 0;
                                                            }

                                                            boolean value =
                                                                    target.getData(
                                                                            AUTO_GIVE.get()
                                                                    );

                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Player " + name +
                                                                                    " autogive = " + value
                                                                    ),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                                )
                                        )

                                        // SET
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            for (ServerPlayer p :
                                                                    ctx.getSource().getServer()
                                                                            .getPlayerList().getPlayers()) {
                                                                builder.suggest(
                                                                        p.getName().getString()
                                                                );
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                                .executes(ctx -> {
                                                                    String name =
                                                                            StringArgumentType.getString(ctx, "player");

                                                                    boolean value =
                                                                            BoolArgumentType.getBool(ctx, "value");

                                                                    ServerPlayer target =
                                                                            ctx.getSource().getServer()
                                                                                    .getPlayerList()
                                                                                    .getPlayerByName(name);

                                                                    if (target == null) {
                                                                        ctx.getSource().sendFailure(
                                                                                Component.literal(
                                                                                        "Player '" + name + "' not found."
                                                                                )
                                                                        );
                                                                        return 0;
                                                                    }

                                                                    target.setData(
                                                                            AUTO_GIVE.get(),
                                                                            value
                                                                    );

                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Set autogive for " +
                                                                                            name + " = " + value
                                                                            ),
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
