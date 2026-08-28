package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;


/** Any catch, junk and treasure included. */
@AutoDailyTask
public final class GoFishingTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.FISHING_ROD);

    @Override
    public String id() {
        return "go_fishing";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.FISH;
    }

    @Override
    public int baseAmount() {
        return 8;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof ItemStack stack && !stack.isEmpty();
    }
}
