package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/** Any ore block, vanilla or modded, via the common ore tag. */
@AutoDailyTask
public final class MineOresTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.IRON_ORE);

    @Override
    public String id() {
        return "mine_ores";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.MINE;
    }

    @Override
    public int baseAmount() {
        return 32;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof BlockState state && state.is(Tags.Blocks.ORES);
    }
}
