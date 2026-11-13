package com.roll_54.roll_mod.minestar.multiplayer;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.mojang.logging.LogUtils;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The server list, rebuilt in LdLib and wearing the hub theme ({@link RollUi}): the saved servers,
 * then the LAN scan and any worlds found on the LAN, in a scroller, with vanilla's buttons beneath —
 * Join, Direct Connection, Add Server / Edit, Delete, Refresh, Back.
 *
 * <p>Behaves as vanilla's {@link JoinMultiplayerScreen}, whose data side it reuses as is: the
 * {@code servers.dat} list ({@link ServerList}), the status pinger, LAN discovery, and vanilla's own
 * Add / Edit / Direct Connection / delete-confirmation screens, which come back here. Double-click
 * or Enter joins; Shift+Up/Down or the arrows on a server's icon reorder; F5 refreshes. Opened in
 * place of vanilla's by {@link MultiplayerScreenReplacer}.
 */
@OnlyIn(Dist.CLIENT)
public final class RollMultiplayerScreen extends ModularUIScreen {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** Vanilla's layout: a 32px header with the title, a 64px footer with two rows of buttons. */
    private static final int HEADER = 32;
    private static final int FOOTER = 64;

    private final Screen lastScreen;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final ServerList servers = new ServerList(minecraft);
    private final LanServerDetection.LanServerList lanServers = new LanServerDetection.LanServerList();
    @Nullable private LanServerDetection.LanServerDetector lanDetector;
    /** One favicon texture per address, kept across list rebuilds — vanilla registers them at a fixed location per address. */
    private final Map<String, FaviconTexture> icons = new HashMap<>();

    private final ScrollerView list = new ScrollerView();
    private final List<ServerRow> rows = new ArrayList<>();
    private final ServerRow lanHeader = new ServerRow.LanHeader(this);
    private final List<ServerRow.Lan> lanRows = new ArrayList<>();
    @Nullable private ServerRow selected;

    private final Button joinButton = button("selectServer.select", this::joinSelected);
    private final Button editButton = button("selectServer.edit", this::editSelected);
    private final Button deleteButton = button("selectServer.delete", this::deleteSelected);

    /** The server Add / Edit / Direct Connection is filling in, as vanilla keeps it between screens. */
    private ServerData editing;

    public RollMultiplayerScreen(Screen lastScreen) {
        this(lastScreen, new UIElement());
    }

    private RollMultiplayerScreen(Screen lastScreen, UIElement root) {
        super(ModularUI.of(UI.of(root, RollUi.sheets(), size -> size)), Component.translatable("multiplayer.title"));
        this.lastScreen = lastScreen;
        servers.load();
        build(root);
        rebuildSavedRows();
    }

    /* ------------------------------------------------------ layout ------------------------------------------------------ */

    private void build(UIElement root) {
        root.layout(l -> l.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER));

        Label title = new Label();
        title.setText(getTitle());
        title.layout(l -> l.widthPercent(100).height(HEADER).flexShrink(0));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).textShadow(true));
        root.addChild(title);

        // flexBasis(0): an auto basis sizes the scroller to its rows and it never scrolls (see WelcomeUI).
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewPort(v -> v.layout(l -> l.paddingAll(0)));
        list.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .alignItems(AlignItems.CENTER).paddingVertical(4)));
        root.addChild(list);

        UIElement footer = new UIElement();
        footer.layout(l -> l.widthPercent(100).height(FOOTER).flexShrink(0).flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER).gapRow(4));
        footer.addChild(buttonRow(
                sized(joinButton, 100),
                sized(button("selectServer.direct", this::directConnect), 100),
                sized(button("selectServer.add", this::addServer), 100)));
        footer.addChild(buttonRow(
                sized(editButton, 74),
                sized(deleteButton, 74),
                sized(button("selectServer.refresh", this::refresh), 74),
                sized(textButton(CommonComponents.GUI_BACK, this::onClose), 74)));
        root.addChild(footer);
    }

    /** A 308px row of buttons spread evenly, as vanilla's {@code EqualSpacingLayout}. */
    private static UIElement buttonRow(Button... buttons) {
        UIElement row = new UIElement();
        row.layout(l -> l.width(308).height(20).flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.SPACE_BETWEEN));
        for (Button button : buttons) row.addChild(button);
        return row;
    }

    private static Button sized(Button button, int width) {
        button.layout(l -> l.width(width).height(20).flexShrink(0));
        return button;
    }

    private static Button button(String key, Runnable onClick) {
        return textButton(Component.translatable(key), onClick);
    }

    private static Button textButton(Component text, Runnable onClick) {
        Button button = new Button();
        button.setText(text);
        button.setOnClick(e -> onClick.run());
        return button;
    }

    /* -------------------------------------------------------- rows -------------------------------------------------------- */

    /** Rebuilds the saved servers' rows from {@link #servers}, keeping the LAN part below them. */
    private void rebuildSavedRows() {
        @Nullable ServerData keep = selected instanceof ServerRow.Saved saved ? saved.data : null;
        rows.removeIf(row -> row instanceof ServerRow.Saved);
        List<ServerRow> saved = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            ServerData data = servers.get(i);
            ServerRow.Saved row = new ServerRow.Saved(this, data, i,
                    icons.computeIfAbsent(data.ip, ip -> FaviconTexture.forServer(minecraft.getTextureManager(), ip)));
            saved.add(row);
            if (data == keep) selected = row;
        }
        if (keep == null && selected instanceof ServerRow.Saved) selected = null;
        rows.addAll(0, saved);
        if (!rows.contains(lanHeader)) rows.add(lanHeader);
        refillList();
    }

    private void rebuildLanRows(List<LanServer> found) {
        @Nullable LanServer keep = selected instanceof ServerRow.Lan lan ? lan.server : null;
        rows.removeAll(lanRows);
        lanRows.clear();
        for (LanServer server : found) {
            ServerRow.Lan row = new ServerRow.Lan(this, server);
            lanRows.add(row);
            if (server == keep) selected = row;
        }
        if (keep != null && !(selected instanceof ServerRow.Lan lan && lanRows.contains(lan))) selected = null;
        rows.addAll(lanRows);
        refillList();
    }

    private void refillList() {
        list.clearAllScrollViewChildren();
        for (ServerRow row : rows) list.addScrollViewChild(row);
        if (selected != null && !rows.contains(selected)) selected = null;
        updateButtons();
    }

    @Nullable
    ServerRow selected() {
        return selected;
    }

    void select(@Nullable ServerRow row) {
        selected = row != null && row.selectable() ? row : null;
        updateButtons();
    }

    /** Selects {@code row} and scrolls it into view — for keyboard moves, where it may be off the list's edge. */
    private void selectAndReveal(ServerRow row) {
        select(row);
        if (selected != null) list.scrollToChild(selected);
    }

    private void updateButtons() {
        boolean joinable = selected != null && selected.selectable();
        boolean saved = selected instanceof ServerRow.Saved;
        joinButton.setActive(joinable);
        editButton.setActive(saved);
        deleteButton.setActive(saved);
    }

    ServerList servers() {
        return servers;
    }

    ServerStatusPinger pinger() {
        return pinger;
    }

    void saveServers() {
        servers.save();
    }

    /** Swaps two saved servers and keeps the moved one selected, as vanilla's arrows and Shift+Up/Down. */
    void moveServer(int from, int to) {
        servers.swap(from, to);
        servers.save();
        selected = null;
        rebuildSavedRows();
        selectAndReveal(rows.get(to));
    }

    /* ------------------------------------------------------ actions ------------------------------------------------------ */

    void joinSelected() {
        if (selected instanceof ServerRow.Saved saved) {
            join(saved.data);
        } else if (selected instanceof ServerRow.Lan lan) {
            join(new ServerData(lan.server.getMotd(), lan.server.getAddress(), ServerData.Type.LAN));
        }
    }

    private void join(ServerData server) {
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(server.ip), server, false, null);
    }

    private void directConnect() {
        editing = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
        minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, editing));
    }

    private void addServer() {
        editing = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
        minecraft.setScreen(new EditServerScreen(this, this::addServerCallback, editing));
    }

    private void editSelected() {
        if (!(selected instanceof ServerRow.Saved saved)) return;
        editing = new ServerData(saved.data.name, saved.data.ip, ServerData.Type.OTHER);
        editing.copyFrom(saved.data);
        minecraft.setScreen(new EditServerScreen(this, this::editServerCallback, editing));
    }

    private void deleteSelected() {
        if (!(selected instanceof ServerRow.Saved saved) || saved.data.name == null) return;
        minecraft.setScreen(new ConfirmScreen(this::deleteCallback,
                Component.translatable("selectServer.deleteQuestion"),
                Component.translatable("selectServer.deleteWarning", saved.data.name),
                Component.translatable("selectServer.deleteButton"),
                CommonComponents.GUI_CANCEL));
    }

    /** A fresh list, pings and all — vanilla reopens the screen. */
    private void refresh() {
        minecraft.setScreen(new RollMultiplayerScreen(lastScreen));
    }

    private void deleteCallback(boolean confirmed) {
        if (confirmed && selected instanceof ServerRow.Saved saved) {
            servers.remove(saved.data);
            servers.save();
            selected = null;
            rebuildSavedRows();
        }
        minecraft.setScreen(this);
    }

    private void editServerCallback(boolean confirmed) {
        if (confirmed && selected instanceof ServerRow.Saved saved) {
            ServerData data = saved.data;
            data.name = editing.name;
            data.ip = editing.ip;
            data.copyFrom(editing);
            servers.save();
            rebuildSavedRows();
        }
        minecraft.setScreen(this);
    }

    private void addServerCallback(boolean confirmed) {
        if (confirmed) {
            ServerData existing = servers.unhide(editing.ip);
            if (existing != null) {
                existing.copyNameIconFrom(editing);
            } else {
                servers.add(editing, false);
            }
            servers.save();
            selected = null;
            rebuildSavedRows();
        }
        minecraft.setScreen(this);
    }

    private void directJoinCallback(boolean confirmed) {
        if (!confirmed) {
            minecraft.setScreen(this);
            return;
        }
        ServerData existing = servers.get(editing.ip);
        if (existing == null) {
            servers.add(editing, true);
            servers.save();
            join(editing);
        } else {
            join(existing);
        }
    }

    /* ---------------------------------------------------- lifecycle ---------------------------------------------------- */

    /**
     * Each time the list is shown. Vanilla stops LAN discovery whenever it is left — for an Edit
     * screen too — and never restarts it; here it restarts on coming back. Pings the leaving cut
     * short start over.
     */
    @Override
    public void added() {
        super.added();
        if (lanDetector == null) {
            try {
                lanDetector = new LanServerDetection.LanServerDetector(lanServers);
                lanDetector.start();
            } catch (Exception e) {
                LOGGER.warn("Unable to start LAN server detection: {}", e.getMessage());
            }
        }
        for (int i = 0; i < servers.size(); i++) {
            ServerData data = servers.get(i);
            if (data.state() == ServerData.State.PINGING) data.setState(ServerData.State.INITIAL);
        }
    }

    @Override
    public void removed() {
        if (lanDetector != null) {
            lanDetector.interrupt();
            lanDetector = null;
        }
        pinger.removeAll();
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        List<LanServer> found = lanServers.takeDirtyServers();
        if (found != null) rebuildLanRows(found);
        pinger.tick();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    /* ------------------------------------------------------- input ------------------------------------------------------- */

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_F5) {
            refresh();
            return true;
        }
        if (selected == null) return false;
        if (CommonInputs.selected(keyCode)) {
            joinSelected();
            return true;
        }
        boolean down = keyCode == GLFW.GLFW_KEY_DOWN, up = keyCode == GLFW.GLFW_KEY_UP;
        if (!down && !up) return false;
        int step = down ? 1 : -1;
        int index = rows.indexOf(selected);
        if (hasShiftDown() && selected instanceof ServerRow.Saved) {
            // Saved servers are the list's first rows, so a row's index is its index in servers.dat.
            int target = index + step;
            if (target >= 0 && target < servers.size()) moveServer(index, target);
            return true;
        }
        // Up / Down walk the list, skipping the LAN header.
        for (int i = index + step; i >= 0 && i < rows.size(); i += step) {
            if (rows.get(i).selectable()) {
                selectAndReveal(rows.get(i));
                break;
            }
        }
        return true;
    }

    /* ----------------------------------------------------- rendering ----------------------------------------------------- */

    /** Vanilla's menu background: the panorama when there's no world, blurred, darkened. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) renderPanorama(graphics, partialTick);
        renderBlurredBackground(partialTick);
        renderMenuBackground(graphics);
    }
}
