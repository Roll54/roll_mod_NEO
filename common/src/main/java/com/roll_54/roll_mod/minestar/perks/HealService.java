package com.roll_54.roll_mod.minestar.perks;

import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;

import java.util.List;

/**
 * {@code /heal} — full health, a full stomach, no fire and no poison, on a cooldown.
 *
 * <p>Everything that was hurting the player is undone at once; beneficial effects are left alone,
 * since stripping a potion the player drank a moment ago would make the command a punishment.
 *
 * <p>The wait follows the {@code /rtp} shape exactly — bypass node, then rank meta, then the config
 * — and is stamped in ticks but stored as epoch millis, so a server restart does not hand everyone
 * a free heal.
 */
public final class HealService {

    /** When this player last healed, epoch millis. */
    public static final String TAG_HEAL_LAST_USED = "roll_mod:heal_last_used";

    private HealService() {}

    /**
     * Heals the player, or explains why it will not.
     *
     * @return whether the player was healed.
     */
    public static boolean heal(ServerPlayer player) {
        if (!LuckPermsCompat.canHeal(player)) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.heal.denied")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        long remaining = cooldownRemainingMillis(player);
        if (remaining > 0) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.heal.cooldown",
                    Durations.format(remaining)).withStyle(ChatFormatting.RED));
            return false;
        }

        player.setHealth(player.getMaxHealth());

        FoodData food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        food.setExhaustion(0.0F);

        player.clearFire();
        player.setTicksFrozen(0);

        // Copied first: removeEffect writes to the same collection.
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }

        player.getPersistentData().putLong(TAG_HEAL_LAST_USED, System.currentTimeMillis());
        player.sendSystemMessage(Component.translatable("msg.roll_mod.heal.done"));
        return true;
    }

    /** Milliseconds left before this player may heal again; {@code 0} when ready. */
    public static long cooldownRemainingMillis(ServerPlayer player) {
        if (LuckPermsCompat.canBypassHealCooldown(player)) return 0L;

        Integer meta = LuckPermsCompat.healCooldownTicks(player);
        long ticks = meta != null ? meta : MyConfig.INSTANCE.heal.cooldownTicks.get();
        if (ticks <= 0) return 0L;

        long last = player.getPersistentData().getLong(TAG_HEAL_LAST_USED);
        if (last <= 0) return 0L;

        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0L, ticks * 50L - elapsed);
    }
}
