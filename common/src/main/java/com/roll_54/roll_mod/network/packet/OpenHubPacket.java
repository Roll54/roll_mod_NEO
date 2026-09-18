package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.HubCommand;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: open the hub, on a particular tab. Like the daily-tasks screen it is a LdLib2
 * container UI, so only the server can open it.
 *
 * <p>The tab travels with the request because selecting one is a client-side click the server never
 * sees — see {@code HubUI}'s class javadoc. Both openers — the hub key and the inventory button —
 * ask for whichever tab the player left the hub on, which only the client knows.
 */
public record OpenHubPacket(int tab) implements CustomPacketPayload {

    public static final Type<OpenHubPacket> TYPE = new Type<>(RollMod.id("open_hub"));

    public static final StreamCodec<ByteBuf, OpenHubPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(OpenHubPacket::new, OpenHubPacket::tab);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenHubPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Clamped: the index is a number a client sent, and an out-of-range one would leave
                // the hub showing nothing at all.
                HubCommand.open(player, HubUI.clampTab(payload.tab()));
            }
        });
    }
}
