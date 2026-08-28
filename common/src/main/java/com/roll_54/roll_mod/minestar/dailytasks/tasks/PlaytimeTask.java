package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * Time spent online, in minutes. 15 base, so a player in no party sees 30 minutes.
 *
 * <p>In a party the requirement scales like any other task, but so does the rate: every member's
 * time counts towards the shared total, so a party of three still finishes it in about the same
 * wall-clock time as a solo player.
 */
@AutoDailyTask
public final class PlaytimeTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.CLOCK);

    @Override
    public String id() {
        return "playtime";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.PLAYTIME;
    }

    @Override
    public int baseAmount() {
        return 15;
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
