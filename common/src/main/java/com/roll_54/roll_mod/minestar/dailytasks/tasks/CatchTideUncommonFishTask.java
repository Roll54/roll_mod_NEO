package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.TideFish;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Catch uncommon fish of Tide's (by the rarity its fishing journal gives them). */
@AutoDailyTask
public final class CatchTideUncommonFishTask implements DailyTask {

    private DailyTaskIcon icon;

    @Override
    public String id() {
        return "tide_uncommon";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.FISH;
    }

    @Override
    public int baseAmount() {
        return 2;
    }

    @Override
    public String requiredMod() {
        return "tide";
    }

    @Override
    public DailyTaskIcon icon() {
        // Resolved on first use: Tide's items are not registered when the task is made.
        if (icon == null) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tide", "angelfish"));
            icon = DailyTaskIcon.of(item == Items.AIR ? Items.COD : item);
        }
        return icon;
    }

    @Override
    public boolean matches(Object subject) {
        return TideFish.is(subject, TideFish.Rarity.UNCOMMON);
    }
}
