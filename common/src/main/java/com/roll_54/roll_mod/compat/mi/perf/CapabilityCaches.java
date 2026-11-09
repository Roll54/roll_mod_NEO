package com.roll_54.roll_mod.compat.mi.perf;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * MI looks up the neighbour's capability with {@code Level.getCapability} every tick in machine auto-insert, fluid
 * auto-extract and energy auto-output (2.8% of the live server thread, through block-entity lookups and every
 * capability provider of the neighbour). Item auto-extract already uses a {@link BlockCapabilityCache}; the others now
 * do too. NeoForge invalidates the cache when the neighbouring block or block entity changes, so the answer is the
 * same as an uncached lookup.
 */
public final class CapabilityCaches {
    private CapabilityCaches() {}

    /**
     * Returns {@code cache} when it still points at the same level, position and side, otherwise a new cache for them.
     * A cache is only ever used for one capability, so that is not compared.
     */
    public static <T, C> BlockCapabilityCache<T, C> forTarget(@Nullable BlockCapabilityCache<T, C> cache,
                                                               BlockCapability<T, C> capability,
                                                               ServerLevel level, BlockPos pos, C context) {
        if (cache != null && cache.level() == level && Objects.equals(cache.context(), context) && cache.pos().equals(pos)) {
            return cache;
        }
        return BlockCapabilityCache.create(capability, level, pos.immutable(), context);
    }
}
