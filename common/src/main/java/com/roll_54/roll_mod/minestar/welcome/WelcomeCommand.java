package com.roll_54.roll_mod.minestar.welcome;

import com.mojang.brigadier.CommandDispatcher;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMAttachment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

/**
 * {@code /rollmod welcome …} — the only way to show the starting screen besides a first login.
 * Operator only.
 * <ul>
 *   <li>{@code open <targets>} — shows it now. The usual rules apply: decline or close and they are kicked.</li>
 *   <li>{@code reset <targets>} — marks them as not having accepted, so it opens on their next login.</li>
 * </ul>
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class WelcomeCommand {

    private WelcomeCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("welcome")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("open")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> open(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> reset(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")))))));
    }

    private static int open(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            WelcomeService.open(target);
        }
        source.sendSuccess(() -> Component.translatable("command.roll_mod.welcome.open", targets.size()), true);
        return targets.size();
    }

    private static int reset(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            target.setData(RMMAttachment.NEW_PLAYER, false);
        }
        source.sendSuccess(() -> Component.translatable("command.roll_mod.welcome.reset", targets.size()), true);
        return targets.size();
    }
}
