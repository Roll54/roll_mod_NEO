package com.roll_54.roll_mod.blocks.regenblock;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Concrete implementation of AbstractRegenBlock for NeoForge.
 * This is a standard regen block with basic properties.
 *
 * To create a custom regen block:
 * 1. Extend AbstractRegenBlock
 * 2. Override getTimerSeconds() if you need custom regeneration timers
 * 3. Override getTexturePathComponent() if you need to imitate specific block textures
 * 4. Register your custom block in BlockRegistry
 */
public class RegenBlock extends AbstractRegenBlock {

    public RegenBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected int getTimerSeconds() {
        // Default 20 minutes (1200 seconds)
        return 1200;
    }

    @Override
    protected String getTexturePathComponent() {
        // Override this to define the texture to imitate
        return "minecraft:stone";
    }
}
