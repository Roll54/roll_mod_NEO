package com.roll_54.roll_mod.blocks;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import com.roll_54.roll_mod.blocks.gui.ModuleInstallationTableUI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Module Installation Table: the one place a modular drill's modules can be changed. A
 * smithing table for drills — put the drill on it, and its module slots open up.
 *
 * <p>Deliberately exposes no item handler capability: hoppers and pipes must not install or strip
 * modules, because "only there, by hand" is the rule the whole module system leans on.
 */
public class ModuleInstallationTableBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {

    public static final MapCodec<ModuleInstallationTableBlock> CODEC =
            simpleCodec(ModuleInstallationTableBlock::new);

    public ModuleInstallationTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModuleInstallationTableBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
                instanceof ModuleInstallationTableBlockEntity table) {
            return ModuleInstallationTableUI.build(table, holder.player);
        }
        return ModularUI.of(UI.empty(), holder.player);
    }

    /** LdLib2's default has no distance check at all; see {@code HydroponicGardenBedBlock}. */
    @Override
    public boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        return holder.player.level().getBlockState(holder.pos).is(this)
                && holder.player.canInteractWithBlock(holder.pos, 4.0D);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ModuleInstallationTableBlockEntity table) {
            table.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
