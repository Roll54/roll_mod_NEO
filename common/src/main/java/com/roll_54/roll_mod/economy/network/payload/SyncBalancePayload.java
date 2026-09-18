package com.roll_54.roll_mod.economy.network.payload;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncBalancePayload(long amount, CurrencyType currencyType)
    implements CustomPacketPayload {

  public static final Type<SyncBalancePayload> TYPE =
      new Type<>(
          RollMod.id("sync_balance"));

  public static final StreamCodec<ByteBuf, SyncBalancePayload> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_LONG,
          SyncBalancePayload::amount,
          ByteBufCodecs.STRING_UTF8.map(CurrencyType::valueOf, CurrencyType::name),
          SyncBalancePayload::currencyType,
          SyncBalancePayload::new);

  @Override
  public @NotNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
