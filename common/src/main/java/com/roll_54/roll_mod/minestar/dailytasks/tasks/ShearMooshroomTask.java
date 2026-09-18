package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

/**
 * One mooshroom sheared, red or brown. Finding one is the hard part, so a party is not asked for
 * more — shearing turns it into a plain cow, and there may be only the one nearby.
 */
@AutoDailyTask
public final class ShearMooshroomTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.RED_MUSHROOM);

    @Override
    public String id() {
        return "shear_mooshroom";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.SHEAR;
    }

    @Override
    public int baseAmount() {
        return 1;
    }

    @Override
    public boolean scalesWithTeam() {
        return false;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof LivingEntity entity && entity.getType() == EntityType.MOOSHROOM;
    }
}
