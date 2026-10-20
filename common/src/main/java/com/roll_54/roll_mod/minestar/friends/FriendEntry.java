package com.roll_54.roll_mod.minestar.friends;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One Minestar friend, as the hub's friends list shows it.
 *
 * <p>The first five fields come from Minestar ({@code UserFriend}); {@code here} and {@code teammate}
 * are this server's view, filled in by {@link FriendsService} when the list is sent, so they are as
 * current as the last sync rather than the last API call.
 *
 * @param lastSeenMillis epoch millis the launcher last reported in, {@code -1} if never
 * @param playing        names of the modpacks they are in, empty when offline
 * @param here           online on <em>this</em> server right now
 * @param teammate       in the viewer's FTB party
 * @param skin           the {@code textures} property's value for the face, empty when unknown —
 *                       see {@link SkinLookup}
 * @param skinSignature  its signature, empty when unsigned
 */
public record FriendEntry(UUID id, String name, boolean online, long lastSeenMillis, List<String> playing,
                          boolean here, boolean teammate, String skin, String skinSignature) {

    private static final int MAX_PACKS = 8;
    private static final int MAX_SKIN = 8192;

    /** As Minestar reported them, before this server's flags and skin are added. */
    public FriendEntry(UUID id, String name, boolean online, long lastSeenMillis, List<String> playing) {
        this(id, name, online, lastSeenMillis, playing, false, false, "", "");
    }

    public FriendEntry withServerView(boolean here, boolean teammate, String skin, String skinSignature) {
        return new FriendEntry(id, name, online, lastSeenMillis, playing, here, teammate, skin, skinSignature);
    }

    /** The first modpack they are playing, or empty. What the server sort groups by. */
    public String pack() {
        return playing.isEmpty() ? "" : playing.get(0);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeBoolean(online);
        buf.writeLong(lastSeenMillis);
        int count = Math.min(MAX_PACKS, playing.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) buf.writeUtf(playing.get(i), 128);
        buf.writeBoolean(here);
        buf.writeBoolean(teammate);
        buf.writeUtf(skin.length() <= MAX_SKIN ? skin : "", MAX_SKIN);
        buf.writeUtf(skinSignature.length() <= MAX_SKIN ? skinSignature : "", MAX_SKIN);
    }

    public static FriendEntry decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(64);
        boolean online = buf.readBoolean();
        long lastSeen = buf.readLong();
        int count = Math.min(MAX_PACKS, buf.readVarInt());
        List<String> playing = new ArrayList<>(count);
        for (int i = 0; i < count; i++) playing.add(buf.readUtf(128));
        return new FriendEntry(id, name, online, lastSeen, List.copyOf(playing), buf.readBoolean(), buf.readBoolean(),
                buf.readUtf(MAX_SKIN), buf.readUtf(MAX_SKIN));
    }
}
