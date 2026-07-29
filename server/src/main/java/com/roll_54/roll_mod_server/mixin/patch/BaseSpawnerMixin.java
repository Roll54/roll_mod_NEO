package com.roll_54.roll_mod_server.mixin.patch;

import com.roll_54.roll_mod_server.util.SpawnerChestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

/**
 * Repurposes the vanilla mob spawner as a drop generator when a chest sits directly above it.
 *
 * <p>The chest-less case is already halted by {@link SpawnerBlockEntityMixin} (it cancels
 * {@code SpawnerBlockEntity.serverTick} when no chest is above), so these redirects only ever take
 * their chest-present branch for a block spawner with a chest above; every {@code else} path is the
 * unchanged vanilla behavior (also covering the rare minecart spawner, which never has a chest).
 *
 * <p>The trailing {@code (ServerLevel, BlockPos)} parameters on each handler are the enclosing
 * {@code serverTick(ServerLevel, BlockPos)} arguments, captured so we can find the chest.
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {

    // Vanilla's own private "player within requiredPlayerRange" test; shadowed so the minecart
    // (no-chest) fallback below still behaves like vanilla.
    @Shadow
    private boolean isNearPlayer(Level level, BlockPos pos) {
        throw new AssertionError("shadow");
    }

    // Bypass the "is a player nearby?" gate: with a chest above, the spawner produces whenever its
    // chunk is ticking, regardless of player distance.
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BaseSpawner;isNearPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean roll_mod$bypassPlayerRange(BaseSpawner instance, Level nearLevel, BlockPos nearPos,
                                               ServerLevel level, BlockPos pos) {
        if (SpawnerChestHelper.hasContainerAbove(level, pos)) {
            return true;
        }
        return this.isNearPlayer(nearLevel, nearPos);
    }

    // Bypass the crowd cap (maxNearbyEntities): with a chest above, report zero nearby entities so a
    // build-up of same-type mobs around the spawner never pauses production.
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List roll_mod$bypassCrowdCap(ServerLevel instance, EntityTypeTest test, AABB box, Predicate pred,
                                         ServerLevel level, BlockPos pos) {
        if (SpawnerChestHelper.hasContainerAbove(level, pos)) {
            return List.of();
        }
        return instance.getEntities(test, box, pred);
    }

    // Bypass the "is there open space for the mob?" gate so drops are produced regardless of space.
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/phys/AABB;)Z"
            )
    )
    private boolean roll_mod$bypassCollision(ServerLevel instance, AABB box, ServerLevel level, BlockPos pos) {
        if (SpawnerChestHelper.hasContainerAbove(level, pos)) {
            return true;
        }
        return instance.noCollision(box);
    }

    // Bypass the light/placement spawn rules so hostile mobs "produce" even in lit, occupied spots.
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/SpawnPlacements;checkSpawnRules(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"
            )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean roll_mod$bypassRules(EntityType type, ServerLevelAccessor accessor, MobSpawnType spawnType,
                                         BlockPos spawnPos, RandomSource random, ServerLevel level, BlockPos pos) {
        if (SpawnerChestHelper.hasContainerAbove(level, pos)) {
            return true;
        }
        return SpawnPlacements.checkSpawnRules(type, accessor, spawnType, spawnPos, random);
    }

    // Replace "add the mob to the world" with "simulate its drops into the chest above".
    // Returning false when the chest is full makes vanilla take its delay()+return branch, pausing
    // production for this cycle without dropping or discarding any items.
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean roll_mod$captureDrops(ServerLevel instance, Entity entity, ServerLevel level, BlockPos pos) {
        IItemHandler dest = SpawnerChestHelper.containerAbove(level, pos);
        if (dest != null) {
            return SpawnerChestHelper.depositMobDrops(level, dest, entity);
        }
        return instance.tryAddFreshEntityWithPassengers(entity);
    }
}
