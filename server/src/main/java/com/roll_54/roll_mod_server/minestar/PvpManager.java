package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = RollModServer.MODID)
public class PvpManager {
    public static final String TAG_PVP_DISABLED = "roll_mod:pvp_disabled";
    public static final String TAG_PVP_LAST_TOGGLED = "roll_mod:pvp_last_toggled";

    public static final long PVP_COOLDOWN_MS = Duration.of(30, ChronoUnit.MINUTES).toMillis();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;

        if (!player.getPersistentData().contains(TAG_PVP_LAST_TOGGLED)) {
            player.getPersistentData().putLong(TAG_PVP_LAST_TOGGLED, 0);
        }

        sendPvpStatusMessage(player);
    }


    public static boolean hasPvpDisabled(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TAG_PVP_DISABLED);
    }

    public static TogglePvpResult togglePvp(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long lastToggledTimeMillis = data.getLong(TAG_PVP_LAST_TOGGLED);
        long currentTimeMillis = System.currentTimeMillis();
        long timeElapsedMillis = currentTimeMillis - lastToggledTimeMillis;

        boolean isToggleOnCooldown = timeElapsedMillis < PVP_COOLDOWN_MS;
        if (isToggleOnCooldown) {
            long timeLeft = PVP_COOLDOWN_MS - timeElapsedMillis;
            long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeft) + 1;

            return new TogglePvpResult(false, secondsLeft);
        } else {
            data.putBoolean(TAG_PVP_DISABLED, !data.getBoolean(TAG_PVP_DISABLED));
            data.putLong(TAG_PVP_LAST_TOGGLED, System.currentTimeMillis());

            return new TogglePvpResult(true, null);
        }
    }


    public static void sendPvpStatusMessage(ServerPlayer player) {
        boolean isPvPDisabled = hasPvpDisabled(player);

        Component message;

        if (isPvPDisabled) {
            message = Adventure.miniMessage(
                    """
                        <yellow>Ваш PVP статус: <green>Вимкнено
                        <red><click:run_command:'/pvp'>Натисність сюди щоб ввімкнути.</click>
                        """.strip()
            );
        } else {
            message = Adventure.miniMessage(
                    """
                        <yellow>Ваш PVP статус: <red>Увімкнено
                        <green><hover:show_text:'<yellow>Це клікабельний текст'><click:run_command:'/pvp'>Натисність сюди щоб вимкнути.</click>
                        """.strip()
            );
        }

        player.sendSystemMessage(message);
    }

    public record TogglePvpResult(
            boolean success,
            Long cooldown
    ) {
    }
}
