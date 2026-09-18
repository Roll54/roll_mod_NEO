package com.roll_54.roll_mod.minestar.hub;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMAttachment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The progression ladder a player climbs, stored per player as {@link RMMAttachment#TIER}.
 *
 * <p>The tier is a plain int rather than an enum because it is persisted: a number survives a
 * reordering or renaming of the ladder, where a serialized enum name would not. The names live in
 * lang under {@code tier.roll_mod.<n>}, so they can be retitled without touching code.
 */
public final class PlayerTier {

    /** Lowest tier, and what a player who has never been assigned one has. */
    public static final int MIN = 0;

    /** Highest tier. Adding a rung means bumping this and adding two lang keys. */
    public static final int MAX = 8;

    private PlayerTier() {}

    public static int of(Player player) {
        return clamp(player.getData(RMMAttachment.TIER));
    }

    public static void set(Player player, int tier) {
        player.setData(RMMAttachment.TIER, clamp(tier));
    }

    /**
     * Guards both ends: a value outside the ladder would render as a missing translation key rather
     * than a tier name, and stored data can predate a change to {@link #MAX}.
     */
    public static int clamp(int tier) {
        return Math.max(MIN, Math.min(MAX, tier));
    }

    /** {@code tier.roll_mod.<n>} — e.g. "Steampunk". */
    public static Component name(int tier) {
        return Component.translatable("tier." + RollMod.MODID + "." + clamp(tier));
    }

    /** The player's tier as a name, for the hub's home screen. */
    public static Component name(Player player) {
        return name(of(player));
    }
}
