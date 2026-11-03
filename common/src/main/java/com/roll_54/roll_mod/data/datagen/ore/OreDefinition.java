package com.roll_54.roll_mod.data.datagen.ore;

import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockOverlay;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.ItemBase;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.RawBlockBase;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.StoneVariant;

import java.util.Arrays;
import java.util.List;

public record OreDefinition(
        String id,
        String hexColor,
        List<BlockSubLayer> bases,
        BlockOverlay overlay,
        ItemBase itemBase,
        int minExperience,
        int maxExperience,
        String enUsName,
        String ukUaName,
        RawBlockBase rawBlock
) {
    public OreDefinition {

    }

    /** The rock variants that come with {@link #bases()}: every {@link StoneVariant} whose layer is listed. */
    public List<StoneVariant> variants() {
        return Arrays.stream(StoneVariant.values()).filter(v -> bases.contains(v.parent())).toList();
    }

    /** Registry path of this ore in a rock variant, {@code <variant>_<ore>}. */
    public String variantBlockId(StoneVariant variant) {
        return variant.id() + "_" + id;
    }
}
