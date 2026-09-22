package com.roll_54.roll_mod.economy.currency;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyOfferService;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.database.DatabaseManager;
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

    // Without a database the read would answer 0, and greeting everyone with a balance of zero is
    // worse than greeting them with nothing.
    if (!DatabaseManager.getInstance().isAvailable()) return;

    CurrencyService.get(player, CurrencyType.MAIN)
        .thenAcceptAsync(
            balance ->
                player.sendSystemMessage(
                    Component.translatable(
                        "roll_mod.balance.main",
                        balance,
                        Component.translatable("currency.rollcurrency.name"))),
            player.server);

    // An offer stands for a week, so for anyone paid while they were away this is the first they
    // hear of it — and the line it prints carries the accept and deny buttons.
    CurrencyOfferService.greet(player);
  }
}
