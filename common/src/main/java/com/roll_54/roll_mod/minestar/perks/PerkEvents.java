package com.roll_54.roll_mod.minestar.perks;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps {@code /fly} and {@code /heal} whole across the three moments that would otherwise lose
 * them: a login, a death and a dimension change.
 *
 * <p>{@code getPersistentData()} survives logout and restart on its own, but the player entity is
 * recreated on death and on a portal, so the two keys are carried over in {@code Clone} — the same
 * reason, and the same handler, as {@code PlaytimeManager}. The flight modifier is transient and so
 * is never saved at all, which is why {@link FlyService#apply} runs on every entry point.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class PerkEvents {

    private PerkEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FlyService.apply(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FlyService.apply(player);
        }
    }

    /**
     * A dimension the config forbids flight in takes it away on arrival, and says so — silently
     * dropping someone out of the sky would read as a bug.
     */
    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean losingIt = FlyService.enabled(player) && !FlyService.allowedHere(player);
        FlyService.apply(player);
        if (losingIt) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.fly.turnedOff")
                    .withStyle(ChatFormatting.RED));
        }
    }

    /**
     * Death and dimension changes hand out a new player entity with empty persistent data. Both keys
     * are copied rather than the whole tag: another mod's data is not this class's to move.
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        CompoundTag fresh = event.getEntity().getPersistentData();

        if (old.contains(FlyService.TAG_FLY)) {
            fresh.putBoolean(FlyService.TAG_FLY, old.getBoolean(FlyService.TAG_FLY));
        }
        if (old.contains(HealService.TAG_HEAL_LAST_USED)) {
            fresh.putLong(HealService.TAG_HEAL_LAST_USED, old.getLong(HealService.TAG_HEAL_LAST_USED));
        }
    }
}
