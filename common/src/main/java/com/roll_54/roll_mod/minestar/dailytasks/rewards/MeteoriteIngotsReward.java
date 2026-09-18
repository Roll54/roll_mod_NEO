package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.minestar.dailytasks.api.RewardRange;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * A rolled payout: how many ingots it pays is decided when it is claimed, so the strip advertises
 * the range instead of a count — see {@link RewardRange}.
 */
@AutoDailyReward
public final class MeteoriteIngotsReward implements DailyReward {

    private static final RewardRange INGOTS = RewardRange.of(ItemRegistry.METEORITE_METAL_INGOT, 1, 4);

    /** The emblem above Collect: the ingot alone. The amount goes on the strip, not here. */
    private static final DailyTaskIcon ICON = INGOTS.itemIcon();

    /** The strip: "1-4" over the ingot, since the count it will pay does not exist yet. */
    private static final List<DailyTaskIcon> PREVIEW = List.of(INGOTS.icon());

    @Override
    public String id() {
        return "meteorite_metal";
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
        INGOTS.grant(player);
    }
}
