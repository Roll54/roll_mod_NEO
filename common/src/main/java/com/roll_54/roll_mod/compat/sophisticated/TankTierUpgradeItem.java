package com.roll_54.roll_mod.compat.sophisticated;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import javax.annotation.Nullable;

/**
 * A Sophisticated Storage tier upgrade for tanks only: it upgrades a tank exactly as SS's own upgrade of the same tier
 * would (the whole column at once, one upgrade per tank in it), and does nothing on chests, barrels or shulker
 * boxes. SS's own upgrades, in turn, no longer work on tanks ({@code mixin.StorageTierUpgradeItemMixin}).
 */
public class TankTierUpgradeItem extends StorageTierUpgradeItem {

    public TankTierUpgradeItem(TierUpgrade tier) {
        super(tier);
    }

    @Override
    public InteractionResult tryUpgradeStorage(ItemStack stack, Level level, BlockPos pos, BlockState state,
                                               @Nullable Player player) {
        if (!(state.getBlock() instanceof TankBlock)) return InteractionResult.PASS;
        return super.tryUpgradeStorage(stack, level, pos, state, player);
    }
}
