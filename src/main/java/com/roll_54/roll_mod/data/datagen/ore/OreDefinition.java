package com.roll_54.roll_mod.data.datagen.ore;

import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockOverlay;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.ItemBase;

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
        String ukUaName
) {
    public OreDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (hexColor == null || hexColor.isBlank()) {
            throw new IllegalArgumentException("hexColor is required");
        }
        if (bases == null || bases.isEmpty()) {
            throw new IllegalArgumentException("bases are required");
        }
        if (overlay == null) {
            throw new IllegalArgumentException("overlay is required");
        }
        if (itemBase == null) {
            throw new IllegalArgumentException("itemBase is required");
        }
        if (enUsName == null || enUsName.isBlank()) {
            throw new IllegalArgumentException("enUsName is required");
        }
        if (ukUaName == null || ukUaName.isBlank()) {
            throw new IllegalArgumentException("ukUaName is required");
        }
    }
}
