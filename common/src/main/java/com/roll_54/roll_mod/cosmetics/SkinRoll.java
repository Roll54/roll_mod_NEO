package com.roll_54.roll_mod.cosmetics;

import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A random skin, opened like a case: the server picks the skin and unlocks it at once (so a closed screen or a crash
 * never loses it), then sends the player a strip of skins with the winner in it, which their client spins and stops
 * on the winner.
 */
public final class SkinRoll {

    /** Cards in the strip, and where the winner sits — far enough in for a long spin. */
    public static final int STRIP = 48;
    public static final int WINNER_AT = 40;

    private SkinRoll() {}

    /**
     * Unlocks a random skin of {@code category} ({@code null} for any) that {@code player} does not own yet and shows
     * the roll. Returns false — after telling them — when they own every one.
     */
    public static boolean roll(ServerPlayer player, @Nullable SkinCategory category) {
        List<ItemSkinDefinition> pool = new ArrayList<>();
        List<ItemSkinDefinition> fresh = new ArrayList<>();
        var owned = ItemSkinService.get(player);
        for (ItemSkinDefinition skin : ItemSkinRegistry.all()) {
            if (category != null && skin.category() != category) continue;
            pool.add(skin);
            if (!owned.hasUnlocked(skin.skinId())) fresh.add(skin);
        }
        if (fresh.isEmpty()) {
            player.sendSystemMessage(Component.translatable("ftbquests.reward.roll_mod.random_weapon_skin.all")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        Random rng = new Random();
        ItemSkinDefinition winner = fresh.get(rng.nextInt(fresh.size()));
        if (!ItemSkinService.unlock(player, winner.skinId())) return false;

        ListTag strip = new ListTag();
        for (int i = 0; i < STRIP; i++) {
            ItemSkinDefinition card = i == WINNER_AT ? winner : pool.get(rng.nextInt(pool.size()));
            strip.add(StringTag.valueOf(card.skinId().toString()));
        }
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("roll", true);
        tag.put("strip", strip);
        tag.putInt("winner", WINNER_AT);
        // Where inside the winner's card the marker stops, so no two rolls stop alike: -0.4 .. 0.4 of a card.
        tag.putFloat("jitter", (rng.nextFloat() - 0.5f) * 0.8f);
        PacketDistributor.sendToPlayer(player, new BattlePassPackets.Data(tag));
        player.sendSystemMessage(Component.translatable("ftbquests.reward.roll_mod.random_weapon_skin.claimed",
                winner.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }
}
