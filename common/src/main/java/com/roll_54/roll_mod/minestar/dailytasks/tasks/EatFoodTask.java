package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;


/** Any food finished; the EAT hook already screens out non-food. */
@AutoDailyTask
public final class EatFoodTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.COOKED_BEEF);

    @Override
    public String id() {
        return "eat_food";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.EAT;
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
