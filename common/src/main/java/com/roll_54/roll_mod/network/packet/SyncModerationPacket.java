package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.moderation.ModerationRow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the moderation tab shows, or {@link #DENIED} for everyone who may not see it.
 *
 * <p>{@code allowed} has to travel here rather than as a UI binding: {@code HubUI} reads it to hide
 * the tab's bookmark, and the bookmark sits outside the tab's subtree, where no binding of the tab's
 * could reach.
 */
public record SyncModerationPacket(boolean allowed, boolean mayBan, boolean mayWriteLetters,
                                   boolean whitelist, String whitelistMessage,
                                   List<ModerationRow> rows)
        implements CustomPacketPayload {

    private static final int MAX_ROWS = 1000;

    public static final SyncModerationPacket DENIED =
            new SyncModerationPacket(false, false, false, false, "", List.of());

    public static final Type<SyncModerationPacket> TYPE = new Type<>(RollMod.id("sync_moderation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncModerationPacket> STREAM_CODEC =
            StreamCodec.of(SyncModerationPacket::encode, SyncModerationPacket::decode);

    /** The same snapshot with one viewer's permissions, so the list is built once per resync. */
    public SyncModerationPacket forViewer(boolean mayBan, boolean mayWriteLetters) {
        return new SyncModerationPacket(allowed, mayBan, mayWriteLetters, whitelist,
                whitelistMessage, rows);
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncModerationPacket p) {
        buf.writeBoolean(p.allowed);
        buf.writeBoolean(p.mayBan);
        buf.writeBoolean(p.mayWriteLetters);
        buf.writeBoolean(p.whitelist);
        buf.writeUtf(clamp(p.whitelistMessage, 512), 512);
        int rows = Math.min(MAX_ROWS, p.rows.size());
        buf.writeVarInt(rows);
        for (int i = 0; i < rows; i++) {
            ModerationRow row = p.rows.get(i);
            new ModerationRow(row.id(), clamp(row.name(), 64), row.online(), row.warns(),
                    row.muteRemaining(), row.banRemaining(), clamp(row.banRule(), 32),
                    clamp(row.banReason(), 256)).encode(buf);
        }
    }

    private static SyncModerationPacket decode(RegistryFriendlyByteBuf buf) {
        boolean allowed = buf.readBoolean();
        boolean mayBan = buf.readBoolean();
        boolean mayWriteLetters = buf.readBoolean();
        boolean whitelist = buf.readBoolean();
        String message = buf.readUtf(512);
        int rowCount = Math.min(MAX_ROWS, buf.readVarInt());
        List<ModerationRow> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            rows.add(ModerationRow.decode(buf));
        }
        return new SyncModerationPacket(allowed, mayBan, mayWriteLetters, whitelist, message,
                List.copyOf(rows));
    }

    /** {@code writeUtf} throws past its limit, so an over-long file entry is cut, not fatal. */
    private static String clamp(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncModerationPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientModerationCache.accept(payload));
    }
}
