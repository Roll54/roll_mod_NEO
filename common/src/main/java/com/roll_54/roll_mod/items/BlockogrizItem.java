package com.roll_54.roll_mod.items;

import com.roll_54.roll_mod.registry.TagRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import javax.annotation.Nullable;
import java.util.Optional;

public class BlockogrizItem extends DiggerItem{

    public BlockogrizItem(Tier tier, Properties properties) {
        super(tier, TagRegistry.MINEABLE_WITH_PAXEL, properties);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return getTier().getUses() * 3;
    }


    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Player player = context.getPlayer();
        if (playerHasShieldUseIntent(context)) {
            return InteractionResult.PASS;
        } else {
            Optional<BlockState> optional = this.evaluateNewBlockState(level, blockpos, player, level.getBlockState(blockpos), context);
            if (optional.isEmpty()) {
                return InteractionResult.PASS;
            } else {
                ItemStack itemstack = context.getItemInHand();
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
                }

                level.setBlock(blockpos, optional.get(), 11);
                level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, optional.get()));
                if (player != null) {
                    itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
    }

    private static boolean playerHasShieldUseIntent(UseOnContext context) {
        Player player = context.getPlayer();
        return context.getHand().equals(InteractionHand.MAIN_HAND) && player.getOffhandItem().is(Items.SHIELD) && !player.isSecondaryUseActive();
    }

    private Optional<BlockState> evaluateNewBlockState(
            Level level,
            BlockPos pos,
            @Nullable Player player,
            BlockState state,
            UseOnContext context
    ) {
        BlockState modifiedState;

        // Axe: strip logs
        modifiedState = state.getToolModifiedState(
                context,
                ItemAbilities.AXE_STRIP,
                false
        );

        if (modifiedState != null) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.AXE_STRIP,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            return Optional.of(modifiedState);
        }

        // Axe: scrape oxidation
        modifiedState = state.getToolModifiedState(
                context,
                ItemAbilities.AXE_SCRAPE,
                false
        );

        if (modifiedState != null) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.AXE_SCRAPE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            level.levelEvent(player, 3005, pos, 0);
            return Optional.of(modifiedState);
        }

        // Axe: remove wax
        modifiedState = state.getToolModifiedState(
                context,
                ItemAbilities.AXE_WAX_OFF,
                false
        );

        if (modifiedState != null) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.AXE_WAX_OFF,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            level.levelEvent(player, 3004, pos, 0);
            return Optional.of(modifiedState);
        }

        // Shovel: create paths
        modifiedState = state.getToolModifiedState(
                context,
                ItemAbilities.SHOVEL_FLATTEN,
                false
        );

        if (modifiedState != null
                && level.getBlockState(pos.above()).isAir()) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.SHOVEL_FLATTEN,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            return Optional.of(modifiedState);
        }

        // Shovel: extinguish campfires
        if (state.getBlock() instanceof CampfireBlock
                && state.getValue(CampfireBlock.LIT)) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );

            CampfireBlock.dowse(player, level, pos, state);

            return Optional.of(
                    state.setValue(CampfireBlock.LIT, false)
            );
        }

        return Optional.empty();
    }
}
