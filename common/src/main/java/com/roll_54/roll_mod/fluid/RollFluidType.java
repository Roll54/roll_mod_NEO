package com.roll_54.roll_mod.fluid;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/** A {@link FluidType} whose name — in tanks, JEI and every fluid tooltip — carries the fluid's colour. */
public class RollFluidType extends FluidType {

    private final FluidNameStyle nameStyle;

    public RollFluidType(Properties properties, FluidNameStyle nameStyle) {
        super(properties);
        this.nameStyle = nameStyle;
    }

    @Override
    public Component getDescription() {
        return nameStyle.apply(super.getDescription());
    }

    @Override
    public Component getDescription(FluidStack stack) {
        return nameStyle.apply(super.getDescription(stack));
    }
}
