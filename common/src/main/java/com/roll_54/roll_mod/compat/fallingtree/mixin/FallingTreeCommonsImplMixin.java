package com.roll_54.roll_mod.compat.fallingtree.mixin;

import fr.rakambda.fallingtree.common.wrapper.IBlock;
import fr.rakambda.fallingtree.neoforge.common.FallingTreeCommonsImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FallingTree answers "is this block a log / leaf / wart block" from three caches filled with
 * {@code HashMap.computeIfAbsent}, and asks on every block update (its leaf-decay listener). With DimThreads every
 * dimension's block updates run on their own thread at the same time: TNT going off in three dimensions in one tick
 * threw {@code ConcurrentModificationException} in all three (seen in the DimThreads stress test). The values are
 * plain booleans and the lookups never recurse into the same cache, so concurrent maps are a drop-in.
 */
@Mixin(value = FallingTreeCommonsImpl.class, remap = false)
public abstract class FallingTreeCommonsImplMixin {
    @Shadow @Final @Mutable private Map<IBlock, Boolean> isLogBlockCache;
    @Shadow @Final @Mutable private Map<IBlock, Boolean> isLeafBlockCache;
    @Shadow @Final @Mutable private Map<IBlock, Boolean> isWartBlockCache;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void roll_mod$concurrentCaches(CallbackInfo ci) {
        this.isLogBlockCache = new ConcurrentHashMap<>(this.isLogBlockCache);
        this.isLeafBlockCache = new ConcurrentHashMap<>(this.isLeafBlockCache);
        this.isWartBlockCache = new ConcurrentHashMap<>(this.isWartBlockCache);
    }
}
