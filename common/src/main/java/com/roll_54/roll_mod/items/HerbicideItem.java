package com.roll_54.roll_mod.items;

import com.agricraft.agricraft.api.crop.AgriGrowthStage;
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

    /**
     * Handled in {@link #onItemUseFirst} rather than {@link #useOn} so the herbicide is applied
     * <em>before</em> AgriCraft's {@code CropBlock} gets to process the right-click. On cross crop
     * sticks AgriCraft's interaction consumes the click (it removes a crop stick) before the held
     * item's {@code useOn} would ever run, which previously made herbicide impossible to apply there.
     * {@code onItemUseFirst} runs at the very start of the interaction on both sides, so herbicide
     * applies consistently to single sticks, cross sticks and planted crops alike.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CropBlockEntity crop)) {
            return InteractionResult.PASS; // not a crop — let the normal interaction proceed
        }
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        if (HerbicideHelper.applyHerbicide(stack, crop)) {
            if (crop.hasWeeds()) {
                // Capture how grown the weed is before removal so the biomass yield can scale with it.
                AgriGrowthStage weedStage = crop.getWeedGrowthStage();
                crop.removeWeeds();
                HerbicideHelper.dropBiomass(level, pos, weedStage);
            }
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        // No herbicide charge left: don't swallow the interaction, let AgriCraft handle it.
        return InteractionResult.PASS;
    }
}
