package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

/** Any log, of any wood type. */
@AutoDailyTask
public final class ChopLogsTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.OAK_LOG);

    @Override
    public String id() {
        return "chop_logs";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.MINE;
    }

    @Override
    public int baseAmount() {
        return 64;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof BlockState state && state.is(BlockTags.LOGS);
    }
}
