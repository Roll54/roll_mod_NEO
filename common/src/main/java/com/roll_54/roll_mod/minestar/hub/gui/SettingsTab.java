package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatButton;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.minestar.tpa.TpaMode;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod.network.packet.TpaModePacket;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * What the hub's settings popup (the gear among the window controls, {@link HubSettings}) holds:
 *
 * <ul>
 *   <li><b>Appearance</b> — the theme every LdLib screen of the mod is drawn in ({@link RollUi}), and
 *       the experimental switch that also forces it on other mods' LdLib screens.
 *   <li><b>Chat buttons</b> — the buttons over the chat box: which show, in what order, and the
 *       player's own ones, sending through a template such as {@code /msg Steve {msg}}; see
 *       {@link ChatButton}.
 * </ul>
 *
 * <p>Was a hub tab of its own; the gear popup is now its only home. Everything here is client
 * preference ({@link UiThemeState}, {@link ChatChannelState}), read and written on the client copy
 * of the tree only. Nothing here syncs.
 */
final class SettingsTab {

    private static final int ROW_H = 16;
    private static final int LABEL_W = 70;
    private static final int SMALL = 14;
    /** Space between a row's tile and the text and controls inside it. */
    private static final int INSET = 4;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int WARNING = 12;

    private SettingsTab() {}

    /**
     * The settings, as a scroller that fills whatever it is given.
     *
     * @param dialogZ the z-index drop-downs must open at to sit above the popup holding this
     */
    static ScrollerView content(Player player, int dialogZ) {
        boolean client = player.level().isClientSide();

        ScrollerView body = new ScrollerView();
        body.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        body.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        body.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(4)));
        // Inset from the popup's edge and clear of the scroll bar.
        body.viewPort(v -> v.layout(l -> l.paddingAll(INSET).paddingRight(INSET + 6)));

        /* ----------------------------------------------- appearance ---------------------------------------------- */

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.appearance"));
        UIElement themeRow = row();
        Selector<UiTheme> theme = new Selector<>();
        // Null-safe: setCandidateUIProvider re-renders the current value, and there is none yet.
        theme.setCandidateUIProvider(UIElementProvider.text(t -> t == null ? Component.empty() : t.title()));
        theme.setCandidates(List.of(UiTheme.values()));
        theme.setSelected(client ? UiThemeState.get() : UiTheme.DEFAULT, false);
        theme.layout(l -> l.flexGrow(1).height(ROW_H));
        // The list hangs off the UI root, under the popup unless lifted above it.
        theme.dialog.style(s -> s.zIndex(dialogZ));
        theme.setOnValueChanged(picked -> {
            if (picked != null && client) RollUi.set(picked, theme.getModularUI());
        });
        themeRow.addChildren(caption("gui.roll_mod.hub.settings.theme"), theme);
        body.addScrollViewChild(themeRow);

        // Experimental: the theme on other mods' LdLib screens too, flagged with a warning mark.
        UIElement forceRow = row();
        Label forceLabel = new Label();
        forceLabel.setText(Component.translatable("gui.roll_mod.hub.settings.force"));
        forceLabel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        forceLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        hintOn(forceLabel, "gui.roll_mod.hub.settings.force.tip");
        UIElement warning = new UIElement();
        warning.layout(l -> l.width(WARNING).height(WARNING).flexShrink(0));
        warning.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/gui/hub/exclamation_mark.png"))));
        warning.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                Component.translatable("gui.roll_mod.hub.settings.experimental").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.roll_mod.hub.settings.force.warning").withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));
        Switch force = new Switch();
        force.setOn(client && UiThemeState.forceEverywhere());
        force.layout(l -> l.width(28).height(14).flexShrink(0));
        force.setOnSwitchChanged(on -> {
            if (client) UiThemeState.setForceEverywhere(on);
        });
        forceRow.addChildren(forceLabel, warning, force);
        body.addScrollViewChild(forceRow);

        /* --------------------------------------------------- hub ------------------------------------------------- */

        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.hub"));
        UIElement sideRow = row();
        Selector<HubTabSide> side = new Selector<>();
        side.setCandidateUIProvider(UIElementProvider.text(t -> t == null ? Component.empty() : t.title()));
        side.setCandidates(List.of(HubTabSide.values()));
        side.setSelected(client ? HubWindowState.get().tabSide() : HubTabSide.TOP, false);
        side.layout(l -> l.flexGrow(1).height(ROW_H));
        side.dialog.style(s -> s.zIndex(dialogZ));
        side.setOnValueChanged(picked -> {
            if (picked == null || !client) return;
            HubWindowState state = HubWindowState.get();
            if (state.tabSide() == picked) return;
            state.setTabSide(picked);
            state.save();
            // The bookmarks are placed when the hub is built, so it is reopened on the same tab.
            PacketDistributor.sendToServer(new OpenHubPacket(HubUI.liveTab()));
        });
        sideRow.addChildren(caption("gui.roll_mod.hub.settings.tabs"), side);
        hintOn(sideRow, "gui.roll_mod.hub.settings.tabs.tip");
        body.addScrollViewChild(sideRow);

        UIElement movableRow = row();
        Label movableLabel = new Label();
        movableLabel.setText(Component.translatable("gui.roll_mod.hub.settings.movable"));
        movableLabel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        movableLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        hintOn(movableLabel, "gui.roll_mod.hub.settings.movable.tip");
        Switch movable = new Switch();
        movable.setOn(!client || HubWindowState.get().movable());
        movable.layout(l -> l.width(28).height(14).flexShrink(0));
        movable.setOnSwitchChanged(on -> {
            if (!client) return;
            HubWindowState state = HubWindowState.get();
            state.setMovable(on);
            state.save();
        });
        movableRow.addChildren(movableLabel, movable);
        body.addScrollViewChild(movableRow);

        // Whether the info window and the hub's popups close when the hub does. On by default: an
        // always-on-top window left over the game with nothing behind it is the surprising case.
        UIElement windowsRow = row();
        Label windowsLabel = new Label();
        windowsLabel.setText(Component.translatable("gui.roll_mod.hub.settings.closeWindows"));
        windowsLabel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        windowsLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        hintOn(windowsLabel, "gui.roll_mod.hub.settings.closeWindows.tip");
        Switch closeWindows = new Switch();
        closeWindows.setOn(!client || HubWindowState.get().closeWindowsWithHub());
        closeWindows.layout(l -> l.width(28).height(14).flexShrink(0));
        closeWindows.setOnSwitchChanged(on -> {
            if (!client) return;
            HubWindowState state = HubWindowState.get();
            state.setCloseWindowsWithHub(on);
            state.save();
        });
        windowsRow.addChildren(windowsLabel, closeWindows);
        body.addScrollViewChild(windowsRow);

        /* ------------------------------------------------ teleports ---------------------------------------------- */

        // Who may send this player teleport requests. Unlike the rest of this popup it is the server's
        // to keep: a pick only sends the packet, and the shown value follows ClientTpaCache, which the
        // hub's TPA sync fills on open and again after every change — so a refused change never sticks.
        body.addScrollViewChild(heading("gui.roll_mod.hub.settings.section.teleports"));
        UIElement tpaRow = row();
        Selector<TpaMode> tpaMode = new Selector<>();
        tpaMode.setCandidateUIProvider(UIElementProvider.text(m -> m == null ? Component.empty() : m.title()));
        tpaMode.setCandidates(List.of(TpaMode.values()));
        tpaMode.setSelected(client ? ClientTpaCache.MODE : TpaMode.DEFAULT, false);
        tpaMode.layout(l -> l.flexGrow(1).height(ROW_H));
        tpaMode.dialog.style(s -> s.zIndex(dialogZ));
        tpaMode.setOnValueChanged(picked -> {
            if (picked != null && client) PacketDistributor.sendToServer(TpaModePacket.of(picked));
        });
        // Ticks only while the popup is shown, which is the only time the value has to be right.
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
            lines.add(Component.translatable("gui.roll_mod.hub.tpa.settings.tip"));
            lines.add(mode.description().copy().withStyle(ChatFormatting.GRAY));
            if (mode.needsTeams() && !ModList.get().isLoaded("ftbteams")) {
                lines.add(Component.translatable("gui.roll_mod.hub.tpa.settings.needsTeams")
                        .withStyle(ChatFormatting.RED));
            }
            e.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY);
        });
        tpaRow.addChildren(caption("gui.roll_mod.hub.settings.tpa"), tpaMode);
        body.addScrollViewChild(tpaRow);

        /* -------------------------------------------------- chat ------------------------------------------------- */

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
        return body;
    }

    /** One chat button: shown or not, its name (and template), moved up or down, and removed if custom. */
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

        row.addChildren(shown, title,
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
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));
        return label;
    }

    private static Label caption(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key));
        label.layout(l -> l.width(LABEL_W).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        return label;
    }

    private static void hintOn(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }
}
