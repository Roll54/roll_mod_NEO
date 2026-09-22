package com.roll_54.roll_mod.hydroponics;

import com.roll_54.roll_mod.data.datamap.AcidityValue;
import com.roll_54.roll_mod.data.datamap.FertilizerValue;
import com.roll_54.roll_mod.data.datamap.ModDataMaps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Turns what the player (or a pipe) puts into a Hydroponic Garden Bed into buffer contents.
 *
 * <p>Water is the one hardcoded reagent — anything in {@code c:water} counts, 1 mB of fluid to 1 mB
 * of buffer. Acidity and fertilizer are entirely data-driven through {@link ModDataMaps}, so real
 * materials (sulfur, sodium hydroxide, …) are added in a datapack rather than here.
 *
 * <p><b>Fluid data map values are per bucket</b> (1000 mB), items are per item.
 */
public final class HydroponicReagents {

    /** Which of the three shared inputs a stack or fluid is being offered to. */
    public enum Kind {
        ACIDITY(HydroponicNode.SLOT_ACIDITY),
        WATER(HydroponicNode.SLOT_WATER),
        FERTILIZER(HydroponicNode.SLOT_FERTILIZER);

        public final int slot;

        Kind(int slot) {
            this.slot = slot;
        }
    }

    private HydroponicReagents() {
    }

    // ---------------------------------------------------------------- acceptance

    /** The per-bucket (fluids) or per-item value this kind would get, or 0 if it is not accepted. */
    public static int fluidValue(Kind kind, Fluid fluid) {
        if (fluid == null) {
            return 0;
        }
        return switch (kind) {
            case WATER -> fluid.defaultFluidState().is(Tags.Fluids.WATER) ? 1000 : 0;
            case ACIDITY -> {
                AcidityValue value = ModDataMaps.acidity(fluid);
                yield value == null ? 0 : value.value();
            }
            case FERTILIZER -> {
                FertilizerValue value = ModDataMaps.fertilizer(fluid);
                yield value == null ? 0 : value.amount();
            }
        };
    }

    public static int itemValue(Kind kind, ItemStack stack) {
        return switch (kind) {
            case WATER -> 0; // water only ever arrives as a fluid, via a bucket or a pipe
            case ACIDITY -> {
                AcidityValue value = ModDataMaps.acidity(stack);
                yield value == null ? 0 : value.value();
            }
            case FERTILIZER -> {
                FertilizerValue value = ModDataMaps.fertilizer(stack);
                yield value == null ? 0 : value.amount();
            }
        };
    }

    /** Probes a stack for a fluid container without disturbing it. */
    @Nullable
    public static IFluidHandlerItem fluidHandlerOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        // The copy is mandatory: IFluidHandlerItem mutates the stack it wraps, so probing the live
        // stack in a slot would corrupt it.
        return stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
    }

    /** True if this slot should accept the stack at all — either as a reagent or as a container. */
    public static boolean accepts(Kind kind, ItemStack stack) {
        if (itemValue(kind, stack) != 0) {
            return true;
        }
        IFluidHandlerItem handler = fluidHandlerOf(stack);
        if (handler == null) {
            return false;
        }
        FluidStack contained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        return !contained.isEmpty() && fluidValue(kind, contained.getFluid()) != 0;
    }

    // ---------------------------------------------------------------- filling

    /**
     * Pushes fluid into the buffer this kind feeds.
     *
     * @return the millibuckets of fluid actually consumed
     */
    public static int fill(HydroponicNode node, Kind kind, FluidStack resource, boolean simulate) {
        int perBucket = fluidValue(kind, resource.getFluid());
        if (perBucket == 0 || resource.isEmpty()) {
            return 0;
        }
        int amount = resource.getAmount();
        return switch (kind) {
            case WATER -> node.addWater(amount, simulate);
            case FERTILIZER -> {
                int offered = (int) Math.min(Integer.MAX_VALUE, (long) amount * perBucket / 1000L);
                int accepted = node.addFertilizer(offered, fluidId(resource), simulate);
                yield accepted <= 0 ? 0 : (int) ((long) accepted * 1000L / perBucket);
            }
            case ACIDITY -> {
                int offered = (int) Math.clamp((long) amount * perBucket / 1000L,
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
                int accepted = node.addAcidity(offered, true);
                if (accepted == 0) {
                    yield 0;
                }
                // Back-convert to millibuckets so a partially-full reservoir only drinks what it
                // can actually hold. Both values carry the reagent's sign, so this stays positive.
                int millibuckets = (int) ((long) accepted * 1000L / perBucket);
                if (millibuckets <= 0) {
                    yield 0;
                }
                if (!simulate) {
                    node.addAcidity((int) ((long) millibuckets * perBucket / 1000L), false);
                }
                yield millibuckets;
            }
        };
    }

    private static String fluidId(FluidStack stack) {
        return BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
    }

    // ---------------------------------------------------------------- slot processing

    /**
     * Runs one input slot for one tick: drains a fluid container in place (handing the empty
     * container back to the same slot) or eats a single reagent item.
     */
    public static void processSlot(HydroponicNode node, Kind kind) {
        ItemStackHandler inputs = node.inputs();
        ItemStack stack = inputs.getStackInSlot(kind.slot);
        if (stack.isEmpty()) {
            return;
        }

        IFluidHandlerItem handler = fluidHandlerOf(stack);
        if (handler != null) {
            // The emptied container goes back into this same slot, so anything beyond a single item
            // would be destroyed. The handler caps insertion at one; this guards the paths that do
            // not go through it, such as setStackInSlot.
            if (stack.getCount() != 1) {
                return;
            }
            FluidStack contained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (!contained.isEmpty() && fluidValue(kind, contained.getFluid()) != 0) {
                int room = fill(node, kind, contained, true);
                if (room > 0) {
                    FluidStack drained = handler.drain(room, IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        fill(node, kind, drained, false);
                        // Slots holding a container are capped at one item, so the emptied
                        // container always has somewhere to go.
                        inputs.setStackInSlot(kind.slot, handler.getContainer());
                    }
                }
            }
            return;
        }

        int value = itemValue(kind, stack);
        if (value == 0) {
            return;
        }
        boolean consumed = switch (kind) {
            case ACIDITY -> node.addAcidity(value, true) == value && node.addAcidity(value, false) == value;
            case FERTILIZER -> node.addFertilizer(value, "", true) == value
                    && node.addFertilizer(value, "", false) == value;
            case WATER -> false;
        };
        if (!consumed) {
            return;
        }
        ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        stack.shrink(1);
        if (stack.isEmpty() && !remainder.isEmpty()) {
            inputs.setStackInSlot(kind.slot, remainder);
        } else {
            inputs.setStackInSlot(kind.slot, stack);
        }
    }
}
