package com.roll_54.roll_mod.data.datagen.ore;

import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockOverlay;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.ItemBase;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.RawBlockBase;

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
}
