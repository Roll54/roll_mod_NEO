package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import aztech.modern_industrialization.util.Simulation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The MI hatches built into a greenhouse, read the way an MI multiblock reads its own: through
 * {@link HatchBlockEntity}'s {@code append*} methods, which hand out the hatch's own slots and energy
 * buffer. Items go into output hatches as MI's {@code CrafterComponent.putItemOutputs} puts them.
 */
final class GreenhouseHatches {

    final List<ConfigurableItemStack> itemOutputs = new ArrayList<>();
    final List<ConfigurableFluidStack> fluidInputs = new ArrayList<>();
    final List<EnergyComponent> energyInputs = new ArrayList<>();

    private GreenhouseHatches() {}

    static GreenhouseHatches collect(Level level, Iterable<BlockPos> positions) {
        GreenhouseHatches hatches = new GreenhouseHatches();
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) {
                hatch.appendItemOutputs(hatches.itemOutputs);
                hatch.appendFluidInputs(hatches.fluidInputs);
                hatch.appendEnergyInputs(hatches.energyInputs);
            }
        }
        return hatches;
    }

    /** Takes up to {@code max} EU from the energy hatches; returns what it got. */
    long consumeEu(long max) {
        long left = max;
        for (EnergyComponent energy : energyInputs) {
            if (left <= 0) break;
            left -= energy.consumeEu(left, Simulation.ACT);
        }
        return max - left;
    }

    /** Whether all of {@code stacks} fit into the item output hatches. */
    boolean fits(List<ItemStack> stacks) {
        return insert(ConfigurableItemStack.copyList(itemOutputs), stacks);
    }

    /** Puts {@code stacks} into the item output hatches; check {@link #fits} first. */
    void put(List<ItemStack> stacks) {
        insert(itemOutputs, stacks);
    }

    /**
     * MI's insertion order: stacks already holding the item (or locked to it) first, then empty ones.
     * Returns false if anything was left over.
     */
    private static boolean insert(List<ConfigurableItemStack> slots, List<ItemStack> stacks) {
        boolean ok = true;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            ItemVariant variant = ItemVariant.of(stack);
            long remaining = stack.getCount();
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (ConfigurableItemStack slot : slots) {
                    if (remaining <= 0) break;
                    ItemVariant key = slot.getResource();
                    boolean empty = key.isBlank();
                    if (!empty && !key.equals(variant)) continue;
                    if (empty && !((slot.isMachineLocked() || slot.isPlayerLocked() || pass == 1) && slot.isValid(stack))) continue;
                    long inserted = Math.min(remaining, slot.getRemainingCapacityFor(variant));
                    if (inserted <= 0) continue;
                    if (empty) {
                        slot.setAmount(inserted);
                        slot.setKey(variant);
                    } else {
                        slot.increment(inserted);
                    }
                    remaining -= inserted;
                }
            }
            if (remaining > 0) ok = false;
        }
        return ok;
    }
}
