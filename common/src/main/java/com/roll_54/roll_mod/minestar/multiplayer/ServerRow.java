package com.roll_54.roll_mod.minestar.multiplayer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * One row of {@link RollMultiplayerScreen}'s list — a saved server, the LAN "scanning" line, or a
 * server found on the LAN. The drawing, the click areas and the ping handling are vanilla's
 * {@code ServerSelectionList} entries, moved into an LdLib element so the list can sit in a themed
 * scroller: same favicon, MOTD, ping bars, player-count tooltip, and the join / move-up / move-down
 * arrows over the icon on hover.
 *
 * <p>The row draws into its box ({@link #WIDTH} × {@link #HEIGHT}); the content sits 2px in, as in
 * vanilla, leaving room for the selection outline.
 */
@OnlyIn(Dist.CLIENT)
abstract class ServerRow extends UIElement {

    static final int WIDTH = 305;
    static final int HEIGHT = 36;
    private static final int ICON = 32;
    /** Vanilla's double-click window. */
    private static final long DOUBLE_CLICK_MS = 250;
    private static final int GREY = 0xFF808080;
    private static final int DARK_GREY = 0xFF303030;

    protected final RollMultiplayerScreen screen;
    protected final Minecraft minecraft = Minecraft.getInstance();
    private long lastClickTime;

    ServerRow(RollMultiplayerScreen screen) {
        this.screen = screen;
        layout(l -> l.width(WIDTH).height(HEIGHT).flexShrink(0));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
    }

    /** Whether the row can be selected and joined; the LAN header cannot. */
    boolean selectable() {
        return true;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        GuiGraphics graphics = context.graphics;
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        graphics.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (screen.selected() == this) {
            graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFFFFFFFF);
            graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFF000000);
        }
        drawContent(graphics, x + 2, y + 2, WIDTH - 4, context.mouseX, context.mouseY, isHover());
        graphics.flush();
    }

    abstract void drawContent(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY, boolean hovered);

    private void onMouseDown(UIEvent event) {
        if (event.button != 0 || !selectable()) return;
        int dx = Math.round(event.x - getPositionX()) - 2;
        int dy = Math.round(event.y - getPositionY()) - 2;
        if (clickedIcon(dx, dy)) return;
        screen.select(this);
        if (Util.getMillis() - lastClickTime < DOUBLE_CLICK_MS) screen.joinSelected();
        lastClickTime = Util.getMillis();
    }

    /** A click on the icon area, relative to the content's corner; true if it did something there. */
    boolean clickedIcon(int dx, int dy) {
        return false;
    }

    /* =================================================== saved server =================================================== */

    /** A server from {@code servers.dat}. */
    static final class Saved extends ServerRow {

        private static final Logger LOGGER = LogUtils.getLogger();
        /** Vanilla's pinger pool ({@code ServerSelectionList.THREAD_POOL} is package-private). */
        private static final ThreadPoolExecutor PINGERS = new ScheduledThreadPoolExecutor(5,
                new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true)
                        .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());

        private static final ResourceLocation INCOMPATIBLE = sprite("incompatible");
        private static final ResourceLocation UNREACHABLE = sprite("unreachable");
        private static final ResourceLocation[] PING = {sprite("ping_1"), sprite("ping_2"), sprite("ping_3"), sprite("ping_4"), sprite("ping_5")};
        private static final ResourceLocation[] PINGING = {sprite("pinging_1"), sprite("pinging_2"), sprite("pinging_3"), sprite("pinging_4"), sprite("pinging_5")};
        private static final ResourceLocation JOIN = sprite("join");
        private static final ResourceLocation JOIN_HIGHLIGHTED = sprite("join_highlighted");
        private static final ResourceLocation MOVE_UP = sprite("move_up");
        private static final ResourceLocation MOVE_UP_HIGHLIGHTED = sprite("move_up_highlighted");
        private static final ResourceLocation MOVE_DOWN = sprite("move_down");
        private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED = sprite("move_down_highlighted");

        private static final Component CANT_RESOLVE = Component.translatable("multiplayer.status.cannot_resolve").withColor(0xFFFF0000);
        private static final Component CANT_CONNECT = Component.translatable("multiplayer.status.cannot_connect").withColor(0xFFFF0000);
        private static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
        private static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
        private static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");

        final ServerData data;
        private final int index;
        private final FaviconTexture icon;
        @Nullable private byte[] lastIconBytes;
        @Nullable private ResourceLocation statusIcon;
        @Nullable private Component statusTooltip;
        @Nullable private List<Component> playersTooltip;
        // Where the status icon and the player count were last drawn, relative to the content's corner, for tooltips.
        private int statusIconX, statusTextX, statusTextWidth;

        Saved(RollMultiplayerScreen screen, ServerData data, int index, FaviconTexture icon) {
            super(screen);
            this.data = data;
            this.index = index;
            this.icon = icon;
            refreshStatus();
            addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        }

        @Override
        void drawContent(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY, boolean hovered) {
            if (data.state() == ServerData.State.INITIAL) startPing();
            Font font = minecraft.font;

            graphics.drawString(font, data.name, left + ICON + 3, top + 1, 0xFFFFFFFF, false);
            List<FormattedCharSequence> motd = font.split(data.motd, width - ICON - 2);
            for (int i = 0; i < Math.min(motd.size(), 2); i++) {
                graphics.drawString(font, motd.get(i), left + ICON + 3, top + 12 + 9 * i, GREY, false);
            }

            uploadIconIfChanged();
            RenderSystem.enableBlend();
            graphics.blit(icon.textureLocation(), left, top, 0f, 0f, ICON, ICON, ICON, ICON);
            RenderSystem.disableBlend();

            if (data.state() == ServerData.State.PINGING) {
                int frame = (int) (Util.getMillis() / 100L + index * 2L & 7L);
                if (frame > 4) frame = 8 - frame;
                statusIcon = PINGING[frame];
            }
            statusIconX = width - 10 - 5;
            if (statusIcon != null) graphics.blitSprite(statusIcon, left + statusIconX, top, 10, 8);

            Component status = data.state() == ServerData.State.INCOMPATIBLE
                    ? data.version.copy().withStyle(ChatFormatting.RED) : data.status;
            statusTextWidth = font.width(status);
            statusTextX = statusIconX - statusTextWidth - 5;
            graphics.drawString(font, status, left + statusTextX, top + 1, GREY, false);

            if (minecraft.options.touchscreen().get() || hovered) {
                int dx = mouseX - left, dy = mouseY - top;
                graphics.fill(left, top, left + ICON, top + ICON, 0xA0909090);
                graphics.blitSprite(dx < ICON && dx > 16 ? JOIN_HIGHLIGHTED : JOIN, left, top, ICON, ICON);
                if (index > 0) {
                    graphics.blitSprite(dx < 16 && dy < 16 ? MOVE_UP_HIGHLIGHTED : MOVE_UP, left, top, ICON, ICON);
                }
                if (index < screen.servers().size() - 1) {
                    graphics.blitSprite(dx < 16 && dy > 16 ? MOVE_DOWN_HIGHLIGHTED : MOVE_DOWN, left, top, ICON, ICON);
                }
            }
        }

        @Override
        boolean clickedIcon(int dx, int dy) {
            if (dx > ICON || dx < 0) return false;
            if (dx > 16 && dx < ICON) {
                screen.select(this);
                screen.joinSelected();
                return true;
            }
            if (dx < 16 && dy < 16 && index > 0) {
                screen.moveServer(index, index - 1);
                return true;
            }
            if (dx < 16 && dy > 16 && index < screen.servers().size() - 1) {
                screen.moveServer(index, index + 1);
                return true;
            }
            return false;
        }

        private void onHoverTooltips(UIEvent event) {
            int dx = Math.round(event.x - getPositionX()) - 2;
            int dy = Math.round(event.y - getPositionY()) - 2;
            List<Component> lines = null;
            if (statusTooltip != null && dx >= statusIconX && dx <= statusIconX + 10 && dy >= 0 && dy <= 8) {
                lines = List.of(statusTooltip);
            } else if (playersTooltip != null && dx >= statusTextX && dx <= statusTextX + statusTextWidth && dy >= 0 && dy <= 8) {
                lines = playersTooltip;
            }
            if (lines != null) event.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY);
        }

        /** Vanilla's ping: on the pinger pool, results handed back to the render thread. */
        private void startPing() {
            data.setState(ServerData.State.PINGING);
            data.motd = CommonComponents.EMPTY;
            data.status = CommonComponents.EMPTY;
            PINGERS.submit(() -> {
                try {
                    screen.pinger().pingServer(data, () -> minecraft.execute(screen::saveServers), () -> {
                        data.setState(data.protocol == SharedConstants.getCurrentVersion().getProtocolVersion()
                                ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                        minecraft.execute(this::refreshStatus);
                    });
                } catch (UnknownHostException e) {
                    data.setState(ServerData.State.UNREACHABLE);
                    data.motd = CANT_RESOLVE;
                    minecraft.execute(this::refreshStatus);
                } catch (Exception e) {
                    data.setState(ServerData.State.UNREACHABLE);
                    data.motd = CANT_CONNECT;
                    minecraft.execute(this::refreshStatus);
                }
            });
        }

        private void refreshStatus() {
            playersTooltip = null;
            switch (data.state()) {
                case INITIAL, PINGING -> {
                    statusIcon = PING[0];
                    statusTooltip = PINGING_STATUS;
                }
                case INCOMPATIBLE -> {
                    statusIcon = INCOMPATIBLE;
                    statusTooltip = INCOMPATIBLE_STATUS;
                    playersTooltip = data.playerList;
                }
                case UNREACHABLE -> {
                    statusIcon = UNREACHABLE;
                    statusTooltip = NO_CONNECTION_STATUS;
                }
                case SUCCESSFUL -> {
                    long ping = data.ping;
                    statusIcon = PING[ping < 150 ? 4 : ping < 300 ? 3 : ping < 600 ? 2 : ping < 1000 ? 1 : 0];
                    statusTooltip = Component.translatable("multiplayer.status.ping", ping);
                    playersTooltip = data.playerList;
                }
            }
        }

        private void uploadIconIfChanged() {
            byte[] bytes = data.getIconBytes();
            if (Arrays.equals(bytes, lastIconBytes)) return;
            if (bytes == null) {
                icon.clear();
                lastIconBytes = null;
                return;
            }
            try {
                icon.upload(NativeImage.read(bytes));
                lastIconBytes = bytes;
            } catch (Throwable t) {
                RollMod.LOGGER.error("Invalid icon for server {} ({})", data.name, data.ip, t);
                data.setIconBytes(null);
                screen.saveServers();
            }
        }

        private static ResourceLocation sprite(String name) {
            return ResourceLocation.withDefaultNamespace("server_list/" + name);
        }
    }

    /* ==================================================== LAN header ==================================================== */

    /** "Scanning for games on your local network", with vanilla's loading dots. */
    static final class LanHeader extends ServerRow {

        private static final Component SCANNING = Component.translatable("lanServer.scanning");

        LanHeader(RollMultiplayerScreen screen) {
            super(screen);
        }

        @Override
        boolean selectable() {
            return false;
        }

        @Override
        void drawContent(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY, boolean hovered) {
            Font font = minecraft.font;
            int center = left + width / 2;
            int y = top + 16 - font.lineHeight / 2;
            graphics.drawString(font, SCANNING, center - font.width(SCANNING) / 2, y, 0xFFFFFFFF, false);
            String dots = LoadingDotsText.get(Util.getMillis());
            graphics.drawString(font, dots, center - font.width(dots) / 2, y + font.lineHeight, GREY, false);
        }
    }

    /* ==================================================== LAN server ==================================================== */

    /** A world opened to LAN on this network. */
    static final class Lan extends ServerRow {

        private static final Component TITLE = Component.translatable("lanServer.title");
        private static final Component HIDDEN_ADDRESS = Component.translatable("selectServer.hiddenAddress");

        final LanServer server;

        Lan(RollMultiplayerScreen screen, LanServer server) {
            super(screen);
            this.server = server;
        }

        @Override
        void drawContent(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY, boolean hovered) {
            Font font = minecraft.font;
            graphics.drawString(font, TITLE, left + ICON + 3, top + 1, 0xFFFFFFFF, false);
            graphics.drawString(font, server.getMotd(), left + ICON + 3, top + 12, GREY, false);
            if (minecraft.options.hideServerAddress) {
                graphics.drawString(font, HIDDEN_ADDRESS, left + ICON + 3, top + 23, DARK_GREY, false);
            } else {
                graphics.drawString(font, server.getAddress(), left + ICON + 3, top + 23, DARK_GREY, false);
            }
        }
    }
}
