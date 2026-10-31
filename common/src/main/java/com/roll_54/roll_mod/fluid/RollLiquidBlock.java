package com.roll_54.roll_mod.fluid;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/** The placed fluid, named in its colour like MI's {@code MIFluidBlock} (which, unlike this one, can't be placed). */
public class RollLiquidBlock extends LiquidBlock {

    private final FluidNameStyle nameStyle;

    public RollLiquidBlock(FlowingFluid fluid, Properties properties, FluidNameStyle nameStyle) {
        super(fluid, properties);
        this.nameStyle = nameStyle;
    }

    @Override
    public MutableComponent getName() {
        return nameStyle.apply(super.getName());
    }
}
