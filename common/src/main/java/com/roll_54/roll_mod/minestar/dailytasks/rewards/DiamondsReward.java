package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskRewards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** The plain one: a stack straight into the inventory. */
@AutoDailyReward
public final class DiamondsReward implements DailyReward {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.DIAMOND);
    private static final int COUNT = 8;
    private static final List<DailyTaskIcon> PREVIEW =
            List.of(DailyTaskIcon.of(new ItemStack(Items.DIAMOND, COUNT)));

    @Override
    public String id() {
        return "diamonds";
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    /** The full stack, so the strip renders the diamond with its count on it. */
    @Override
    public List<DailyTaskIcon> icons() {
        return PREVIEW;
    }

    @Override
    public void grant(ServerPlayer player) {
        DailyTaskRewards.give(player, new ItemStack(Items.DIAMOND, COUNT));
    }
}
