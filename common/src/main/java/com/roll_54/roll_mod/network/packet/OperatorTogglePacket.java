package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.op.OperatorStore;
import com.roll_54.roll_mod.minestar.op.OperatorViewers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * The hub's operator switch.
 *
 * <p>The sender's right to flip it is checked here and not taken from the packet: the panel is part
 * of a tree every client builds, so the button exists on every client whether or not the server
 * would honour it.
 */
public record OperatorTogglePacket(String name, UUID target, boolean op) implements CustomPacketPayload {

    public static final Type<OperatorTogglePacket> TYPE = new Type<>(RollMod.id("operator_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorTogglePacket> STREAM_CODEC =
            StreamCodec.of(OperatorTogglePacket::encode, OperatorTogglePacket::decode);

    private static final UUID NONE = new UUID(0L, 0L);

    public static OperatorTogglePacket of(String name, UUID target, boolean op) {
        return new OperatorTogglePacket(name, target == null ? NONE : target, op);
    }

    private static void encode(RegistryFriendlyByteBuf buf, OperatorTogglePacket payload) {
        buf.writeUtf(payload.name(), 16);
        buf.writeUUID(payload.target());
        buf.writeBoolean(payload.op());
    }

    private static OperatorTogglePacket decode(RegistryFriendlyByteBuf buf) {
        return new OperatorTogglePacket(buf.readUtf(16), buf.readUUID(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OperatorTogglePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!OperatorStore.canManage(player)) return;

            UUID target = NONE.equals(payload.target()) ? null : payload.target();
            if (!OperatorStore.set(player.server, target, payload.name(), payload.op())) {
                player.sendSystemMessage(Component.translatable("msg.roll_mod.op.anchored",
                        payload.name()).withStyle(ChatFormatting.RED));
                return;
            }
            player.sendSystemMessage(Component.translatable(payload.op()
                    ? "msg.roll_mod.op.granted" : "msg.roll_mod.op.revoked", payload.name()));
            OperatorViewers.resync(player.server);
        });
    }
}
