package com.roll_54.roll_mod_server.mixin.crash;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_server.crash.ChunkFreezer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Entity and block entity side of {@link ChunkFreezer}. Frozen block entities are already skipped through
 * {@code shouldTickBlocksAt} (see {@link ChunkFreezeServerLevelMixin}); here entities in frozen chunks skip
 * {@code guardEntityTick}, and a throwing entity / block entity freezes its chunk instead of reaching vanilla's
 * crash report.
 */
@Mixin(Level.class)
public abstract class ChunkFreezeLevelMixin {

    /**
     * Both the frozen-chunk skip and the crash freeze. The skip used to be a cancellable HEAD inject, which allocated a
     * CallbackInfo for every entity tick; folded in here it costs nothing beyond the wrap that is there anyway.
     */
    @WrapOperation(
            method = "guardEntityTick",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void rollmodserver$freezeOnEntityCrash(Consumer<Object> consumer, Object entity, Operation<Void> original) {
        Entity e = (Entity) entity;
        if (ChunkFreezer.shouldSkip(e)) return;
        try {
            original.call(consumer, entity);
        } catch (Throwable t) {
            if (!ChunkFreezer.freezeForCrash((Level) (Object) this, e.chunkPosition().toLong(), "entity " + e, t)) throw ChunkFreezer.rethrow(t);
        }
    }

    /**
     * NeoForge's {@code freshBlockEntities.forEach(be -> be.onLoad())} at the top of {@code tickBlockEntities}. One
     * throwing {@code onLoad} used to abort the forEach before {@code freshBlockEntities.clear()}, so it retried every
     * tick and the block entity ticker loop below never ran — no block entity in the dimension ticked. Run each
     * element on its own and freeze the chunk of the one that throws.
     */
    @WrapOperation(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;forEach(Ljava/util/function/Consumer;)V"))
    private void rollmodserver$freezeOnBlockEntityLoadCrash(ArrayList<BlockEntity> fresh, Consumer<BlockEntity> onLoad,
                                                            Operation<Void> original) {
        for (BlockEntity be : fresh) {
            try {
                onLoad.accept(be);
            } catch (Throwable t) {
                if (!ChunkFreezer.freezeForCrash((Level) (Object) this, ChunkPos.asLong(be.getBlockPos()),
                        "onLoad of block entity " + BlockEntityType.getKey(be.getType()) + " at " + be.getBlockPos().toShortString(), t)) {
                    throw ChunkFreezer.rethrow(t);
                }
            }
        }
    }

    @WrapOperation(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void rollmodserver$freezeOnBlockEntityCrash(TickingBlockEntity ticker, Operation<Void> original) {
        try {
            original.call(ticker);
        } catch (Throwable t) {
            if (!ChunkFreezer.freezeForCrash((Level) (Object) this, ChunkPos.asLong(ticker.getPos()),
                    "block entity " + ticker.getType() + " at " + ticker.getPos().toShortString(), t)) throw ChunkFreezer.rethrow(t);
        }
    }
}
