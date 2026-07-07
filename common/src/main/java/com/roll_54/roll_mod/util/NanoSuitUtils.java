package com.roll_54.roll_mod.util;

import earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.resources.fluid.util.FluidAmounts;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.swedz.extended_industrialization.item.nanosuit.NanoSuitArmorItem;

/**
 * Shared helpers for treating Extended Industrialization's nano suit as an Ad Astra space suit.
 *
 * <p>Nano detection is deliberately class-based ({@code instanceof NanoSuitArmorItem}) rather than a
 * hardcoded item-id list: every nano piece and its quantum variant is the same class, so any future
 * piece is covered automatically. The oxygen tank lives only on the chestplate (see
 * {@code NanoArmorMixin#getFluids}), so oxygen is always read from the CHEST slot.
 */
public final class NanoSuitUtils {

    private NanoSuitUtils() {
    }

    /** True only when all four armor slots are worn nano suit pieces. Mirrors {@code SpaceSuitItem.hasFullSet}. */
    public static boolean hasFullNanoSet(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!(stack.getItem() instanceof NanoSuitArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /** True when the worn chestplate's oxygen tank still holds oxygen. Mirrors {@code SpaceSuitItem.hasOxygen}. */
    public static boolean chestHasOxygen(LivingEntity entity) {
        return chestOxygenAmount(entity) > FluidAmounts.toPlatformAmount(1L);
    }

    /** Raw platform amount of oxygen in the worn chestplate's tank, or 0 if it has none. */
    public static long chestOxygenAmount(LivingEntity entity) {
        CommonStorage<FluidResource> tank = chestTank(entity);
        return tank == null ? 0L : tank.getAmount(0);
    }

    /** Capacity (platform amount) of the worn chestplate's oxygen tank, or 0 if it has none. */
    public static long chestOxygenCapacity(LivingEntity entity) {
        CommonStorage<FluidResource> tank = chestTank(entity);
        return tank == null ? 0L : tank.getLimit(0, FluidResource.BLANK);
    }

    /** Energy stored in the worn nano chestplate (grandpower units), or 0 if the chest isn't a nano piece. */
    public static long chestEnergyAmount(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof NanoSuitArmorItem nano ? nano.getStoredEnergy(chest) : 0L;
    }

    /** Energy capacity of the worn nano chestplate (grandpower units), or 0 if the chest isn't a nano piece. */
    public static long chestEnergyCapacity(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof NanoSuitArmorItem nano ? nano.getEnergyCapacity(chest) : 0L;
    }

    private static CommonStorage<FluidResource> chestTank(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return new ModifyOnlyContext(chest).find(FluidApi.ITEM);
    }
}
