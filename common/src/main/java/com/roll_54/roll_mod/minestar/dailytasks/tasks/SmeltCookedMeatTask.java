package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

/**
 * Cooked meat out of a furnace or smoker, counted by stack size. {@code c:foods/cooked_meat} is
 * NeoForge's own tag, and mods like Farmer's Delight add their meats to it.
 */
@AutoDailyTask
public final class SmeltCookedMeatTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.COOKED_BEEF);

    @Override
    public String id() {
        return "smelt_cooked_meat";
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
        return subject instanceof ItemStack stack && stack.is(Tags.Items.FOODS_COOKED_MEAT);
    }
}
