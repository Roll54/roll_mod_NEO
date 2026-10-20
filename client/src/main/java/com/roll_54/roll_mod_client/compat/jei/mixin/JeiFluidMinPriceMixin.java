package com.roll_54.roll_mod_client.compat.jei.mixin;

import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a fluid's minimal price (per bucket) to its JEI tooltip. JEI has no API for extra fluid
 * tooltip lines, but every fluid it shows — ingredient list and recipe slots alike — is described
 * by this one method, so one line here covers all of them.
 *
 * <p>Targets JEI by name (only its API is on the compile classpath); the config is soft, so
 * without JEI this is skipped.
 */
@Mixin(targets = "mezz.jei.neoforge.platform.FluidHelper", remap = false)
public abstract class JeiFluidMinPriceMixin {

    @Inject(
            method = "getTooltip(Ljava/util/List;Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("TAIL"))
    private void roll_mod$minPrice(List<Component> tooltip, FluidStack stack, TooltipFlag flag, CallbackInfo ci) {
        Component line = ClientMinPrices.fluidLine(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
        if (line == null) return;
        // Right under the name, like the item tooltip; the mod name and the id stay last.
        tooltip.add(Math.min(1, tooltip.size()), line.copy().withStyle(ChatFormatting.GOLD));
    }
}
