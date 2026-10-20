package com.roll_54.roll_mod.network.packet;

import com.mojang.authlib.properties.Property;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.friends.ClientSkinCache;
import com.roll_54.roll_mod.minestar.friends.SkinLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Faces for any player head the client draws: the client names the players it has no skin for, the
 * server answers with the {@code textures} property its own session service (authlib) returned — see
 * {@link SkinLookup}. Offline players and players never seen on this server get their real face this
 * way, with no screen having to carry skins in its own packet.
 */
public final class SkinPackets {

    /** Ids per packet, both ways: a full leaderboard page fits in one. */
    public static final int MAX_IDS = 64;
    private static final int MAX_SKIN = 8192;

    private SkinPackets() {}

    /** Client → server: send me these players' skins. */
    public record Request(List<UUID> ids) implements CustomPacketPayload {

        public static final Type<Request> TYPE = new Type<>(RollMod.id("skin_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    int count = Math.min(MAX_IDS, payload.ids().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) buf.writeUUID(payload.ids().get(i));
                },
                buf -> {
                    int count = Math.min(MAX_IDS, buf.readVarInt());
                    List<UUID> ids = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) ids.add(buf.readUUID());
                    return new Request(List.copyOf(ids));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /**
         * Answers what is known at once and the rest when their lookups land. An id whose lookup is
         * already in flight for someone else gets no answer here; the client asks again later.
         */
        public static void handle(Request payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                MinecraftServer server = player.server;
                Set<UUID> ids = new LinkedHashSet<>(payload.ids());
                List<UUID> known = new ArrayList<>();
                List<UUID> missing = new ArrayList<>();
                for (UUID id : ids) (SkinLookup.known(server, id) ? known : missing).add(id);
                if (!known.isEmpty()) send(server, player.getUUID(), known);
                if (!missing.isEmpty()) {
                    UUID viewer = player.getUUID();
                    SkinLookup.prefetch(server, missing, () -> send(server, viewer, missing));
                }
            });
        }

        private static void send(MinecraftServer server, UUID viewer, List<UUID> ids) {
            ServerPlayer online = server.getPlayerList().getPlayer(viewer);
            if (online == null) return;
            List<Skin> skins = new ArrayList<>(ids.size());
            for (UUID id : ids) {
                if (!SkinLookup.known(server, id)) continue;
                Property textures = SkinLookup.get(server, id);
                skins.add(new Skin(id, textures == null ? "" : textures.value(),
                        textures == null || textures.signature() == null ? "" : textures.signature()));
            }
            if (!skins.isEmpty()) PacketDistributor.sendToPlayer(online, new Sync(skins));
        }
    }

    /** One player's {@code textures} property; both strings empty when they have no skin. */
    public record Skin(UUID id, String value, String signature) {}

    /** Server → client: the skins asked for. */
    public record Sync(List<Skin> skins) implements CustomPacketPayload {

        public static final Type<Sync> TYPE = new Type<>(RollMod.id("skin_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    int count = Math.min(MAX_IDS, payload.skins().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) {
                        Skin skin = payload.skins().get(i);
                        boolean fits = skin.value().length() <= MAX_SKIN && skin.signature().length() <= MAX_SKIN;
                        buf.writeUUID(skin.id());
                        buf.writeUtf(fits ? skin.value() : "", MAX_SKIN);
                        buf.writeUtf(fits ? skin.signature() : "", MAX_SKIN);
                    }
                },
                buf -> {
                    int count = Math.min(MAX_IDS, buf.readVarInt());
                    List<Skin> skins = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        skins.add(new Skin(buf.readUUID(), buf.readUtf(MAX_SKIN), buf.readUtf(MAX_SKIN)));
                    }
                    return new Sync(List.copyOf(skins));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientSkinCache.accept(payload.skins()));
        }
    }
}
