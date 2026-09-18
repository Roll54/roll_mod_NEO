package com.roll_54.roll_mod.minestar.hub;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import java.util.Collection;

/**
 * {@code /rollmod tier …} — reads and sets a player's progression tier.
 *
 * <p>Registered from {@code common} rather than the server module's {@code CommandRegistry}, like
 * {@code DailyTasksCommand}, so it also exists in the dev client run where it is actually tested.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class TierCommand {

    private TierCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("tier")
                        // Reading your own tier is harmless; changing anyone's is operator work.
                        .then(Commands.literal("get")
                                .executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> get(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target")))))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("tier", IntegerArgumentType.integer(
                                                        PlayerTier.MIN, PlayerTier.MAX))
                                                .executes(ctx -> set(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        IntegerArgumentType.getInteger(ctx, "tier"))))))));
    }

    private static int get(CommandSourceStack source, ServerPlayer target) {
        int tier = PlayerTier.of(target);
        source.sendSuccess(() -> Component.translatable("command.roll_mod.tier.get",
                target.getDisplayName(), tier, PlayerTier.name(tier)), false);
        return tier;
    }

    private static int set(CommandSourceStack source, Collection<ServerPlayer> targets, int tier) {
        for (ServerPlayer target : targets) {
            PlayerTier.set(target, tier);
        }
        source.sendSuccess(() -> Component.translatable("command.roll_mod.tier.set",
                targets.size(), tier, PlayerTier.name(tier)), true);
        return targets.size();
    }
}
