package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.kits.ClientKitWindowCache;
import com.roll_54.roll_mod.minestar.kits.KitService;
import com.roll_54.roll_mod.minestar.kits.KitWindowLayout;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The kit window ({@code KitWindows}): its layout out ahead of the open, a player's preview request in. */
public final class KitWindowPackets {

    private KitWindowPackets() {}

    /** Server → client, just before the window opens: what to build. */
    public record Layout(KitWindowLayout layout) implements CustomPacketPayload {

        public static final Type<Layout> TYPE = new Type<>(RollMod.id("kit_window_layout"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Layout> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> p.layout().encode(buf),
                buf -> new Layout(KitWindowLayout.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Layout payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientKitWindowCache.LAYOUT = payload.layout());
        }
    }

    /** Client → server: show me what this kit gives. Checked against the kits the player may see. */
    public record Preview(String kit) implements CustomPacketPayload {

        public static final Type<Preview> TYPE = new Type<>(RollMod.id("kit_preview"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Preview> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.kit(), 32),
                buf -> new Preview(buf.readUtf(32)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Preview payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) KitService.openPreview(player, payload.kit());
            });
        }
    }
}
