package com.roll_54.roll_mod.data.datagen.py;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Enumerates expected template PNGs under assets/roll_mod/py_datagen to catch typos at compile-time and
 * provide simple path resolution for datagen.
 */
public final class PyTextureTemplates {
    private PyTextureTemplates() {}

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
        ZINC("zinc");

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
        TIN("tin"),
        URANIUM("uranium"),
        ZINC("zinc"),
        CRYSTAL("crystal"),
        SALT_CRYSTAL("salt_crystal");

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
