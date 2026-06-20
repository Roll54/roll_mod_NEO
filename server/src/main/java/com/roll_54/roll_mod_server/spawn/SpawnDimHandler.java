package com.roll_54.roll_mod_server.spawn;

import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

/**
 * Sends players to the dimension configured via {@code /setworldspawn <pos> <angle> <dimension>}
 * (persisted in {@link WorldSpawnState}) on first join and on bedless death-respawn.
 *
 * <p>The world-spawn <em>coordinates</em> are vanilla's and are already shared with the target
 * dimension (see {@link WorldSpawnState}); here we only override the <em>dimension</em>. Beds and
 * respawn anchors keep working untouched, and returning from the End is left vanilla.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public class SpawnDimHandler {

    /** Persisted-data flag marking that a player has already had their first-join spawn handled. */
    private static final String SPAWNED_FLAG = "roll_mod_first_spawn_done";

    /**
     * Bedless respawn (death without a working bed/anchor) would land the player at the overworld
     * world spawn — redirect those to the configured spawn dimension instead.
     */
    @SubscribeEvent
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (event.isFromEndFight()) return; // leave the End-credits return vanilla
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // A valid bed/anchor sets a respawn position and does not flag a missing respawn block.
        boolean hasValidRespawnBlock =
                player.getRespawnPosition() != null && !event.getDimensionTransition().missingRespawnBlock();
        if (hasValidRespawnBlock) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ResourceKey<Level> dim = WorldSpawnState.get(server).spawnDimension;
        if (dim.equals(Level.OVERWORLD)) return; // vanilla already sends bedless respawns here

        ServerLevel target = server.getLevel(dim);
        if (target == null) return;

        event.setDimensionTransition(new DimensionTransition(
                target,
                target.getSharedSpawnPos().getBottomCenter(),
                Vec3.ZERO,
                target.getSharedSpawnAngle(),
                0.0F,
                DimensionTransition.DO_NOTHING
        ));
    }

    /** First-ever join: vanilla places new players at the overworld spawn; move them to the configured dim. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(SPAWNED_FLAG)) return; // already handled on an earlier first join
        data.putBoolean(SPAWNED_FLAG, true);

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ResourceKey<Level> dim = WorldSpawnState.get(server).spawnDimension;
        if (dim.equals(Level.OVERWORLD)) return; // already spawned in the overworld

        ServerLevel target = server.getLevel(dim);
        if (target == null) return;

        BlockPos spawn = target.getSharedSpawnPos();
        player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                target.getSharedSpawnAngle(), 0.0F);
    }
}