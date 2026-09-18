package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * See a village raid through to victory.
 *
 * <p>The one task that does not scale with the party: a raid is a single large undertaking, and
 * progress is shared, so one member seeing one off finishes it for everybody.
 */
@AutoDailyTask
public final class CompleteRaidTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.OMINOUS_BOTTLE);

    @Override
    public String id() {
        return "complete_raid";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.RAID;
    }

    @Override
    public int baseAmount() {
        return 1;
    }

    @Override
    public boolean scalesWithTeam() {
        return false;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof ServerPlayer;
    }
}
