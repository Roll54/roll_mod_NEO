package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractElectricMultipliedCraftingMultiblockBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Tesseract batch multiblock whose recipe type is missing — an MI Tweaks machine (its large thermal centrifuge,
 * electrolyzer, ore washers) whose type the pack registers from KubeJS, run where that script is absent — wrote its
 * tooltip from {@code getRecipeType().getPath()}, a {@link NullPointerException}. TEI's item list and the creative
 * search tree read every item's tooltip, so hovering it crashed the game. Without a recipe type it shows no batcher
 * lines instead, and the machine is named in the log once.
 */
@Mixin(value = AbstractElectricMultipliedCraftingMultiblockBlockEntity.class, remap = false)
public abstract class MultipliedMachineTooltipGuardMixin {

    @Unique
    private static final Set<String> roll_mod$reported = new HashSet<>();

    @Inject(method = "getTooltips", at = @At("HEAD"), cancellable = true, require = 0)
    private void roll_mod$noRecipeType(Item.TooltipContext context, CallbackInfoReturnable<List<Component>> cir) {
        AbstractElectricMultipliedCraftingMultiblockBlockEntity self = (AbstractElectricMultipliedCraftingMultiblockBlockEntity) (Object) this;
        MachineRecipeType type = self.getRecipeType();
        if (type != null) return;
        String id = String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(self.getType()));
        if (roll_mod$reported.add(id)) {
            RollMod.LOGGER.warn("[MI] Batch multiblock '{}' has no recipe type (its KubeJS type is not registered?)", id);
        }
        cir.setReturnValue(List.of());
    }
}
