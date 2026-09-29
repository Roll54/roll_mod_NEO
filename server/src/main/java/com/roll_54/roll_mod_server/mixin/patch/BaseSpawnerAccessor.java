package com.roll_54.roll_mod_server.mixin.patch;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The private {@link BaseSpawner} state that {@code SpawnerChestHelper.tick} drives in place of
 * vanilla's {@code serverTick} when a chest sits above the spawner.
 */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {

    @Accessor("spawnDelay")
    int roll_mod$getSpawnDelay();

    @Accessor("spawnDelay")
    void roll_mod$setSpawnDelay(int spawnDelay);

    @Accessor("spawnCount")
    int roll_mod$getSpawnCount();

    /** Rolls the next delay and next mob from the spawn potentials, and syncs the delay to clients. */
    @Invoker("delay")
    void roll_mod$delay(Level level, BlockPos pos);

    @Invoker("getOrCreateNextSpawnData")
    SpawnData roll_mod$getOrCreateNextSpawnData(Level level, RandomSource random, BlockPos pos);
}
