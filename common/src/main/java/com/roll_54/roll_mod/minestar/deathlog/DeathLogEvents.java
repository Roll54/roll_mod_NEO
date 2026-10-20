package com.roll_54.roll_mod.minestar.deathlog;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes a death down in two steps.
 *
 * <p>HIGH takes the snapshot, because Create Cybernetics' own death handler runs at the default
 * priority and strips the implants out before anything later could see them. LOWEST files it, and
 * only runs for a death that was not cancelled — a totem-like save or another mod's revive leaves
 * the staged snapshot behind, to be overwritten by the next one.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DeathLogEvents {

    private static final Map<UUID, DeathRecord> STAGED = new ConcurrentHashMap<>();

    private DeathLogEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void stage(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MyConfig.INSTANCE.deathlog.enabled.get()) return;
        try {
            STAGED.put(player.getUUID(), DeathLogService.capture(player, event.getSource()));
        } catch (RuntimeException e) {
            // A broken item or a mod's odd state must cost the log one entry, never the death itself.
            RollMod.LOGGER.error("[DeathLog] could not snapshot {}", player.getGameProfile().getName(), e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void commit(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DeathRecord record = STAGED.remove(player.getUUID());
        if (record == null) return;
        try {
            DeathLogStore.add(player.server, record);
        } catch (RuntimeException e) {
            RollMod.LOGGER.error("[DeathLog] could not save the death of {}", record.victimName(), e);
        }
    }
}
