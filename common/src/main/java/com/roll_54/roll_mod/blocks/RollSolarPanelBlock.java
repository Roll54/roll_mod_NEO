package com.roll_54.roll_mod.blocks;

import aztech.modern_industrialization.blocks.TickableBlock;
import com.roll_54.roll_mod.blocks.entity.RollSolarPanelBlockEntity;
import com.roll_54.roll_mod.config.MyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class RollSolarPanelBlock extends Block implements EntityBlock, TickableBlock {

    private final long baseProduction;

    public RollSolarPanelBlock(Properties props, long baseProduction) {
        super(props);
        this.baseProduction = baseProduction;
    }

    public long getBaseProduction() {
        return baseProduction;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RollSolarPanelBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Skip ticking entirely in blacklisted dimensions; the dimension can't change
        // for a placed block, so this is checked once instead of every tick.
        if (MyConfig.INSTANCE.solarPanelOptions.restrictedDimensions.contains(level.dimension().location())) {
            return null;
        }
        return TickableBlock.super.getTicker(level, state, type);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            sendProductionInfo(level, player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Reports this panel's per-tick energy output to the player, with the dimension math applied
     * (the same calculation {@code RollSolarPanelBlockEntity#tick} uses): base × round(multiplier).
     */
    public void sendProductionInfo(Level level, Player player) {
        double multiplier = RollSolarPanelBlockEntity.getDimensionMultiplier(level);
        long roundedMultiplier = Math.round(multiplier);
        long production = getBaseProduction() * roundedMultiplier;

        player.displayClientMessage(
                Component.translatable("message.roll_mod.solar_panel.production",
                        getBaseProduction(), roundedMultiplier, multiplier, production),
                false);
    }

}