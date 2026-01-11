package com.roll_54.roll_mod.block;

import com.roll_54.roll_mod.init.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RollPlushBlock extends FacingPlushBlock {

    public RollPlushBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            level.playSound(
                    null,
                    pos,
                    SoundRegistry.ROLL_CHIPUNK.get(),
                    SoundSource.BLOCKS,
                    2.0F,
                    1.0F
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
