package com.roll_54.roll_mod.economy.plot.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.plot.PlotService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client &rarr; server owner action on a plot, addressed by its stable id. {@code price} is only
 * used by {@link Action#LIST} (the resale base price the seller will receive). The server
 * re-validates ownership in {@link PlotService}.
 */
public record PlotActionPacket(Action action, String plotId, long price)
    implements CustomPacketPayload {

  public enum Action {
    LIST,
    UNLIST,
    SELL_TO_SERVER;

    private static final Action[] VALUES = values();
  }

  public static final Type<PlotActionPacket> TYPE =
      new Type<>(RollMod.id("plot_action"));

  public static final StreamCodec<RegistryFriendlyByteBuf, PlotActionPacket> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_INT.map(i -> Action.VALUES[i], a -> a.ordinal()),
          PlotActionPacket::action,
          ByteBufCodecs.STRING_UTF8,
          PlotActionPacket::plotId,
          ByteBufCodecs.VAR_LONG,
          PlotActionPacket::price,
          PlotActionPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static PlotActionPacket list(String plotId, long price) {
    return new PlotActionPacket(Action.LIST, plotId, price);
  }

  public static PlotActionPacket unlist(String plotId) {
    return new PlotActionPacket(Action.UNLIST, plotId, 0L);
  }

  public static PlotActionPacket sellToServer(String plotId) {
    return new PlotActionPacket(Action.SELL_TO_SERVER, plotId, 0L);
  }

  public static void handle(PlotActionPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof ServerPlayer player)) {
            return;
          }
          switch (packet.action()) {
            case LIST -> PlotService.listForResale(player, packet.plotId(), packet.price());
            case UNLIST -> PlotService.unlist(player, packet.plotId());
            case SELL_TO_SERVER -> PlotService.sellToServer(player, packet.plotId());
          }
        });
  }
}
