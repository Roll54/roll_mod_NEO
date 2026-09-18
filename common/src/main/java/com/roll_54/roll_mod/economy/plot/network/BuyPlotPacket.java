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
 * Client &rarr; server request to buy a plot, addressed by its stable id so it is independent of
 * any client-side rendering. The server re-validates everything (vacancy, price, funds) in {@link
 * PlotService#purchase}.
 */
public record BuyPlotPacket(String plotId) implements CustomPacketPayload {

  public static final Type<BuyPlotPacket> TYPE =
      new Type<>(RollMod.id("buy_plot"));

  public static final StreamCodec<RegistryFriendlyByteBuf, BuyPlotPacket> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BuyPlotPacket::plotId, BuyPlotPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(BuyPlotPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof ServerPlayer player) {
            PlotService.purchase(player, packet.plotId());
          }
        });
  }
}
