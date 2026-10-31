package com.roll_54.roll_mod.items;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.roll_54.roll_mod.compat.argicraft.AgriCropNutrition;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Injects {@value CropBreedingSettings#MUTAGEN_PER_SYRINGE} mutagen into a planted AgriCraft crop and
 * gives the empty syringe back (crops wiki §11.7). Mutagen bleeds away at
 * {@value CropBreedingSettings#MUTAGEN_BLEED_PER_SECOND}/s, so by hand it only bridges a short window;
 * the HV Crop Manager is the other way to apply it.
 *
 * <p>Like {@link HerbicideItem}, this runs in {@link #onItemUseFirst} so AgriCraft's crop block never
 * gets the click first.
 */
public class MutagenSyringeItem extends Item {

    public MutagenSyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof CropBlockEntity crop) || !(crop instanceof AgriCropNutrition nutrition)
                || !crop.hasPlant()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        nutrition.roll_mod$addMutagen(CropBreedingSettings.MUTAGEN_PER_SYRINGE);
        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) {
            stack.shrink(1);
            ItemStack empty = new ItemStack(ItemRegistry.SYRINGE.get());
            if (player == null || !player.addItem(empty)) {
                net.minecraft.world.level.block.Block.popResource(level, context.getClickedPos(), empty);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.roll_mod.mutagen_syringe",
                CropBreedingSettings.MUTAGEN_PER_SYRINGE, CropBreedingSettings.MUTAGEN_BLEED_PER_SECOND)
                .withStyle(ChatFormatting.GRAY));
    }
}
