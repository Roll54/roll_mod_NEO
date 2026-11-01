package com.roll_54.roll_mod.minestar.chat.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.emoji.ChatLinks;
import com.roll_54.roll_mod.minestar.chat.emoji.GifStrip;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>The chat's GIF viewer: clicking a {@code [GIF]} link opens a small animated preview over the
 * chat, bottom right, with "open in browser" and close. The player's own game fetches the GIF, only
 * on that click (a Tenor/Giphy page link is resolved to its .gif through the page's og:image); sites
 * that refuse it show a note and keep the browser button. Esc, ✕ or a click outside closes it.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class GifViewer {

    /** Frames kept at most (longer GIFs stop there) and their largest side in pixels. */
    private static final int MAX_FRAMES = 80;
    private static final int MAX_SIDE = 320;
    /** The preview's largest side on screen (GUI pixels). */
    private static final int VIEW_SIDE = 160;
    private static final int TITLE = 12;
    private static final ResourceLocation TEXTURE = RollMod.id("gif_viewer");

    private enum Status { LOADING, READY, FAILED }

    @Nullable
    private static String url;
    private static Status status = Status.LOADING;
    private static List<NativeImage> frames = List.of();
    private static List<Integer> delays = List.of();
    private static int loop;
    private static long started;
    private static int shownFrame = -1;
    @Nullable
    private static DynamicTexture texture;
    /** Bumped on every open/close: a download finishing late for an older GIF is dropped. */
    private static int generation;

    private GifViewer() {}

    /** Opens the viewer for a GIF link (replacing any open one) and starts loading it. */
    public static void open(String link) {
        close();
        url = link;
        status = Status.LOADING;
        int ticket = ++generation;
        Util.backgroundExecutor().execute(() -> {
            GifStrip.Frames decoded = null;
            try {
                decoded = MediaLoader.load(link, MAX_FRAMES, MAX_SIDE);
            } catch (IOException | RuntimeException e) {
                RollMod.LOGGER.info("[GIF] Can't preview {}: {}", link, e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            GifStrip.Frames result = decoded;
            Minecraft.getInstance().execute(() -> {
                if (ticket == generation) ready(result);
            });
        });
    }

    /** Closes the viewer and frees its images. */
    public static void close() {
        generation++;
        url = null;
        frames.forEach(NativeImage::close);
        frames = List.of();
        delays = List.of();
        shownFrame = -1;
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE);
            texture = null;
        }
    }

    private static void ready(@Nullable GifStrip.Frames decoded) {
        if (decoded == null || decoded.images().isEmpty()) {
            status = Status.FAILED;
            return;
        }
        List<NativeImage> images = new ArrayList<>();
        for (BufferedImage frame : decoded.images()) images.add(MediaLoader.toNative(frame));
        frames = images;
        delays = decoded.delays();
        loop = Math.max(1, delays.stream().mapToInt(Integer::intValue).sum());
        started = Util.getMillis();
        NativeImage first = images.get(0);
        texture = new DynamicTexture(first.getWidth(), first.getHeight(), true);
        Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        status = Status.READY;
    }

    /** Panel {x0, y0, x1, y1}: above the chat input on the right. */
    private static int[] panel(int screenWidth, int screenHeight) {
        int width = VIEW_SIDE;
        int height;
        if (status == Status.READY) {
            NativeImage first = frames.get(0);
            double scale = (double) VIEW_SIDE / Math.max(first.getWidth(), first.getHeight());
            width = Math.max(60, (int) Math.round(first.getWidth() * scale));
            height = (int) Math.round(first.getHeight() * scale);
        } else {
            height = 40;
        }
        int x1 = screenWidth - 4;
        int y1 = screenHeight - 18;
        return new int[]{x1 - width - 4, y1 - height - 4 - TITLE, x1, y1};
    }

    /* ------------------------------------------------- events ------------------------------------------------- */

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (url == null || !(event.getScreen() instanceof ChatScreen screen)) return;
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int mouseX = event.getMouseX(), mouseY = event.getMouseY();
        int[] p = panel(screen.width, screen.height);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.fill(p[0], p[1], p[2], p[3], 0xE0101010);
        graphics.renderOutline(p[0], p[1], p[2] - p[0], p[3] - p[1], 0xFF505050);
        graphics.drawString(font, Component.translatable("gui.roll_mod.chat.gif.title"), p[0] + 3, p[1] + 2, 0xFFC878FF, false);
        int openX = p[2] - 34;
        int closeX = p[2] - 12;
        boolean overOpen = over(mouseX, mouseY, openX, p[1], closeX - 2, p[1] + TITLE);
        boolean overClose = over(mouseX, mouseY, closeX, p[1], p[2], p[1] + TITLE);
        graphics.drawString(font, "↗", openX + 6, p[1] + 2, overOpen ? 0xFFFFFFFF : 0xFFA0A0A0, false);
        graphics.drawString(font, "✕", closeX + 2, p[1] + 2, overClose ? 0xFFFF6060 : 0xFFA0A0A0, false);
        int top = p[1] + TITLE;
        if (status == Status.READY && texture != null) {
            int frame = MediaLoader.frameAt(delays, loop, Util.getMillis() - started);
            if (frame != shownFrame) {
                texture.getPixels().copyFrom(frames.get(frame));
                texture.upload();
                shownFrame = frame;
            }
            NativeImage image = frames.get(0);
            graphics.blit(TEXTURE, p[0] + 2, top, p[2] - p[0] - 4, p[3] - top - 2, 0, 0, image.getWidth(), image.getHeight(),
                    image.getWidth(), image.getHeight());
        } else {
            Component note = status == Status.LOADING ? Component.translatable("gui.roll_mod.chat.gif.loading")
                    : Component.translatable("gui.roll_mod.chat.gif.failed").withStyle(ChatFormatting.GRAY);
            graphics.drawString(font, note, p[0] + 4, top + 6, 0xFFA0A0A0, false);
        }
        graphics.pose().popPose();
        if (overOpen) graphics.renderTooltip(font, Component.translatable("gui.roll_mod.chat.gif.browser"), mouseX, mouseY);
    }

    /**
     * Clicks in the chat screen: on the open viewer its buttons act, a click inside is kept, outside
     * closes it; a click on a {@code [GIF]} link opens the viewer instead of the browser prompt.
     */
    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen screen) || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        double mouseX = event.getMouseX(), mouseY = event.getMouseY();
        if (url != null) {
            int[] p = panel(screen.width, screen.height);
            if (over(mouseX, mouseY, p[2] - 34, p[1], p[2] - 14, p[1] + TITLE)) {
                String link = url;
                close();
                ConfirmLinkScreen.confirmLinkNow(screen, link);
                event.setCanceled(true);
                return;
            }
            if (over(mouseX, mouseY, p[2] - 12, p[1], p[2], p[1] + TITLE)) {
                close();
                event.setCanceled(true);
                return;
            }
            if (over(mouseX, mouseY, p[0], p[1], p[2], p[3])) {
                event.setCanceled(true);
                return;
            }
            close();
        }
        Style style = Minecraft.getInstance().gui.getChat().getClickedComponentStyleAt(mouseX, mouseY);
        ClickEvent click = style == null ? null : style.getClickEvent();
        if (click != null && click.getAction() == ClickEvent.Action.OPEN_URL && ChatLinks.isGif(click.getValue())) {
            open(click.getValue());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (url != null && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE && event.getScreen() instanceof ChatScreen) {
            close();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ChatScreen) close();
    }

    private static boolean over(double mouseX, double mouseY, int x0, int y0, int x1, int y1) {
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }
}
