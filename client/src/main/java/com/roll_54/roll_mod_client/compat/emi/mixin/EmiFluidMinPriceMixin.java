package com.roll_54.roll_mod_client.compat.emi.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * A fluid's minimal price (per bucket) under its name in EMI, like items get it from the tooltip event.
 * EMI builds fluid tooltips itself, so that event never sees them.
 */
@Mixin(value = FluidEmiStack.class, remap = false)
public abstract class EmiFluidMinPriceMixin {

    @ModifyReturnValue(method = "getTooltipText", at = @At("RETURN"))
    private List<Component> roll_mod$minPrice(List<Component> lines) {
        if (!(((EmiStack) (Object) this).getKey() instanceof Fluid fluid)) return lines;
        Component line = ClientMinPrices.fluidLine(BuiltInRegistries.FLUID.getKey(fluid).toString());
        if (line == null) return lines;
        List<Component> out = new ArrayList<>(lines);
        out.add(Math.min(1, out.size()), line.copy().withStyle(ChatFormatting.GOLD));
        return out;
    }
}
