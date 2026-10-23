package com.roll_54.roll_mod.compat.functionalstorage.mixin;

import com.buuz135.functionalstorage.util.ConnectedDrawers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a drawer network rebuild from hanging the server thread while chunks load.
 *
 * <p>When a drawer's chunk loads, {@code LevelChunk.runPostLoad} calls the drawer's
 * {@code clearRemoved()}, which calls {@code invalidateCapabilities()}, which finds the controller
 * (through {@code getChunkNow}, so safely) and calls {@code ConnectedDrawers.rebuild()}. That walks
 * every drawer in the network:
 *
 * <pre>{@code
 * if (!level.isLoaded(pos)) continue;
 * BlockEntity entity = level.getBlockEntity(pos);   // blocking chunk load
 * }</pre>
 *
 * <p>{@code isLoaded} is {@code ServerChunkCache.hasChunk}, and that only says the chunk's ticket
 * level allows FULL — not that it has finished loading. For a drawer in a neighbouring chunk that is
 * still on its way in, {@code getBlockEntity} goes to {@code getChunk} and {@code managedBlock}s on
 * its FULL future. But we are running inside the main-thread chunk task that
 * {@code ChunkTaskPriorityQueueSorter} hands out one at a time, so the neighbour's FULL step is not
 * dispatched until this task returns. The server thread parks for good — GTMOGS's stall watchdog
 * reported it as {@code ConnectedDrawers.rebuild(ConnectedDrawers.java:77)} under
 * {@code ChunkSerializer.postLoadChunk}. (The drawer's own chunk is not a problem: NeoForge's
 * {@code currentlyLoading} bypass covers it.)
 *
 * <p>Both {@code getBlockEntity} calls in {@code rebuild()} now go through
 * {@link net.minecraft.world.level.chunk.ChunkSource#getChunkNow}, which never blocks and already
 * honours the {@code currentlyLoading} bypass. When the chunk is not there yet:
 * <ul>
 *   <li>first pass (which drawers stay in the network): the position is <em>kept</em>. Dropping it
 *       would unlink the drawer for good — a drawer never re-adds itself when its chunk loads.</li>
 *   <li>second pass (collecting handlers): it is skipped. When that chunk finishes loading its own
 *       drawers run {@code clearRemoved()} and rebuild again, picking the handlers up then.</li>
 * </ul>
 *
 * <p>Positions where {@code isLoaded} is false are untouched and still dropped, as upstream does.
 *
 * <p>Soft config ({@code required = false}, {@code defaultRequire = 0}) — Functional Storage is a
 * pack mod, not a dependency of this one. Checked against 1.5.8.
 */
@Mixin(value = ConnectedDrawers.class, remap = false)
public abstract class ConnectedDrawersMixin {

    @WrapOperation(
            method = "rebuild",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
                    ordinal = 0
            )
    )
    private BlockEntity roll_mod$validateWithoutLoading(
            Level level, BlockPos pos, Operation<BlockEntity> original,
            @Local List<Long> validDrawers, @Local Long connectedDrawer) {
        LevelChunk chunk = roll_mod$chunkNow(level, pos);
        if (chunk == null) {
            // Still loading: keep the link, and return null so rebuild() does not add it twice.
            validDrawers.add(connectedDrawer);
            return null;
        }
        return chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
    }

    @WrapOperation(
            method = "rebuild",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
                    ordinal = 1
            )
    )
    private BlockEntity roll_mod$collectWithoutLoading(
            Level level, BlockPos pos, Operation<BlockEntity> original) {
        LevelChunk chunk = roll_mod$chunkNow(level, pos);
        return chunk == null ? null : chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
    }

    @Unique
    @Nullable
    private static LevelChunk roll_mod$chunkNow(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return null;
        }
        return level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
