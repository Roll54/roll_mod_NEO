package com.roll_54.roll_mod.minestar.hub.warp.ftb;

import com.roll_54.roll_mod.RollMod;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

/**
 * The FTB Quests reward type "unlock a warp", {@code roll_mod:warp_unlock}, with the quest-locked warp's
 * yellow shield as its icon. Only touched when FTB Quests is loaded (see {@code RollMod.onCommonSetup}).
 */
public final class WarpUnlockRewardType {

    public static final RewardType WARP_UNLOCK = RewardTypes.register(RollMod.id("warp_unlock"), WarpUnlockReward::new,
            () -> Icon.getIcon(RollMod.id("textures/gui/hub/teleportation/warp/locked_warp_by_quest.png")));

    private WarpUnlockRewardType() {}

    public static void init() {
        RewardType unused = WARP_UNLOCK;
    }
}
