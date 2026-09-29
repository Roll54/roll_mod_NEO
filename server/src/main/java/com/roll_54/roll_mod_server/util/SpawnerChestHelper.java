package com.roll_54.roll_mod_server.util;

import com.roll_54.roll_mod_server.mixin.patch.BaseSpawnerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Turns a vanilla mob spawner into a passive drop generator: instead of spawning mobs, the
 * spawner simulates the mob's loot-table drops and deposits them into a chest placed directly
 * above it. {@code SpawnerBlockEntityMixin} in the server module runs {@link #tick} in place of
 * vanilla's spawner tick whenever a container is above.
 */
public final class SpawnerChestHelper {
    private SpawnerChestHelper() {
    }

    /**
     * The item inventory of the block directly above the spawner, or {@code null} if there is none.
     * Works for chests, barrels, trapped chests, or any block exposing an item handler.
     */
    @Nullable
    public static IItemHandler containerAbove(ServerLevel level, BlockPos spawnerPos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, spawnerPos.above(), Direction.DOWN);
    }

    /**
     * One spawner tick, replacing vanilla's {@code BaseSpawner.serverTick}: counts the delay down,
     * then produces up to {@code spawnCount} mobs' worth of drops into {@code dest} and rolls the next
     * delay. No mob is ever added to the world, so vanilla's spawn gates — player range, light,
     * liquid, obstruction, crowd cap, {@code MobSpawnEvent.PositionCheck}, custom spawn rules — have
     * nothing to check and are skipped; the spawner only needs its chunk to tick.
     *
     * <p>The mob is created from its spawn data but not finalized: {@code finalizeSpawn} can add
     * entities to the world on its own (a zombie's chicken jockey, for one), which a drop generator
     * must not do.
     */
    public static void tick(ServerLevel level, BlockPos pos, BaseSpawner spawner, IItemHandler dest) {
        BaseSpawnerAccessor state = (BaseSpawnerAccessor) spawner;
        if (state.roll_mod$getSpawnDelay() == -1) {
            state.roll_mod$delay(level, pos);
        }
        if (state.roll_mod$getSpawnDelay() > 0) {
            state.roll_mod$setSpawnDelay(state.roll_mod$getSpawnDelay() - 1);
            return;
        }

        SpawnData data = state.roll_mod$getOrCreateNextSpawnData(level, level.getRandom(), pos);
        boolean produced = false;
        for (int i = 0; i < state.roll_mod$getSpawnCount(); i++) {
            Entity entity = EntityType.loadEntityRecursive(data.getEntityToSpawn(), level, e -> {
                e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, e.getYRot(), e.getXRot());
                return e;
            });
            // No entity (empty or unknown spawn data), or the chest can't take the batch: stop here.
            if (entity == null || !depositMobDrops(level, dest, entity)) {
                break;
            }
            produced = true;
        }

        if (produced) {
            level.levelEvent(2004, pos, 0); // vanilla's spawner flame puff
        }
        // Whatever happened, wait a full cycle before the next attempt, like vanilla after a spawn.
        state.roll_mod$delay(level, pos);
    }


    /**
     * Simulates {@code entity}'s loot-table drops and deposits them into {@code dest}.
     *
     * <p>All-or-nothing: the drops are inserted only if the whole batch fits. If the inventory is
     * full (or the batch cannot fit entirely) nothing is inserted and {@code false} is returned, so
     * the caller can pause the spawner without dropping or discarding any items.
     *
     * @return {@code true} if drops were produced and stored, {@code false} if nothing was stored
     *         (not a living entity, empty loot, or the inventory could not hold the whole batch).
     */
    public static boolean depositMobDrops(ServerLevel level, IItemHandler dest, Entity entity) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);

        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(living.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, living)
                .withParameter(LootContextParams.ORIGIN, living.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(fakePlayer))
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                .create(LootContextParamSets.ENTITY);
        List<ItemStack> drops = lootTable.getRandomItems(params);
        if (drops.isEmpty()) {
            return false;
        }

        // Fit check on a scratch copy of the destination so we never partially insert or lose items.
        ItemStackHandler scratch = new ItemStackHandler(dest.getSlots());
        for (int i = 0; i < dest.getSlots(); i++) {
            scratch.setStackInSlot(i, dest.getStackInSlot(i).copy());
        }
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(scratch, drop.copy(), false);
            if (!leftover.isEmpty()) {
                return false; // Whole batch would not fit -> pause, keep everything.
            }
        }

        // Everything fits: commit the real insertion.
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                ItemHandlerHelper.insertItem(dest, drop.copy(), false);
            }
        }
        return true;
    }
}
