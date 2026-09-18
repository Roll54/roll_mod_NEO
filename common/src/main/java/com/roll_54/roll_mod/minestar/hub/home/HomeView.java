package com.roll_54.roll_mod.minestar.hub.home;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * A home as one particular player is allowed to see it.
 *
 * <p>Deliberately not {@link PlayerHome}. The server's record holds the guest list as ids, and the
 * client is never told those — the UI only ever prints names, and shipping the ids would hand any
 * modified client a map of who has access to whose base. A separate record means the tab physically
 * cannot read a field the server did not send, rather than reading a field that was filled in with
 * something plausible. ({@code SyncWarpsPacket} takes the other route and invents synthetic reporter
 * ids to carry a count; this avoids needing that trick.)
 *
 * @param relation what this viewer is to this home, which decides the whole row and panel
 * @param roster the owner's guest list; empty for everyone else
 */
public record HomeView(UUID id, UUID owner, String ownerName, String name,
                       ResourceLocation dimension, double x, double y, double z,
                       long created, Relation relation, List<ShareView> roster) {

    public enum Relation {
        /** Mine. */
        OWNER,
        /** Someone else's, and I accepted. */
        GUEST,
        /** Someone else's, and they are waiting on my answer. */
        INVITED,
        /**
         * Mine, but still FTB Essentials'. Not in {@code HomeData} at all — synthesised from FTB's
         * player data so the tab can offer to bring it across, and reachable by nothing else: it
         * cannot be teleported to, shared or deleted, because there is no {@code PlayerHome} behind
         * it. {@code MIGRATE} is the only action that accepts one.
         */
        FTB
    }

    /** One line of the owner's "shared with" list. */
    public record ShareView(String name, boolean accepted) {}
}
