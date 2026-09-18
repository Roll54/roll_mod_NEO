package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Client → server: everything the warp tab can ask for. Sent optimistically — every rule is
 * re-checked in {@link WarpService}, so a spoofed packet achieves nothing.
 *
 * @param warp the target, unused by {@link Action#CREATE}
 */
public record WarpActionPacket(Action action, UUID warp, String name, String description, long price)
        implements CustomPacketPayload {

    public enum Action { CREATE, DELETE, TELEPORT, CANCEL }

    public static final Type<WarpActionPacket> TYPE = new Type<>(RollMod.id("warp_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpActionPacket> STREAM_CODEC =
            StreamCodec.of(WarpActionPacket::encode, WarpActionPacket::decode);

    private static final UUID NONE = new UUID(0, 0);

    /**
     * Text is clamped here rather than trusted from the caller: the create form's fields have no
     * length cap, and {@code writeUtf} throws when the string is over the limit — which colour codes
     * make far easier to hit, since each one spends two characters of the budget. {@code Warp.at}
     * trims again server-side; this only keeps the encoder from failing.
     */
    private static String clamp(String text, int max) {
        String clean = text == null ? "" : text.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    public static WarpActionPacket create(String name, String description, long price) {
        return new WarpActionPacket(Action.CREATE, NONE, clamp(name, Warp.MAX_NAME),
                clamp(description, Warp.MAX_DESCRIPTION), price);
    }

    public static WarpActionPacket delete(UUID warp) {
        return new WarpActionPacket(Action.DELETE, warp, "", "", 0);
    }

    public static WarpActionPacket teleport(UUID warp) {
        return new WarpActionPacket(Action.TELEPORT, warp, "", "", 0);
    }

    public static WarpActionPacket cancel() {
        return new WarpActionPacket(Action.CANCEL, NONE, "", "", 0);
    }

    private static void encode(RegistryFriendlyByteBuf buf, WarpActionPacket packet) {
        buf.writeVarInt(packet.action.ordinal());
        buf.writeUUID(packet.warp);
        buf.writeUtf(packet.name, Warp.MAX_NAME);
        buf.writeUtf(packet.description, Warp.MAX_DESCRIPTION);
        buf.writeVarLong(packet.price);
    }

    private static WarpActionPacket decode(RegistryFriendlyByteBuf buf) {
        Action action = Action.values()[buf.readVarInt() % Action.values().length];
        return new WarpActionPacket(action, buf.readUUID(),
                buf.readUtf(Warp.MAX_NAME), buf.readUtf(Warp.MAX_DESCRIPTION), buf.readVarLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WarpActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case CREATE -> WarpService.create(player, payload.name, payload.description, payload.price);
                case DELETE -> WarpService.delete(player, payload.warp);
                case TELEPORT -> WarpService.requestTeleport(player, payload.warp);
                case CANCEL -> WarpService.cancel(player.getUUID());
            }
        });
    }
}
