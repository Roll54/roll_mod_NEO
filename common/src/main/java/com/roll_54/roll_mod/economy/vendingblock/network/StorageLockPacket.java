package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorStorage;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StorageLockPacket(BlockPos pos, int slot, boolean locked)
    implements CustomPacketPayload {

  public static final Type<StorageLockPacket> TYPE =
      new Type<>(RollMod.id("storage_lock"));

  public static final StreamCodec<ByteBuf, StorageLockPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          StorageLockPacket::pos,
          ByteBufCodecs.VAR_INT,
          StorageLockPacket::slot,
          ByteBufCodecs.BOOL,
          StorageLockPacket::locked,
          StorageLockPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(StorageLockPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendor
              && (vendor.isOwner(player)
                  || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))
              && packet.slot() >= 0
              && packet.slot() < VendorStorage.SLOTS) {
            vendor.storage.setLocked(packet.slot(), packet.locked());
          }
        });
  }
}
