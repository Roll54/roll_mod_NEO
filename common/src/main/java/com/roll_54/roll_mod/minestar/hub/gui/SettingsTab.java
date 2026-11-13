package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatButton;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import com.roll_54.roll_mod.minestar.chat.client.ClientChatStore;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.minestar.tpa.TpaMode;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod.network.packet.TpaModePacket;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

/**
 * The hub's Settings page, laid out like Eden_LD's: the sections down the left (Appearance, Hub, Chat, Chat
 * buttons, Privacy), and the open section's options on the right, each a tile with its name, its control and a
 * line saying what it does. A hub tab of its own, without a bookmark: the gear among the window controls opens it.
 *
 * <ul>
 *   <li><b>Appearance</b> — the hub theme, picked from cards that preview it, and the experimental switch that
 *       also forces it on other mods' LdLib screens.
 *   <li><b>Hub</b> — where the tabs sit, moving the window, closing the extra windows with it.
 *   <li><b>Chat</b> — Eden's chat settings, adapted: whose deaths and advancements show (everyone, the team,
 *       nobody), Discord and Wixie lines, the mention volume and what each channel's lines do, the "!" shortcut.
 *   <li><b>Chat buttons</b> — the buttons over the chat box: which show, which are in All, their order, and the
 *       player's own ones ({@link ChatButton}).
 *   <li><b>Notifications</b> — what the tabs point out, and as a badge, a glow or both, in which colour
 *       ({@link HubNotifications}).
 *   <li><b>Privacy</b> — who may send teleport requests (the server's to keep).
 * </ul>
 *
 * <p>Everything but the teleport requests is client preference ({@link UiThemeState}, {@link HubWindowState},
 * {@link ChatChannelState}), read and written on the client copy of the tree only.
 */
final class SettingsTab {

    private static final int ROW_H = 16;
    private static final int SMALL = 14;
    /** The "In All" tick with its caption, in each chat button's row. */
    private static final int IN_ALL_W = 64;
    /** Space between a tile and the text and controls inside it. */
    private static final int INSET = 4;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int WARNING = 12;
    /** The section list on the left. */
    private static final int NAV_W = 92;
    private static final int NAV_H = 16;
    private static final int NAV_IDLE = 0x20FFFFFF;
    private static final int NAV_HOVER = 0x38FFFFFF;
    private static final int NAV_PICKED = 0x60FFFFFF;
    /** A control on the right of an option tile. */
    private static final int CONTROL_W = 110;
    /** Theme cards: three to a row. */
    private static final int CARD_W = 104;
    private static final int CARD_PREVIEW_H = 54;
    private static final int PICKED = 0xFF8BE36B;
    /** Where drop-downs open: above the page's tiles. */
    private static final int DIALOG_Z = 50;

    private enum Section {
        APPEARANCE, HUB, CHAT, BUTTONS, NOTIFICATIONS, PRIVACY;

        Component title() {
            return Component.translatable("gui.roll_mod.hub.settings.tab." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private SettingsTab() {}

    /** The Settings hub tab. */
    static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));
        // A title and the "i" with what the page holds (hub_info/<lang>/settings.json), as every tab has.
        HubSection.Info section = HubSection.info("settings");
        root.addChildren(HubSection.header("settings", section), page(player, DIALOG_Z), section.overlay());
        return root;
    }

    /**
     * The page, filling whatever it is given. Also what the deprecated settings popup ({@link HubSettings})
     * holds.
     */
    static UIElement content(Player player, int dialogZ) {
        return page(player, dialogZ);
    }

    private static UIElement page(Player player, int dialogZ) {
        boolean client = player.level().isClientSide();

        UIElement page = new UIElement();
        page.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1).gapColumn(6));

        UIElement nav = new UIElement();
        nav.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(NAV_W).flexShrink(0).heightPercent(100).gapRow(2));

        ScrollerView body = new ScrollerView();
        body.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        body.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        body.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(4)));
        body.viewPort(v -> v.layout(l -> l.paddingRight(8)));

        Section[] open = {Section.APPEARANCE};
        Consumer<Section> show = section -> {
            open[0] = section;
            body.clearAllScrollViewChildren();
            switch (section) {
                case APPEARANCE -> appearance(body, client, dialogZ);
                case HUB -> hub(body, client, dialogZ);
                case CHAT -> chat(body, client, dialogZ);
                case BUTTONS -> buttons(body, client);
                case NOTIFICATIONS -> notifications(body, client, dialogZ);
                case PRIVACY -> privacy(body, client, dialogZ);
            }
        };
        for (Section section : Section.values()) {
            nav.addChild(navItem(section, () -> open[0] == section, () -> show.accept(section)));
        }
        show.accept(Section.APPEARANCE);

        page.addChildren(nav, body);
        return page;
    }

    private static UIElement navItem(Section section, java.util.function.BooleanSupplier picked, Runnable click) {
        UIElement item = new UIElement();
        item.layout(l -> l.widthPercent(100).height(NAV_H).paddingHorizontal(6).flexShrink(0));
        item.style(s -> s.background(IGuiTexture.dynamic(() -> new ColorRectTexture(
                picked.getAsBoolean() ? NAV_PICKED : item.isHover() ? NAV_HOVER : NAV_IDLE))));
        Label label = new Label();
        label.setText(section.title());
        label.layout(l -> l.widthPercent(100).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        item.addChild(label);
        item.addEventListener(UIEvents.CLICK, e -> click.run());
        return item;
    }

    /* ------------------------------------------------ sections ------------------------------------------------ */

    private static void appearance(ScrollerView body, boolean client, int dialogZ) {
        // As Eden's Visual tab: the hub's language, its theme, and how notifications look.
        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.themes"));
        UIElement grid = new UIElement();
        // A pixel of room on every side, so the outer cards' frames are not cut by the scroller's edge.
        grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).widthPercent(100)
                .gapColumn(4).gapRow(4).paddingAll(1));
        for (UiTheme theme : UiTheme.values()) grid.addChild(themeCard(theme, client));
        body.addScrollViewChild(grid);

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.appearance"));
        // The font, apart from the theme (UiFont): laid over whichever theme is picked.
        Selector<com.roll_54.roll_mod.minestar.theme.UiFont.Choice> font = selector(
                com.roll_54.roll_mod.minestar.theme.UiFont.offered(),
                com.roll_54.roll_mod.minestar.theme.UiFont.Choice::title,
                client ? com.roll_54.roll_mod.minestar.theme.UiFont.get() : com.roll_54.roll_mod.minestar.theme.UiFont.Choice.MINECRAFT,
                dialogZ, picked -> {});
        font.setOnValueChanged(picked -> {
            if (picked == null || !client || picked == com.roll_54.roll_mod.minestar.theme.UiFont.get()) return;
            com.roll_54.roll_mod.minestar.theme.UiFont.set(picked);
            RollUi.apply(font.getModularUI());
        });
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.font", "gui.roll_mod.hub.settings.font.tip", font));
        // Experimental: the theme on other mods' LdLib screens too, flagged with a warning mark.
        UIElement warning = new UIElement();
        warning.layout(l -> l.width(WARNING).height(WARNING).flexShrink(0));
        warning.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/gui/hub/exclamation_mark.png"))));
        warning.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                Component.translatable("gui.roll_mod.hub.settings.experimental").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.roll_mod.hub.settings.force.warning").withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));
        UIElement controls = new UIElement();
        controls.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapColumn(4));
        controls.addChildren(toggle(client && UiThemeState.forceEverywhere(), on -> {
            if (client) UiThemeState.setForceEverywhere(on);
        }));
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.force", "gui.roll_mod.hub.settings.force.tip", controls));

        // Each colour written in itself, and the badge beside the list in the colour picked.
        Selector<HubNotifications.Color> notifyColor = selector(List.of(HubNotifications.Color.values()),
                c -> c.title().copy().withColor(c.argb() & 0xFFFFFF),
                client ? HubNotifications.color() : HubNotifications.Color.THEME, dialogZ, picked -> {
                    if (client) HubNotifications.setColor(picked);
                });
        UIElement badge = new UIElement();
        badge.layout(l -> l.width(12).height(12).flexShrink(0));
        badge.style(s -> s.background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(HubBadge::coloured)));
        UIElement colorControl = new UIElement();
        colorControl.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapColumn(4).flexShrink(0));
        colorControl.addChildren(badge, notifyColor);
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.notify_color", "gui.roll_mod.hub.settings.notify_color.tip",
                colorControl));
    }

    /**
     * A theme as a card, like Eden's hub theme picker: a small window drawn in the theme's own colours (a caption,
     * a text field, a bar in its accent and a button), its name, and "Picked" on the one in use. A click picks it.
     */
    private static UIElement themeCard(UiTheme theme, boolean client) {
        UiTheme.Recolor recolor = theme.recolor();
        UiTheme.Palette palette = theme.palette();
        int outline = recolor != null ? recolor.outline() : 0xFF000000;
        int surface = recolor != null ? recolor.surface() : 0xFFC6C6C6;
        int well = recolor != null ? recolor.well() : 0xFF8B8B8B;
        int text = recolor != null ? recolor.text() : 0xFF404040;
        int accent = palette != null ? palette.accent() : 0xFF3F76E4;
        int raised = palette != null ? palette.raised() : 0xFF9C9C9C;
        int line = palette != null ? palette.line() | 0xFF000000 : 0xFF000000;

        UIElement card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(CARD_W).paddingAll(3).paddingBottom(4).gapRow(3));
        card.style(s -> s.background(IGuiTexture.dynamic(() -> {
            boolean picked = client && UiThemeState.get() == theme;
            return new GuiTextureGroup(new ColorRectTexture(card.isHover() ? 0x40FFFFFF : 0x24FFFFFF),
                    new ColorBorderTexture(1, picked ? PICKED : card.isHover() ? 0xFFB0B0B0 : 0x60FFFFFF));
        })));

        UIElement preview = new UIElement();
        preview.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).height(CARD_PREVIEW_H)
                .paddingAll(4).gapRow(3));
        preview.style(s -> s.background(new GuiTextureGroup(new ColorRectTexture(surface), new ColorBorderTexture(1, outline))));
        Label caption = new Label();
        caption.setText(Component.literal("Aa"));
        caption.layout(l -> l.widthPercent(100).height(9));
        caption.textStyle(t -> t.textColor(text).textWrap(TextWrap.HIDE));
        UIElement field = new UIElement();
        field.layout(l -> l.widthPercent(100).height(10));
        field.style(s -> s.background(new GuiTextureGroup(new ColorRectTexture(well), new ColorBorderTexture(1, line))));
        UIElement bottom = new UIElement();
        bottom.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(9).alignItems(AlignItems.CENTER)
                .gapColumn(4));
        UIElement bar = new UIElement();
        bar.layout(l -> l.flexBasis(0).flexGrow(1).height(2));
        bar.style(s -> s.background(new ColorRectTexture(accent)));
        UIElement button = new UIElement();
        button.layout(l -> l.width(26).height(9));
        button.style(s -> s.background(new GuiTextureGroup(new ColorRectTexture(raised), new ColorBorderTexture(1, line))));
        bottom.addChildren(bar, button);
        preview.addChildren(caption, field, bottom);

        UIElement footer = new UIElement();
        footer.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(10).gapColumn(2).marginTop(5)
                .paddingHorizontal(2));
        Label name = new Label();
        name.setText(theme.title());
        name.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        name.textStyle(t -> t.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
        Label state = new Label();
        state.setText(Component.translatable("gui.roll_mod.hub.settings.theme.picked"));
        state.layout(l -> l.heightPercent(100));
        state.textStyle(t -> t.textColor(PICKED).textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.RIGHT));
        state.addEventListener(UIEvents.TICK, e -> state.setDisplay(client && UiThemeState.get() == theme));
        footer.addChildren(name, state);

        card.addChildren(preview, footer);
        card.addEventListener(UIEvents.CLICK, e -> {
            if (client && UiThemeState.get() != theme) RollUi.set(theme, card.getModularUI());
        });
        return card;
    }

    private static void hub(ScrollerView body, boolean client, int dialogZ) {
        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.hub"));
        Selector<HubTabSide> side = selector(List.of(HubTabSide.values()), HubTabSide::title,
                client ? HubWindowState.get().tabSide() : HubTabSide.TOP, dialogZ, picked -> {
                    if (!client) return;
                    HubWindowState state = HubWindowState.get();
                    if (state.tabSide() == picked) return;
                    state.setTabSide(picked);
                    state.save();
                    // The bookmarks are placed when the hub is built, so it is reopened on the same tab.
                    PacketDistributor.sendToServer(new OpenHubPacket(HubUI.liveTab()));
                });
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.tabs", "gui.roll_mod.hub.settings.tabs.tip", side));

        body.addScrollViewChild(option("gui.roll_mod.hub.settings.movable", "gui.roll_mod.hub.settings.movable.tip",
                toggle(client && HubWindowState.get().movable(), on -> {
                    if (!client) return;
                    HubWindowState state = HubWindowState.get();
                    state.setMovable(on);
                    state.save();
                })));

        // Whether the info window and the hub's popups close when the hub does. On by default: an
        // always-on-top window left over the game with nothing behind it is the surprising case.
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.closeWindows",
                "gui.roll_mod.hub.settings.closeWindows.tip",
                toggle(!client || HubWindowState.get().closeWindowsWithHub(), on -> {
                    if (!client) return;
                    HubWindowState state = HubWindowState.get();
                    state.setCloseWindowsWithHub(on);
                    state.save();
                })));

        tabOrder(body, client);
    }

    /** The bookmarks' order: a row per tab, moved up or down; applied to the open hub at once. */
    private static void tabOrder(ScrollerView body, boolean client) {
        UIElement tile = option(Component.translatable("gui.roll_mod.hub.settings.tab_order"),
                Component.translatable("gui.roll_mod.hub.settings.tab_order.tip"), new UIElement());
        UIElement rows = new UIElement();
        rows.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(2));
        tile.addChild(rows);
        Runnable[] rebuild = {() -> {}};
        rebuild[0] = () -> {
            rows.clearAllChildren();
            if (!client) return;
            List<String> ids = HubUI.orderedTabIds();
            for (int i = 0; i < ids.size(); i++) {
                final int at = i;
                String id = ids.get(i);
                UIElement row = row();
                row.layout(l -> l.height(SMALL + 2));
                UIElement icon = new UIElement();
                icon.layout(l -> l.width(SMALL).height(SMALL).flexShrink(0));
                icon.style(s -> s.background(HubUI.tabIcon(id)));
                Label name = new Label();
                name.setText(HubUI.tabTitle(id));
                name.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
                row.addChildren(icon, name,
                        small("▲", at > 0, () -> { move(ids, at, -1); rebuild[0].run(); }),
                        small("▼", at < ids.size() - 1, () -> { move(ids, at, 1); rebuild[0].run(); }));
                rows.addChild(row);
            }
        };
        rebuild[0].run();
        Button reset = new Button();
        reset.setText(Component.translatable("gui.roll_mod.hub.settings.tab_order.reset"));
        reset.layout(l -> l.width(CONTROL_W).height(ROW_H).flexShrink(0));
        reset.setOnClick(e -> {
            if (!client) return;
            HubUI.setTabOrder(List.of());
            rebuild[0].run();
        });
        tile.addChild(reset);
        body.addScrollViewChild(tile);
    }

    private static void move(List<String> ids, int at, int by) {
        List<String> order = new java.util.ArrayList<>(ids);
        int to = at + by;
        if (to < 0 || to >= order.size()) return;
        order.add(to, order.remove(at));
        HubUI.setTabOrder(order);
    }

    private static void chat(ScrollerView body, boolean client, int dialogZ) {
        ChatChannelState state = client ? ChatChannelState.get() : null;

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.messages"));
        List<ChatChannelState.MessagesFrom> from = List.of(ChatChannelState.MessagesFrom.values());
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.deaths", "gui.roll_mod.hub.settings.deaths.tip",
                selector(from, ChatChannelState.MessagesFrom::title,
                        state != null ? state.deaths() : ChatChannelState.MessagesFrom.EVERYONE, dialogZ, picked -> {
                            if (state != null) state.setDeaths(picked);
                        })));
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.advancements",
                "gui.roll_mod.hub.settings.advancements.tip",
                selector(from, ChatChannelState.MessagesFrom::title,
                        state != null ? state.advancements() : ChatChannelState.MessagesFrom.EVERYONE, dialogZ, picked -> {
                            if (state != null) state.setAdvancements(picked);
                        })));
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.discord", "gui.roll_mod.hub.settings.discord.tip",
                toggle(state == null || !state.hideDiscord(), on -> {
                    if (state != null) state.setHideDiscord(!on);
                })));
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.wixie", "gui.roll_mod.hub.settings.wixie.tip",
                toggle(state == null || !state.hideWixie(), on -> {
                    if (state != null) state.setHideWixie(!on);
                })));

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.notifications"));
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.mention_volume",
                "gui.roll_mod.hub.settings.mention_volume.tip", volumeSlider(state)));

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.channel_notify"));
        List<ChatChannelState.Notify> levels = List.of(ChatChannelState.Notify.values());
        boolean first = true;
        for (ChatChannel channel : List.of(ChatChannel.GLOBAL, ChatChannel.LOCAL, ChatChannel.TEAM, ChatChannel.TRADE,
                ChatChannel.FRIENDS, ChatChannel.PM)) {
            Selector<ChatChannelState.Notify> selector = selector(levels, ChatChannelState.Notify::title,
                    state != null ? state.notify(channel) : ChatChannelState.Notify.MENTIONS, dialogZ, picked -> {
                        if (state != null) state.setNotify(channel, picked);
                    });
            // The explanation once, under the first channel.
            body.addScrollViewChild(option(channel.title(),
                    first ? Component.translatable("gui.roll_mod.hub.settings.channel_notify.tip") : null, selector));
            first = false;
        }

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.typing"));
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.chat.bang", "gui.roll_mod.hub.settings.chat.bang.tip",
                toggle(state != null && state.bangGlobal(), on -> {
                    if (state != null) state.setBangGlobal(on);
                })));
    }

    /** The mention ping's volume, 0-100: 0 is off. Plays it at the new volume once the slider is let go. */
    private static UIElement volumeSlider(ChatChannelState state) {
        UIElement box = new UIElement();
        box.layout(l -> l.flexDirection(FlexDirection.ROW).width(CONTROL_W).height(ROW_H).alignItems(AlignItems.CENTER)
                .gapColumn(4));
        Slider.Horizontal slider = new Slider.Horizontal();
        slider.setRange(0, 100);
        slider.setValue(state != null ? (float) state.mentionVolume() : 100f, false);
        slider.layout(l -> l.flexBasis(0).flexGrow(1).height(10));
        Label value = new Label();
        value.layout(l -> l.width(24).heightPercent(100));
        value.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.RIGHT));
        Runnable label = () -> {
            int volume = Math.round(slider.getValue());
            value.setText(volume == 0 ? Component.translatable("gui.roll_mod.hub.settings.mention_volume.off")
                    : Component.literal(volume + "%"));
        };
        label.run();
        slider.setOnValueChanged(v -> label.run());
        // Saved (a file write) and previewed once the slider is let go, not on every step of a drag — that wrote
        // the settings file dozens of times a second and made the page stutter.
        slider.addEventListener(UIEvents.TICK, e -> {
            if (state == null || slider.isDragging()) return;
            int volume = Math.round(slider.getValue());
            if (volume == state.mentionVolume()) return;
            state.setMentionVolume(volume);
            com.roll_54.roll_mod.minestar.chat.client.MentionSound.preview(volume);
        });
        box.addChildren(slider, value);
        return box;
    }

    private static void buttons(ScrollerView body, boolean client) {
        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.chat"));
        Label hint = new Label();
        hint.setText(Component.translatable("gui.roll_mod.hub.settings.chat.hint").withStyle(ChatFormatting.GRAY));
        hint.layout(l -> l.widthPercent(100).paddingHorizontal(2));
        hint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        body.addScrollViewChild(hint);

        UIElement channels = new UIElement();
        channels.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(2));
        body.addScrollViewChild(channels);
        Runnable[] rebuild = {() -> {}};
        rebuild[0] = () -> {
            channels.clearAllChildren();
            if (!client) return;
            ChatChannelState state = ChatChannelState.get();
            List<ChatButton> all = state.all();
            for (int i = 0; i < all.size(); i++) {
                channels.addChild(channelRow(state, all.get(i), i == 0, i == all.size() - 1, rebuild[0]));
            }
        };
        rebuild[0].run();

        // Adding one: a caption and a template.
        UIElement addRow = row();
        addRow.layout(l -> l.paddingHorizontal(2).marginTop(2));
        TextField name = new TextField();
        name.setText("");
        name.layout(l -> l.width(80).height(ROW_H));
        hintOn(name, "gui.roll_mod.hub.settings.chat.name.tip");
        TextField template = new TextField();
        template.setText("");
        template.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H));
        hintOn(template, "gui.roll_mod.hub.settings.chat.template.tip");
        Button add = new Button();
        add.setText(Component.translatable("gui.roll_mod.hub.settings.chat.add"));
        add.layout(l -> l.width(50).height(ROW_H));
        add.setOnClick(e -> {
            if (!client || name.getText().isBlank() || template.getText().isBlank()) return;
            ChatChannelState.get().addCustom(name.getText(), template.getText());
            name.setText("");
            template.setText("");
            rebuild[0].run();
        });
        addRow.addChildren(name, template, add);
        body.addScrollViewChild(addRow);
    }

    /**
     * Eden's Notifications tab, without its guild, party, activity and PvP kinds: what the hub's tabs point
     * out ({@link HubNotifications.Kind}), and how it looks.
     */
    private static void notifications(ScrollerView body, boolean client, int dialogZ) {
        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.notify_kinds"));
        for (HubNotifications.Kind kind : HubNotifications.Kind.values()) {
            body.addScrollViewChild(option(kind.title(), kind.hint(),
                    toggle(!client || HubNotifications.enabled(kind), on -> {
                        if (client) HubNotifications.setEnabled(kind, on);
                    })));
        }
    }

    private static void privacy(ScrollerView body, boolean client, int dialogZ) {
        // Who may send this player teleport requests. Unlike the rest of the page it is the server's
        // to keep: a pick only sends the packet, and the shown value follows ClientTpaCache, which the
        // hub's TPA sync fills on open and again after every change — so a refused change never sticks.
        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.teleports"));
        Selector<TpaMode> tpaMode = selector(List.of(TpaMode.values()), TpaMode::title,
                client ? ClientTpaCache.MODE : TpaMode.DEFAULT, dialogZ, picked -> {
                    if (client) PacketDistributor.sendToServer(TpaModePacket.of(picked));
                });
        // Ticks only while the page is shown, which is the only time the value has to be right.
        TpaMode[] shownMode = {client ? ClientTpaCache.MODE : TpaMode.DEFAULT};
        tpaMode.addEventListener(UIEvents.TICK, e -> {
            TpaMode mode = ClientTpaCache.MODE;
            if (mode != null && mode != shownMode[0]) {
                shownMode[0] = mode;
                tpaMode.setSelected(mode, false);
            }
        });
        // What the current mode means, plus a warning when it cannot mean anything. Checked in the
        // listener: the tree is built on both sides, but a hover only ever happens on a client.
        tpaMode.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            TpaMode mode = shownMode[0];
            List<Component> lines = new java.util.ArrayList<>();
            lines.add(mode.description().copy().withStyle(ChatFormatting.GRAY));
            if (mode.needsTeams() && !ModList.get().isLoaded("ftbteams")) {
                lines.add(Component.translatable("gui.roll_mod.hub.tpa.settings.needsTeams")
                        .withStyle(ChatFormatting.RED));
            }
            e.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY);
        });
        body.addScrollViewChild(option("gui.roll_mod.hub.settings.tpa", "gui.roll_mod.hub.tpa.settings.tip", tpaMode));
    }

    /* ------------------------------------------------- options ------------------------------------------------ */

    /** One option as a tile: its name and control on top, what it does under them in gray. */
    private static UIElement option(String titleKey, String hintKey, UIElement control) {
        return option(Component.translatable(titleKey), Component.translatable(hintKey), control);
    }

    private static UIElement option(Component title, @org.jetbrains.annotations.Nullable Component hint, UIElement control) {
        UIElement tile = new UIElement();
        tile.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(INSET).gapRow(2));
        tile.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        UIElement top = new UIElement();
        top.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).alignItems(AlignItems.CENTER)
                .gapColumn(4));
        Label name = new Label();
        name.setText(title);
        name.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        top.addChildren(name, control);
        tile.addChild(top);

        if (hint != null) {
            Label text = new Label();
            text.setText(hint.copy().withStyle(ChatFormatting.GRAY));
            text.layout(l -> l.widthPercent(100));
            text.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            tile.addChild(text);
        }
        return tile;
    }

    private static Switch toggle(boolean on, Consumer<Boolean> changed) {
        Switch toggle = new Switch();
        toggle.setOn(on);
        toggle.layout(l -> l.width(24).height(14).flexShrink(0));
        toggle.setOnSwitchChanged(changed::accept);
        return toggle;
    }

    private static <T> Selector<T> selector(List<T> values, java.util.function.Function<T, Component> title, T selected,
                                            int dialogZ, Consumer<T> changed) {
        Selector<T> selector = new Selector<>();
        // Null-safe: setCandidateUIProvider re-renders the current value, and there is none yet.
        selector.setCandidateUIProvider(UIElementProvider.text(v -> v == null ? Component.empty() : title.apply(v)));
        selector.setCandidates(values);
        selector.setSelected(selected, false);
        selector.layout(l -> l.width(CONTROL_W).height(ROW_H).flexShrink(0));
        // The list hangs off the UI root, under the page's tiles unless lifted above them.
        selector.dialog.style(s -> s.zIndex(dialogZ));
        selector.setOnValueChanged(picked -> {
            if (picked != null) changed.accept(picked);
        });
        return selector;
    }

    /** One chat button: shown or not, its name (and template), in All or not, moved up or down, and removed if custom. */
    private static UIElement channelRow(ChatChannelState state, ChatButton button, boolean first, boolean last,
                                        Runnable rebuild) {
        UIElement row = new UIElement();
        // A tile with its contents kept off its edges.
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H + 4)
                .alignItems(AlignItems.CENTER).gapColumn(4).paddingHorizontal(INSET).paddingVertical(2));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        Toggle shown = new Toggle();
        shown.noText();
        shown.setOn(!state.isHidden(button.id()), false);
        shown.layout(l -> l.width(SMALL).height(SMALL).flexShrink(0));
        shown.setOnToggleChanged(on -> {
            state.setHidden(button.id(), !on);
            // The last visible button refuses to hide; put the tick back if so.
            if (shown.isOn() == state.isHidden(button.id())) shown.setOn(!state.isHidden(button.id()), false);
        });
        hintOn(shown, "gui.roll_mod.hub.settings.chat.shown.tip");

        Label title = new Label();
        Component text = button.custom()
                ? button.title().copy().append(Component.literal("  " + button.template()).withStyle(ChatFormatting.GRAY))
                : button.title();
        title.setText(text);
        title.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        // Whether All shows this channel's lines; only built-in channels have any (not All itself, not custom).
        UIElement inAll;
        if (button.custom() || ChatButton.ALL.equals(button.id())) {
            inAll = new UIElement();
        } else {
            Toggle toggle = new Toggle();
            toggle.setText(Component.translatable("gui.roll_mod.hub.settings.chat.in_all"));
            toggle.setOn(state.isInAll(button.id()), false);
            toggle.setOnToggleChanged(on -> {
                state.setInAll(button.id(), on);
                ClientChatStore.refresh();
            });
            hintOn(toggle, "gui.roll_mod.hub.settings.chat.in_all.tip");
            inAll = toggle;
        }
        inAll.layout(l -> l.width(IN_ALL_W).height(SMALL).flexShrink(0));

        row.addChildren(shown, title, inAll,
                small("▲", !first, () -> { state.move(button.id(), -1); rebuild.run(); }),
                small("▼", !last, () -> { state.move(button.id(), 1); rebuild.run(); }));
        if (button.custom()) {
            row.addChild(small("✖", true, () -> { state.removeCustom(button.id()); rebuild.run(); }));
        }
        return row;
    }

    private static Button small(String glyph, boolean active, Runnable action) {
        Button button = new Button();
        button.setText(Component.literal(glyph));
        button.layout(l -> l.width(SMALL).height(SMALL).paddingAll(0).flexShrink(0));
        button.setActive(active);
        button.setOnClick(e -> action.run());
        return button;
    }

    private static UIElement row() {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .alignItems(AlignItems.CENTER).gapColumn(4).paddingHorizontal(2));
        return row;
    }

    private static Label heading(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.BOLD));
        label.layout(l -> l.widthPercent(100).height(12).marginTop(4).paddingLeft(2));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return label;
    }

    private static void hintOn(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }
}
