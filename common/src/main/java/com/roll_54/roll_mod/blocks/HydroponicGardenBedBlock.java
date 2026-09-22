package com.roll_54.roll_mod.blocks;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.blocks.entity.HydroponicGardenBedBlockEntity;
import com.roll_54.roll_mod.hydroponics.HydroponicNodeData;
import com.roll_54.roll_mod.hydroponics.gui.HydroponicGardenBedUI;
import com.roll_54.roll_mod.registry.BlockEntites;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that pretends to be soil: AgriCraft crops planted on top read their humidity, acidity
 * and nutrients from this block instead of from a datapack entry, and grow at 6x while it is fed.
 *
 * <p>Beds merge with their four horizontal neighbours into a shared node (see
 * {@link HydroponicNodeData}). Membership is maintained here rather than in the block entity because
 * {@code onPlace} runs before the block entity exists.
 */
public class HydroponicGardenBedBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {

    public static final MapCodec<HydroponicGardenBedBlock> CODEC = simpleCodec(HydroponicGardenBedBlock::new);

    public HydroponicGardenBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydroponicGardenBedBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Pistons must not relocate a member behind the node bookkeeping's back. */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockUIMenuType.openUI((ServerPlayer) player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos)
                instanceof HydroponicGardenBedBlockEntity bed) {
            return HydroponicGardenBedUI.build(bed, holder.player);
        }
        return ModularUI.of(UI.empty(), holder.player);
    }

    /**
     * LdLib2's default checks only that the block is still this one, with no distance at all. The
     * vanilla menu this UI replaced got its 8-block leash from {@code AbstractContainerMenu
     * .stillValid}, so without this a player could open a bed and keep editing the node from across
     * the world.
     */
    @Override
    public boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        return holder.player.level().getBlockState(holder.pos).is(this)
                && holder.player.canInteractWithBlock(holder.pos, 4.0D);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // Deliberately onPlace and not setPlacedBy, so /setblock, structures and WorldEdit build
        // real members rather than beds that belong to no node.
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            HydroponicNodeData.get(serverLevel).attach(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            HydroponicNodeData.get(serverLevel).detach(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, BlockEntites.HYDROPONIC_GARDEN_BED_BE.get(),
                HydroponicGardenBedBlockEntity::tick);
    }
}
