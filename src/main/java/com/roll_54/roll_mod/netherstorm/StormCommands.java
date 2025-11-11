package com.roll_54.roll_mod.netherstorm;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = RollMod.MODID)
public class StormCommands {

    private static final String[] ACTIONS = {"start", "end", "status"};

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        var dispatcher = e.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root =
                literal("rollmod")
                        .requires(src -> src.hasPermission(2))
                        .then(literal("netherstorm")
                                // /rollmod netherstorm start [ticks]
                                .then(literal("start")
                                        .executes(ctx -> {
                                            StormHandler.forceStart(ctx.getSource().getServer(), null); // null = random
                                            ctx.getSource().sendSuccess(() -> Component.literal("Started NetherStorm (random duration)"), false);
                                            return 1;
                                        })
                                        .then(argument("ticks", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                                    StormHandler.forceStart(ctx.getSource().getServer(), ticks);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Started NetherStorm for " + ticks + " ticks"), false);
                                                    return 1;
                                                })
                                        )
                                )
                                // /rollmod netherstorm end
                                .then(literal("end")
                                        .executes(ctx -> {
                                            StormHandler.forceEnd(ctx.getSource().getServer());
                                            ctx.getSource().sendSuccess(() -> Component.literal("Ended NetherStorm"), false);
                                            return 1;
                                        })
                                )
                                // /rollmod netherstorm status
                                .then(literal("status")
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
                        );

        dispatcher.register(root);
    }

    private static CompletableFuture<Suggestions> suggestActions(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder
    ) {
        for (String s : ACTIONS) {
            Component hint = switch (s) {
                case "start" -> Component.translatable("command.roll_mod_neo.netherstorm.start");
                case "end" -> Component.translatable("command.roll_mod_neo.netherstorm.end");
                case "status" -> Component.translatable("command.roll_mod_neo.netherstorm.status");
                default -> Component.literal(s);
            };
            builder.suggest(s, hint);
        }
        return builder.buildFuture();
    }
    private static String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return seconds + "s";
    }
}
