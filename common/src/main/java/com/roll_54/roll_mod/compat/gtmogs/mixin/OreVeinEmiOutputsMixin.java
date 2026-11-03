package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.roll_54.roll_mod.compat.gtmogs.OreVeinLookups;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * A vein's EMI page lists only the ore blocks it places as its outputs, so looking up the recipes of
 * {@code raw_hematite} found no vein. Here the raw ores of those ores, and the same ores in the other rocks,
 * are outputs too ({@link OreVeinLookups}); the page itself looks the same.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS (or EMI) this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.integration.emi.orevein.GTEmiOreVein")
public abstract class OreVeinEmiOutputsMixin {

    @ModifyReturnValue(method = "getOutputs", at = @At("RETURN"))
    private List<EmiStack> roll_mod$withRawOres(List<EmiStack> outputs) {
        List<ItemStack> ores = new ArrayList<>();
        for (EmiStack stack : outputs) {
            ItemStack item = stack.getItemStack();
            if (!item.isEmpty()) ores.add(item);
        }
        var related = OreVeinLookups.related(ores);
        if (related.isEmpty()) return outputs;
        List<EmiStack> all = new ArrayList<>(outputs);
        related.forEach(item -> all.add(EmiStack.of(item)));
        return all;
    }
}
