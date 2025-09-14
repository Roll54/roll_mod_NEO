package com.roll_54.roll_mod.netherstorm;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = Roll_mod.MODID)
public class StormCommands {

    private static final String[] ACTIONS = {"start", "end", "status"};

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        var dispatcher = e.getDispatcher();

        // /rollmod netherstorm <action>
        LiteralArgumentBuilder<CommandSourceStack> root =
                literal("rollmod")
                        .requires(src -> src.hasPermission(2))
                        .then(literal("netherstorm")
                                .then(argument("action", StringArgumentType.word())
                                        .suggests(StormCommands::suggestActions)
                                        .executes(ctx -> {
                                            String action = StringArgumentType.getString(ctx, "action")
                                                    .toLowerCase(Locale.ROOT);
                                            switch (action) {
                                                case "start" -> StormHandler.forceStart(ctx.getSource().getServer());
                                                case "end"   -> StormHandler.forceEnd(ctx.getSource().getServer());
                                                case "status" -> ctx.getSource().sendSuccess(
                                                        () -> Component.literal(
                                                                "active=" + StormHandler.isStormActive() +
                                                                        ", stormTicks=" + StormHandler.getStormTicks() +
                                                                        ", stormDuration=" + StormHandler.getStormDuration() +
                                                                        ", untilNext=" + StormHandler.getTicksUntilNextStorm()
                                                        ), false);
                                                default -> ctx.getSource().sendFailure(
                                                        Component.translatable("command.roll_mod_neo.netherstorm.invalid"));
                                            }
                                            return 1;
                                        })));

        dispatcher.register(root);
    }

    private static CompletableFuture<Suggestions> suggestActions(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder
    ) {
        // Показуємо локалізовані підказки, але підставляємо сирі значення
        for (String s : ACTIONS) {
            Component hint = switch (s) {
                case "start"  -> Component.translatable("command.roll_mod_neo.netherstorm.start");
                case "end"    -> Component.translatable("command.roll_mod_neo.netherstorm.end");
                case "status" -> Component.translatable("command.roll_mod_neo.netherstorm.status");
                default       -> Component.literal(s);
            };
            builder.suggest(s, hint);
        }
        return builder.buildFuture();
    }
}
