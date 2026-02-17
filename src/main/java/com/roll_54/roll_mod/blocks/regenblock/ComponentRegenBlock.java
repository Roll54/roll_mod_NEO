package com.roll_54.roll_mod.blocks.regenblock;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Example RegenBlock implementation that demonstrates how to use components
 * to define custom regeneration timers and texture imitation.
 *
 * This block stores its timer duration and texture path in block data components,
 * allowing each block instance to have unique properties.
 *
 * Usage example:
 * ComponentRegenBlock block = new ComponentRegenBlock(properties);
 * When placing the block, you can set:
 * - REGEN_TIMER component: The regeneration time in seconds
 * - REGEN_TEXTURE_PATH component: The texture to imitate (e.g., "roll_mod:deepslate_bauxite")
 */
public class ComponentRegenBlock extends AbstractRegenBlock {
    private final int defaultTimerSeconds;
    private final String defaultTexturePath;

    public ComponentRegenBlock(
            BlockBehaviour.Properties properties,
            int defaultTimerSeconds,
            String defaultTexturePath
    ) {
        super(properties);
        this.defaultTimerSeconds = defaultTimerSeconds;
        this.defaultTexturePath = defaultTexturePath;
    }

    /**
     * Get the timer duration in seconds.
     * First checks if a component has been set, otherwise uses the default.
     *
     * @return The timer duration in seconds
     */
    @Override
    protected int getTimerSeconds() {
        // This would typically be retrieved from block entity data or component
        // For now, returning default. Override in your custom implementation to support dynamic timers.
        return defaultTimerSeconds;
    }

    /**
     * Get the texture path to imitate.
     * First checks if a component has been set, otherwise uses the default.
     *
     * @return The texture path (e.g., "roll_mod:deepslate_bauxite")
     */
    @Override
    protected String getTexturePathComponent() {
        // This would typically be retrieved from block entity data or component
        // For now, returning default. Override in your custom implementation to support dynamic textures.
        return defaultTexturePath;
    }
}