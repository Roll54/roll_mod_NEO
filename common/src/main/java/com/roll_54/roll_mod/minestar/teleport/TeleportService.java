package com.roll_54.roll_mod.minestar.teleport;

import com.roll_54.roll_mod.minestar.data.PlayerPositions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Every delayed teleport in the mod, and the countdown that goes with it.
 *
 * <p>Warps grew this first; kits, {@code /rtp}, {@code /back}, {@code /spawn} and tpa all want the
 * same thing, so it lives here once. A caller says how long to wait and what to do when the wait is
 * over, and this keeps the green countdown over the player's hotbar until then.
 *
 * <p>Nothing cancels a started teleport but leaving the server — the same rule warps settled on. The
 * wait exists to be seen, not to be interrupted.
 */
public final class TeleportService {

    /** The pause before a teleport fires. Short enough to bear, long enough to read the countdown. */
    public static final int WARMUP_SECONDS = 3;

    /** A teleport waiting out its warm-up. */
    private record Pending(Consumer<ServerPlayer> arrive, long readyAtTick) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private TeleportService() {}

    /**
     * Starts the countdown. A second call for the same player replaces the first, so a player who
     * clicks twice arrives once.
     */
    public static void schedule(ServerPlayer player, int warmupSeconds, Consumer<ServerPlayer> arrive) {
        PENDING.put(player.getUUID(),
                new Pending(arrive, player.server.getTickCount() + warmupSeconds * 20L));
    }

    /** As {@link #schedule}, with the standard wait. */
    public static void schedule(ServerPlayer player, Consumer<ServerPlayer> arrive) {
        schedule(player, WARMUP_SECONDS, arrive);
    }

    /** Whether the player already has a teleport counting down. */
    public static boolean isPending(UUID player) {
        return PENDING.containsKey(player);
    }

    public static void cancel(UUID player) {
        PENDING.remove(player);
    }

    /**
     * Moves the player, remembering where they were so {@code /back} can undo it.
     *
     * <p>Every teleport this mod performs goes through here, which is the whole reason {@code /back}
     * needs no hooks of its own.
     */
    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z,
                                float yaw, float pitch) {
        PlayerPositions.rememberBack(player);
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    /**
     * Advances every warm-up. Called once a tick from {@code HubEvents}. The action bar fades on its
     * own, so only a resend holds the countdown there.
     */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;

        PENDING.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) return true;

            Pending pending = entry.getValue();
            long remaining = pending.readyAtTick() - server.getTickCount();
            if (remaining > 0) {
                // A tenth of a second is two ticks, so a resend in between would show the same
                // number twice. Rounding up keeps the first frame at the full wait and the last at
                // 0.1 rather than a flash of 0.0.
                if (remaining % 2 == 0) {
                    player.displayClientMessage(countdown(remaining), true);
                }
                return false;
            }

            pending.arrive().accept(player);
            return true;
        });
    }

    /* ------------------------------------------ destinations ---------------------------------------- */

    /**
     * Sends the player back to where they last were, if there is such a place.
     *
     * @return whether a trip was started.
     */
    public static boolean back(ServerPlayer player) {
        PlayerPositions.Spot spot = PlayerPositions.back(player.getUUID());
        if (spot == null) {
            refuse(player, "msg.roll_mod.back.nowhere");
            return false;
        }

        ServerLevel level = spot.level(player.server);
        if (level == null) {
            // The dimension the spot names is gone — a removed mod, a renamed id. Drop the spot
            // rather than offering it again every time.
            PlayerPositions.clearBack(player.getUUID());
            refuse(player, "msg.roll_mod.back.noDimension");
            return false;
        }

        player.closeContainer();
        schedule(player, arriving -> {
            teleport(arriving, level, spot.x(), spot.y(), spot.z(), spot.yaw(), spot.pitch());
            arriving.sendSystemMessage(Component.translatable("msg.roll_mod.back.arrived"));
        });
        return true;
    }

    /**
     * Sends the player to the world spawn — the coordinates {@code /setworldspawn} writes, so moving
     * spawn moves this with it.
     *
     * <p>No warm-up: spawn is the one destination nobody is escaping to, and the wait bought nothing
     * but three seconds of standing still.
     */
    public static boolean spawn(ServerPlayer player) {
        ServerLevel level = player.server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();

        player.closeContainer();
        teleport(player, level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                level.getSharedSpawnAngle(), 0.0F);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.spawn.arrived"));
        return true;
    }

    private static void refuse(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    /** The green hotbar line, exposed so a caller can show the same wording for an instant trip. */
    public static Component countdown(long remainingTicks) {
        String seconds = String.format(Locale.ROOT, "%.1f", Math.ceil(remainingTicks / 2.0) / 10.0);
        return Component.translatable("msg.roll_mod.warp.countdown", seconds)
                .withStyle(ChatFormatting.GREEN);
    }
}
