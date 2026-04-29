package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.items.ActivatableItem;
import aztech.modern_industrialization.items.armor.GraviChestPlateItem;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GraviChestPlateItem.class)
public abstract class GraviChestPlateItemMixin implements ActivatableItem, ISimpleEnergyItem {



    @ModifyConstant(method = "getEnergyCapacity", constant = @Constant(longValue = 1 << 24))
    private long injectedCapacity(long old) {
        return 1L << 30;
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void injectedBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        long energy = ((ISimpleEnergyItem)this).getStoredEnergy(stack);
        long cap = 1L << 30;

        int width = (int) Math.round(energy / (double) cap * 13);
        cir.setReturnValue(width);
    }

    @Overwrite
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (this.getStoredEnergy(stack) > 0 && this.isActivated(stack)) {
            return ItemAttributeModifiers.builder()
                    .add(
                            NeoForgeMod.CREATIVE_FLIGHT,
                            new AttributeModifier(MI.id("gravichestplate_flight"), 1, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.CHEST)
                    .add(
                            AttributeRegistry.SULFUR_ARMOR,
                            new AttributeModifier(MI.id("gravichestplate_sulfur_armor"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                            EquipmentSlotGroup.CHEST
                    )
                    .build();
        }
        return ItemAttributeModifiers.EMPTY;
    }

}