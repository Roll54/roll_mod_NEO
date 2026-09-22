package com.roll_54.roll_mod.minestar.rtp;

import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.minestar.data.PlayerPositions;
import com.roll_54.roll_mod.minestar.teleport.TeleportService;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.SafeSpot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code /rtp} — a random spot in the world, found without stalling the server.
 *
 * <p>FTB Essentials used to do this. The search is the whole difficulty: a candidate position is
 * worthless until its chunk exists, and generating a chunk can cost tens of milliseconds, so trying
 * a hundred of them inside the command would freeze the server. Instead a search is spread over
 * ticks, and each candidate is first tested against the generator's own height and biome data,
 * which costs no chunk at all. Only the few that survive that are worth loading.
 *
 * <p>The cooldown is stamped on arrival, not on the request: a search that finds nothing has cost
 * the player their wait for nothing, and charging them for it as well would be unkind.
 */
public final class RtpService {

    /**
     * How many players may be searching at once.
     *
     * <p>Each one may generate {@code triesPerTick} chunks in a tick, so without a cap a popular
     * server could spend an entire tick on nothing but random teleports.
     */
    private static final int MAX_CONCURRENT = 4;

    /** How far below the generator's surface reading a standable block may still be. */
    private static final int SURFACE_DEPTH = 24;

    /**
     * How far inside the world border a random teleport may still land.
     *
     * <p>Not zero: the border pushes anything touching it back, and arriving with the wall in your
     * face is not the welcome the command is for.
     */
    private static final int BORDER_MARGIN = 16;

    private record Search(ResourceKey<Level> level, int triesLeft) {}

    private static final Map<UUID, Search> SEARCHING = new ConcurrentHashMap<>();

    private RtpService() {}

    /* -------------------------------------------- request ------------------------------------------- */

    /**
     * Starts a search, or explains why it will not.
     *
     * @return whether a search was started.
     */
    public static boolean request(ServerPlayer player) {
        if (SEARCHING.containsKey(player.getUUID())) {
            refuse(player, "msg.roll_mod.rtp.searching");
            return false;
        }

        long remaining = cooldownRemainingMillis(player);
        if (remaining > 0) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.rtp.cooldown",
                    Durations.format(remaining)).withStyle(ChatFormatting.RED));
            return false;
        }

        ServerLevel level = target(player);
        if (level == null) {
            refuse(player, "msg.roll_mod.rtp.nowhere");
            return false;
        }

        if (SEARCHING.size() >= MAX_CONCURRENT) {
            refuse(player, "msg.roll_mod.rtp.busy");
            return false;
        }

        // Out of the hub and back to the world, the same reason warps close it: the countdown and
        // the search line both run over the hotbar, which an open hub covers.
        player.closeContainer();

        if (level != player.level()) {
            // Blacklisted dimension: the overworld is where the spread happens, so go there first.
            BlockPos spawn = level.getSharedSpawnPos();
            TeleportService.teleport(player, level, spawn.getX() + 0.5, spawn.getY(),
                    spawn.getZ() + 0.5, level.getSharedSpawnAngle(), 0.0F);
        }

        SEARCHING.put(player.getUUID(), new Search(level.dimension(), MyConfig.INSTANCE.rtp.maxTries.get()));
        return true;
    }

    /** Drops a player's search, for logout and for {@code /rollmod reload}. */
    public static void cancel(UUID player) {
        SEARCHING.remove(player);
    }

    /* --------------------------------------------- search ------------------------------------------- */

    /**
     * Advances every search. Called once a tick alongside {@link TeleportService#tick}.
     *
     * <p>Over a copy of the keys, with {@code remove} and {@code replace} rather than
     * {@code entrySet().removeIf}: a {@link ConcurrentHashMap} hands that predicate an immutable
     * entry, so {@code setValue} on it throws {@link UnsupportedOperationException} — and it threw
     * out of the server tick event, taking every listener after this one with it.
     *
     * <p>{@code replace}, not {@code put}: a player who logs out while their own search is being
     * advanced has already been dropped by {@link #cancel}, and {@code put} would bring the entry
     * back for a search nobody is waiting on.
     */
    public static void tick(MinecraftServer server) {
        if (SEARCHING.isEmpty()) return;

        int perTick = MyConfig.INSTANCE.rtp.triesPerTick.get();

        for (UUID id : new ArrayList<>(SEARCHING.keySet())) {
            Search search = SEARCHING.get(id);
            if (search == null) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(id);
            ServerLevel level = server.getLevel(search.level());
            if (player == null || level == null) {
                SEARCHING.remove(id);
                continue;
            }

            player.displayClientMessage(Component.translatable("msg.roll_mod.rtp.searching")
                    .withStyle(ChatFormatting.GREEN), true);

            int tries = Math.min(perTick, search.triesLeft());
            BlockPos found = null;
            for (int i = 0; i < tries && found == null; i++) {
                found = candidate(level);
            }

            if (found != null) {
                SEARCHING.remove(id);
                arrive(player, level, found);
                continue;
            }

            int left = search.triesLeft() - tries;
            if (left <= 0) {
                SEARCHING.remove(id);
                player.sendSystemMessage(Component.translatable("msg.roll_mod.rtp.failed")
                        .withStyle(ChatFormatting.RED));
                continue;
            }
            SEARCHING.replace(id, new Search(search.level(), left));
        }
    }

    /**
     * One candidate position, or {@code null} when it did not pass.
     *
     * <p>The radius is square-rooted so the points are spread evenly by area; picking the radius
     * uniformly would bunch everybody near the inner ring.
     *
     * <p>The world border is a hard ceiling on the radius, not merely a filter: a border smaller
     * than the configured radius would otherwise throw away nearly every candidate and leave the
     * player with "found nowhere safe" after a hundred tries. So the configured radii are first
     * clamped to what fits inside the border, and the point is checked against it afterwards
     * anyway, because the border can be off-centre or moving.
     */
    private static BlockPos candidate(ServerLevel level) {
        MyConfig.RtpSettings config = MyConfig.INSTANCE.rtp;
        WorldBorder border = level.getWorldBorder();

        // Spawn, unless spawn itself is outside the border, in which case there is no sense
        // measuring from it — the border's own middle is the only place every direction has room.
        BlockPos spawn = level.getSharedSpawnPos();
        double originX = spawn.getX();
        double originZ = spawn.getZ();
        if (!border.isWithinBounds(originX, originZ)) {
            originX = border.getCenterX();
            originZ = border.getCenterZ();
        }

        // The biggest circle around the origin that still fits: distance to the nearest wall.
        double room = Math.min(
                Math.min(originX - border.getMinX(), border.getMaxX() - originX),
                Math.min(originZ - border.getMinZ(), border.getMaxZ() - originZ)) - BORDER_MARGIN;
        if (room < 1) return null;

        int max = (int) Math.min(Math.max(1, config.maxRadius.get()), room);
        int min = Math.min(Math.max(0, config.minRadius.get()), max - 1);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radius = min + (max - min) * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Math.PI * 2;

        int x = (int) Math.round(originX + Math.cos(angle) * radius);
        int z = (int) Math.round(originZ + Math.sin(angle) * radius);

        // Negative offset shrinks the bounds rather than widening them, so this is the margin.
        if (!border.isWithinBounds(x, z, -BORDER_MARGIN)) return null;

        if (!promising(level, x, z)) return null;

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                level.getChunkSource().randomState());

        // Only now is a chunk worth generating.
        level.getChunk(x >> 4, z >> 4);
        return SafeSpot.inColumnFrom(level, x, z, surface + 4, SURFACE_DEPTH);
    }

    /**
     * The cheap half of the test: the generator can say how high the land is and which biome is
     * there without a chunk existing, which throws out oceans, rivers and the void for free.
     */
    private static boolean promising(ServerLevel level, int x, int z) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();

        int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        if (surface <= level.getMinBuildHeight() + 8) return false;
        // Nether has no meaningful sea level, and its surface reading is the roof, so skip the test.
        if (level.dimension() != Level.NETHER && surface < level.getSeaLevel() + 2) return false;

        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(surface), QuartPos.fromBlock(z),
                randomState.sampler());
        return !biome.is(BiomeTags.IS_OCEAN)
                && !biome.is(BiomeTags.IS_DEEP_OCEAN)
                && !biome.is(BiomeTags.IS_RIVER);
    }

    private static void arrive(ServerPlayer player, ServerLevel level, BlockPos spot) {
        double x = spot.getX() + 0.5;
        double z = spot.getZ() + 0.5;
        TeleportService.schedule(player, arriving -> {
            TeleportService.teleport(arriving, level, x, spot.getY(), z,
                    arriving.getYRot(), arriving.getXRot());
            PlayerPositions.rtpUsed(arriving.getUUID(), System.currentTimeMillis());
            arriving.sendSystemMessage(Component.translatable("msg.roll_mod.rtp.arrived",
                    spot.getX(), spot.getY(), spot.getZ()));
        });
    }

    /* -------------------------------------------- cooldown ------------------------------------------ */

    /** Milliseconds left before this player may use {@code /rtp} again; {@code 0} when ready. */
    public static long cooldownRemainingMillis(ServerPlayer player) {
        if (LuckPermsCompat.canBypassRtpCooldown(player)) return 0L;

        Integer meta = LuckPermsCompat.rtpCooldownTicks(player);
        long ticks = meta != null ? meta : MyConfig.INSTANCE.rtp.cooldownTicks.get();
        if (ticks <= 0) return 0L;

        long last = PlayerPositions.rtpLastUsed(player.getUUID());
        if (last <= 0) return 0L;

        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0L, ticks * 50L - elapsed);
    }

    /* --------------------------------------------- helpers ------------------------------------------ */

    /**
     * Where the spread should happen: here, unless this dimension is blacklisted, in which case the
     * overworld — or nowhere at all, when the overworld is blacklisted too.
     */
    private static ServerLevel target(ServerPlayer player) {
        Set<ResourceLocation> blacklist = blacklist();
        if (!blacklist.contains(player.level().dimension().location())) {
            return player.serverLevel();
        }
        if (blacklist.contains(Level.OVERWORLD.location())) return null;
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, Level.OVERWORLD.location()));
    }

    private static Set<ResourceLocation> blacklist() {
        Set<ResourceLocation> set = new HashSet<>();
        for (String id : MyConfig.INSTANCE.rtp.blacklistedDimensions) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null) set.add(parsed);
        }
        return set;
    }

    private static void refuse(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

}
