package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;


/** Any successful breeding; the subject is one of the parents. */
@AutoDailyTask
public final class BreedAnimalsTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.WHEAT_SEEDS);

    @Override
    public String id() {
        return "breed_animals";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.BREED;
    }

    @Override
    public int baseAmount() {
        return 6;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof LivingEntity;
    }
}
