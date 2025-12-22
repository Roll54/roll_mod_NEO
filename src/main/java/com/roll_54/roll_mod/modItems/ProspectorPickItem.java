package com.roll_54.roll_mod.modItems;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ProspectorPickItem extends Item {

    public ProspectorPickItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);


        if (!level.isClientSide || player.isShiftKeyDown() ){
            return InteractionResultHolder.pass(stack);
        }


        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    };
}
