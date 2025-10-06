package com.roll_54.roll_mod.Util;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

@EventBusSubscriber(modid = RollMod.MODID) // реєстрація на ігровому (Forge) bus
public final class EndPortalOnlyPlayers {
    private EndPortalOnlyPlayers() {}

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent e) {
        Entity entity = e.getEntity();
        ResourceKey<Level> from = entity.level().dimension();
        ResourceKey<Level> to   = e.getDimension();

        // Дозволяємо ТІЛЬКИ гравців
        if (entity instanceof ServerPlayer) return;

        // Будь-яка взаємодія з Ендом заборонена для не-гравців:
        boolean touchesEnd = to == Level.END || from == Level.END;

        if (touchesEnd) {
            e.setCanceled(true);
             RollMod.LOGGER.debug("[EndPortalOnlyPlayers] Blocked {} ({}) from {} -> {}, Кординат X Y Z: {} {} {} ХТОСЬ НАМАГАВСЯ ДЮПАТИ!", entity.getName().getString(), entity.getType().toShortString(), from.location(),
                     to.location(), entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
        }
    }
}