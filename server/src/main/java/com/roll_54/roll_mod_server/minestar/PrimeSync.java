package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

import static ua.com.minestar.Minestar.minestar;

/**
 * On login, Minestar says whether the player has Prime, and {@link PrimeManager} puts them in (or out of) the
 * LuckPerms {@code prime} group. Kept apart from {@code PrimeManager} so the rest of the server builds and runs
 * without the Minestar mod: this file is only compiled when its jar is there (server/build.gradle).
 */
@EventBusSubscriber(modid = RollModServer.MODID, value = Dist.DEDICATED_SERVER)
public final class PrimeSync {

    private PrimeSync() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String name = player.getName().getString();
        UUID playerUuid = player.getUUID();

        minestar.getUserByProfileUuid(playerUuid)
                .onSuccess(minestarUser -> minestarUser.getPrimeStatus()
                        .ifPresentOrElse(primeStatus -> {
                            RollMod.LOGGER.info("Player {} have prime", name);
                            PrimeManager.setPrimeGroup(playerUuid, true);
                        }, () -> {
                            RollMod.LOGGER.info("Player {} don't have prime", name);
                            PrimeManager.setPrimeGroup(playerUuid, false);
                        }))
                .onFailure(cause -> RollMod.LOGGER.error("Failed to get prime status", cause));
    }
}
