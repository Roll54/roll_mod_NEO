package com.roll_54.roll_mod.economy.plot.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.plot.PlotSnapshot;
import com.roll_54.roll_mod.economy.plot.client.ClientPlotCache;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server &rarr; client snapshot of every plot for the receiving player (price and affordability are
 * baked per-viewer). Sent on open and whenever a purchase changes the board.
 */
public record SyncPlotsPacket(List<PlotSnapshot> plots) implements CustomPacketPayload {

  public static final Type<SyncPlotsPacket> TYPE =
      new Type<>(RollMod.id("sync_plots"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlotsPacket> STREAM_CODEC =
      StreamCodec.of(SyncPlotsPacket::encode, SyncPlotsPacket::decode);

  private static void encode(RegistryFriendlyByteBuf buf, SyncPlotsPacket packet) {
    buf.writeVarInt(packet.plots.size());
    for (PlotSnapshot p : packet.plots) {
      buf.writeUtf(p.id());
      buf.writeVarInt(p.pixelX());
      buf.writeVarInt(p.pixelY());
      buf.writeVarInt(p.pixelW());
      buf.writeVarInt(p.pixelH());
      buf.writeVarInt(p.kind().ordinal());
      buf.writeBoolean(p.ownerName() != null);
      if (p.ownerName() != null) {
        buf.writeUtf(p.ownerName());
      }
      buf.writeVarLong(p.price());
      buf.writeBoolean(p.affordable());
      buf.writeVarLong(p.startingPrice());
      buf.writeBoolean(p.listed());
      buf.writeVarLong(p.listingPrice());
    }
  }

  private static SyncPlotsPacket decode(RegistryFriendlyByteBuf buf) {
    int count = buf.readVarInt();
    List<PlotSnapshot> plots = new ArrayList<>(count);
    PlotSnapshot.Kind[] kinds = PlotSnapshot.Kind.values();
    for (int i = 0; i < count; i++) {
      String id = buf.readUtf();
      int px = buf.readVarInt();
      int py = buf.readVarInt();
      int pw = buf.readVarInt();
      int ph = buf.readVarInt();
      PlotSnapshot.Kind kind = kinds[buf.readVarInt()];
      String ownerName = buf.readBoolean() ? buf.readUtf() : null;
      long price = buf.readVarLong();
      boolean affordable = buf.readBoolean();
      long startingPrice = buf.readVarLong();
      boolean listed = buf.readBoolean();
      long listingPrice = buf.readVarLong();
      plots.add(
          new PlotSnapshot(
              id,
              px,
              py,
              pw,
              ph,
              kind,
              ownerName,
              price,
              affordable,
              startingPrice,
              listed,
              listingPrice));
    }
    return new SyncPlotsPacket(plots);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  /** Client-side: refresh the cache so any open plot UI re-renders on its next tick. */
  public static void handle(SyncPlotsPacket packet, IPayloadContext context) {
    context.enqueueWork(() -> ClientPlotCache.set(packet.plots()));
  }
}
