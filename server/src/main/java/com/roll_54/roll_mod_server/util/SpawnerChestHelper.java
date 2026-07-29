package com.roll_54.roll_mod_server.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
 * above it. See {@code SpawnerBlockEntityMixin} / {@code BaseSpawnerMixin} in the server module.
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

    public static boolean hasContainerAbove(ServerLevel level, BlockPos spawnerPos) {
        return containerAbove(level, spawnerPos) != null;
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
