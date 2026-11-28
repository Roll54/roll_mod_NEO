package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.prospecting.ClientProspects;
import com.roll_54.roll_mod.prospecting.Discovery;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What both maps draw as one element: one ore vein over neighbouring prospected chunks (sides and corners), merged. A
 * chunk may hold several veins ({@link ProspectVeins#split}); each is its own element, with only its own ores. Its
 * icon stands at the chunks' middle and grows with how much ore it holds; its ores are the chunks' shares summed (how
 * many blocks, how many mined, the heights). Veins the filter hides ({@link ProspectMapState#shown}) are left out. In
 * the ore layer every chunk stays its own element, with the ores of its shown veins.
 *
 * @param key the vein's key ({@link ProspectVeins})
 * @param chunks the chunks, each with only this vein's ores
 * @param x,z the middle of the chunks, in blocks
 * @param slot,slots this icon's place among the icons standing at the same spot (several veins in one chunk), left
 *     to right; they are spread side by side instead of drawn on top of each other
 */
@OnlyIn(Dist.CLIENT)
public record ProspectVein(String key, List<ProspectChunk> chunks, double x, double z, List<Discovery> ores,
                           int slot, int slots) {

    /** How far apart icons at one spot stand, in icon sizes. */
    private static final float SPREAD = 1.05f;

    /** This icon's sideways shift from the spot, in GUI pixels, for an icon {@code size} wide. */
    float offset(float size) {
        return (slot - (slots - 1) / 2f) * size * SPREAD;
    }

    /** Ore blocks a vein needs for a normal-size icon; each doubling of it adds {@link #STEP}. */
    private static final double BASE_ORES = 300;
    private static final double STEP = 0.25;
    private static final float MIN_SCALE = 0.75f, MAX_SCALE = 2.0f;

    /**
     * The icon's size against a normal one, from how much ore the vein holds (all its ores, in every chunk of it):
     * 300 blocks 1×, 600 1.25×, 1200 1.5×, 2400 1.75×, 4800 and more 2×; small finds down to 0.75×. Each doubling
     * adds the same, so the growth slows down as the vein gets bigger.
     */
    float iconScale() {
        int total = 0;
        for (Discovery d : ores) total += d.veinCount;
        if (total <= 0) return 1;
        double scale = 1 + STEP * (Math.log(total / BASE_ORES) / Math.log(2));
        return (float) Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /** Whether the block column at {@code bx, bz} is in one of the vein's chunks. */
    boolean covers(double bx, double bz) {
        int cx = Math.floorDiv((int) Math.floor(bx), 16), cz = Math.floorDiv((int) Math.floor(bz), 16);
        for (ProspectChunk c : chunks) if (c.x() == cx && c.z() == cz) return true;
        return false;
    }

    private static int cachedVersion = -1;
    private static String cachedDimension = "";
    private static List<ProspectVein> cached = List.of();

    private static int cachedFilter = -1;

    /** The prospected veins of the player's dimension (merged; one per chunk in the ore layer), or none while hidden. */
    static Iterator<ProspectVein> current() {
        if (!ProspectMapState.visible()) return java.util.Collections.emptyIterator();
        if (ProspectMapState.oreLayer()) {
            List<ProspectVein> single = new ArrayList<>();
            ProspectChunk.current().forEachRemaining(c -> {
                List<Discovery> shown = new ArrayList<>();
                for (ProspectVeins.Piece p : ProspectVeins.split(c.ores())) {
                    if (ProspectMapState.shown(p.key())) shown.addAll(p.ores());
                }
                if (!shown.isEmpty()) {
                    ProspectChunk only = new ProspectChunk(c.x(), c.z(), List.copyOf(shown));
                    single.add(new ProspectVein("", List.of(only), c.x() * 16, c.z() * 16, only.ores(), 0, 1));
                }
            });
            return single.iterator();
        }
        String dim = ProspectMapState.dimension();
        // Grouping reads every vein definition per chunk: done again only when the finds or the filter change.
        if (cachedVersion != ClientProspects.version || !cachedDimension.equals(dim)
                || cachedFilter != ProspectMapState.filterVersion()) {
            cached = group();
            cachedVersion = ClientProspects.version;
            cachedDimension = dim;
            cachedFilter = ProspectMapState.filterVersion();
        }
        return cached.iterator();
    }

    private static List<ProspectVein> group() {
        // Each chunk split into its veins; per vein key, the chunks holding it (with only its share of their ores).
        Map<String, Map<Long, ProspectChunk>> byKey = new java.util.TreeMap<>();
        ProspectChunk.all().forEachRemaining(c -> {
            for (ProspectVeins.Piece p : ProspectVeins.split(c.ores())) {
                if (!ProspectMapState.shown(p.key())) continue;
                byKey.computeIfAbsent(p.key(), k -> new HashMap<>())
                        .put(pos(c.x(), c.z()), new ProspectChunk(c.x(), c.z(), p.ores()));
            }
        });
        List<ProspectVein> merged = new ArrayList<>();
        byKey.forEach((key, chunks) -> {
            java.util.Set<Long> seen = new java.util.HashSet<>();
            for (long start : chunks.keySet()) {
                if (!seen.add(start)) continue;
                List<ProspectChunk> group = new ArrayList<>();
                ArrayDeque<Long> queue = new ArrayDeque<>();
                queue.add(start);
                while (!queue.isEmpty()) {
                    ProspectChunk c = chunks.get(queue.poll());
                    group.add(c);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            long n = pos(c.x() + dx, c.z() + dz);
                            if (chunks.containsKey(n) && seen.add(n)) queue.add(n);
                        }
                    }
                }
                merged.add(of(key, group));
            }
        });
        // Icons at one spot (several veins whose chunks have the same middle) get places side by side.
        Map<Long, List<ProspectVein>> bySpot = new LinkedHashMap<>();
        for (ProspectVein v : merged) {
            bySpot.computeIfAbsent(pos((int) Math.round(v.x()), (int) Math.round(v.z())), k -> new ArrayList<>()).add(v);
        }
        List<ProspectVein> out = new ArrayList<>(merged.size());
        bySpot.values().forEach(spot -> {
            for (int i = 0; i < spot.size(); i++) {
                ProspectVein v = spot.get(i);
                out.add(new ProspectVein(v.key(), v.chunks(), v.x(), v.z(), v.ores(), i, spot.size()));
            }
        });
        return List.copyOf(out);
    }

    private static ProspectVein of(String key, List<ProspectChunk> group) {
        double x = 0, z = 0;
        Map<String, Discovery> merged = new LinkedHashMap<>();
        for (ProspectChunk c : group) {
            x += c.x() * 16 + 8;
            z += c.z() * 16 + 8;
            for (Discovery d : c.ores()) {
                Discovery sum = merged.computeIfAbsent(d.oreKey, k -> new Discovery(k, d.rawItem, d.displayBlock));
                sum.absorb(d);
            }
        }
        return new ProspectVein(key, List.copyOf(group), x / group.size(), z / group.size(),
                List.copyOf(merged.values()), 0, 1);
    }

    private static long pos(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
