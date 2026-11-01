package com.roll_54.roll_mod.util;

import earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.resources.fluid.util.FluidAmounts;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.armors.QuantumChestplate;
import net.swedz.extended_industrialization.item.nanosuit.NanoSuitArmorItem;
import com.roll_54.roll_mod.compat.aae.AaeOxygenCard;

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

    /** Oxygen tank of a nano chestplate and of a plain Advanced AE quantum chestplate, in mB. */
    public static final long TANK_SIZE = 10_000L;
    /** What the oxygen tank card multiplies a quantum chestplate's tank by. */
    public static final int OXYGEN_CARD_MULTIPLIER = 4;

    /**
     * True only when all four armor slots are worn nano suit pieces, or all four are Advanced AE
     * quantum armour (which also carries an oxygen tank, see {@code QuantumChestplateOxygenMixin}).
     * Mirrors {@code SpaceSuitItem.hasFullSet}.
     */
    public static boolean hasFullNanoSet(LivingEntity entity) {
        boolean nano = true;
        boolean quantum = true;
        for (ItemStack stack : entity.getArmorSlots()) {
            nano &= stack.getItem() instanceof NanoSuitArmorItem;
            quantum &= stack.getItem() instanceof QuantumArmorBase;
        }
        return nano || quantum;
    }

    /** Whether this stack is a suit piece roll_mod gives an oxygen tank (bar in the inventory). */
    public static boolean hasOxygenTank(ItemStack stack) {
        return stack.getItem() instanceof NanoSuitArmorItem || stack.getItem() instanceof QuantumChestplate;
    }

    /**
     * Tank size (mB) of an Advanced AE quantum chestplate: x4 while the oxygen tank card is installed
     * and switched on in AAE's upgrade list.
     */
    public static long quantumTankSize(ItemStack chest) {
        boolean card = AaeOxygenCard.TYPE != null
                && chest.getItem() instanceof QuantumChestplate quantum
                && quantum.isUpgradeEnabled(chest, AaeOxygenCard.TYPE);
        return card ? TANK_SIZE * OXYGEN_CARD_MULTIPLIER : TANK_SIZE;
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
