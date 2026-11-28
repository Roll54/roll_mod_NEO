package com.roll_54.roll_mod.prospecting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;

/**
 * One ore a player found with the prospector's pick in one chunk: where in height it lies (a range per layer, stone
 * and deepslate apart), the whole vein in that chunk (how many blocks, and where they are, for the map's ore layer),
 * and how many of them the player has mined since.
 *
 * <p>Positions are packed per block: {@code x & 15 | (z & 15) << 4 | (y + 2048) << 8}. At most {@link #MAX_POSITIONS}
 * are kept per ore and chunk.
 */
public final class Discovery {

    public static final int MAX_POSITIONS = 4096;
    private static final int NONE_MIN = Integer.MAX_VALUE, NONE_MAX = Integer.MIN_VALUE;

    public final String oreKey;
    public ResourceLocation rawItem;
    /** The block named in messages and tooltips: the stone variant when there is one. */
    public ResourceLocation displayBlock;
    public int stoneMin = NONE_MIN, stoneMax = NONE_MAX;
    public int deepMin = NONE_MIN, deepMax = NONE_MAX;
    /** Blocks of this ore in the whole chunk when it was last prospected. */
    public int veinCount;
    /** How many of them the player has mined since. */
    public int mined;
    /**
     * The whole vein was opened up by mining {@link ProspectService#REVEAL_AFTER} of it: every chunk it spans is
     * recorded, and the map shows the counts at once.
     */
    public boolean revealed;
    public int[] positions = new int[0];

    public Discovery(String oreKey, ResourceLocation rawItem, ResourceLocation displayBlock) {
        this.oreKey = oreKey;
        this.rawItem = rawItem;
        this.displayBlock = displayBlock;
    }

    public boolean hasStone() {
        return stoneMin <= stoneMax;
    }

    public boolean hasDeepslate() {
        return deepMin <= deepMax;
    }

    /** Takes in another find of the same ore in the same chunk (saved apart under an older key). */
    public void absorb(Discovery other) {
        if (other.hasStone()) {
            include(false, other.stoneMin);
            include(false, other.stoneMax);
        }
        if (other.hasDeepslate()) {
            include(true, other.deepMin);
            include(true, other.deepMax);
        }
        veinCount += other.veinCount;
        mined += other.mined;
        revealed |= other.revealed;
        int[] merged = java.util.stream.IntStream.concat(Arrays.stream(positions), Arrays.stream(other.positions))
                .distinct().limit(MAX_POSITIONS).toArray();
        positions = merged;
    }

    public void include(boolean deepslate, int y) {
        if (deepslate) {
            deepMin = Math.min(deepMin, y);
            deepMax = Math.max(deepMax, y);
        } else {
            stoneMin = Math.min(stoneMin, y);
            stoneMax = Math.max(stoneMax, y);
        }
    }

    public static int pack(int x, int y, int z) {
        return (x & 15) | (z & 15) << 4 | (y + 2048) << 8;
    }

    public static int x(int packed) { return packed & 15; }

    public static int z(int packed) { return (packed >> 4) & 15; }

    public static int y(int packed) { return (packed >>> 8) - 2048; }

    /** The mined block is no longer part of the vein on the map. */
    public void mine(int packed) {
        mined++;
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] == packed) {
                int[] rest = new int[positions.length - 1];
                System.arraycopy(positions, 0, rest, 0, i);
                System.arraycopy(positions, i + 1, rest, i, positions.length - i - 1);
                positions = rest;
                return;
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ore", oreKey);
        tag.putString("raw", rawItem.toString());
        tag.putString("block", displayBlock.toString());
        tag.putIntArray("ranges", new int[] { stoneMin, stoneMax, deepMin, deepMax });
        tag.putInt("vein", veinCount);
        tag.putInt("mined", mined);
        tag.putBoolean("revealed", revealed);
        tag.putIntArray("pos", positions);
        return tag;
    }

    public static Discovery load(CompoundTag tag) {
        ResourceLocation raw = ResourceLocation.parse(tag.getString("raw"));
        ResourceLocation block = ResourceLocation.parse(tag.getString("block"));
        // Finds saved before the map icon learnt an ore's product (diamond, not diamond ore) are re-resolved from the
        // ore block itself; the saved one stays when that block is gone (its mod removed).
        // Its key too: roll_mod's ores were once keyed by rock (stone_tin apart from tin); ProspectData merges what
        // now shares one key.
        var kind = BuiltInRegistries.BLOCK.getOptional(block).filter(b -> b != Blocks.AIR).map(OreKind::of);
        Discovery d = new Discovery(kind.map(OreKind::key).orElse(tag.getString("ore")),
                kind.map(OreKind::rawItem).orElse(raw), block);
        int[] r = tag.getIntArray("ranges");
        if (r.length == 4) {
            d.stoneMin = r[0];
            d.stoneMax = r[1];
            d.deepMin = r[2];
            d.deepMax = r[3];
        }
        d.veinCount = tag.getInt("vein");
        d.mined = tag.getInt("mined");
        d.revealed = tag.getBoolean("revealed");
        d.positions = tag.getIntArray("pos");
        return d;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(oreKey, 256);
        buf.writeResourceLocation(rawItem);
        buf.writeResourceLocation(displayBlock);
        buf.writeInt(stoneMin);
        buf.writeInt(stoneMax);
        buf.writeInt(deepMin);
        buf.writeInt(deepMax);
        buf.writeVarInt(veinCount);
        buf.writeVarInt(mined);
        buf.writeBoolean(revealed);
        buf.writeVarIntArray(positions.length > MAX_POSITIONS ? Arrays.copyOf(positions, MAX_POSITIONS) : positions);
    }

    public static Discovery read(FriendlyByteBuf buf) {
        Discovery d = new Discovery(buf.readUtf(256), buf.readResourceLocation(), buf.readResourceLocation());
        d.stoneMin = buf.readInt();
        d.stoneMax = buf.readInt();
        d.deepMin = buf.readInt();
        d.deepMax = buf.readInt();
        d.veinCount = buf.readVarInt();
        d.mined = buf.readVarInt();
        d.revealed = buf.readBoolean();
        d.positions = buf.readVarIntArray(MAX_POSITIONS);
        return d;
    }
}
