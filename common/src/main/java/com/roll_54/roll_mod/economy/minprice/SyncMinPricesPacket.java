package com.roll_54.roll_mod.economy.minprice;

import com.roll_54.roll_mod.RollMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The minimal price table as the client shows it in tooltips: item id → price of one, fluid id →
 * price of one bucket (1000 mB). Only keys with a floor are sent; an empty packet clears the
 * client's copy (module off, or the server stopped).
 */
public record SyncMinPricesPacket(Map<String, Double> items, Map<String, Double> fluids)
    implements CustomPacketPayload {

  /** A sanity cap on what a client accepts; the real table is a few thousand entries. */
  private static final int MAX = 200_000;

  public static final Type<SyncMinPricesPacket> TYPE = new Type<>(RollMod.id("sync_min_prices"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SyncMinPricesPacket> STREAM_CODEC =
      StreamCodec.of(SyncMinPricesPacket::encode, SyncMinPricesPacket::decode);

  public static SyncMinPricesPacket empty() {
    return new SyncMinPricesPacket(Map.of(), Map.of());
  }

  private static void encode(RegistryFriendlyByteBuf buf, SyncMinPricesPacket payload) {
    write(buf, payload.items());
    write(buf, payload.fluids());
  }

  private static SyncMinPricesPacket decode(RegistryFriendlyByteBuf buf) {
    return new SyncMinPricesPacket(read(buf), read(buf));
  }

  private static void write(RegistryFriendlyByteBuf buf, Map<String, Double> map) {
    buf.writeVarInt(map.size());
    map.forEach(
        (key, price) -> {
          buf.writeUtf(key);
          buf.writeDouble(price);
        });
  }

  private static Map<String, Double> read(RegistryFriendlyByteBuf buf) {
    int count = buf.readVarInt();
    if (count < 0 || count > MAX) throw new IllegalArgumentException("min price table too large: " + count);
    Map<String, Double> map = new HashMap<>(count * 2);
    for (int i = 0; i < count; i++) map.put(buf.readUtf(), buf.readDouble());
    return Map.copyOf(map);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(SyncMinPricesPacket payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          ClientMinPrices.ITEMS = payload.items();
          ClientMinPrices.FLUIDS = payload.fluids();
        });
  }
}
