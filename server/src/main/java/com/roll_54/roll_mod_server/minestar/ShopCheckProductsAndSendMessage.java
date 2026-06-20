package com.roll_54.roll_mod_server.minestar;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.TimeUnit;

import static ua.com.minestar.Minestar.minestar;


public final class ShopCheckProductsAndSendMessage {

    private ShopCheckProductsAndSendMessage() {}

    public static void checkAndNotifyOnLogin(ServerPlayer player) {
        minestar.getScheduler().schedule(() -> checkAndNotify(player), 3, TimeUnit.SECONDS);
    }

    private static void checkAndNotify(ServerPlayer player) {
        minestar.getUserPendingShopProductsByProfileUuidAndServerId(player.getUUID())
                .onSuccess(products -> {
                    if (products != null && !products.isEmpty()) {
                        RollMod.LOGGER.info("Found {} pending products for {}. Sending notification.", products.size(), player.getName().getString());
                        sendClickableMessage(player);
                    }
                })
                .onFailure(error -> {
                    RollMod.LOGGER.error("Failed to fetch products for {}: {}", player.getName().getString(), error.getMessage());
                });
    }

    private static void sendClickableMessage(ServerPlayer player) {
        Component message;
        message = Adventure.miniMessage(
                """
                    <gold> У вас є незабрані товари! <hover:show_text:'<dark_red>Перед тим як забрати товар, перевірте, чи у вас достатньо місця в інвентарі</dark_red>'><b><color:#00ff2a><click:run_command:'/shop-receive'>НАТИСНІТЬ на цей текст, щоб забрати їх.</click></color></b></hover>
                    """.strip()
        );
        player.sendSystemMessage(message);
    }
}