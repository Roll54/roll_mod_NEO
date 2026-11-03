package com.roll_54.roll_mod.minestar.hub.warp.ftb;

import com.roll_54.roll_mod.minestar.hub.warp.ClientWarpCache;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpData;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens a quest-locked official warp (the ones with the yellow shield) to whoever claims it. Nothing is
 * stored here: FTB Quests remembers who claimed which reward, on both sides, and {@code QuestGate}
 * asks it — so a warp is open to a player once they have claimed any such reward naming it.
 *
 * <p>The warp is picked in the quest editor from a dropdown and kept by id, so renaming it changes nothing.
 * A name typed into the quest file by hand (any case) works too.
 */
public class WarpUnlockReward extends Reward {

    private String warp = "";

    public WarpUnlockReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return WarpUnlockRewardType.WARP_UNLOCK;
    }

    /** Whether this reward opens {@code target}. */
    public boolean opens(Warp target) {
        String name = warp.strip();
        if (name.isEmpty()) return false;
        return name.equalsIgnoreCase(target.name()) || name.equalsIgnoreCase(target.id().toString());
    }

    /** The claim itself is what opens the warp; this only tells the player. */
    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (notify && !warp.isBlank()) {
            player.sendSystemMessage(Component.translatable("ftbquests.reward.roll_mod.warp_unlock.claimed", serverName(player))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public Component getAltTitle() {
        return Component.translatable("ftbquests.reward.roll_mod.warp_unlock.title",
                displayName(warp.strip()));
    }

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeData(tag, provider);
        tag.putString("warp", warp);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider provider) {
        super.readData(tag, provider);
        warp = tag.getString("warp");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(warp);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        warp = buffer.readUtf();
    }

    /**
     * The warp is picked from a dropdown of the official warps the editing client knows (the list is sent
     * at login and whenever the hub is open), stored by id. A value that is not among them — a warp
     * created since, or one named by hand in the quest file — stays selectable as it is.
     */
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        List<String> options = new ArrayList<>();
        options.add("");
        for (Warp known : ClientWarpCache.WARPS) {
            if (known.isAdmin()) options.add(known.id().toString());
        }
        if (!options.contains(warp)) options.add(warp);
        NameMap<String> names = NameMap.of("", options)
                .name(WarpUnlockReward::displayName)
                .create();
        config.addEnum("warp", warp, v -> warp = v, names).setNameKey("roll_mod.reward.warp");
    }

    /** The warp's name for the claim message, from the server's own list. */
    private String serverName(ServerPlayer player) {
        for (Warp known : WarpData.get(player.server).all()) {
            if (opens(known)) return known.name();
        }
        return warp.strip();
    }

    /** What a stored value reads as: the warp's name when this side knows it, the value as written otherwise. */
    private static Component displayName(String value) {
        if (value.isBlank()) return Component.translatable("roll_mod.reward.warp.none");
        for (Warp known : ClientWarpCache.WARPS) {
            if (value.equalsIgnoreCase(known.id().toString()) || value.equalsIgnoreCase(known.name())) {
                return Component.literal(known.name());
            }
        }
        return Component.literal(value);
    }
}
