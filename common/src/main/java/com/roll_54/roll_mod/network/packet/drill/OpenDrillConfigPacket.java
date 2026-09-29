package com.roll_54.roll_mod.network.packet.drill;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.items.modulardrill.gui.ModularDrillConfigUI;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: open the config GUI for the held modular drill. An LdLib2 container UI, so
 * only the server can open it — the same reason the hub key sends {@code OpenHubPacket}.
 */
public record OpenDrillConfigPacket() implements CustomPacketPayload {

    public static final Type<OpenDrillConfigPacket> TYPE = new Type<>(RollMod.id("open_drill_config"));

    public static final StreamCodec<ByteBuf, OpenDrillConfigPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenDrillConfigPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDrillConfigPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getMainHandItem().getItem() instanceof ModularDrillItem) {
                PlayerUIMenuType.openUI(player, ModularDrillConfigUI.UI_ID);
            }
        });
    }
}
