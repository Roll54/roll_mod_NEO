package com.roll_54.roll_mod.minestar.interactions;

import java.util.Map;
import java.util.UUID;

/** The viewer's own interaction settings, as the last {@code SyncInteractionSettingsPacket} had them. */
public final class ClientInteractionCache {

    public static Map<UUID, InteractionSettings.Flags> FLAGS = Map.of();
    /** Bumped on every sync, so a screen can rebuild only when something arrived. */
    public static int VERSION = 0;

    private ClientInteractionCache() {}

    public static InteractionSettings.Flags of(UUID target) {
        return FLAGS.getOrDefault(target, InteractionSettings.Flags.NONE);
    }

    public static boolean ignores(UUID target) {
        return of(target).ignore();
    }
}
