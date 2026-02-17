package com.roll_54.roll_mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public class SoundPlushBlock extends FacingPlushBlock {

    private final Supplier<SoundEvent> sound;

    public SoundPlushBlock(Properties properties, Supplier<SoundEvent> sound) {
        super(properties);
        this.sound = sound;
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
                    sound.get(),
                    SoundSource.BLOCKS,
                    2.0F,
                    1.0F
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
