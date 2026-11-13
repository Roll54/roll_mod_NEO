package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import dev.lorp.tei.fluid.TeiFluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * A fluid's minimal price (per bucket) in the tooltip of a fluid slot of a JEI recipe shown by TEI. TEI builds
 * those tooltips itself ({@code JeiFluids.tooltip}), so JEI's own {@code FluidHelper} (patched by
 * {@code JeiFluidMinPriceMixin}) is not involved. The fluid shown is caught where the method reads it; tag and list
 * slots return before that and get no line, like in JEI.
 *
 * <p>Targets TEI internals by name (the class is package-private); the config is soft.
 */
@Mixin(targets = "dev.lorp.tei.plugin.jei.JeiFluids", remap = false)
public abstract class TeiJeiFluidMinPriceMixin {

    @Unique
    private static TeiFluidStack roll_mod$shown;

    @Inject(method = "tooltip", at = @At("HEAD"))
    private static void roll_mod$reset(CallbackInfoReturnable<List<Component>> cir) {
        roll_mod$shown = null;
    }

    @ModifyExpressionValue(method = "tooltip", at = @At(value = "INVOKE",
            target = "Ldev/lorp/tei/plugin/jei/JeiFluids;displayed(Ldev/lorp/tei/plugin/jei/JeiLayout$Slot;)Ldev/lorp/tei/fluid/TeiFluidStack;"))
    private static TeiFluidStack roll_mod$catch(TeiFluidStack shown) {
        roll_mod$shown = shown;
        return shown;
    }

    @ModifyReturnValue(method = "tooltip", at = @At("RETURN"))
    private static List<Component> roll_mod$minPrice(List<Component> lines) {
        TeiFluidStack shown = roll_mod$shown;
        roll_mod$shown = null;
        if (shown == null || shown.isEmpty()) return lines;
        Component line = ClientMinPrices.fluidLine(BuiltInRegistries.FLUID.getKey(shown.getFluid()).toString());
        if (line == null) return lines;
        List<Component> out = new ArrayList<>(lines);
        // Right under the name, like the item tooltip; amount and mod name stay below.
        out.add(Math.min(1, out.size()), line.copy().withStyle(ChatFormatting.GOLD));
        return out;
    }
}
