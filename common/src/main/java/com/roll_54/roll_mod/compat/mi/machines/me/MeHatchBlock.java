package com.roll_54.roll_mod.compat.mi.machines.me;

import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The block of every ME hatch: MI's machine block, plus
 * <ul>
 *   <li>the LDLib2 screen ({@link BlockUIMenuType} finds a block's screen through its block);</li>
 *   <li>an item output hatch's leftover buffer copied onto its own drop (the loot params carry the block
 *       entity); see {@link MeItemOutputHatchBlockEntity#dropExtra} for how it is never also flushed.</li>
 * </ul>
 */
public class MeHatchBlock extends MachineBlock implements BlockUIMenuType.BlockUI {

    public MeHatchBlock(BiFunction<BlockPos, BlockState, MachineBlockEntity> blockEntityConstructor, Properties properties) {
        super(blockEntityConstructor, properties);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof MeHatchBlockEntity hatch) {
            return hatch.createUI(holder.player);
        }
        return ModularUI.of(UI.empty(), holder.player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MeItemOutputHatchBlockEntity hatch) {
            hatch.copyBufferTo(drops, asItem());
        }
        return drops;
    }
}
