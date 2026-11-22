package com.roll_54.roll_mod.minestar.teleport;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.PlayerPositions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * The one thing {@code /back} needs that a teleport cannot provide: where somebody died.
 *
 * <p>Every other position it offers is recorded by {@link TeleportService#teleport}, which every
 * teleport in the mod goes through.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class TeleportEvents {

    private TeleportEvents() {}

    // Last, so a death a totem (or another mod) cancelled is not remembered: the player never left.
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerPositions.rememberBack(player);
        }
    }
}
