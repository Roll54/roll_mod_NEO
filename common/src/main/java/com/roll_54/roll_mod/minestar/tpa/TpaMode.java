package com.roll_54.roll_mod.minestar.tpa;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Who a player lets ask them for a teleport.
 *
 * <p>The player's own setting, not a permission: it is theirs to loosen or tighten, and staff who
 * need to move somebody use vanilla {@code /tp}, which this does not touch.
 *
 * <p>{@link #id()} is the one spelling used everywhere — the command literal, the value written to
 * {@code minestar/tpa.json}, and the leaf of the lang key — so adding a mode is a constant here and
 * two strings per language, and nothing else.
 */
public enum TpaMode {

    EVERYONE,
    TEAM_AND_ALLIES,
    TEAM_ONLY,
    NOBODY;

    /** What a player who has never opened the window is on, so nothing changes until they say so. */
    public static final TpaMode DEFAULT = EVERYONE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.hub.tpa.mode." + id());
    }

    public Component description() {
        return Component.translatable("gui.roll_mod.hub.tpa.mode." + id() + ".desc");
    }

    /** Whether this mode has nothing to work with unless FTB Teams is installed. */
    public boolean needsTeams() {
        return this == TEAM_ONLY || this == TEAM_AND_ALLIES;
    }

    /** Parses a hand-written value; anything unrecognised reads as the default rather than failing. */
    public static TpaMode byId(@Nullable String id) {
        if (id != null) {
            String wanted = id.strip();
            for (TpaMode mode : values()) {
                if (mode.id().equalsIgnoreCase(wanted)) return mode;
            }
        }
        return DEFAULT;
    }
}
