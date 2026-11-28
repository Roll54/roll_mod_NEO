package com.roll_54.roll_mod.prospecting;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The prospector's pick on the server: scans the column, records what the player found per chunk
 * ({@link ProspectData}) and sends the finds to the player ({@link ProspectPacket}), whose client outlines them.
 *
 * <p>The column is {@code (2 * radius + 1)²} blocks around the block hit, from it down to bedrock, or — used on a
 * ceiling — from it up to the top of the world. For every chunk the column touches, each ore found in it is recorded
 * with its height ranges (stone and deepslate apart) and its whole vein in that chunk: how many blocks of it the
 * chunk holds and where, for the map's ore layer.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ProspectService {

    private static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    /** Mint green, the pick's working light. */
    private static final DustParticleOptions MINT = new DustParticleOptions(new Vector3f(0.243f, 0.902f, 0.690f), 1.0f);

    private ProspectService() {}

    /** Scans and records; {@code up}: used on a ceiling, so the column runs up instead of down. */
    public static void prospect(ServerPlayer player, BlockPos hit, boolean up, int radius) {
        ServerLevel level = player.serverLevel();
        int minX = hit.getX() - radius, maxX = hit.getX() + radius;
        int minZ = hit.getZ() - radius, maxZ = hit.getZ() + radius;
        int minY = up ? hit.getY() - 1 : level.getMinBuildHeight();
        int maxY = up ? level.getMaxBuildHeight() - 1 : hit.getY() + 1;
        minY = Math.max(minY, level.getMinBuildHeight());
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);

        // What the column holds: per ore, its ranges, and which chunks it was met in.
        Map<String, Discovery> column = new HashMap<>();
        Map<String, Integer> nearest = new HashMap<>();
        Map<Long, Set<String>> perChunk = new HashMap<>();
        for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
                if (!(level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false)
                        instanceof LevelChunk chunk)) continue;
                long chunkKey = ChunkPos.asLong(cx, cz);
                int x0 = Math.max(minX, cx << 4), x1 = Math.min(maxX, (cx << 4) + 15);
                int z0 = Math.max(minZ, cz << 4), z1 = Math.min(maxZ, (cz << 4) + 15);
                forEachOre(chunk, minY, maxY, x0, x1, z0, z1, (state, x, y, z) -> {
                    OreKind kind = OreKind.of(state.getBlock());
                    Discovery d = column.computeIfAbsent(kind.key(), k -> new Discovery(k, kind.rawItem(),
                            BuiltInRegistries.BLOCK.getKey(state.getBlock())));
                    if (!kind.deepslate()) d.displayBlock = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    d.include(kind.deepslate(), y);
                    nearest.merge(kind.key(), Math.abs(y - hit.getY()), Math::min);
                    perChunk.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(kind.key());
                });
            }
        }

        // Each chunk the column touched: its ores' whole veins, merged into what the player already knew there.
        ProspectData data = ProspectData.get(player.server);
        String dim = level.dimension().location().toString();
        List<ProspectPacket.ChunkFinds> changed = new ArrayList<>();
        perChunk.forEach((chunkKey, keys) -> {
            ChunkPos pos = new ChunkPos(chunkKey);
            if (!(level.getChunk(pos.x, pos.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false)
                    instanceof LevelChunk chunk)) return;
            Map<String, Discovery> known = data.chunk(player.getUUID(), dim, chunkKey);
            Map<String, List<Integer>> veins = new HashMap<>();
            Map<String, Integer> counts = new HashMap<>();
            int bx = pos.getMinBlockX(), bz = pos.getMinBlockZ();
            forEachOre(chunk, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1, bx, bx + 15, bz, bz + 15,
                    (state, x, y, z) -> {
                        OreKind kind = OreKind.of(state.getBlock());
                        if (!keys.contains(kind.key())) return;
                        counts.merge(kind.key(), 1, Integer::sum);
                        List<Integer> list = veins.computeIfAbsent(kind.key(), k -> new ArrayList<>());
                        if (list.size() < Discovery.MAX_POSITIONS) list.add(Discovery.pack(x, y, z));
                    });
            for (String key : keys) {
                Discovery found = column.get(key);
                Discovery d = known.computeIfAbsent(key, k -> new Discovery(k, found.rawItem, found.displayBlock));
                d.rawItem = found.rawItem;
                d.displayBlock = found.displayBlock;
                if (found.hasStone()) {
                    d.include(false, found.stoneMin);
                    d.include(false, found.stoneMax);
                }
                if (found.hasDeepslate()) {
                    d.include(true, found.deepMin);
                    d.include(true, found.deepMax);
                }
                d.veinCount = counts.getOrDefault(key, 0);
                d.positions = veins.getOrDefault(key, List.of()).stream().mapToInt(Integer::intValue).toArray();
            }
            changed.add(new ProspectPacket.ChunkFinds(dim, chunkKey, List.copyOf(known.values())));
        });
        if (!perChunk.isEmpty()) data.setDirty();

        List<Discovery> report = new ArrayList<>(column.values());
        report.sort((a, b) -> Integer.compare(nearest.get(a.oreKey), nearest.get(b.oreKey)));
        // The finds are outlined on the client (ClientProspects.highlight) instead of the old particle cloud.
        PacketDistributor.sendToPlayer(player, new ProspectPacket(false, changed, report));
    }

    /**
     * A cloud of mint green in the first blocks of the column, seen by everyone near.
     *
     * @deprecated replaced by the client's outline of the found ore blocks; kept, no longer called.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static void particles(ServerLevel level, BlockPos hit, boolean up, int radius) {
        double cx = hit.getX() + 0.5, cz = hit.getZ() + 0.5;
        double cy = hit.getY() + (up ? 0.0 : 1.0);
        // Across the column's width, a few blocks into the direction it runs.
        level.sendParticles(MINT, cx, cy + (up ? 1.5 : -1.5), cz, 60, radius * 0.6, 1.5, radius * 0.6, 0.0);
        level.sendParticles(MINT, cx, cy, cz, 20, radius * 0.6, 0.2, radius * 0.6, 0.0);
    }

    private interface OreVisitor {
        void visit(BlockState state, int x, int y, int z);
    }

    /** Every ore block in the box, skipping chunk sections whose palette holds none. */
    private static void forEachOre(LevelChunk chunk, int minY, int maxY, int x0, int x1, int z0, int z1, OreVisitor visitor) {
        for (int sy = SectionPos.blockToSectionCoord(minY); sy <= SectionPos.blockToSectionCoord(maxY); sy++) {
            int index = chunk.getSectionIndexFromSectionY(sy);
            if (index < 0 || index >= chunk.getSectionsCount()) continue;
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(C_ORES))) continue;
            int y0 = Math.max(minY, sy << 4), y1 = Math.min(maxY, (sy << 4) + 15);
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                        if (state.is(C_ORES)) visitor.visit(state, x, y, z);
                    }
                }
            }
        }
    }

    /** Everything the player has found, on login. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<ProspectPacket.ChunkFinds> all = new ArrayList<>();
        ProspectData.get(player.server).all(player.getUUID()).forEach((dim, chunks) -> chunks.forEach((chunk, ores) ->
                all.add(new ProspectPacket.ChunkFinds(dim, chunk, List.copyOf(ores.values())))));
        PacketDistributor.sendToPlayer(player, new ProspectPacket(true, all, null));
    }

    /** How many blocks of a vein a player mines before the whole vein is opened up to them. */
    public static final int REVEAL_AFTER = 3;
    /** How far (in chunks, each way) and how many chunks a revealed vein is followed. */
    private static final int REVEAL_RADIUS = 8, REVEAL_MAX_CHUNKS = 64;
    /**
     * Ores mined in chunks the player never prospected, per player, dimension, chunk and ore: they count towards
     * {@link #REVEAL_AFTER} too. In memory only; a restart starts the count over.
     */
    private static final Map<UUID, Map<String, Integer>> UNRECORDED = new HashMap<>();

    /**
     * A player mining an ore: one more mined from that vein, and the block off the map. The {@link #REVEAL_AFTER}th
     * block mined from it, prospected or not, opens up the whole vein ({@link #revealVein}).
     */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !event.getState().is(C_ORES)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        String dim = level.dimension().location().toString();
        long chunkKey = ChunkPos.asLong(pos);
        String oreKey = OreKind.of(event.getState().getBlock()).key();
        ProspectData data = ProspectData.get(player.server);
        Map<String, Discovery> ores = data.chunkOrNull(player.getUUID(), dim, chunkKey);
        Discovery d = ores == null ? null : ores.get(oreKey);
        if (d == null) {
            String counter = dim + "|" + chunkKey + "|" + oreKey;
            int mined = UNRECORDED.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).merge(counter, 1, Integer::sum);
            if (mined >= REVEAL_AFTER) {
                UNRECORDED.get(player.getUUID()).remove(counter);
                revealVein(player, level, new ChunkPos(pos), oreKey, pos, mined);
            }
            return;
        }
        d.mine(Discovery.pack(pos.getX(), pos.getY(), pos.getZ()));
        data.setDirty();
        if (!d.revealed && d.mined >= REVEAL_AFTER) {
            revealVein(player, level, new ChunkPos(pos), oreKey, pos, d.mined);
            return;
        }
        PacketDistributor.sendToPlayer(player, new ProspectPacket(false,
                List.of(new ProspectPacket.ChunkFinds(dim, chunkKey, List.copyOf(ores.values()))), null));
    }

    /**
     * Opens up the vein {@code oreKey} belongs to: from {@code start}, every neighbouring loaded chunk (diagonals too)
     * that holds that ore, within {@link #REVEAL_RADIUS} and at most {@link #REVEAL_MAX_CHUNKS}. In each, the vein's
     * ores (those the starting chunk holds alongside it) are recorded in full, as the pick records them, and marked
     * revealed so the map shows their counts.
     *
     * @param brokenPos the block being broken right now: still in the world during the event, so left out of the scan
     * @param minedHere how many of the ore were mined in the starting chunk, this one included
     */
    private static void revealVein(ServerPlayer player, ServerLevel level, ChunkPos start, String oreKey,
                                   BlockPos brokenPos, int minedHere) {
        Map<Long, Map<String, Discovery>> scanned = new HashMap<>();
        Map<String, Discovery> first = scanChunk(level, start, brokenPos);
        if (first == null) return;
        Set<String> veinOres = new HashSet<>(first.keySet());
        veinOres.add(oreKey);
        scanned.put(start.toLong(), first);
        java.util.ArrayDeque<ChunkPos> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && scanned.size() < REVEAL_MAX_CHUNKS) {
            ChunkPos at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ChunkPos next = new ChunkPos(at.x + dx, at.z + dz);
                    if (scanned.containsKey(next.toLong()) || Math.abs(next.x - start.x) > REVEAL_RADIUS
                            || Math.abs(next.z - start.z) > REVEAL_RADIUS || scanned.size() >= REVEAL_MAX_CHUNKS) continue;
                    Map<String, Discovery> found = scanChunk(level, next, brokenPos);
                    if (found == null || !found.containsKey(oreKey)) continue;
                    scanned.put(next.toLong(), found);
                    queue.add(next);
                }
            }
        }

        ProspectData data = ProspectData.get(player.server);
        String dim = level.dimension().location().toString();
        List<ProspectPacket.ChunkFinds> changed = new ArrayList<>();
        scanned.forEach((chunkKey, found) -> {
            Map<String, Discovery> known = data.chunk(player.getUUID(), dim, chunkKey);
            found.forEach((key, f) -> {
                if (!veinOres.contains(key)) return;
                Discovery d = known.computeIfAbsent(key, k -> new Discovery(k, f.rawItem, f.displayBlock));
                d.rawItem = f.rawItem;
                d.displayBlock = f.displayBlock;
                d.stoneMin = f.stoneMin;
                d.stoneMax = f.stoneMax;
                d.deepMin = f.deepMin;
                d.deepMax = f.deepMax;
                d.veinCount = f.veinCount;
                d.positions = f.positions;
                d.revealed = true;
            });
            if (chunkKey == start.toLong() && brokenPos != null) {
                Discovery d = known.computeIfAbsent(oreKey, k -> {
                    OreKind kind = OreKind.of(level.getBlockState(brokenPos).getBlock());
                    return new Discovery(k, kind.rawItem(), BuiltInRegistries.BLOCK.getKey(level.getBlockState(brokenPos).getBlock()));
                });
                d.mined = Math.max(d.mined, minedHere);
                d.revealed = true;
            }
            changed.add(new ProspectPacket.ChunkFinds(dim, chunkKey, List.copyOf(known.values())));
        });
        data.setDirty();
        PacketDistributor.sendToPlayer(player, new ProspectPacket(false, changed, null));
        Discovery main = data.chunk(player.getUUID(), dim, start.toLong()).get(oreKey);
        if (main != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.roll_mod.prospector.vein_revealed",
                    net.minecraft.network.chat.Component.translatable(
                            BuiltInRegistries.BLOCK.get(main.displayBlock).getDescriptionId()),
                    scanned.size()));
        }
    }

    /**
     * Every ore in a loaded chunk, top to bottom, as the pick records it: heights per layer, count and positions.
     * {@code null} when the chunk is not loaded. {@code skip} (a block being broken) is left out.
     */
    private static Map<String, Discovery> scanChunk(ServerLevel level, ChunkPos pos, BlockPos skip) {
        if (!(level.getChunk(pos.x, pos.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false)
                instanceof LevelChunk chunk)) return null;
        Map<String, Discovery> found = new HashMap<>();
        Map<String, List<Integer>> positions = new HashMap<>();
        int bx = pos.getMinBlockX(), bz = pos.getMinBlockZ();
        forEachOre(chunk, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1, bx, bx + 15, bz, bz + 15,
                (state, x, y, z) -> {
                    if (skip != null && skip.getX() == x && skip.getY() == y && skip.getZ() == z) return;
                    OreKind kind = OreKind.of(state.getBlock());
                    Discovery d = found.computeIfAbsent(kind.key(), k -> new Discovery(k, kind.rawItem(),
                            BuiltInRegistries.BLOCK.getKey(state.getBlock())));
                    if (!kind.deepslate()) d.displayBlock = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    d.include(kind.deepslate(), y);
                    d.veinCount++;
                    List<Integer> list = positions.computeIfAbsent(kind.key(), k -> new ArrayList<>());
                    if (list.size() < Discovery.MAX_POSITIONS) list.add(Discovery.pack(x, y, z));
                });
        found.forEach((key, d) -> d.positions = positions.get(key).stream().mapToInt(Integer::intValue).toArray());
        return found;
    }

    /** Forget a player's unrecorded counts once they leave. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UNRECORDED.remove(event.getEntity().getUUID());
    }
}
