package com.roll_54.roll_mod.minestar.dailytasks.ftb;

import com.roll_54.roll_mod.RollMod;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

/**
 * The FTB Quests reward type "+1 daily task", {@code roll_mod:daily_slot}, with the daily tasks' tab icon. Only
 * touched when FTB Quests is loaded (see {@code RollMod.onCommonSetup}).
 */
public final class DailySlotRewardType {

    public static final RewardType DAILY_SLOT = RewardTypes.register(RollMod.id("daily_slot"), DailySlotReward::new,
            () -> Icon.getIcon(RollMod.id("textures/gui/hub/daily/daily_tasks.png")));

    private DailySlotRewardType() {}

    public static void init() {
        RewardType unused = DAILY_SLOT;
    }
}
