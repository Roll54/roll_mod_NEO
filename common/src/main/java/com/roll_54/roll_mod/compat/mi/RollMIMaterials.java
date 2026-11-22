package com.roll_54.roll_mod.compat.mi;

import aztech.modern_industrialization.materials.Material;
import aztech.modern_industrialization.materials.MaterialBuilder;
import aztech.modern_industrialization.materials.MaterialRegistry;
import aztech.modern_industrialization.materials.property.MaterialProperty;
import aztech.modern_industrialization.materials.recipe.StandardRecipes;

import java.util.List;

import static aztech.modern_industrialization.materials.part.MIParts.DUST;
import static aztech.modern_industrialization.materials.part.MIParts.TINY_DUST;
import static aztech.modern_industrialization.materials.property.MaterialHardness.SOFT;
import static aztech.modern_industrialization.materials.set.MaterialSet.STONE;

/**
 * Materials this mod adds through Modern Industrialization's own material system, the same path MI's
 * KubeJS {@code MIMaterialEvents.addMaterials} takes. Parts are MI items ({@code modern_industrialization:<material>_<part>}),
 * show up in MI's creative tab, and get MI's standard material recipes (tiny dust 3x3 crafting, packer/unpacker).
 *
 * <p>Must run from this mod's constructor: MI registers its own materials in its constructor and this mod
 * is ordered AFTER it, so MI's item DeferredRegister still accepts entries here.
 *
 * <p>Assets and recipes are not shipped by MI for these; {@code RollMIMaterialProviders} generates them
 * into this mod's datagen output.
 */
public final class RollMIMaterials {
    public static Material SODIUM_CHLORATE;

    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SODIUM_CHLORATE = MaterialRegistry.addMaterial(new MaterialBuilder("Sodium Chlorate", "sodium_chlorate")
                .set(MaterialProperty.MAIN_PART, DUST)
                .set(MaterialProperty.SET, STONE)
                .set(MaterialProperty.MEAN_RGB, 0xDCE6EA)
                .set(MaterialProperty.HARDNESS, SOFT)
                .addParts(DUST, TINY_DUST)
                .addRecipes(StandardRecipes::apply));
    }

    /** Every material registered here, for datagen. */
    public static List<Material> all() {
        return List.of(SODIUM_CHLORATE);
    }

    private RollMIMaterials() {
    }
}
