package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassService;
import com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The battle pass's two payloads: an action from the tab ("get", "claim") and the player's state back. */
public final class BattlePassPackets {

    private BattlePassPackets() {}

    public record Act(String action, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(RollMod.id("battlepass_act"));
        public static final StreamCodec<ByteBuf, Act> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Act::action, ByteBufCodecs.COMPOUND_TAG, Act::args, Act::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Act payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                switch (payload.action) {
                    case "get" -> {
                        BattlePassService.rollIfNeeded(player.server);
                        BattlePassService.sync(player);
                    }
                    case "claim" -> BattlePassService.claim(player, payload.args.getBoolean("prime"),
                            payload.args.getInt("level"), payload.args.contains("pick") ? payload.args.getInt("pick") : -1);
                    case "admin/get" -> BattlePassService.sendAdmin(player);
                    case "daily/get" -> com.roll_54.roll_mod.minestar.dailytasks.DailyRewardConfig.sync(player);
                    case "daily/set" -> com.roll_54.roll_mod.minestar.dailytasks.DailyRewardConfig.applyAdmin(player, payload.args);
                    case "tasks/get" -> com.roll_54.roll_mod.minestar.dailytasks.TaskSettings.sync(player);
                    case "tasks/set" -> com.roll_54.roll_mod.minestar.dailytasks.TaskSettings.applyAdmin(player, payload.args);
                    case "admin/set" -> BattlePassService.applyAdmin(player, payload.args);
                    default -> { }
                }
            });
        }
    }

    public record Data(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(RollMod.id("battlepass_data"));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(Data::new, Data::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Data payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                // A skin case to spin (client only; this packet only ever goes to clients).
                if (payload.data.getBoolean("roll")) com.roll_54.roll_mod.cosmetics.client.SkinRollScreen.open(payload.data);
                // The daily tasks' reward pool, as the server has it.
                else if (payload.data.getBoolean("dailyPool")) {
                    com.roll_54.roll_mod.minestar.dailytasks.DailyRewardConfig.applyTag(payload.data);
                    ClientBattlePassCache.putDaily(payload.data);
                }
                // The task settings: the registry's amounts and made tasks, and the editor's copy.
                else if (payload.data.getBoolean("taskSettings")) {
                    com.roll_54.roll_mod.minestar.dailytasks.TaskSettings.applyTag(payload.data);
                    ClientBattlePassCache.putTasks(payload.data);
                }
                else if (payload.data.getBoolean("admin")) ClientBattlePassCache.putAdmin(payload.data);
                else ClientBattlePassCache.put(payload.data);
            });
        }
    }
}
