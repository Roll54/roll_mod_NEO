package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * A payout that touches the player rather than their inventory: extra hearts for the rest of the
 * day, topped up so the new hearts arrive full.
 *
 * <p>Deliberately an effect rather than a permanent max-health attribute modifier — a bonus that
 * can be earned every single day must not stack into an unbounded health pool. The duration
 * outlasts a normal session but expires on its own, so nothing has to clean it up at the daily
 * roll.
 */
@AutoDailyReward
public final class VitalityReward implements DailyReward {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.GOLDEN_APPLE);

    /** Six hours of real time, in ticks. */
    private static final int DURATION_TICKS = 20 * 60 * 60 * 6;

    /** Amplifier 1 → +4 hearts (the effect gives 2 hearts per level). */
    private static final int AMPLIFIER = 1;

    @Override
    public String id() {
        return "vitality";
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public void grant(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, DURATION_TICKS, AMPLIFIER));
        // Health Boost adds empty hearts; heal so the reward is felt immediately.
        player.heal(4f * (AMPLIFIER + 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
    }
}
