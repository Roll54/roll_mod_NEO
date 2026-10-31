package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.genetic.AgriGenePair;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * The genome's stat pairs, keyed by gene id. The map is a plain {@code HashMap}, so gene ceilings and
 * regression can swap pairs in place instead of rebuilding the genome (whose species pair is
 * immutable anyway).
 */
@Mixin(AgriGenome.class)
public interface AgriGenomeAccessor {

    @Accessor("stats")
    Map<String, AgriGenePair<Integer>> roll_mod$getStats();
}
