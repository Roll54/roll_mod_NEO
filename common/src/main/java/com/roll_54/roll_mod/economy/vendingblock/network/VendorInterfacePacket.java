package com.roll_54.roll_mod.economy.vendingblock.network;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The owner edits the vending block's ME interface: a config slot (item and amount, the held
 * stack's count), the priority, or the fuzzy mode. Owner or vendor key only, like the other vendor settings.
 *
 * @param action one of the {@code SET_*} / {@code CLEAR_CONFIG} constants
 * @param slot the config slot, for the config actions
 * @param stack the item to stock, for {@link #SET_CONFIG}
 * @param value the config amount, the priority, or the fuzzy mode's ordinal
 */
public record VendorInterfacePacket(BlockPos pos, int action, int slot, ItemStack stack, long value)
    implements CustomPacketPayload {

  public static final int SET_CONFIG = 0;
  public static final int CLEAR_CONFIG = 1;
  public static final int SET_PRIORITY = 2;
  public static final int SET_FUZZY_MODE = 3;

  public static final Type<VendorInterfacePacket> TYPE =
      new Type<>(RollMod.id("vendor_interface"));

  public static final StreamCodec<RegistryFriendlyByteBuf, VendorInterfacePacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          VendorInterfacePacket::pos,
          ByteBufCodecs.VAR_INT,
          VendorInterfacePacket::action,
          ByteBufCodecs.VAR_INT,
          VendorInterfacePacket::slot,
          ItemStack.OPTIONAL_STREAM_CODEC,
          VendorInterfacePacket::stack,
          ByteBufCodecs.VAR_LONG,
          VendorInterfacePacket::value,
          VendorInterfacePacket::new);

  public static VendorInterfacePacket setConfig(BlockPos pos, int slot, ItemStack stack, long amount) {
    return new VendorInterfacePacket(pos, SET_CONFIG, slot, stack.copyWithCount(1), amount);
  }

  public static VendorInterfacePacket clearConfig(BlockPos pos, int slot) {
    return new VendorInterfacePacket(pos, CLEAR_CONFIG, slot, ItemStack.EMPTY, 0);
  }

  public static VendorInterfacePacket setPriority(BlockPos pos, int priority) {
    return new VendorInterfacePacket(pos, SET_PRIORITY, 0, ItemStack.EMPTY, priority);
  }

  public static VendorInterfacePacket setFuzzyMode(BlockPos pos, FuzzyMode mode) {
    return new VendorInterfacePacket(pos, SET_FUZZY_MODE, 0, ItemStack.EMPTY, mode.ordinal());
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(VendorInterfacePacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          if (!(player.level().getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendor)
              || !(vendor.isOwner(player)
                  || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))) {
            return;
          }
          var logic = vendor.getInterfaceLogic();
          var config = logic.getConfig();
          boolean slotOk = packet.slot() >= 0 && packet.slot() < config.size();
          switch (packet.action()) {
            case SET_CONFIG -> {
              AEItemKey key = AEItemKey.of(packet.stack());
              if (slotOk && key != null) {
                // setStack clamps the amount to what the slot can hold (a stack, for items).
                config.setStack(packet.slot(), new GenericStack(key, Math.max(1, packet.value())));
              }
            }
            case CLEAR_CONFIG -> {
              if (slotOk) config.setStack(packet.slot(), null);
            }
            case SET_PRIORITY ->
                logic.setPriority((int) Math.clamp(packet.value(), Integer.MIN_VALUE, Integer.MAX_VALUE));
            case SET_FUZZY_MODE -> {
              FuzzyMode[] modes = FuzzyMode.values();
              if (packet.value() >= 0 && packet.value() < modes.length) {
                logic.getConfigManager().putSetting(Settings.FUZZY_MODE, modes[(int) packet.value()]);
              }
            }
            default -> {}
          }
        });
  }
}
