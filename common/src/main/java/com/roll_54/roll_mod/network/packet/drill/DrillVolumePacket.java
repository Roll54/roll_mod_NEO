package com.roll_54.roll_mod.network.packet.drill;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.DrillModules;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the mining volume the player dialed in, sent once when the config GUI closes.
 *
 * <p>Absolute values, not deltas: the GUI edits a local copy for instant feedback (the held
 * stack's component cannot reach the client while a menu without inventory slots is open) and
 * commits the final result here. The server clamps every axis, so the client carries no
 * authority.
 */
public record DrillVolumePacket(int volX, int volY, int volZ) implements CustomPacketPayload {

    public static final Type<DrillVolumePacket> TYPE = new Type<>(RollMod.id("drill_volume"));

    public static final StreamCodec<ByteBuf, DrillVolumePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DrillVolumePacket::volX,
            ByteBufCodecs.VAR_INT, DrillVolumePacket::volY,
            ByteBufCodecs.VAR_INT, DrillVolumePacket::volZ,
            DrillVolumePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DrillVolumePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof ModularDrillItem)) return;

            int max = DrillModuleHelper.maxEdge(stack);
            int x = DrillModuleHelper.clampAxis(payload.volX(), max);
            int y = DrillModuleHelper.clampAxis(payload.volY(), max);
            int z = DrillModuleHelper.clampAxis(payload.volZ(), max);

            DrillModules modules = DrillModuleHelper.get(stack);
            if (x == modules.volX() && y == modules.volY() && z == modules.volZ()) return;
            DrillModuleHelper.set(stack, modules.withVolume(x, y, z));
        });
    }
}
