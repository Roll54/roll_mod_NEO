package com.roll_54.roll_mod.network.packet.drill;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.DrillModules;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: nudge the held modular drill's throttle by {@code delta} percent (Ctrl+scroll).
 *
 * <p>A delta, not an absolute value: scroll events race the server's own state, and "5 percent up
 * from wherever you are" merges cleanly where "set to 85" would jump around. The server clamps to
 * what the drill's modules actually allow, so the client needs no authority.
 */
public record DrillThrottlePacket(int delta) implements CustomPacketPayload {

    public static final Type<DrillThrottlePacket> TYPE = new Type<>(RollMod.id("drill_throttle"));

    public static final StreamCodec<ByteBuf, DrillThrottlePacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DrillThrottlePacket::new, DrillThrottlePacket::delta);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DrillThrottlePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof ModularDrillItem)) return;

            DrillModules modules = DrillModuleHelper.get(stack);
            // Long arithmetic: the ceiling is Integer.MAX_VALUE and the delta is client-supplied,
            // so the plain int sum could wrap negative and slam an overcharged drill to 0.
            int throttle = (int) Mth.clamp((long) modules.throttle() + payload.delta(),
                    0L, DrillModuleHelper.maxThrottle(stack));
            if (throttle == modules.throttle()) return;

            DrillModuleHelper.set(stack, modules.withThrottle(throttle));
            player.displayClientMessage(
                    Component.translatable("message.roll_mod.drill_throttle", throttle)
                            .withStyle(throttle > 100 ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
                    true);
        });
    }
}
