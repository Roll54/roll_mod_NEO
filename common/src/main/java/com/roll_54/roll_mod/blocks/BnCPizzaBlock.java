package com.roll_54.roll_mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import umpaz.brewinandchewin.common.block.PizzaBlock;

import java.util.function.Supplier;

/**
 * Brewin' and Chewin''s {@link PizzaBlock} — its shapes, {@code servings} property (3 = whole, down to
 * 0 = one quarter left) and models — serving our own slice item. BnC's private {@code takeServing}
 * hands out its own {@code pizza_slice}, so it is reproduced here 1:1 with {@link #slice} instead:
 * right-click puts a slice into the hand (or tops up a stack of them), otherwise drops it; taking the
 * last quarter removes the block.
 */
public class BnCPizzaBlock extends PizzaBlock {
    private final Supplier<? extends Item> slice;

    public BnCPizzaBlock(Properties properties, Supplier<? extends Item> slice) {
        super(properties);
        this.slice = slice;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide && takeServing(level, pos, state, player, player.getUsedItemHand()).consumesAction()) {
            return InteractionResult.SUCCESS;
        }
        return takeServing(level, pos, state, player, player.getUsedItemHand());
    }

    private InteractionResult takeServing(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand) {
        int servings = state.getValue(SERVINGS);
        Item sliceItem = slice.get();
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            player.setItemInHand(hand, new ItemStack(sliceItem));
        } else if (held.is(sliceItem) && held.getCount() < held.getMaxStackSize()) {
            held.setCount(held.getCount() + 1);
        } else {
            // BnC replaces a full stack of slices with a single one here, deleting the rest; drop it instead.
            Block.popResource(level, pos, new ItemStack(sliceItem, 1));
        }
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level.getBlockState(pos).getValue(SERVINGS) == 0) {
            level.destroyBlock(pos, false);
        } else if (level.getBlockState(pos).getValue(SERVINGS) > 0) {
            level.setBlock(pos, state.setValue(SERVINGS, servings - 1), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
