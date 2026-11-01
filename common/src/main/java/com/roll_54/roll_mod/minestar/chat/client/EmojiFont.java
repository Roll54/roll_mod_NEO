package com.roll_54.roll_mod.minestar.chat.client;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import com.roll_54.roll_mod.network.packet.EmojiPackets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt), without its
 * stickers, standard emojis, favorites, groups and guests.
 *
 * <p>The {@code roll_mod:emoji} font, built at runtime from the server's Discord emoji images — no
 * resource pack. Each emoji is one private-use character, drawn 9 px tall on the text baseline in its
 * own colors, in any font ({@code FontSetEmojiMixin}). Animated emojis play while they are on screen.
 *
 * <p>Images are cached on disk by Discord emoji id ({@code <game dir>/roll_mod/emoji_cache/}; an id
 * never changes its image): the server sends only the list, and the client asks for the images it
 * doesn't have yet — after the first join, nothing is downloaded until emojis change.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class EmojiFont {

    /** On-screen glyph height; the text line is 9 px. */
    private static final float HEIGHT = 9f;
    /** Emojis are 32 px (animated: a strip of up to 64 frames); anything far bigger isn't one. */
    private static final int MAX_WIDTH = 256;
    private static final int MAX_HEIGHT = 256 * 64;
    /** An animated emoji not drawn for this long (ms) is off screen: its animation pauses. */
    private static final long PAUSE_AFTER = 1000;
    /** GL_TEXTURE_BINDING_2D: which texture the font atlas bound when an emoji was placed in it. */
    private static final int TEXTURE_BINDING_2D = 0x8069;
    private static final int FIRST_CODEPOINT = 0xF4000;
    private static final int LAST_CODEPOINT = 0xF7FFF;
    private static final Path CACHE = FMLPaths.GAMEDIR.get().resolve("roll_mod").resolve("emoji_cache");

    /** FontManager's font sets ({@code FontManagerEmojiMixin}). */
    public interface FontSets {
        Map<ResourceLocation, FontSet> roll_mod$fontSets();
    }

    /** Minecraft's font manager ({@code MinecraftFontManagerAccessor}). */
    public interface MinecraftFonts {
        FontManager roll_mod$fontManager();
    }

    private static List<EmojiPackets.EmojiRef> list = List.of();
    /** Loaded images by emoji id; owned here (providers only borrow them). */
    private static final Map<String, NativeImage> IMAGES = new HashMap<>();
    @Nullable
    private static Provider provider;
    @Nullable
    private static FontSet fontSet;

    private EmojiFont() {}

    /**
     * For {@code FontSetEmojiMixin}: the emoji font set when {@code other} (any other font, with no
     * glyph of its own there) is asked for an emoji's character, else null.
     */
    @Nullable
    public static FontSet borrowingFrom(FontSet other, int codepoint) {
        if (!ours(codepoint)) return null;
        FontSet emoji = fontSet;
        return emoji != null && other != emoji && provider != null && provider.getGlyph(codepoint) != null ? emoji : null;
    }

    /** Whether the character is an emoji this font draws. */
    public static boolean isEmoji(int codepoint) {
        Provider current = provider;
        return ours(codepoint) && current != null && current.getGlyph(codepoint) != null;
    }

    /** The emojis that can be drawn now (their image is loaded), in the server's order: what the picker shows. */
    public static List<EmojiPackets.EmojiRef> entries() {
        List<EmojiPackets.EmojiRef> shown = new ArrayList<>();
        for (EmojiPackets.EmojiRef ref : list) {
            if (IMAGES.containsKey(ref.id())) shown.add(ref);
        }
        return shown;
    }

    private static boolean ours(int codepoint) {
        return codepoint >= FIRST_CODEPOINT && codepoint <= LAST_CODEPOINT;
    }

    /* ------------------------------------------------ incoming ------------------------------------------------ */

    /** The server's emoji list (render thread): cached images load now, the rest are requested. */
    public static void onList(List<EmojiPackets.EmojiRef> received) {
        list = List.copyOf(received);
        Set<String> wanted = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (EmojiPackets.EmojiRef ref : list) {
            if (!validId(ref.id()) || !wanted.add(ref.id()) || IMAGES.containsKey(ref.id())) continue;
            NativeImage cached = readCached(ref);
            if (cached != null) {
                IMAGES.put(ref.id(), cached);
            } else {
                missing.add(ref.id());
            }
        }
        List<NativeImage> dropped = new ArrayList<>();
        IMAGES.keySet().removeIf(id -> {
            if (wanted.contains(id)) return false;
            dropped.add(IMAGES.get(id));
            return true;
        });
        rebuild();
        // Only after the new font stopped using them.
        dropped.forEach(NativeImage::close);
        if (!missing.isEmpty()) PacketDistributor.sendToServer(new EmojiPackets.EmojiRequest(missing));
    }

    /** Images the client asked for: cached to disk and added to the font. */
    public static void onImages(List<EmojiPackets.EmojiImage> received) {
        Map<String, EmojiPackets.EmojiRef> listed = new HashMap<>();
        list.forEach(ref -> listed.put(ref.id(), ref));
        boolean added = false;
        for (EmojiPackets.EmojiImage image : received) {
            EmojiPackets.EmojiRef ref = listed.get(image.id());
            if (ref == null || !validId(image.id()) || IMAGES.containsKey(image.id())) continue;
            NativeImage decoded = decode(image.png(), ref);
            if (decoded == null) continue;
            IMAGES.put(image.id(), decoded);
            added = true;
            try {
                Files.createDirectories(CACHE);
                Files.write(cacheFile(ref), image.png());
            } catch (IOException e) {
                RollMod.LOGGER.warn("[Emoji] Can't cache emoji {}: {}", image.id(), e.toString());
            }
        }
        if (added) rebuild();
    }

    /** Left the server: its emojis go with it (the disk cache stays). */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        list = List.of();
        provider = null;
        install();
        IMAGES.values().forEach(NativeImage::close);
        IMAGES.clear();
    }

    /** Discord ids are digits — never trust a server-sent id as a file name otherwise. */
    private static boolean validId(String id) {
        return !id.isEmpty() && id.length() <= 24 && id.chars().allMatch(Character::isDigit);
    }

    /** Still images as {@code <id>.png}; animated ones as their frame strip, {@code <id>.anim.png}. */
    private static Path cacheFile(EmojiPackets.EmojiRef ref) {
        return CACHE.resolve(ref.id() + (ref.animated() ? ".anim.png" : ".png"));
    }

    @Nullable
    private static NativeImage readCached(EmojiPackets.EmojiRef ref) {
        Path file = cacheFile(ref);
        if (!Files.isRegularFile(file)) return null;
        try {
            NativeImage image = decode(Files.readAllBytes(file), ref);
            if (image == null) Files.deleteIfExists(file);
            return image;
        } catch (IOException e) {
            return null;
        }
    }

    /** The PNG as an image fit for this emoji (animated: exactly one square frame per delay), else null. */
    @Nullable
    private static NativeImage decode(byte[] png, EmojiPackets.EmojiRef ref) {
        int frames = ref.animated() ? ref.delays().size() : 1;
        try {
            NativeImage image = NativeImage.read(png);
            boolean fits = image.getWidth() <= MAX_WIDTH && image.getHeight() <= MAX_HEIGHT
                    && (frames == 1 || image.getHeight() == image.getWidth() * frames);
            if (!fits) {
                image.close();
                return null;
            }
            return image;
        } catch (IOException e) {
            return null;
        }
    }

    /** The font from the list and the images loaded so far. */
    private static void rebuild() {
        Int2ObjectMap<Glyph> byCodepoint = new Int2ObjectOpenHashMap<>();
        for (EmojiPackets.EmojiRef ref : list) {
            NativeImage image = IMAGES.get(ref.id());
            if (image != null && !byCodepoint.containsKey(ref.codepoint())) {
                byCodepoint.put(ref.codepoint(), new Glyph(image, ref.animated() ? image.getWidth() : image.getHeight(),
                        ref.animated() ? ref.delays() : List.of()));
            }
        }
        provider = byCodepoint.isEmpty() ? null : new Provider(byCodepoint);
        install();
    }

    /* ------------------------------------------------ drawing ------------------------------------------------- */

    /** Text is drawing this character now ({@code FontSetEmojiMixin}): an animated emoji keeps playing. */
    public static void markDrawn(int codepoint) {
        if (!ours(codepoint)) return;
        Provider current = provider;
        Glyph glyph = current == null ? null : current.glyphs.get(codepoint);
        if (glyph != null && glyph.animated()) glyph.lastDrawn = Util.getMillis();
    }

    /**
     * Every rendered frame: animated emojis whose frame changed get it drawn over their spot in the
     * font texture — every text showing them animates, at no extra cost. Emojis nothing has drawn
     * for a second (scrolled away, chat faded) are skipped until seen again.
     */
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Provider current = provider;
        if (current == null || current.animated.isEmpty()) return;
        long now = Util.getMillis();
        int previousUnit = GlStateManager._getActiveTexture();
        boolean switched = false;
        for (Glyph glyph : current.animated) {
            if (glyph.texture < 0 || now - glyph.lastDrawn > PAUSE_AFTER) continue;
            int frame = glyph.frameAt(now);
            if (frame == glyph.shownFrame) continue;
            if (!switched) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                switched = true;
            }
            GlStateManager._bindTexture(glyph.texture);
            glyph.uploadFrame(glyph.x, glyph.y, frame);
        }
        if (switched) GlStateManager._activeTexture(previousUnit);
    }

    /** (Re)registers the emoji font set with the game's FontManager (also after resource reloads). */
    public static void install() {
        Minecraft minecraft = Minecraft.getInstance();
        FontManager manager = ((MinecraftFonts) minecraft).roll_mod$fontManager();
        if (manager == null) return;
        Map<ResourceLocation, FontSet> sets = ((FontSets) manager).roll_mod$fontSets();
        // Still registered: ours to close. Gone: a resource reload already closed it.
        if (fontSet != null && sets.get(EmojiRegistry.FONT) == fontSet) {
            sets.remove(EmojiRegistry.FONT);
            fontSet.close();
        }
        fontSet = null;
        if (provider == null) return;
        // A fresh font set places every glyph again; old spots belong to closed textures.
        provider.glyphs.values().forEach(Glyph::forgetPlacement);
        fontSet = new FontSet(minecraft.getTextureManager(), EmojiRegistry.FONT);
        fontSet.reload(List.of(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS)), Set.of());
        sets.put(EmojiRegistry.FONT, fontSet);
    }

    private static final class Provider implements GlyphProvider {
        private final Int2ObjectMap<Glyph> glyphs;
        private final List<Glyph> animated = new ArrayList<>();

        Provider(Int2ObjectMap<Glyph> glyphs) {
            this.glyphs = glyphs;
            glyphs.values().forEach(glyph -> {
                if (glyph.animated()) animated.add(glyph);
            });
        }

        @Nullable
        @Override
        public GlyphInfo getGlyph(int codepoint) {
            return glyphs.get(codepoint);
        }

        @Override
        public IntSet getSupportedGlyphs() {
            return glyphs.keySet();
        }

        @Override
        public void close() {
            // The images belong to EmojiFont (reused across rebuilds).
        }
    }

    /**
     * One emoji: a frame of the image scaled to {@link #HEIGHT}, 1 px above the cap line to 1 px
     * below the baseline. Animated ones remember where the font texture placed them, so
     * {@link #onRenderFrame} can draw later frames there.
     */
    private static final class Glyph implements GlyphInfo {
        private final NativeImage image;
        private final int frameHeight;
        private final List<Integer> delays;
        private final int loop;
        private int texture = -1;
        private int x;
        private int y;
        private int shownFrame;
        /** When text last drew this emoji (ms). */
        private long lastDrawn;

        Glyph(NativeImage image, int frameHeight, List<Integer> delays) {
            this.image = image;
            this.frameHeight = frameHeight;
            this.delays = delays;
            this.loop = delays.stream().mapToInt(Integer::intValue).sum();
        }

        boolean animated() {
            return delays.size() > 1 && loop > 0;
        }

        int frameAt(long millis) {
            long t = millis % loop;
            for (int i = 0; i < delays.size(); i++) {
                t -= delays.get(i);
                if (t < 0) return i;
            }
            return 0;
        }

        void uploadFrame(int atX, int atY, int frame) {
            image.upload(0, atX, atY, 0, frame * frameHeight, image.getWidth(), frameHeight, false, false);
            shownFrame = frame;
        }

        void forgetPlacement() {
            texture = -1;
        }

        private float scale() {
            return HEIGHT / frameHeight;
        }

        @Override
        public float getAdvance() {
            return Math.round(image.getWidth() * scale()) + 1;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> baker) {
            return baker.apply(new SheetGlyphInfo() {
                @Override
                public int getPixelWidth() {
                    return image.getWidth();
                }

                @Override
                public int getPixelHeight() {
                    return frameHeight;
                }

                @Override
                public void upload(int atX, int atY) {
                    // The font texture is bound right now: remember the spot for animation.
                    if (animated()) {
                        texture = GlStateManager._getInteger(TEXTURE_BINDING_2D);
                        x = atX;
                        y = atY;
                    }
                    uploadFrame(atX, atY, 0);
                }

                @Override
                public boolean isColored() {
                    return true;
                }

                @Override
                public float getOversample() {
                    return 1f / scale();
                }

                @Override
                public float getBearingTop() {
                    return 8f;
                }
            });
        }
    }
}
