package com.roll_54.roll_mod.economy.network.client;

import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.network.payload.PlaytimeRewardPayload;
import com.roll_54.roll_mod.economy.network.payload.SyncBalancePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {

  private ClientPayloadHandler() {}

  public static void handle(final SyncBalancePayload payload, final IPayloadContext context) {
    context.enqueueWork(
        () -> {
          ClientCurrencyHolder.set(payload.currencyType(), payload.amount());
        });
  }

  public static void handle(final PlaytimeRewardPayload payload, final IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (CurrencyConfig.MAIN == null || !CurrencyConfig.MAIN.client.showPlaytimeRewards) {
            return;
          }

          var player = Minecraft.getInstance().player;
          if (player == null) return;

          String key =
              payload.session()
                  ? "roll_mod.playtime.reward.session"
                  : "roll_mod.playtime.reward.persistent";

          player.displayClientMessage(
              Component.translatable(
                  key, payload.amount(), Component.translatable("currency.rollcurrency.name")),
              false);
        });
  }
}
