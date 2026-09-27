package com.roll_54.roll_mod.compat.createcybernetics;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Remembers, per attachment instance, the last value a mixin decided was worth syncing.
 *
 * <p>Kept out of the mixin itself so the mixin stays a pair of thin wrappers, and kept free of any
 * Create Cybernetics type — the owner is an opaque {@link Object} — so nothing here class-loads that
 * mod. Only the mixin does, and the mixin only applies when the mod is present.
 *
 * <p>The map is weak and keyed by identity: {@code PlayerCyberwareData} overrides neither
 * {@code equals} nor {@code hashCode}, and the attachment is recreated when a player relogs. So a
 * fresh login is a fresh key with no entry, which reads as "changed" and syncs — the safe answer —
 * and the old entry is collected with the old attachment. Nothing to clear on logout, nothing to
 * leak. Do not key this on {@code ServerPlayer}: {@link net.minecraft.world.entity.Entity} compares
 * by network id, and ids are reused.
 *
 * <p>Server thread only, which is where player tick events run. Plain {@link WeakHashMap} is fine
 * there and cheaper than a synchronized one.
 */
public final class SyncWitness {

    private static final Map<Object, Integer> LAST_SYNCED = new WeakHashMap<>();

    private SyncWitness() {}

    /** True when {@code witness} differs from the value last passed to {@link #remember}. */
    public static boolean changed(Object owner, int witness) {
        Integer last = LAST_SYNCED.get(owner);
        return last == null || last != witness;
    }

    /** Records the value that was just synced, so the next identical tick can be skipped. */
    public static void remember(Object owner, int witness) {
        LAST_SYNCED.put(owner, witness);
    }
}
