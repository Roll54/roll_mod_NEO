package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.HubCommand;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.minestar.moderation.invsee.ClientInvSeeCache;
import com.roll_54.roll_mod.minestar.moderation.invsee.InvSeeLayout;
import com.roll_54.roll_mod.minestar.moderation.invsee.InvSeeSessions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The moderator's inventory window. The client asks for it with {@link Open}, the server answers
 * with the window's {@link Layout} and then opens it, and {@link Back} returns to the hub.
 */
public final class InvSeePackets {

    /** A click is fine, a flood of file reads is not. */
    private static final long MIN_INTERVAL_MILLIS = 250L;
    private static final Map<UUID, Long> LAST_OPEN = new ConcurrentHashMap<>();
    private static final UUID NONE = new UUID(0L, 0L);

    private InvSeePackets() {}

    /* ------------------------------------------ client → server ------------------------------------------ */

    /**
     * Client → server: open a player's inventory or implants.
     *
     * @param id      the player, or the nil UUID to look them up by {@code name}
     * @param offline the moderator chose the offline option; refused if the player is in fact online
     */
    public record Open(UUID id, String name, InvSeeLayout.Page page, boolean offline)
            implements CustomPacketPayload {

        public static final Type<Open> TYPE = new Type<>(RollMod.id("invsee_open"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.id());
                    buf.writeUtf(p.name().length() > 64 ? p.name().substring(0, 64) : p.name(), 64);
                    buf.writeVarInt(p.page().ordinal());
                    buf.writeBoolean(p.offline());
                },
                buf -> new Open(buf.readUUID(), buf.readUtf(64),
                        InvSeeLayout.Page.values()[Math.floorMod(buf.readVarInt(), InvSeeLayout.Page.values().length)],
                        buf.readBoolean()));

        public static Open online(UUID id, InvSeeLayout.Page page) {
            return new Open(id, "", page, false);
        }

        public static Open offline(@Nullable UUID id, String name) {
            return new Open(id == null ? NONE : id, name, InvSeeLayout.Page.INVENTORY, true);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                if (!ModerationPermissions.canInvSee(player)) return;
                long now = System.currentTimeMillis();
                Long last = LAST_OPEN.put(player.getUUID(), now);
                if (last != null && now - last < MIN_INTERVAL_MILLIS) return;
                UUID id = NONE.equals(payload.id()) ? null : payload.id();
                if (id == null && payload.name().isBlank()) return;
                InvSeeSessions.open(player, id, payload.name().strip(), payload.page(), payload.offline());
            });
        }
    }

    /** Client → server: close the window and go back to the hub's moderation tab. */
    public record Back() implements CustomPacketPayload {

        public static final Type<Back> TYPE = new Type<>(RollMod.id("invsee_back"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Back> STREAM_CODEC =
                StreamCodec.unit(new Back());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Back payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                HubCommand.open(player, HubUI.clampTab(HubUI.indexOf("moderation")));
            });
        }
    }

    /* ------------------------------------------ server → client ------------------------------------------ */

    /** Server → client: the shape of the window about to open. */
    public record Layout(InvSeeLayout layout) implements CustomPacketPayload {

        public static final Type<Layout> TYPE = new Type<>(RollMod.id("invsee_layout"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Layout> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> p.layout().encode(buf),
                buf -> new Layout(InvSeeLayout.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Layout payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientInvSeeCache.LAYOUT = payload.layout());
        }
    }
}
