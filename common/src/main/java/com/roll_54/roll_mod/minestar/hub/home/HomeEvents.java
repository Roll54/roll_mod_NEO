package com.roll_54.roll_mod.minestar.hub.home;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Keeps {@link KnownPlayers} — the invite dropdown's roster — current. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HomeEvents {

    private HomeEvents() {}

    /**
     * Folds a joining player into the roster.
     *
     * <p>Without this, someone who logged in for the first time after the roster was built would be
     * uninvitable until the next restart — and "my friend just joined and I cannot invite them" is
     * the exact case the dropdown exists to serve.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KnownPlayers.remember(player);
        }
    }

    /**
     * Drops the roster with the world.
     *
     * <p>It is keyed to one server's playerdata directory, so a client that leaves a singleplayer
     * world and opens another must not carry the first world's players into the second.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        KnownPlayers.clear();
    }
}
