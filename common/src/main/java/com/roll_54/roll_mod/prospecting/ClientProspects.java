package com.roll_54.roll_mod.prospecting;

import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own prospecting finds on the client, as the server last sent them ({@link ProspectPacket}): dimension
 * → chunk → ore → find. Read by the map overlays. Client thread only.
 */
public final class ClientProspects {

    public static final Map<String, Map<Long, Map<String, Discovery>>> FINDS = new HashMap<>();
    /** Bumped on every change, so map layers know to redraw. */
    public static int version;
    /** What the last prospect found, outlined in the world until {@link #highlightUntil} (System.currentTimeMillis). */
    public static List<ProspectPacket.ChunkFinds> highlight = List.of();
    public static long highlightUntil;
    /** The prospected column's first blocks, outlined mint green until {@link #columnUntil}; null when none. */
    public static net.minecraft.core.BlockPos columnHit;
    public static boolean columnUp;
    public static int columnRadius;
    public static long columnUntil;
    /** How many blocks into the column (from the block hit) are outlined: where the particle cloud was. */
    public static final int COLUMN_DEPTH = 3;
    /** How long a prospect's finds stay outlined. */
    public static final long HIGHLIGHT_MILLIS = 2_500;

    private ClientProspects() {}

    static void accept(ProspectPacket packet) {
        if (packet.replaceAll()) FINDS.clear();
        for (ProspectPacket.ChunkFinds c : packet.chunks()) {
            Map<Long, Map<String, Discovery>> chunks = FINDS.computeIfAbsent(c.dimension(), k -> new HashMap<>());
            if (c.ores().isEmpty()) {
                chunks.remove(c.chunk());
                continue;
            }
            Map<String, Discovery> ores = new LinkedHashMap<>();
            for (Discovery d : c.ores()) ores.put(d.oreKey, d);
            chunks.put(c.chunk(), ores);
        }
        version++;
        if (packet.report() != null) {
            // A fresh prospect (not the login's whole list): its chunks' veins are outlined for a while.
            highlight = List.copyOf(packet.chunks());
            highlightUntil = System.currentTimeMillis() + HIGHLIGHT_MILLIS;
            Chat.report(packet.report());
        }
    }

    /** The pick was used on {@code hit}: its column's first blocks are outlined for {@link #HIGHLIGHT_MILLIS}. */
    public static void markColumn(net.minecraft.core.BlockPos hit, boolean up, int radius) {
        columnHit = hit.immutable();
        columnUp = up;
        columnRadius = radius;
        columnUntil = System.currentTimeMillis() + HIGHLIGHT_MILLIS;
    }

    /** The finds in a chunk of a dimension, or an empty map. */
    public static Map<String, Discovery> at(String dimension, long chunk) {
        Map<Long, Map<String, Discovery>> chunks = FINDS.get(dimension);
        if (chunks == null) return Map.of();
        return chunks.getOrDefault(chunk, Map.of());
    }

    /** "Y 40-5", top first; "Y 40-5, Y -10 – -50" for an ore in both layers (a spaced dash once a height is negative). */
    /** One height range over every layer the ore was found in (stone, tuff, deepslate together): top…bottom. */
    public static MutableComponent ranges(Discovery d) {
        int top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
        if (d.hasStone()) {
            top = Math.max(top, d.stoneMax);
            bottom = Math.min(bottom, d.stoneMin);
        }
        if (d.hasDeepslate()) {
            top = Math.max(top, d.deepMax);
            bottom = Math.min(bottom, d.deepMin);
        }
        if (top == Integer.MIN_VALUE) return Component.empty();
        return range(top, bottom).copy();
    }

    private static Component range(int top, int bottom) {
        return Component.translatable(top < 0 || bottom < 0
                ? "message.roll_mod.prospector.range_spaced" : "message.roll_mod.prospector.range", top, bottom);
    }

    public static Component name(Discovery d) {
        return BuiltInRegistries.BLOCK.get(d.displayBlock).getName();
    }

    /** Kept apart so nothing on the server side ever loads the chat store. */
    private static final class Chat {
        static void report(List<Discovery> found) {
            if (found.isEmpty()) {
                post(Component.translatable("message.roll_mod.prospector.none").withStyle(ChatFormatting.RED));
                return;
            }
            for (Discovery d : found) {
                // Grey all through; only the pick in front is green.
                post(Component.literal("⛏ ").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("message.roll_mod.prospector.found_range",
                                name(d), Component.literal("(").append(ranges(d)).append(")"))
                                .withStyle(ChatFormatting.GRAY)));
            }
        }

        private static void post(Component message) {
            com.roll_54.roll_mod.minestar.chat.client.ClientChatStore.receive(ChatChannel.LOCAL, message, "", "", 0L);
        }
    }
}
