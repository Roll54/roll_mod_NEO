package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;

/** Zombies, skeletons, drowned, wither skeletons and friends. */
@AutoDailyTask
public final class KillUndeadTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.ROTTEN_FLESH);

    @Override
    public String id() {
        return "kill_undead";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.KILL;
    }

    @Override
    public int baseAmount() {
        return 24;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof LivingEntity entity && entity.getType().is(EntityTypeTags.UNDEAD);
    }
}
