package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import com.roll_54.roll_mod.minestar.hub.warp.WarpData;
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

    /** Append only: {@link #decode} indexes by ordinal. */
    public enum Action { CREATE, DELETE, TELEPORT, CANCEL, SET_APPROVAL }

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

    /**
     * A moderator's verdict. The approval's id rides in {@code name}, which this action has no other
     * use for — cheaper than a field every other action would carry empty.
     */
    public static WarpActionPacket setApproval(UUID warp, WarpApproval approval) {
        return new WarpActionPacket(Action.SET_APPROVAL, warp, approval.id(), "", 0);
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
                case SET_APPROVAL -> {
                    // Re-checked here, not trusted from the tab being visible.
                    if (!WarpService.isModerator(player)) return;
                    Warp warp = WarpData.get(player.server).byId(payload.warp);
                    if (warp != null) {
                        WarpService.setApproval(player, warp, WarpApproval.byId(payload.name));
                    }
                }
            }
        });
    }
}
