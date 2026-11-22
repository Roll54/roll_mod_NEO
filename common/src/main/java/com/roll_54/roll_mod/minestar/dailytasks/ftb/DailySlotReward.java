package com.roll_54.roll_mod.minestar.dailytasks.ftb;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * "+1 daily task": claiming it gives the player one more daily task slot for good, shown on a bluish row. Today's
 * board gets the slot at once when it has room; it is kept in the daily tasks' saved data, not here.
 */
public class DailySlotReward extends Reward {

    public DailySlotReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return DailySlotRewardType.DAILY_SLOT;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        DailyTaskManager.grantExtraSlot(player);
        if (notify) {
            player.sendSystemMessage(Component.translatable("ftbquests.reward.roll_mod.daily_slot.claimed")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public Component getAltTitle() {
        return Component.translatable("ftbquests.reward.roll_mod.daily_slot.title");
    }
}
