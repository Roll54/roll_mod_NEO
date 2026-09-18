package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.home.ClientHomeCache;
import com.roll_54.roll_mod.minestar.hub.home.HomeView;
import com.roll_54.roll_mod.minestar.hub.home.PlayerHome;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the homes this player owns or was invited to, how many they may own, and the
 * names the invite dropdown offers.
 *
 * <p>Per-recipient, unlike {@code SyncWarpsPacket}: each row already carries the receiver's relation
 * to that home, and the guest list is written only for rows they own. Guest UUIDs are never sent at
 * all — the UI only ever prints names, and the ids would hand a modified client a map of who can get
 * into whose base. Nor are yaw and pitch: nothing displays them and the teleport is server-side.
 *
 * <p>{@code knownPlayers} rides along rather than getting a packet of its own: it is wanted at
 * exactly the moments this one is already sent — the tab opening, and any change to it — so a
 * separate payload would double the plumbing to deliver the same list at the same time. It is bounded
 * by {@code KnownPlayers.MAX_NAMES}.
 */
public record SyncHomesPacket(List<HomeView> homes, int limit, List<String> knownPlayers)
        implements CustomPacketPayload {

    public static final Type<SyncHomesPacket> TYPE = new Type<>(RollMod.id("sync_homes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHomesPacket> STREAM_CODEC =
            StreamCodec.of(SyncHomesPacket::encode, SyncHomesPacket::decode);

    /** Mojang's own cap, and what the roster names are bounded by. */
    private static final int MAX_PLAYER_NAME = 16;

    /** Matches KnownPlayers.MAX_NAMES; only here to bound a malformed packet. */
    private static final int MAX_KNOWN_PLAYERS = 300;

    private static void encode(RegistryFriendlyByteBuf buf, SyncHomesPacket packet) {
        buf.writeVarInt(packet.limit);
        buf.writeVarInt(Math.min(packet.knownPlayers.size(), MAX_KNOWN_PLAYERS));
        int written = 0;
        for (String name : packet.knownPlayers) {
            if (written++ == MAX_KNOWN_PLAYERS) break;
            buf.writeUtf(name, MAX_PLAYER_NAME);
        }
        buf.writeVarInt(packet.homes.size());
        for (HomeView home : packet.homes) {
            buf.writeUUID(home.id());
            buf.writeUUID(home.owner());
            buf.writeUtf(home.ownerName(), MAX_PLAYER_NAME);
            buf.writeUtf(home.name(), PlayerHome.MAX_NAME);
            buf.writeResourceLocation(home.dimension());
            buf.writeDouble(home.x());
            buf.writeDouble(home.y());
            buf.writeDouble(home.z());
            buf.writeVarLong(home.created());
            buf.writeByte(home.relation().ordinal());
            buf.writeVarInt(home.roster().size());
            for (HomeView.ShareView share : home.roster()) {
                buf.writeUtf(share.name(), MAX_PLAYER_NAME);
                buf.writeBoolean(share.accepted());
            }
        }
    }

    private static SyncHomesPacket decode(RegistryFriendlyByteBuf buf) {
        int limit = buf.readVarInt();
        int knownCount = Math.min(buf.readVarInt(), MAX_KNOWN_PLAYERS);
        List<String> knownPlayers = new ArrayList<>(knownCount);
        for (int i = 0; i < knownCount; i++) {
            knownPlayers.add(buf.readUtf(MAX_PLAYER_NAME));
        }
        int count = buf.readVarInt();
        List<HomeView> homes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var id = buf.readUUID();
            var owner = buf.readUUID();
            String ownerName = buf.readUtf(MAX_PLAYER_NAME);
            String name = buf.readUtf(PlayerHome.MAX_NAME);
            var dimension = buf.readResourceLocation();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            long created = buf.readVarLong();
            // Modulo for the same reason WarpActionPacket does it: a malformed byte should pick a
            // valid relation rather than throw out of the decoder.
            HomeView.Relation relation =
                    HomeView.Relation.values()[buf.readByte() % HomeView.Relation.values().length];
            int rosterSize = buf.readVarInt();
            List<HomeView.ShareView> roster = new ArrayList<>(rosterSize);
            for (int r = 0; r < rosterSize; r++) {
                roster.add(new HomeView.ShareView(
                        buf.readUtf(MAX_PLAYER_NAME), buf.readBoolean()));
            }
            homes.add(new HomeView(id, owner, ownerName, name, dimension, x, y, z, created,
                    relation, List.copyOf(roster)));
        }
        return new SyncHomesPacket(homes, limit, List.copyOf(knownPlayers));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncHomesPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientHomeCache.HOMES = List.copyOf(payload.homes);
            ClientHomeCache.LIMIT = payload.limit;
            ClientHomeCache.KNOWN_PLAYERS = List.copyOf(payload.knownPlayers);
        });
    }
}
