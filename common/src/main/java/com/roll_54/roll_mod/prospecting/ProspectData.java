package com.roll_54.roll_mod.prospecting;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's prospecting finds, kept with the world ({@code world/data/roll_mod_prospects.dat}): per player, per
 * dimension, per chunk, per ore ({@link Discovery}). Each player sees only their own.
 */
public final class ProspectData extends SavedData {

    private static final String NAME = "roll_mod_prospects";

    /** player → dimension id → chunk ({@code ChunkPos.toLong}) → ore key → find. */
    private final Map<UUID, Map<String, Map<Long, Map<String, Discovery>>>> players = new HashMap<>();

    public static ProspectData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(ProspectData::new, ProspectData::load, null), NAME);
    }

    /** The player's finds in a chunk, created on first use. */
    public Map<String, Discovery> chunk(UUID player, String dimension, long chunk) {
        return players.computeIfAbsent(player, k -> new HashMap<>())
                .computeIfAbsent(dimension, k -> new HashMap<>())
                .computeIfAbsent(chunk, k -> new LinkedHashMap<>());
    }

    /** The player's finds in a chunk, or null when they have none there. */
    public Map<String, Discovery> chunkOrNull(UUID player, String dimension, long chunk) {
        var dims = players.get(player);
        if (dims == null) return null;
        var chunks = dims.get(dimension);
        return chunks == null ? null : chunks.get(chunk);
    }

    /** Everything the player has found: dimension → chunk → ore → find. */
    public Map<String, Map<Long, Map<String, Discovery>>> all(UUID player) {
        return players.getOrDefault(player, Map.of());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        players.forEach((player, dims) -> {
            ListTag entries = new ListTag();
            dims.forEach((dim, chunks) -> chunks.forEach((chunk, ores) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("dim", dim);
                entry.putLong("chunk", chunk);
                ListTag list = new ListTag();
                ores.values().forEach(d -> list.add(d.save()));
                entry.put("ores", list);
                entries.add(entry);
            }));
            all.put(player.toString(), entries);
        });
        tag.put("players", all);
        return tag;
    }

    private static ProspectData load(CompoundTag tag, HolderLookup.Provider registries) {
        ProspectData data = new ProspectData();
        CompoundTag all = tag.getCompound("players");
        for (String key : all.getAllKeys()) {
            UUID player;
            try {
                player = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ListTag entries = all.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                Map<String, Discovery> ores = data.chunk(player, entry.getString("dim"), entry.getLong("chunk"));
                ListTag list = entry.getList("ores", Tag.TAG_COMPOUND);
                for (int j = 0; j < list.size(); j++) {
                    Discovery d = Discovery.load(list.getCompound(j));
                    Discovery known = ores.get(d.oreKey);
                    if (known != null) known.absorb(d);
                    else ores.put(d.oreKey, d);
                }
            }
        }
        return data;
    }
}
