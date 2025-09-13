package com.roll_54.roll_mod.machine;

import com.roll_54.roll_mod.registry.ModBlockEntities;
import com.roll_54.roll_mod.machine.menu.LCRMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LargeChemicalReactorBlock extends Block implements EntityBlock {
    public LargeChemicalReactorBlock() {
        super(Properties.of().strength(5.0F, 6.0F));
    }

    @Override
    public LargeChemicalReactorBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LARGE_CHEMICAL_REACTOR_BE.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            MenuProvider provider = new SimpleMenuProvider(
                    (windowId, inv, ply) -> new LCRMenu(windowId, inv, level, pos),
                    Component.translatable("block.roll_mod.large_chemical_reactor")
            );
            player.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
