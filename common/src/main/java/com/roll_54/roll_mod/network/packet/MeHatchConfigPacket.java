package com.roll_54.roll_mod.network.packet;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.mi.machines.me.MeInputHatchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A player edits a config slot of an ME input hatch from its screen: sets it to an item or fluid and an
 * amount, changes only the amount, or clears it. The hatch's {@code ConfigInventory} clamps the amount
 * (an item slot to {@code MeHatches.ITEM_INPUT_CAPACITY}, a fluid slot to the tank) and ignores keys of the other kind.
 *
 * @param key    the item or fluid, for {@link #SET}
 * @param amount the amount to keep stocked (items, or mB), for {@link #SET} and {@link #SET_AMOUNT}
 */
public record MeHatchConfigPacket(BlockPos pos, int action, int slot, Optional<AEKey> key, long amount)
        implements CustomPacketPayload {

    public static final int SET = 0;
    public static final int SET_AMOUNT = 1;
    public static final int CLEAR = 2;

    /** How far away the screen may be used from, as for a container. */
    private static final double MAX_DISTANCE_SQR = 8 * 8;

    public static final Type<MeHatchConfigPacket> TYPE = new Type<>(RollMod.id("me_hatch_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MeHatchConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MeHatchConfigPacket::pos,
            ByteBufCodecs.VAR_INT, MeHatchConfigPacket::action,
            ByteBufCodecs.VAR_INT, MeHatchConfigPacket::slot,
            ByteBufCodecs.optional(AEKey.STREAM_CODEC), MeHatchConfigPacket::key,
            ByteBufCodecs.VAR_LONG, MeHatchConfigPacket::amount,
            MeHatchConfigPacket::new);

    public static MeHatchConfigPacket set(BlockPos pos, int slot, AEKey key, long amount) {
        return new MeHatchConfigPacket(pos, SET, slot, Optional.of(key), amount);
    }

    public static MeHatchConfigPacket setAmount(BlockPos pos, int slot, long amount) {
        return new MeHatchConfigPacket(pos, SET_AMOUNT, slot, Optional.empty(), amount);
    }

    public static MeHatchConfigPacket clear(BlockPos pos, int slot) {
        return new MeHatchConfigPacket(pos, CLEAR, slot, Optional.empty(), 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeHatchConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > MAX_DISTANCE_SQR) return;
            if (!(player.level().getBlockEntity(packet.pos()) instanceof MeInputHatchBlockEntity hatch)) return;
            var config = hatch.getConfig();
            if (packet.slot() < 0 || packet.slot() >= config.size()) return;
            switch (packet.action()) {
                case SET -> packet.key().ifPresent(key ->
                        config.setStack(packet.slot(), new GenericStack(key, Math.max(1, packet.amount()))));
                case SET_AMOUNT -> {
                    @Nullable AEKey current = config.getKey(packet.slot());
                    // An amount of 0 or less clears the slot, as on an interface.
                    if (current != null) config.setStack(packet.slot(), new GenericStack(current, packet.amount()));
                }
                case CLEAR -> config.setStack(packet.slot(), null);
                default -> {}
            }
        });
    }
}
