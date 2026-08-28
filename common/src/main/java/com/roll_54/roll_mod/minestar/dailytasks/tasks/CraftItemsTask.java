package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;


/** Anything crafted, counted by stack size. */
@AutoDailyTask
public final class CraftItemsTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.CRAFTING_TABLE);

    @Override
    public String id() {
        return "craft_items";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.CRAFT;
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
        return subject instanceof ItemStack stack && !stack.isEmpty();
    }
}
