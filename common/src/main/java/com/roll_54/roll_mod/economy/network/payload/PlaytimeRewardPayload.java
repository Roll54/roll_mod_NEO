package com.roll_54.roll_mod.economy.network.payload;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Sent to a player when they earn an automatic playtime reward, so the client can (optionally)
 * print a chat notification. {@code session} distinguishes the two reward streams: {@code true} for
 * the per-session reward, {@code false} for the persistent one.
 */
public record PlaytimeRewardPayload(long amount, CurrencyType currencyType, boolean session)
    implements CustomPacketPayload {

  public static final Type<PlaytimeRewardPayload> TYPE =
      new Type<>(
          RollMod.id("playtime_reward"));

  public static final StreamCodec<ByteBuf, PlaytimeRewardPayload> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_LONG,
          PlaytimeRewardPayload::amount,
          ByteBufCodecs.STRING_UTF8.map(CurrencyType::valueOf, CurrencyType::name),
          PlaytimeRewardPayload::currencyType,
          ByteBufCodecs.BOOL,
          PlaytimeRewardPayload::session,
          PlaytimeRewardPayload::new);

  @Override
  public @NotNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
