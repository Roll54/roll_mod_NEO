package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.inventory.SlotPositions;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A third fluid input in the recipe viewer for the Fusion Reactor. MI lays out two (26 and 44, the
 * arrow at 66), so a recipe with three fluid inputs showed only two of them.
 *
 * Matched by category in {@code Rei.register()} rather than by the ordinal of the
 * {@code fluids(...)} call in {@code clientInit}, which shifts whenever MI adds a multiblock. The
 * row grows to the left so the gap before the arrow stays MI's. KubeJS's add-slots event still
 * fires after this, inside {@code register()}, and appends to the new builder.
 */
@Mixin(targets = "aztech.modern_industrialization.machines.init.MultiblockMachines$Rei", remap = false)
public abstract class FusionReactorReiSlotsMixin {

    @Shadow @Final private ResourceLocation category;

    @Shadow @Final @Mutable private SlotPositions.Builder fluidInputs;

    @Inject(method = "register", at = @At("HEAD"))
    private void roll_mod$threeFusionFluidInputs(CallbackInfo ci) {
        if (!"modern_industrialization".equals(category.getNamespace())
                || !"fusion_reactor".equals(category.getPath())) return;
        fluidInputs = new SlotPositions.Builder().addSlots(8, 35, 3, 1); // 3x1, x 8..62
    }
}
