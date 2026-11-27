package com.roll_54.roll_mod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

/**
 * {@code /rollmod admin forceloads add <players> [amount]} — gives players more force-loaded chunks in Open Parties and
 * Claims: {@code amount} (1 when left out) is added to their OPAC "bonus chunk forceloads", on top of what they had.
 * {@code /rollmod admin forceloads remove <players> [amount]} takes them away again, the bonus never below 0. OPAC keeps
 * the bonus in {@code <world>/data/openpartiesandclaims/player-configs/<uuid>.toml} ({@code bonusChunkForceloads}).
 * Operators only. Does nothing without OPAC.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class OpacForceloadCommand {

    private OpacForceloadCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("forceloads")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> add(ctx, 1))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                        .executes(ctx -> add(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "amount"))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> add(ctx, -1))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                        .executes(ctx -> add(ctx,
                                                                -IntegerArgumentType.getInteger(ctx, "amount")))))))));
    }

    /** Adds {@code amount} to each player's bonus forceloads; a negative one takes them away, down to 0 at most. */
    private static int add(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        if (!ModList.get().isLoaded("openpartiesandclaims")) {
            source.sendFailure(Component.translatable("command.roll_mod.forceloads.no_opac"));
            return 0;
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        int done = 0;
        for (ServerPlayer player : players) {
            int total;
            try {
                total = Opac.addBonus(player, amount);
            } catch (RuntimeException | LinkageError e) {
                RollMod.LOGGER.warn("[Forceloads] could not change the OPAC bonus forceloads of {}",
                        player.getGameProfile().getName(), e);
                total = -1;
            }
            if (total < 0) {
                source.sendFailure(Component.translatable("command.roll_mod.forceloads.failed",
                        player.getDisplayName()));
                continue;
            }
            done++;
            int bonus = total;
            if (amount > 0) {
                source.sendSuccess(() -> Component.translatable("command.roll_mod.forceloads.added", amount,
                        player.getDisplayName(), bonus), true);
                player.sendSystemMessage(Component.translatable("command.roll_mod.forceloads.received", amount, bonus));
            } else {
                source.sendSuccess(() -> Component.translatable("command.roll_mod.forceloads.removed", -amount,
                        player.getDisplayName(), bonus), true);
                player.sendSystemMessage(Component.translatable("command.roll_mod.forceloads.lost", -amount, bonus));
            }
        }
        return done;
    }

    /** Kept apart so OPAC's classes are only resolved with it loaded. */
    private static final class Opac {
        /** Adds {@code delta} to the player's bonus forceloads; the new bonus, or -1 when OPAC refused it. */
        static int addBonus(ServerPlayer player, int delta) {
            var option = xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions.BONUS_CHUNK_FORCELOADS;
            var config = xaero.pac.common.server.api.OpenPACServerAPI.get(player.server).getPlayerConfigManager()
                    .getLoadedConfig(player.getUUID());
            Integer raw = config.getRaw(option);
            int next = Math.max(0, (raw == null ? 0 : raw) + delta);
            var result = config.tryToSet(option, next);
            if (result != xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI.SetResult.SUCCESS) {
                RollMod.LOGGER.warn("[Forceloads] OPAC refused bonus forceloads {} for {}: {}", next,
                        player.getGameProfile().getName(), result);
                return -1;
            }
            return next;
        }
    }
}
