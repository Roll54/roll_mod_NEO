package com.roll_54.roll_mod.minestar.friends;

import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.Locale;

/** The friends tab's sort orders, picked from its drop-down. Client state only. */
public enum FriendSort {
    /** Grouped by the modpack they are playing: this server first, then other packs A-Z, offline last. */
    SERVER,
    /** Online first — here before elsewhere — then A-Z. */
    ONLINE,
    /**
     * Not friends only: everyone online on this server, A-Z, friends among them marked. Where new
     * friends are added from.
     */
    ONLINE_ONLY,
    A_Z,
    Z_A,
    /** Members of the viewer's FTB party first, then A-Z. The invite button is hidden in this mode. */
    TEAM,
    /** Not a sort but a view: the friend requests waiting either way, with their answers. */
    REQUESTS;

    private static final Comparator<FriendEntry> BY_NAME =
            Comparator.comparing(FriendEntry::name, String.CASE_INSENSITIVE_ORDER);

    public Comparator<FriendEntry> comparator() {
        return switch (this) {
            case SERVER -> Comparator.comparingInt(FriendSort::presence)
                    .thenComparing(FriendEntry::pack, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(BY_NAME);
            case ONLINE -> Comparator.comparingInt(FriendSort::presence).thenComparing(BY_NAME);
            case ONLINE_ONLY, A_Z, REQUESTS -> BY_NAME;
            case Z_A -> BY_NAME.reversed();
            case TEAM -> Comparator.comparing((FriendEntry f) -> !f.teammate()).thenComparing(BY_NAME);
        };
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.hub.friends.sort." + name().toLowerCase(Locale.ROOT));
    }

    /** 0 here, 1 online elsewhere, 2 offline. */
    private static int presence(FriendEntry f) {
        return f.here() ? 0 : f.online() ? 1 : 2;
    }
}
