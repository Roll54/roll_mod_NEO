package com.roll_54.roll_mod.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.netherstorm.StormHandler;

import static com.roll_54.roll_mod.data.RMMAttachment.STORM_PROTECTED;

@EventBusSubscriber(modid = RollMod.MODID)
public class StormCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        var d = e.getDispatcher();

        AutoGiveCommands.register(d);

        var adminRoot = Commands.literal("rollmod")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("admin")

                        // --------------------------
                        // /rollmod admin netherstorm
                        // --------------------------
                        .then(Commands.literal("netherstorm")
                                .then(Commands.literal("start")
                                        .executes(ctx -> {
                                            StormHandler.forceStart(ctx.getSource().getServer(), null);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Started NetherStorm (random duration)"), false);
                                            return 1;
                                        })
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                                    StormHandler.forceStart(ctx.getSource().getServer(), ticks);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("Started NetherStorm for " + ticks + " ticks"), false);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("end")
                                        .executes(ctx -> {
                                            StormHandler.forceEnd(ctx.getSource().getServer());
                                            ctx.getSource().sendSuccess(() -> Component.literal("Ended NetherStorm"), false);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("status")
                                        .executes(ctx -> {
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "active=" + StormHandler.isStormActive() +
                                                                    ", stormTicks=" + StormHandler.getStormTicks() +
                                                                    ", stormDuration=" + StormHandler.getStormDuration() +
                                                                    ", untilNext=" + StormHandler.getTicksUntilNextStorm()
                                                    ), false);
                                            return 1;
                                        })
                                )
                        )

                        // ----------------------------------------------
                        // /rollmod admin storm_protected get/set <player>
                        // ----------------------------------------------
                        .then(Commands.literal("storm_protected")

                                // ===== GET =====
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
                                                    ServerPlayer target = ctx.getSource()
                                                            .getServer()
                                                            .getPlayerList()
                                                            .getPlayerByName(name);

                                                    if (target == null) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("Player '" + name + "' not found.")
                                                        );
                                                        return 0;
                                                    }

                                                    boolean value = target.getData(STORM_PROTECTED);

                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(
                                                                    "Player " + name + " storm_protected = " + value
                                                            ),
                                                            false
                                                    );

                                                    return 1;
                                                })
                                        )
                                )

                                // ===== SET =====
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

                                                            ServerPlayer target = ctx.getSource()
                                                                    .getServer()
                                                                    .getPlayerList()
                                                                    .getPlayerByName(name);

                                                            if (target == null) {
                                                                ctx.getSource().sendFailure(
                                                                        Component.literal("Player '" + name + "' not found.")
                                                                );
                                                                return 0;
                                                            }

                                                            target.setData(
                                                                    STORM_PROTECTED,
                                                                    value
                                                            );

                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Set storm_protected for " + name + " = " + value
                                                                    ),
                                                                    true
                                                            );

                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                );

        d.register(adminRoot);
    }
}