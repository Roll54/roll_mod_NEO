package com.roll_54.roll_mod.blocks.regenblock;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Abstract RegenBlock for NeoForge.
 * This block has a mining speed of 0 so it cannot be broken normally.
 * When mined in state BREAKABLE = true, it transitions to BREAKABLE = false (hardened state).
 * It tracks regeneration time via components and can imitate textures of other blocks.
 *
 * Features:
 * - Mining speed delta is always 0 (unbreakable with tools)
 * - Can be destroyed only through the onDestroyedByPlayer hook
 * - Transitions to hardened state on break
 * - Schedules regeneration tick to return to breakable state
 * - Supports texture imitation through a texture path component
 * - Supports custom timer durations through a timer component
 *
 * Subclasses should define:
 * - The block properties
 * - Optionally override getTimerSeconds() for custom timers
 * - Optionally override getTexturePathComponent() for custom textures
 */
public abstract class AbstractRegenBlock extends Block {
    // Block state for tracking if this block is currently breakable
    public static final BooleanProperty BREAKABLE = BlockStateProperties.ENABLED;
    public int timerSeconds; // Default 20 minutes

    protected AbstractRegenBlock(Properties properties, int timerSeconds) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BREAKABLE, true));
        this.timerSeconds = timerSeconds;
    }

    /**
     * Gets the timer duration in seconds for the block's regeneration.
     * This value is set in the constructor.
     *
     * @return The timer duration in seconds.
     */
    public int getTimerSeconds() {
        return timerSeconds; // Default 20 minutes
    }

    /**
     * Get the texture path to imitate.
     * Default is "minecraft:stone".
     * Override to support dynamic textures or retrieve from block data.
     *
     * @return The texture path (e.g., "roll_mod:deepslate_bauxite")
     */
    protected String getTexturePathComponent() {
        return "minecraft:stone"; // Default placeholder
    }

    /**
     * Called when the block is destroyed by a player.
     * Handles the transition from BREAKABLE to HARDENED state.
     */
    @Override
    public boolean onDestroyedByPlayer(
            @Nullable BlockState state,
            @Nullable Level level,
            @Nullable BlockPos pos,
            @Nullable Player player,
            boolean willHarvest,
            @Nullable FluidState fluid
    ) {
        if (level != null && !level.isClientSide && state != null && pos != null && player != null && state.getValue(BREAKABLE)) {
            // Drop resources before hardening
            dropResources(state, level, pos, null, player, player.getMainHandItem());

            // Transition to hardened state
            BlockState hardened = state.setValue(BREAKABLE, false);
            level.setBlock(pos, hardened, Block.UPDATE_ALL);

            // Schedule regeneration
            int timerSeconds = getTimerSeconds();
            int ticksToRegen = timerSeconds * 20; // Convert seconds to ticks
            level.scheduleTick(pos, this, ticksToRegen);

            RollMod.LOGGER.info("RegenBlock broken by {} at {}. Hardening and will regenerate in {} seconds.",
                    player.getName().getString(), pos, timerSeconds);
        }
        return false; // Return false to prevent normal block breaking behavior
    }

    /**
     * Called when the scheduled tick occurs.
     * Transitions the block back to BREAKABLE state for regeneration.
     */
    @Override
    public void tick(@Nullable BlockState state, @Nullable ServerLevel level, @Nullable BlockPos pos, @Nullable RandomSource random) {
        if (state != null && level != null && pos != null && !state.getValue(BREAKABLE)) {
            BlockState regenerated = state.setValue(BREAKABLE, true);
            level.setBlock(pos, regenerated, Block.UPDATE_ALL);
            RollMod.LOGGER.info("RegenBlock at {} has regenerated and is now breakable again.", pos);
        }
    }

    /**
     * Override this to always return 0.0F so the block cannot be mined with tools.
     * This is the key to making the block unbreakable except through the onDestroyedByPlayer logic.
     */
    @Override
    public float getDestroyProgress(@Nullable BlockState state, @Nullable Player player, net.minecraft.world.level.@NonNull BlockGetter level, @Nullable BlockPos pos) {
        if (player != null && !state.getValue(BREAKABLE)) {
            return 0.0F; // if breakable is false, mining speed is 0 (unbreakable)
        }

        // if breakable is true, we can allow normal mining speed (or you can customize this further)
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BREAKABLE);
    }

    /**
     * Whether this block should have random ticks.
     * Default is false since we use scheduled ticks.
     */
    @Override
    public boolean isRandomlyTicking(@Nullable BlockState state) {
        return false;
    }
}
