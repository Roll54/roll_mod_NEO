package com.roll_54.roll_mod.prospecting;

import com.roll_54.roll_mod.compat.gtmogs.OreMaterials;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.StoneVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What an ore block is to the prospector: one {@link #key} shared by its stone and deepslate variants (iron ore and
 * deepslate iron ore are one ore with two layers; roll_mod's {@code stone_tin}, {@code granite_tin}, {@code mars_tin}
 * are all {@code roll_mod:tin}), whether this block is the deepslate one, and the raw item the ore
 * stands for on the map (see {@link #raw}).
 */
public record OreKind(String key, boolean deepslate, ResourceLocation rawItem) {

    private static final Map<Block, OreKind> CACHE = new ConcurrentHashMap<>();

    public static OreKind of(Block block) {
        return CACHE.computeIfAbsent(block, OreKind::compute);
    }

    private static OreKind compute(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();
        boolean deepslate = path.contains("deepslate");
        String base = path.replace("deepslate_", "").replace("_deepslate", "");
        if (base.endsWith("_ore")) base = base.substring(0, base.length() - 4);
        else if (base.startsWith("ore_")) base = base.substring(4);
        // roll_mod's ores are <rock>_<material>: one key per material, whatever rock it sits in.
        base = withoutRock(base);
        String key = id.getNamespace() + ":" + base;
        return new OreKind(key, deepslate, raw(id.getNamespace(), base, block));
    }

    /**
     * What a vein of this ore stands for on the map, the first of: {@code raw_<base>} (in the ore's namespace, then
     * roll_mod's, then vanilla's), the gem or dust named {@code <base>} itself (diamond, emerald, coal), a vanilla
     * ore whose drop is named otherwise (lapis, nether quartz, nether gold), and only then the ore block's own item.
     * {@code base} is already without its rock ({@link #withoutRock}).
     */
    private static ResourceLocation raw(String namespace, String base, Block block) {
        ResourceLocation own = BuiltInRegistries.ITEM.getKey(block.asItem());
        String material = base;
        for (String ns : new String[] { namespace, "roll_mod", "minecraft" }) {
            ResourceLocation id = item(ns, "raw_" + material, own);
            if (id != null) return id;
        }
        for (String ns : new String[] { namespace, "minecraft" }) {
            ResourceLocation id = item(ns, material, own);
            if (id != null) return id;
        }
        String drop = VANILLA_DROPS.get(material);
        if (drop != null) {
            ResourceLocation id = item("minecraft", drop, own);
            if (id != null) return id;
        }
        return own;
    }

    /** Vanilla ores (by base) whose drop is not named after them. */
    private static final Map<String, String> VANILLA_DROPS = Map.of(
            "lapis", "lapis_lazuli",
            "nether_quartz", "quartz",
            "nether_gold", "gold_nugget");

    /** {@code ns:path} when that item exists and is not the ore block itself. */
    private static ResourceLocation item(String ns, String path, ResourceLocation own) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ns, path);
        if (id.equals(own) || BuiltInRegistries.ITEM.get(id) == Items.AIR) return null;
        return id;
    }

    /** {@code tin} for {@code stone_tin} or {@code granite_tin}: a roll_mod rock prefix off, when the rest is a material. */
    private static String withoutRock(String base) {
        for (BlockSubLayer layer : BlockSubLayer.values()) {
            String rest = strip(base, layer.id());
            if (rest != null) return rest;
        }
        for (StoneVariant variant : StoneVariant.values()) {
            String rest = strip(base, variant.id());
            if (rest != null) return rest;
        }
        return base;
    }

    private static String strip(String base, String rock) {
        if (!base.startsWith(rock + "_")) return null;
        String rest = base.substring(rock.length() + 1);
        return OreMaterials.byId(rest) != null ? rest : null;
    }
}
