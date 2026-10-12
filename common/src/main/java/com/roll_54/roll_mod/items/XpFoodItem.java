package com.roll_54.roll_mod.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A food item that grants experience points to the player when fully eaten.
 * The nutrition / saturation / effects are supplied through the vanilla
 * {@link net.minecraft.world.food.FoodProperties} on the item {@link Properties};
 * this class only adds the extra XP reward on top of normal eating.
 *
 * <p>With a {@code placedBlock}, sneak-right-clicking a block serves it there on a plate
 * ({@link com.roll_54.roll_mod.blocks.PlatedFoodBlock}); a plain right-click still eats it.
 */
public class XpFoodItem extends Item {

    private final int experiencePoints;
    @Nullable
    private final Supplier<? extends Block> placedBlock;

    public XpFoodItem(Properties properties, int experiencePoints, @Nullable Supplier<? extends Block> placedBlock) {
        super(properties);
        this.experiencePoints = experiencePoints;
        this.placedBlock = placedBlock;
    }

    public int experiencePoints() {
        return experiencePoints;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Let vanilla apply nutrition, saturation and any FoodProperties effects first.
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide && entity instanceof Player player && experiencePoints > 0) {
            player.giveExperiencePoints(experiencePoints);
        }

        return result;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (placedBlock == null || player == null || !player.isSecondaryUseActive()) return InteractionResult.PASS;

        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) return InteractionResult.FAIL;
        BlockState state = placedBlock.get().getStateForPlacement(place);
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        if (state == null || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos,
                net.minecraft.world.phys.shapes.CollisionContext.of(player))) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE);
            SoundType sound = state.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (placedBlock != null) {
            tooltip.add(Component.translatable("tooltip.roll_mod.can_be_placed").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
