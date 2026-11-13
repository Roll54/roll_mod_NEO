package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.level.ClientLevelRewards;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the player's Minestar level as the server last saw it and the level reward rules
 * ({@code perk:from:every:amount:until}), for the home tab's "what your level gives" card.
 */
public record LevelRewardsPacket(int level, List<String> rules) implements CustomPacketPayload {

    public static final Type<LevelRewardsPacket> TYPE = new Type<>(RollMod.id("level_rewards"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LevelRewardsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.level());
                int count = Math.min(64, p.rules().size());
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) buf.writeUtf(p.rules().get(i), 128);
            },
            buf -> {
                int level = buf.readVarInt();
                int count = Math.min(64, buf.readVarInt());
                List<String> rules = new ArrayList<>(count);
                for (int i = 0; i < count; i++) rules.add(buf.readUtf(128));
                return new LevelRewardsPacket(level, List.copyOf(rules));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LevelRewardsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLevelRewards.accept(payload));
    }
}
