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

    /**
     * Server → client: what a kit holds, for the Kits tab's preview window inside the hub (no screen of its own,
     * so the hub is not closed and reopened). Main inventory, armour and offhand only, by main-handler slot
     * ({@link com.roll_54.roll_mod.minestar.kits.KitSlots}); curios are not shown.
     */
    public record Contents(String kit, String title, String description, long cooldownTicks, java.util.List<Integer> slots,
                           java.util.List<net.minecraft.world.item.ItemStack> items) implements CustomPacketPayload {

        public static final Type<Contents> TYPE = new Type<>(RollMod.id("kit_contents"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Contents> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.kit(), 32);
                    buf.writeUtf(p.title(), 128);
                    buf.writeUtf(p.description(), 512);
                    buf.writeLong(p.cooldownTicks());
                    int count = Math.min(64, p.slots().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) {
                        buf.writeVarInt(p.slots().get(i));
                        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.items().get(i));
                    }
                },
                buf -> {
                    String kit = buf.readUtf(32);
                    String title = buf.readUtf(128);
                    String description = buf.readUtf(512);
                    long cooldown = buf.readLong();
                    int count = Math.min(64, buf.readVarInt());
                    java.util.List<Integer> slots = new java.util.ArrayList<>(count);
                    java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        slots.add(buf.readVarInt());
                        items.add(net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    return new Contents(kit, title, description, cooldown, slots, items);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Contents payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                com.roll_54.roll_mod.minestar.kits.ClientKitCache.PREVIEW = payload;
                com.roll_54.roll_mod.minestar.kits.ClientKitCache.PREVIEW_VERSION++;
            });
        }
    }
}
