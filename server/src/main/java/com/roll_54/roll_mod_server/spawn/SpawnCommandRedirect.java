package com.roll_54.roll_mod_server.spawn;

import com.mojang.brigadier.context.ParsedCommandNode;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.List;

/**
 * Redirects {@code /spawn} (from FTB Essentials or any other mod) to the dimension configured via
 * {@code /setworldspawn <pos> <angle> <dimension>} (persisted in {@link WorldSpawnState}) instead of
 * the player's bed / the overworld world-spawn that FTB Essentials targets by default.
 *
 * <p>Implemented as a {@link CommandEvent} interceptor rather than a Mixin into FTB Essentials' command
 * class: this needs no compile/runtime dependency on FTB and survives FTB version bumps, since it only
 * keys off the top-level command literal ({@code "spawn"}) at the stable NeoForge command-dispatch layer.
 *
 * <p>Only the bare self-targeted {@code /spawn} is handled; any variant with extra arguments
 * (e.g. an op teleporting another player) is left to the original command.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class SpawnCommandRedirect {

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        List<? extends ParsedCommandNode<CommandSourceStack>> nodes =
                event.getParseResults().getContext().getNodes();
        // Match exactly `/spawn` with no further arguments; let `/spawn <player>` etc. fall through.
        if (nodes.size() != 1 || !nodes.get(0).getNode().getName().equals("spawn")) return;

        CommandSourceStack source = event.getParseResults().getContext().getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return; // console / command block — leave the original command alone

        MinecraftServer server = source.getServer();
        ResourceKey<Level> dim = WorldSpawnState.get(server).spawnDimension;
        ServerLevel target = server.getLevel(dim);
        if (target == null) return; // configured dimension not loaded — fall back to the original command

        event.setCanceled(true);

        BlockPos pos = target.getSharedSpawnPos();
        player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                target.getSharedSpawnAngle(), 0.0F);
        source.sendSuccess(() -> Component.literal("Teleported to spawn ("
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + " in " + target.dimension().location() + ")"), false);
    }
}
