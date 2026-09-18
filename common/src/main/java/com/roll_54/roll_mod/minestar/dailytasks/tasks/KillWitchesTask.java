package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

/** Witches. There is no witch entity tag, so this matches the type itself. */
@AutoDailyTask
public final class KillWitchesTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.GLASS_BOTTLE);

    @Override
    public String id() {
        return "kill_witches";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.KILL;
    }

    @Override
    public int baseAmount() {
        return 3;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof LivingEntity entity && entity.getType() == EntityType.WITCH;
    }
}
