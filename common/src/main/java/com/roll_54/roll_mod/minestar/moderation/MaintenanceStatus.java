package com.roll_54.roll_mod.minestar.moderation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * The server-list face of {@link WhitelistStore}: while the server is closed its MOTD is replaced by
 * a yellow "server on maintenance", and the client recognises that MOTD by its translation key to
 * stop players from joining at all.
 *
 * <p>Translatable so a modded client reads it in its own language; the Ukrainian fallback is what a
 * client without roll_mod sees instead of a raw key.
 */
public final class MaintenanceStatus {

    public static final String MOTD_KEY = "motd.roll_mod.maintenance";
    private static final String FALLBACK = "сервер на технічних роботах";

    private MaintenanceStatus() {}

    public static Component motd() {
        return Component.translatableWithFallback(MOTD_KEY, FALLBACK).withStyle(ChatFormatting.YELLOW);
    }

    /** Whether a pinged MOTD is ours, i.e. the server said it is on maintenance. */
    public static boolean isMaintenance(Component motd) {
        if (motd == null) return false;
        if (motd.getContents() instanceof TranslatableContents t && MOTD_KEY.equals(t.getKey())) return true;
        for (Component sibling : motd.getSiblings()) {
            if (isMaintenance(sibling)) return true;
        }
        return false;
    }
}
