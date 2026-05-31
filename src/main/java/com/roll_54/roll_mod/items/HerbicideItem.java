package com.roll_54.roll_mod.items;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.roll_54.roll_mod.compat.argicraft.HerbicideHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class HerbicideItem extends Item {

    public HerbicideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CropBlockEntity crop)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (HerbicideHelper.applyHerbicide(stack, crop)) {
            if (crop.hasWeeds()) {
                crop.removeWeeds();
            }
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
