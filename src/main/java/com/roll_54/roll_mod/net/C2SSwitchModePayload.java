
package com.roll_54.roll_mod.net;


import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static com.roll_54.roll_mod.Roll_mod.MODID;

public record C2SSwitchModePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<C2SSwitchModePayload> TYPE =
            new Type<>(new ResourceLocation(MODID, "c2s_switch_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSwitchModePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeBlockPos(msg.pos),
                    buf -> new C2SSwitchModePayload(buf.readBlockPos())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
