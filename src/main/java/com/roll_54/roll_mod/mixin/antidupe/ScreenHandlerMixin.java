package com.roll_54.roll_mod.mixin.antidupe;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;


@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {

    @ModifyArg(
            method = "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/inventory/ClickType;SWAP:Lnet/minecraft/world/inventory/ClickType;"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V",
                    ordinal = 2
            ),
            index = 0
    )
    private ItemStack modifyStack(ItemStack stack) {
        return stack.split(stack.getCount());
    }
}