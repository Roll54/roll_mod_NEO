package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * Ground covered on foot, in any direction. 500 base, so a player in no party walks 1000 blocks
 * once the team scaling is applied.
 *
 * <p>Walking, sprinting and crouching count; boats, minecarts, horses, elytra and swimming do not.
 */
@AutoDailyTask
public final class WalkDistanceTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.LEATHER_BOOTS);

    @Override
    public String id() {
        return "walk_distance";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.DISTANCE;
    }

    @Override
    public int baseAmount() {
        return 500;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof ServerPlayer;
    }
}
