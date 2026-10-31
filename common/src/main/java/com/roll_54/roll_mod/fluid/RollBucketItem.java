package com.roll_54.roll_mod.fluid;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** A bucket named in its fluid's colour, as MI's {@code MIBucketItem}. */
public class RollBucketItem extends BucketItem {

    private final FluidNameStyle nameStyle;

    public RollBucketItem(Fluid fluid, Properties properties, FluidNameStyle nameStyle) {
        super(fluid, properties);
        this.nameStyle = nameStyle;
    }

    @Override
    public Component getName(ItemStack stack) {
        return nameStyle.apply(super.getName(stack));
    }
}
