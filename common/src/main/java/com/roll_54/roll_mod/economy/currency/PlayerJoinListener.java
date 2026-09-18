package com.roll_54.roll_mod.economy.currency;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RollMod.MODID)
public final class PlayerJoinListener {

  @SubscribeEvent
  public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {

    if (!(event.getEntity() instanceof ServerPlayer player)) return;

    CurrencyService.get(player, CurrencyType.MAIN)
        .thenAcceptAsync(
            balance ->
                player.sendSystemMessage(
                    Component.translatable(
                        "roll_mod.balance.main",
                        balance,
                        Component.translatable("currency.rollcurrency.name"))),
            player.server);
  }
}
