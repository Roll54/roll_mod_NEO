package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.radio.ClientRadioCache;
import com.roll_54.roll_mod.minestar.radio.ClientRadioCache.Station;
import com.roll_54.roll_mod.network.packet.RadioPackets;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The radio implant's tab: two switches — hear the station, let players nearby hear it — and the
 * stations to tune to. Its bookmark shows only while {@link ClientRadioCache#INSTALLED}.
 *
 * <p>Like the TPA setting in {@code SettingsTab}, everything here is the server's to keep: a flip or
 * a pick only sends a packet, and what is shown follows the cache, so a refused change never sticks.
 * No sync values; the tree is built on both sides and only the client's copy ever sends anything.
 */
final class RadioTab {

    private static final int PADDING = 6;
    private static final int ROW_H = 18;
    private static final int GAP = 3;
    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    private RadioTab() {}

    static UIElement build(Player player) {
        boolean client = player.level().isClientSide();

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING).gapRow(GAP));

        HubSection.Info section = HubSection.info("radio");

        Switch hear = new Switch();
        Switch broadcast = new Switch();
        hear.setOn(ClientRadioCache.HEAR, false);
        broadcast.setOn(ClientRadioCache.BROADCAST, false);
        hear.setOnSwitchChanged(on -> {
            if (!client) return;
            PacketDistributor.sendToServer(new RadioPackets.Tune(ClientRadioCache.STATION, on, ClientRadioCache.BROADCAST));
        });
        broadcast.setOnSwitchChanged(on -> {
            if (!client) return;
            PacketDistributor.sendToServer(new RadioPackets.Tune(ClientRadioCache.STATION, ClientRadioCache.HEAR, on));
        });

        Label nowPlaying = new Label();
        nowPlaying.layout(l -> l.widthPercent(100));
        nowPlaying.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        ScrollerView stations = new ScrollerView();
        stations.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        stations.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        stations.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(2)));
        stations.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        root.addChildren(HubSection.header("radio", section),
                toggle("gui.roll_mod.hub.radio.hear", hear),
                toggle("gui.roll_mod.hub.radio.broadcast", broadcast),
                nowPlaying, caption(), stations, section.overlay());

        int[] shown = {-1};
        root.addEventListener(UIEvents.TICK, e -> {
            if (!client || shown[0] == ClientRadioCache.VERSION) return;
            shown[0] = ClientRadioCache.VERSION;
            // The server's word, not the click: a refused change flips back here.
            hear.setOn(ClientRadioCache.HEAR, false);
            broadcast.setOn(ClientRadioCache.BROADCAST, false);
            nowPlaying.setText(nowPlaying());
            rebuild(stations);
        });

        return root;
    }

    private static Component nowPlaying() {
        Station current = find(ClientRadioCache.STATION);
        if (current == null) {
            return Component.translatable("gui.roll_mod.hub.radio.off").withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("gui.roll_mod.hub.radio.playing",
                Component.literal(current.title()).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY);
    }

    private static void rebuild(ScrollerView list) {
        list.clearAllScrollViewChildren();
        List<Station> all = ClientRadioCache.STATIONS;
        if (all.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.hub.radio.none").withStyle(ChatFormatting.GRAY));
            none.layout(l -> l.widthPercent(100));
            none.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            list.addScrollViewChild(none);
            return;
        }
        for (Station station : all) {
            boolean selected = station.url().equals(ClientRadioCache.STATION);
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).marginBottom(2)
                    .paddingHorizontal(HubSection.ROW_INSET).alignItems(AlignItems.CENTER));
            row.style(s -> s.background(new ColorRectTexture(selected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            // Picking the playing station again switches the radio off.
            row.addEventListener(UIEvents.CLICK, e -> PacketDistributor.sendToServer(new RadioPackets.Tune(
                    selected ? "" : station.url(), ClientRadioCache.HEAR, ClientRadioCache.BROADCAST)));

            MutableComponent text = Component.literal((selected ? "♪ " : "") + station.title())
                    .withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.WHITE);
            if (!station.name().isBlank() && !station.name().equals(station.title())) {
                text.append(Component.literal("  " + station.name()).withStyle(ChatFormatting.DARK_GRAY));
            }
            Label label = new Label();
            label.setText(text);
            label.layout(l -> l.flexGrow(1).height(ROW_H));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            label.setAllowHitTest(false);
            row.addChild(label);
            list.addScrollViewChild(row);
        }
    }

    private static UIElement toggle(String key, Switch control) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(4)
                .paddingHorizontal(4).alignItems(AlignItems.CENTER));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        Label label = new Label();
        label.setText(Component.translatable(key));
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        label.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key + ".tip")), null, null, ItemStack.EMPTY));
        control.layout(l -> l.width(28).height(14).flexShrink(0));
        row.addChildren(label, control);
        return row;
    }

    private static Label caption() {
        Label label = new Label();
        label.setText(Component.translatable("gui.roll_mod.hub.radio.stations").withStyle(ChatFormatting.YELLOW));
        label.layout(l -> l.widthPercent(100).height(12).marginTop(2));
        return label;
    }

    private static Station find(String url) {
        if (url == null || url.isEmpty()) return null;
        for (Station s : ClientRadioCache.STATIONS) {
            if (s.url().equals(url)) return s;
        }
        return null;
    }
}
