package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;


/** Anything pulled from a furnace result slot, counted by stack size. */
@AutoDailyTask
public final class SmeltItemsTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.FURNACE);

    @Override
    public String id() {
        return "smelt_items";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.SMELT;
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
        return subject instanceof ItemStack stack && !stack.isEmpty();
    }
}
