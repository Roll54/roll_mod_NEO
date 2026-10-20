package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.kits.ClientKitNamesCache;
import com.roll_54.roll_mod.minestar.kits.Kit;
import com.roll_54.roll_mod.minestar.kits.KitService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** The moderation tab's "kit names" window: the kits and their names out, a rename back. */
public final class KitNamePackets {

    private static final int MAX = 256;

    private KitNamePackets() {}

    /** Server → client: every kit, with its display name ({@code ""} for none). */
    public record Sync(List<ClientKitNamesCache.Entry> kits) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(RollMod.id("sync_kit_names"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    int count = Math.min(MAX, payload.kits().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) {
                        buf.writeUtf(payload.kits().get(i).name(), 32);
                        buf.writeUtf(payload.kits().get(i).displayName(), Kit.MAX_DISPLAY * 2);
                    }
                },
                buf -> {
                    int count = Math.min(MAX, buf.readVarInt());
                    List<ClientKitNamesCache.Entry> kits = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        kits.add(new ClientKitNamesCache.Entry(buf.readUtf(32), buf.readUtf(Kit.MAX_DISPLAY * 2)));
                    }
                    return new Sync(List.copyOf(kits));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientKitNamesCache.ALLOWED = true;
                ClientKitNamesCache.KITS = payload.kits();
                ClientKitNamesCache.VERSION++;
            });
        }
    }

    /** Client → server: set one kit's display name; blank clears it. Checked on the server. */
    public record Rename(String kit, String displayName) implements CustomPacketPayload {
        public static final Type<Rename> TYPE = new Type<>(RollMod.id("rename_kit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rename> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.kit(), 32);
                    buf.writeUtf(payload.displayName(), Kit.MAX_DISPLAY * 2);
                },
                buf -> new Rename(buf.readUtf(32), buf.readUtf(Kit.MAX_DISPLAY * 2)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Rename payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    KitService.rename(player, payload.kit(), payload.displayName());
                }
            });
        }
    }
}
