package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Jumps, as counted by the vanilla jump statistic. */
@AutoDailyTask
public final class JumpTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.RABBIT_FOOT);

    @Override
    public String id() {
        return "jump";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.JUMP;
    }

    @Override
    public int baseAmount() {
        return 100;
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
