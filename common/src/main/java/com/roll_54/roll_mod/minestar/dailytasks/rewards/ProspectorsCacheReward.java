package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.minestar.dailytasks.api.RewardRange;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Several items at once, each on its own range — the worked example of a multi-part rolled payout.
 *
 * <p>The list is the whole reward: {@link #icons()} draws it and {@link #grant} pays it, so adding
 * a line to {@link #PAYOUT} changes both at once and neither can fall out of step with the other.
 * A fixed amount is welcome in the same list — {@code RewardRange.of(item, count)} is a range whose
 * ends meet, and draws as an ordinary stack with its count on it.
 */
@AutoDailyReward
public final class ProspectorsCacheReward implements DailyReward {

    /** Deliberately vanilla: a reward must not depend on a mod that may not be installed. */
    private static final List<RewardRange> PAYOUT = List.of(
            RewardRange.of(Items.IRON_INGOT, 8, 16),
            RewardRange.of(Items.GOLD_INGOT, 4, 8),
            RewardRange.of(Items.DIAMOND, 1, 3),
            RewardRange.of(Items.EXPERIENCE_BOTTLE, 6));

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.CHEST);

    private static final List<DailyTaskIcon> PREVIEW = PAYOUT.stream().map(RewardRange::icon).toList();

    @Override
    public String id() {
        return "prospectors_cache";
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public List<DailyTaskIcon> icons() {
        return PREVIEW;
    }

    @Override
    public void grant(ServerPlayer player) {
        PAYOUT.forEach(part -> part.grant(player));
    }
}
