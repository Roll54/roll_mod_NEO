package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.client.EmojiFont;
import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The Discord emojis' trip to the client (see {@code EmojiRegistry} and {@code EmojiFont}): at login
 * the server sends only the {@link EmojiList}; the client asks for the images it has not cached
 * yet ({@link EmojiRequest}) and gets them as PNGs ({@link EmojiImages}). After the first join,
 * nothing is downloaded until emojis change.
 */
public final class EmojiPackets {

    /** Most entries a list or request may carry; far above any real emoji list. */
    private static final int MAX_ENTRIES = 1024;
    /** One emoji's PNG; an animated one is a strip of up to 64 frames of 32 px. */
    private static final int MAX_PNG = 1 << 20;

    private EmojiPackets() {}

    /**
     * One emoji: its {@code :name:}, the character the font draws it at, its Discord id (what the
     * client caches its image under), and its frame delays in ms — empty for a still image.
     */
    public record EmojiRef(String name, int codepoint, String id, List<Integer> delays) {

        public boolean animated() {
            return delays.size() > 1;
        }

        static void write(RegistryFriendlyByteBuf buf, EmojiRef ref) {
            buf.writeUtf(ref.name(), 64);
            buf.writeVarInt(ref.codepoint());
            buf.writeUtf(ref.id(), 32);
            buf.writeVarInt(ref.delays().size());
            ref.delays().forEach(buf::writeVarInt);
        }

        static EmojiRef read(RegistryFriendlyByteBuf buf) {
            String name = buf.readUtf(64);
            int codepoint = buf.readVarInt();
            String id = buf.readUtf(32);
            int frames = Math.min(buf.readVarInt(), 256);
            List<Integer> delays = new ArrayList<>(frames);
            for (int i = 0; i < frames; i++) delays.add(buf.readVarInt());
            return new EmojiRef(name, codepoint, id, List.copyOf(delays));
        }
    }

    /** Server → client: every emoji the server has an image for, in list order. */
    public record EmojiList(List<EmojiRef> emojis) implements CustomPacketPayload {

        public static final Type<EmojiList> TYPE = new Type<>(RollMod.id("emoji_list"));

        public static final StreamCodec<RegistryFriendlyByteBuf, EmojiList> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.emojis().size());
                    payload.emojis().forEach(ref -> EmojiRef.write(buf, ref));
                },
                buf -> {
                    int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
                    List<EmojiRef> emojis = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) emojis.add(EmojiRef.read(buf));
                    return new EmojiList(List.copyOf(emojis));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(EmojiList payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.dist == Dist.CLIENT) EmojiFont.onList(payload.emojis());
            });
        }
    }

    /** Client → server: the ids of the emojis whose images the client has not cached. */
    public record EmojiRequest(List<String> ids) implements CustomPacketPayload {

        public static final Type<EmojiRequest> TYPE = new Type<>(RollMod.id("emoji_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, EmojiRequest> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.ids().size());
                    payload.ids().forEach(id -> buf.writeUtf(id, 32));
                },
                buf -> {
                    int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
                    List<String> ids = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) ids.add(buf.readUtf(32));
                    return new EmojiRequest(List.copyOf(ids));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(EmojiRequest payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) EmojiRegistry.sendImages(player, payload.ids());
            });
        }
    }

    /** One emoji's image: a PNG, for an animated emoji its frames stacked top to bottom. */
    public record EmojiImage(String id, byte[] png) {}

    /** Server → client: images the client asked for (split over several packets when large). */
    public record EmojiImages(List<EmojiImage> images) implements CustomPacketPayload {

        public static final Type<EmojiImages> TYPE = new Type<>(RollMod.id("emoji_images"));

        public static final StreamCodec<RegistryFriendlyByteBuf, EmojiImages> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.images().size());
                    for (EmojiImage image : payload.images()) {
                        buf.writeUtf(image.id(), 32);
                        buf.writeByteArray(image.png());
                    }
                },
                buf -> {
                    int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
                    List<EmojiImage> images = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) images.add(new EmojiImage(buf.readUtf(32), buf.readByteArray(MAX_PNG)));
                    return new EmojiImages(List.copyOf(images));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(EmojiImages payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.dist == Dist.CLIENT) EmojiFont.onImages(payload.images());
            });
        }
    }
}
