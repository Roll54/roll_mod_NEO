package com.roll_54.roll_mod.cosmetics.ftb;

import com.roll_54.roll_mod.RollMod;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

/**
 * The FTB Quests reward type "random weapon skin", {@code roll_mod:random_weapon_skin}, with the hub's skins tab icon.
 * Only touched when FTB Quests is loaded (see {@code RollMod.onCommonSetup}).
 */
public final class RandomWeaponSkinRewardType {

    public static final RewardType RANDOM_WEAPON_SKIN = RewardTypes.register(RollMod.id("random_weapon_skin"),
            RandomWeaponSkinReward::new, () -> Icon.getIcon(RollMod.id("textures/gui/hub/skins_tab.png")));

    private RandomWeaponSkinRewardType() {}

    public static void init() {
        RewardType unused = RANDOM_WEAPON_SKIN;
    }
}
