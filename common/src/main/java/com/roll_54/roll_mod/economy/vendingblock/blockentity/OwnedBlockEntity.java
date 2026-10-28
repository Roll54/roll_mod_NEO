package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import com.roll_54.roll_mod.economy.service.ServiceBlock;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A player-owned economy block: the vending block, the display block and the service block. Break
 * protection ({@code BreakEvent}) and the CarryOn pickup guard go through this, so a new owned block
 * only has to implement it.
 */
public interface OwnedBlockEntity {

  @Nullable
  UUID getOwnerID();

  @Nullable
  String getOwnerUser();

  /** Matches by UUID, or by name for a block whose owner was set by username. */
  boolean isOwner(Player player);

  default boolean hasOwner() {
    return getOwnerID() != null || getOwnerUser() != null;
  }

  /**
   * The owned block entity behind {@code pos}, or null. The service block's upper half has no block
   * entity of its own, so it answers for the lower half's.
   */
  @Nullable
  static OwnedBlockEntity at(BlockGetter level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    if (state.getBlock() instanceof ServiceBlock) pos = ServiceBlock.lowerHalf(state, pos);
    return level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned : null;
  }
}
