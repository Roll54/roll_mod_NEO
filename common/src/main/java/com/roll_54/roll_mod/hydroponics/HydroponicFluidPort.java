package com.roll_54.roll_mod.hydroponics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * The fluid face of a Hydroponic Garden Bed, so pipes can feed the shared buffers directly instead
 * of going through the GUI slots.
 *
 * <p>Three tanks, all input-only: water, fertilizer, acidity. The bed is a sink and never a source,
 * so {@code drain} always returns empty — a pipe that could pull the water back out would make the
 * buffers meaningless.
 *
 * <p>The acidity tank reports itself as empty on purpose: its buffer is a <i>signed</i> reservoir
 * that holds acid above zero and base below it, which no single {@link FluidStack} can describe.
 * The GUI shows it properly; pipes only ever need {@code fill} to work.
 */
public record HydroponicFluidPort(Supplier<HydroponicNode> node) implements IFluidHandler {

    public static final int TANK_WATER = 0;
    public static final int TANK_FERTILIZER = 1;
    public static final int TANK_ACIDITY = 2;

    @Override
    public int getTanks() {
        return 3;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        HydroponicNode current = node.get();
        if (current == null) {
            return FluidStack.EMPTY;
        }
        return switch (tank) {
            case TANK_WATER -> current.water() > 0
                    ? new FluidStack(Fluids.WATER, current.water())
                    : FluidStack.EMPTY;
            case TANK_FERTILIZER -> fertilizerStack(current);
            default -> FluidStack.EMPTY;
        };
    }

    private static FluidStack fertilizerStack(HydroponicNode node) {
        if (node.fertilizer() <= 0 || node.fertilizerFluidId().isEmpty()) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(node.fertilizerFluidId());
        if (id == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, node.fertilizer());
    }

    @Override
    public int getTankCapacity(int tank) {
        HydroponicNode current = node.get();
        if (current == null) {
            return 0;
        }
        return switch (tank) {
            case TANK_WATER -> current.waterCapacity();
            case TANK_FERTILIZER -> current.fertilizerCapacity();
            default -> HydroponicNode.ACIDITY_RANGE;
        };
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return HydroponicReagents.fluidValue(kindOf(tank), stack.getFluid()) != 0;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        HydroponicNode current = node.get();
        if (current == null || resource.isEmpty()) {
            return 0;
        }
        // Route by what the fluid actually is rather than by tank index: pipes address the block,
        // not a specific tank, and the data maps already say where each fluid belongs.
        for (HydroponicReagents.Kind kind : new HydroponicReagents.Kind[]{
                HydroponicReagents.Kind.FERTILIZER,
                HydroponicReagents.Kind.ACIDITY,
                HydroponicReagents.Kind.WATER}) {
            if (HydroponicReagents.fluidValue(kind, resource.getFluid()) != 0) {
                return HydroponicReagents.fill(current, kind, resource, action.simulate());
            }
        }
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }

    private static HydroponicReagents.Kind kindOf(int tank) {
        return switch (tank) {
            case TANK_WATER -> HydroponicReagents.Kind.WATER;
            case TANK_FERTILIZER -> HydroponicReagents.Kind.FERTILIZER;
            default -> HydroponicReagents.Kind.ACIDITY;
        };
    }
}
