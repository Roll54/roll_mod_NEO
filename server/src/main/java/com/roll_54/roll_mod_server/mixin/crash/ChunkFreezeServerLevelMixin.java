package com.roll_54.roll_mod_server.mixin.crash;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_server.crash.ChunkFreezer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Block side of {@link ChunkFreezer}:
 * <ul>
 *   <li>{@code shouldTickBlocksAt} gates block entity ticks, random ticks/precipitation ({@code tickChunk}) and
 *       sculk/vibration ticking.</li>
 *   <li>{@code isPositionTickingWithEntitiesLoaded} is the {@code LevelTicks} predicate, so scheduled block and
 *       fluid ticks of a frozen chunk stay queued instead of running (or being dropped).</li>
 *   <li>{@code isNaturalSpawningAllowed} stops mob spawning into it.</li>
 * </ul>
 * Scheduled and random block/fluid ticks that throw freeze their chunk.
 */
@Mixin(ServerLevel.class)
public abstract class ChunkFreezeServerLevelMixin {

    // These run for every block entity / scheduled tick, so they are return-value modifiers rather than cancellable
    // HEAD injects: no CallbackInfoReturnable allocated per call, and the freeze lookup is skipped whenever vanilla
    // already said no.
    @ModifyReturnValue(method = "shouldTickBlocksAt", at = @At("RETURN"))
    private boolean rollmodserver$frozenBlocks(boolean original, long chunkPos) {
        return original && !ChunkFreezer.isFrozen((Level) (Object) this, chunkPos);
    }

    @ModifyReturnValue(method = "isPositionTickingWithEntitiesLoaded", at = @At("RETURN"))
    private boolean rollmodserver$frozenScheduledTicks(boolean original, long chunkPos) {
        return original && !ChunkFreezer.isFrozen((Level) (Object) this, chunkPos);
    }

    @ModifyReturnValue(method = "isNaturalSpawningAllowed(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("RETURN"))
    private boolean rollmodserver$frozenSpawning(boolean original, ChunkPos chunkPos) {
        return original && !ChunkFreezer.isFrozen((Level) (Object) this, chunkPos.toLong());
    }

    @WrapOperation(
            method = "tickBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void rollmodserver$freezeOnBlockTickCrash(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        try {
            original.call(state, level, pos, random);
        } catch (Throwable t) {
            if (!ChunkFreezer.freezeForCrash(level, ChunkPos.asLong(pos), "scheduled tick of " + state + " at " + pos.toShortString(), t)) throw ChunkFreezer.rethrow(t);
        }
    }

    @WrapOperation(
            method = "tickFluid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void rollmodserver$freezeOnFluidTickCrash(FluidState state, Level level, BlockPos pos, Operation<Void> original) {
        try {
            original.call(state, level, pos);
        } catch (Throwable t) {
            if (!ChunkFreezer.freezeForCrash(level, ChunkPos.asLong(pos), "scheduled tick of " + state + " at " + pos.toShortString(), t)) throw ChunkFreezer.rethrow(t);
        }
    }

    @WrapOperation(
            method = "tickChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void rollmodserver$freezeOnRandomTickCrash(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        try {
            original.call(state, level, pos, random);
        } catch (Throwable t) {
            if (!ChunkFreezer.freezeForCrash(level, ChunkPos.asLong(pos), "random tick of " + state + " at " + pos.toShortString(), t)) throw ChunkFreezer.rethrow(t);
        }
    }

    @WrapOperation(
            method = "tickChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void rollmodserver$freezeOnFluidRandomTickCrash(FluidState state, Level level, BlockPos pos, RandomSource random, Operation<Void> original) {
        try {
            original.call(state, level, pos, random);
        } catch (Throwable t) {
            if (!ChunkFreezer.freezeForCrash(level, ChunkPos.asLong(pos), "random tick of " + state + " at " + pos.toShortString(), t)) throw ChunkFreezer.rethrow(t);
        }
    }
}
