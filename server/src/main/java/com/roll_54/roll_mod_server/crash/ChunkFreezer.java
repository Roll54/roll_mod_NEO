package com.roll_54.roll_mod_server.crash;

import com.mojang.logging.LogUtils;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.minestar.moderation.ModerationService;
import com.roll_54.roll_mod_server.RollModServer;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Chunks that stopped ticking. A frozen chunk gets no entity ticks (players excepted), no block entity ticks,
 * no random ticks, no natural spawning, and its scheduled block/fluid ticks stay queued until it thaws. The gates
 * live in {@code ChunkFreezeLevelMixin} / {@code ChunkFreezeServerLevelMixin}.
 *
 * <p>Two reasons to freeze:
 * <ul>
 *   <li>{@link Reason#CRASH} — something ticking inside the chunk threw. Stays frozen until an op unfreezes it
 *       (or the server restarts), since whatever threw will most likely throw again.</li>
 *   <li>{@link Reason#MANUAL} — paused by staff with {@code /rollmod admin chunkfreeze pause}. Stays frozen until
 *       unfrozen by command (or a restart).</li>
 *   <li>{@link Reason#OVERLOAD} — at least {@code chunkGuard.entityLimit} non-player entities. Rescanned every
 *       {@code scanIntervalTicks}; thaws once the count drops below {@code resumeBelow}.</li>
 * </ul>
 * Main-thread only, so plain maps.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class ChunkFreezer {
    private ChunkFreezer() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    /** In ascending strength: a stronger reason replaces a weaker one, never the other way round. */
    public enum Reason { OVERLOAD, MANUAL, CRASH }

    public record Frozen(Reason reason, long sinceMillis, String detail) {}

    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Frozen>> FROZEN = new HashMap<>();

    private static final String LOG_FILE = "chunk_freeze.log";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------------------------------------------------------- queries (hot path)

    public static boolean isFrozen(Level level, long chunkPos) {
        if (FROZEN.isEmpty()) return false;
        Long2ObjectOpenHashMap<Frozen> map = FROZEN.get(level.dimension());
        return map != null && map.containsKey(chunkPos);
    }

    public static boolean isFrozen(Level level, BlockPos pos) {
        return !FROZEN.isEmpty() && isFrozen(level, ChunkPos.asLong(pos));
    }

    /** Whether this entity must skip its tick. Players always tick; dying mobs finish dying so kills clear a chunk. */
    public static boolean shouldSkip(Entity entity) {
        if (FROZEN.isEmpty() || entity instanceof ServerPlayer) return false;
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) return false;
        return isFrozen(entity.level(), entity.chunkPosition().toLong());
    }

    public static Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Frozen>> snapshot() {
        Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Frozen>> copy = new HashMap<>();
        FROZEN.forEach((dim, map) -> copy.put(dim, new Long2ObjectOpenHashMap<>(map)));
        return copy;
    }

    // ---------------------------------------------------------------- freezing

    /**
     * Called by the tick guards when something inside {@code chunkPos} threw. Returns {@code false} when crash
     * freezing is disabled, in which case the caller should rethrow so the outer guards handle it as before.
     */
    public static boolean freezeForCrash(Level level, long chunkPos, String what, Throwable t) {
        if (!(level instanceof ServerLevel serverLevel) || !MyConfig.INSTANCE.chunkGuard.freezeOnCrash) {
            return false;
        }
        CrashReporter.report(serverLevel.getServer(), t);
        String detail = what + " threw " + t;
        if (freeze(serverLevel, chunkPos, Reason.CRASH, detail)) {
            alertOps(serverLevel.getServer(), Component.literal("[roll_mod] Froze chunk ").withStyle(ChatFormatting.RED)
                    .append(chunkLink(serverLevel.dimension(), new ChunkPos(chunkPos)))
                    .append(Component.literal(" (crash): " + detail).withStyle(ChatFormatting.GRAY)));
        }
        return true;
    }

    /** Rethrows {@code t} unchecked so a tick wrapper can pass it on untouched: {@code throw ChunkFreezer.rethrow(t)}. */
    public static RuntimeException rethrow(Throwable t) {
        throw ChunkFreezer.<RuntimeException>sneaky(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneaky(Throwable t) throws T {
        throw (T) t;
    }

    /** Records the freeze; {@code true} if the chunk was not already frozen for this (or a stronger) reason. */
    private static boolean freeze(ServerLevel level, long chunkPos, Reason reason, String detail) {
        Long2ObjectOpenHashMap<Frozen> map = FROZEN.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap<>());
        Frozen previous = map.get(chunkPos);
        // Only overloads thaw by themselves, so a chunk that is also paused or crashed must not stay marked as one.
        if (previous != null && previous.reason().compareTo(reason) >= 0) return false;
        map.put(chunkPos, new Frozen(reason, System.currentTimeMillis(), detail));

        ChunkPos pos = new ChunkPos(chunkPos);
        LOGGER.warn("[roll_mod] Froze chunk {} in {} ({}): {}", pos, level.dimension().location(), reason, detail);
        log("froze", level.dimension(), pos, reason.name().toLowerCase() + ": " + detail);
        return true;
    }

    /**
     * Staff pause from {@code /rollmod admin chunkfreeze pause}. {@code false} if the chunk is already frozen for
     * this or a stronger reason (a crash).
     */
    public static boolean freezeManually(ServerLevel level, long chunkPos, String by) {
        return freeze(level, chunkPos, Reason.MANUAL, "paused by " + by);
    }

    public static boolean unfreeze(ServerLevel level, long chunkPos) {
        Long2ObjectOpenHashMap<Frozen> map = FROZEN.get(level.dimension());
        if (map == null || map.remove(chunkPos) == null) return false;
        if (map.isEmpty()) FROZEN.remove(level.dimension());
        LOGGER.info("[roll_mod] Unfroze chunk {} in {}", new ChunkPos(chunkPos), level.dimension().location());
        log("unfroze", level.dimension(), new ChunkPos(chunkPos), "by command");
        return true;
    }

    public static int unfreezeAll() {
        int count = FROZEN.values().stream().mapToInt(Long2ObjectOpenHashMap::size).sum();
        FROZEN.clear();
        return count;
    }

    // ---------------------------------------------------------------- overload scan

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MyConfig.ChunkGuardSettings cfg = MyConfig.INSTANCE.chunkGuard;
        int limit = cfg.entityLimit;
        Long2ObjectOpenHashMap<Frozen> frozen = FROZEN.get(level.dimension());
        if (limit <= 0 && frozen == null) return;
        if (level.getGameTime() % cfg.scanIntervalTicks != 0) return;

        Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ServerPlayer || entity.isRemoved()) continue;
            counts.addTo(entity.chunkPosition().toLong(), 1);
        }

        if (frozen != null) {
            int resumeBelow = limit <= 0 ? Integer.MAX_VALUE : Math.min(cfg.resumeBelow, limit);
            var it = frozen.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<Frozen> e = it.next();
                if (e.getValue().reason() == Reason.OVERLOAD && counts.get(e.getLongKey()) < resumeBelow) {
                    it.remove();
                    LOGGER.info("[roll_mod] Chunk {} in {} back under the entity limit, resumed", new ChunkPos(e.getLongKey()), level.dimension().location());
                    log("resumed", level.dimension(), new ChunkPos(e.getLongKey()), counts.get(e.getLongKey()) + " entities");
                }
            }
            if (frozen.isEmpty()) FROZEN.remove(level.dimension());
        }

        if (limit > 0) {
            for (var e : counts.long2IntEntrySet()) {
                if (e.getIntValue() >= limit
                        && freeze(level, e.getLongKey(), Reason.OVERLOAD, e.getIntValue() + " entities (limit " + limit + ")")) {
                    alertOverload(level, new ChunkPos(e.getLongKey()), e.getIntValue(), limit);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FROZEN.clear();
    }

    // ---------------------------------------------------------------- chat

    public static MutableComponent chunkLink(ResourceKey<Level> dimension, ChunkPos pos) {
        int x = pos.getMiddleBlockX();
        int z = pos.getMiddleBlockZ();
        String tp = "/execute in " + dimension.location() + " run tp @s " + x + " ~ " + z;
        return Component.literal("[" + pos.x + ", " + pos.z + " @ " + dimension.location() + "]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport (blocks " + x + ", " + z + ")"))));
    }

    /**
     * Same channel as the low-TPS alert ({@code TpsMonitor}): a red line to everyone who can moderate. Names the
     * most common entity type, which is usually the whole story (a mob farm, an item spill).
     */
    private static void alertOverload(ServerLevel level, ChunkPos pos, int count, int limit) {
        AABB box = new AABB(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1, level.getMaxBuildHeight(), pos.getMaxBlockZ() + 1);
        Map<EntityType<?>, Integer> byType = new HashMap<>();
        for (Entity entity : level.getEntities((Entity) null, box, e -> !(e instanceof ServerPlayer))) {
            byType.merge(entity.getType(), 1, Integer::sum);
        }
        Component mostly = byType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .<Component>map(top -> Component.translatable("msg.roll_mod.moderation.chunk.overload.mostly",
                        top.getKey().getDescription(), top.getValue()))
                .orElse(Component.empty());
        ModerationService.broadcastToModerators(level.getServer(), Component.translatable(
                        "msg.roll_mod.moderation.chunk.overload", chunkLink(level.dimension(), pos), count, limit, mostly)
                .withStyle(ChatFormatting.RED));
    }

    /**
     * A line in {@code minestar/chunk_freeze.log}, next to {@code tps.log}, so a freeze that happened overnight is
     * still answerable. A failed write is swallowed — a full disk must not turn a frozen chunk into a crash.
     */
    private static void log(String what, ResourceKey<Level> dimension, ChunkPos pos, String detail) {
        String line = "[%s] %s chunk %d,%d (blocks %d,%d) in %s: %s%n".formatted(
                LocalDateTime.now().format(TIMESTAMP), what, pos.x, pos.z, pos.getMiddleBlockX(), pos.getMiddleBlockZ(),
                dimension.location(), detail);
        try {
            Files.writeString(MinestarFiles.resolve(LOG_FILE), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.error("[roll_mod] could not write the chunk freeze log", e);
        }
    }

    private static void alertOps(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(message);
            }
        }
    }
}
