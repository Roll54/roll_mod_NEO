package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;

/** Zombies specifically. */
@AutoDailyTask
public final class KillZombiesTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.ZOMBIE_HEAD);

    @Override
    public String id() {
        return "kill_zombies";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.KILL;
    }

    @Override
    public int baseAmount() {
        return 16;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof LivingEntity entity && entity.getType().is(EntityTypeTags.ZOMBIES);
    }
}
