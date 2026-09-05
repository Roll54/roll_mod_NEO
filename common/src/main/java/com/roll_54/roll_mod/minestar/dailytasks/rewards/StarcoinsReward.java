package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskRewards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * A payout the mod cannot express in items: it hands the work to another mod's command.
 *
 * <p>This is the reason rewards are pluggable at all — server currency, ranks, permissions and
 * anything else that already has a command can be paid out without this mod taking a dependency on
 * it. Nothing here needs the target mod to be loaded at compile time; if the command does not exist
 * the failure is logged and the claim still completes.
 */
@AutoDailyReward
public final class StarcoinsReward implements DailyReward {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.GOLD_NUGGET);

    /** {@code %player%} is substituted by {@link DailyTaskRewards#runCommand}. */
    private static final String COMMAND = "eco give %player% 250";

    @Override
    public String id() {
        return "starcoins";
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public int weight() {
        // Currency is the most broadly useful payout, so it comes up more often than the others.
        return 2;
    }

    @Override
    public void grant(ServerPlayer player) {
        DailyTaskRewards.runCommand(player, COMMAND);
    }
}
