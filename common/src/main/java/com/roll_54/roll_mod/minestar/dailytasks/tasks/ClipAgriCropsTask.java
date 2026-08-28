package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.agricraft.agricraft.common.registry.ModItems;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;

/**
 * Clipping AgriCraft crops — any plant counts. The subject is the plant id, so a variant that
 * wants one specific crop only has to compare it.
 */
@AutoDailyTask
public final class ClipAgriCropsTask implements DailyTask {

    private static final DailyTaskIcon ICON = DailyTaskIcon.of(ModItems.CLIPPER.get());

    @Override
    public String id() {
        return "clip_agri_crops";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.CLIP;
    }

    @Override
    public int baseAmount() {
        return 12;
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof String plantId && !plantId.isEmpty();
    }
}
