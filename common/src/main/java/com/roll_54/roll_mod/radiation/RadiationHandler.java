package com.roll_54.roll_mod.radiation;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.ModTags;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.registry.ModEffects;
import com.roll_54.roll_mod.registry.TagRegistry;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import static com.roll_54.roll_mod.data.RMMAttachment.RADIATION;

/**
 * Drives the radiation mechanic each server tick:
 * <ul>
 *   <li>Held radioactive items (main hand + offhand) raise the player's stored radiation.</li>
 *   <li>Equipped storm-protective armor reduces the gain (summed per-piece resistance, capped at 1.0).</li>
 *   <li>When not exposed, radiation slowly decays.</li>
 *   <li>The stored amount maps to one of six escalating poisoning tiers, each applying the custom
 *       Radiation Poisoning effect plus vanilla effects.</li>
 *   <li>Drinking milk instantly clears all radiation.</li>
 * </ul>
 * Numbers below are intentionally simple constants; they can be migrated to {@code ModConfigs} (like the
 * storm system) later without changing the mechanic.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public class RadiationHandler {

    /** Hard cap on stored radiation. */
    public static final int MAX_RADIATION = 1_000_000;

    // Radiation gained per tick for each matching held item, by tag tier.
    private static final int LOW_GAIN = 2;
    private static final int MEDIUM_GAIN = 10;
    private static final int HIGH_GAIN = 50;
    private static final int EXTREME_GAIN = 200;

    /** Radiation lost per tick while not exposed to any radioactive item. */
    private static final int DECAY = 5;

    /** Default resistance for a storm-protective piece that lacks the component (e.g. external armor). */
    private static final float DEFAULT_PIECE_RESISTANCE = 0.25f;

    /** How long applied effects last; refreshed every tick so they persist while the tier holds. */
    private static final int EFFECT_DURATION = 100;

    /** Lower bounds (inclusive) for tiers 1..6. */
    private static final int[] TIER_THRESHOLDS = {50_000, 150_000, 300_000, 500_000, 750_000, 900_000};

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        for (ServerPlayer player : e.getServer().getPlayerList().getPlayers()) {
            GameType gm = player.gameMode.getGameModeForPlayer();
            if (gm == GameType.CREATIVE || gm == GameType.SPECTATOR) continue;

            int gain = itemRadiation(player.getMainHandItem()) + itemRadiation(player.getOffhandItem());

            if (gain > 0) {
                float resistance = 0f;
                for (ItemStack armor : player.getInventory().armor) {
                    resistance += pieceResistance(armor);
                }
                resistance = Math.min(1.0f, resistance);
                gain = Math.round(gain * (1.0f - resistance));
            }

            int current = player.getData(RADIATION);
            int next = Mth.clamp(current + (gain > 0 ? gain : -DECAY), 0, MAX_RADIATION);
            if (next != current) {
                player.setData(RADIATION, next);
            }

            applyTierEffects(player, next);
        }
    }

    /** Drinking milk clears accumulated radiation, mirroring how milk clears effects. */
    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer player && e.getItem().is(Items.MILK_BUCKET)) {
            if (player.getData(RADIATION) != 0) {
                player.setData(RADIATION, 0);
            }
        }
    }

    private static int itemRadiation(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(TagRegistry.EXTREME_RADIOACTIVITY)) return EXTREME_GAIN;
        if (stack.is(TagRegistry.HIGH_RADIOACTIVITY)) return HIGH_GAIN;
        if (stack.is(TagRegistry.MEDIUM_RADIOACTIVITY)) return MEDIUM_GAIN;
        if (stack.is(TagRegistry.LOW_RADIOACTIVITY)) return LOW_GAIN;
        return 0;
    }

    private static float pieceResistance(ItemStack stack) {
        if (stack.isEmpty()) return 0f;
        Float resistance = stack.get(ComponentsRegistry.RADIATION_RESISTANCE.get());
        if (resistance != null) return resistance;
        if (stack.is(ModTags.STORM_PROTECTIVE_TAG)) return DEFAULT_PIECE_RESISTANCE;
        return 0f;
    }

    private static int tierFor(int radiation) {
        int tier = 0;
        for (int threshold : TIER_THRESHOLDS) {
            if (radiation >= threshold) tier++;
            else break;
        }
        return tier;
    }

    private static void applyTierEffects(ServerPlayer player, int radiation) {
        int tier = tierFor(radiation);
        if (tier <= 0) {
            player.removeEffect(ModEffects.RADIATION_POISONING);
            return;
        }

        // Amplifiers increase with tier; ascending order lets stronger entries override weaker refreshes.
        addEffect(player, ModEffects.RADIATION_POISONING, tier - 1);
        addEffect(player, MobEffects.CONFUSION, 0);                               // nausea (tier >= 1)
        if (tier >= 2) addEffect(player, MobEffects.WEAKNESS, 0);
        if (tier >= 3) {
            addEffect(player, MobEffects.DIG_SLOWDOWN, 0);                        // mining fatigue
            addEffect(player, MobEffects.POISON, 0);
        }
        if (tier >= 4) {
            addEffect(player, MobEffects.HUNGER, 0);
            addEffect(player, MobEffects.WEAKNESS, 1);
        }
        if (tier >= 5) {
            addEffect(player, MobEffects.WITHER, 0);
            addEffect(player, MobEffects.BLINDNESS, 0);
        }
        if (tier >= 6) {
            addEffect(player, MobEffects.WITHER, 1);
            addEffect(player, MobEffects.POISON, 1);
            addEffect(player, MobEffects.DIG_SLOWDOWN, 1);
        }
    }

    private static void addEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, false, false, true));
    }
}
