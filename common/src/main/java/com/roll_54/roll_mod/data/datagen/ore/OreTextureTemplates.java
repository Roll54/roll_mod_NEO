package com.roll_54.roll_mod.data.datagen.ore;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Enumerates expected template PNGs under assets/roll_mod/py_datagen to catch typos at compile-time and
 * provide simple path resolution for datagen.
 */
public final class OreTextureTemplates {
    private OreTextureTemplates() {}

    public enum BlockSubLayer {
        STONE("stone"),
        DEEPSLATE("deepslate"),
        NETHERRACK("netherrack"),
        MARS("mars"),
        MOON("moon"),
        VENUS("venus"),
        MERCURY("mercury"),
        END("end");

        private final String file;
        BlockSubLayer(String file) { this.file = file; }
        public Path resolve(Path dir) { return dir.resolve(file + ".png"); }
        public String id() { return file; }
        public static BlockSubLayer from(String name) {
            return Arrays.stream(values())
                    .filter(v -> v.file.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown block sub-layer: " + name));
        }
    }

    public enum BlockOverlay {
        BISMUTH("bismuth"),
        COAL("coal"),
        COPPER("copper"),
        DIAMOND("diamond"),
        GOLD("gold"),
        IRON("iron"),
        LAPIS("lapis"),
        LEAD("lead"),
        OSMIUM("osmium"),
        QUARTZ("quartz"),
        REDSTONE("redstone"),
        TIN("tin"),
        URANIUM("uranium"),
        ZINC("zinc"),
        FINE_CRYSTALS("fine_crystals");

        private final String file;
        BlockOverlay(String file) { this.file = file; }
        public Path resolve(Path dir) { return dir.resolve(file + ".png"); }
        public String id() { return file; }
        public static BlockOverlay from(String name) {
            return Arrays.stream(values())
                    .filter(v -> v.file.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown block overlay: " + name));
        }
    }

    public enum ItemBase {
        AMETHYST("amethyst"),
        COAL("coal"),
        COPPER("copper"),
        DIAMOND("diamond"),
        DUST("dust"),
        GOLD("gold"),
        IRIDIUM("iridium"),
        IRON("iron"),
        OSMIUM("osmium"),
        QUARTZ("quartz"),
        URANIUM("uranium"),
        ZINC("zinc"),
        CRYSTAL("crystal"),
        SALT_CRYSTAL("salt_crystal"),
        BISMUTH_ITEM("bismuth_item"),
        PSEUDO_DUST("pseudo_dust");

        private final String file;
        ItemBase(String file) { this.file = file; }
        public Path resolve(Path dir) { return dir.resolve(file + ".png"); }
        public String id() { return file; }
        public static ItemBase from(String name) {
            return Arrays.stream(values())
                    .filter(v -> v.file.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown item base: " + name));
        }
    }

    /** Pre-made raw storage-block textures. Unlike ore blocks these can't be composited, so the
     *  texture is picked from this fixed pool and recoloured per-ore via the definition's hex tint. */
    public enum RawBlockBase {
        CALORITE("calorite_block"),
        COPPER("copper_block"),
        DESH("desh_block"),
        GOLD("gold_block"),
        IRON("iron_block"),
        OSTRUM("ostrum_block");

        private final String file;
        RawBlockBase(String file) { this.file = file; }
        public Path resolve(Path dir) { return dir.resolve(file + ".png"); }
        public String id() { return file; }
        public static RawBlockBase from(String name) {
            return Arrays.stream(values())
                    .filter(v -> v.file.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown raw block base: " + name));
        }
    }

    public enum ItemLayer {
        CRUSHED("crushed"),
        CRUSHED_OVERLAY("crushed_overlay"),
        CRUSHED_REFINED("crushed_refined"),
        CRUSHED_REFINED_OVERLAY("crushed_refined_overlay"),
        CRUSHED_PURIFIED("crushed_purified"),
        DUST_PURE("dust_pure"),
        DUST_PURE_OVERLAY("dust_pure_overlay"),
        DUST_IMPURE("dust_impure"),
        DUST_IMPURE_OVERLAY("dust_impure_overlay");

        private final String file;
        ItemLayer(String file) { this.file = file; }
        public Path resolve(Path dir) { return dir.resolve(file + ".png"); }
        public String id() { return file; }
    }
}
