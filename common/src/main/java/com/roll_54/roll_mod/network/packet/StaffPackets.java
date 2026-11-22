package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.staff.ClientStaffCache;
import com.roll_54.roll_mod.minestar.staff.StaffService;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The command blocker, polls and tickets, as two loose payloads: an action (client → server) naming what to do with
 * its arguments, and a block of data (server → client) under a kind. Every action is checked in
 * {@link StaffService}, so a spoofed one does nothing.
 */
public final class StaffPackets {

    private StaffPackets() {}

    /** Client → server: {@code action} ("polls/vote", "tickets/send", …) with its arguments. */
    public record Act(String action, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(RollMod.id("staff_act"));
        public static final StreamCodec<ByteBuf, Act> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Act::action, ByteBufCodecs.COMPOUND_TAG, Act::args, Act::new);

        public static Act of(String action) {
            return new Act(action, new CompoundTag());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Act payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) StaffService.act(player, payload.action, payload.args);
            });
        }
    }

    /** Server → client: the data for {@code kind} ("commands", "polls", "tickets", "my_tickets"). */
    public record Data(String kind, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(RollMod.id("staff_data"));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Data::kind, ByteBufCodecs.COMPOUND_TAG, Data::data, Data::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Data payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientStaffCache.put(payload.kind, payload.data));
        }
    }
}
