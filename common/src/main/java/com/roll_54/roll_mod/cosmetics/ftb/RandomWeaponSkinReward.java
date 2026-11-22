package com.roll_54.roll_mod.cosmetics.ftb;

import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.ItemSkinService;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "A random weapon skin": unlocks one sword-slot skin (swords and sabers) the player does not own yet, picked at
 * random. Given for each main chapter's last quest. If they already own every one, they are told so.
 */
public class RandomWeaponSkinReward extends Reward {

    public RandomWeaponSkinReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return RandomWeaponSkinRewardType.RANDOM_WEAPON_SKIN;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        // Opened like a case: the client spins a strip of skins and stops on the one unlocked.
        com.roll_54.roll_mod.cosmetics.SkinRoll.roll(player, SkinCategory.SWORD);
    }

    @Override
    public Component getAltTitle() {
        return Component.translatable("ftbquests.reward.roll_mod.random_weapon_skin.title");
    }
}
