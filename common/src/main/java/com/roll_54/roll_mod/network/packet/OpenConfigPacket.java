package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: {@code /rollmod admin config} — open roll_mod's config screen (Fzzy Config's). */
public record OpenConfigPacket() implements CustomPacketPayload {

    public static final OpenConfigPacket INSTANCE = new OpenConfigPacket();
    public static final Type<OpenConfigPacket> TYPE = new Type<>(RollMod.id("open_config"));
    public static final StreamCodec<ByteBuf, OpenConfigPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenConfigPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            // After the chat screen has closed, or closing it would close the config screen too.
            Minecraft.getInstance().tell(() -> me.fzzyhmstrs.fzzy_config.api.ConfigApi.INSTANCE.openScreen(RollMod.MODID));
        });
    }
}
