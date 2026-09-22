package com.roll_54.roll_mod.minestar.op;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Keeps the running server in line with {@code minestar/operators.json}. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class OperatorEvents {

    private OperatorEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        OperatorStore.applyAll(event.getServer());
    }

    /**
     * Reapplies the file to one player as they arrive.
     *
     * <p>This is what makes the file the authority rather than a record: an {@code /op} typed by
     * hand lasts until that player's next login, and no longer.
     */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OperatorStore.applyTo(player);
        }
    }
}
