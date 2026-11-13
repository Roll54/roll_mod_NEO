package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.network.packet.ServerSettingPacket;
import com.roll_54.roll_mod.network.packet.ServerSettingPacket.Key;
import com.roll_54.roll_mod.registry.ModConfigs;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The moderation tab's Settings mode: the server's switches for what was ported from Eden_LD — the chat history a
 * joining player gets, "@Name" mentions — and the chat's other server knobs (advancement lines, the Discord marker,
 * Wixie's interval). Laid out like the hub's own Settings: a tile per option with what it does. Each change is a
 * {@link ServerSettingPacket}, which the server checks and saves; the client's copy of the config is updated at
 * once so the page shows the new value.
 */
final class ModerationSettings {

    private static final int ROW_H = 16;
    private static final int CONTROL_W = 60;
    private static final int COLOR_ROW = 0x30FFFFFF;

    private ModerationSettings() {}

    static UIElement build(boolean client) {
        ScrollerView body = FriendsTab.scroller();
        body.layout(l -> l.heightPercent(100));
        MyConfig config = ModConfigs.MAIN;

        body.addScrollViewChild(FriendsTab.note("gui.roll_mod.hub.moderation.settings.section.chat"));
        body.addScrollViewChild(number(Key.JOIN_HISTORY_MINUTES,
                config == null ? 30 : config.chat_changes.joinHistoryMinutes.get(), client));
        body.addScrollViewChild(number(Key.JOIN_HISTORY_LINES,
                config == null ? 100 : config.chat_changes.joinHistoryLines.get(), client));
        body.addScrollViewChild(toggle(Key.MENTIONS, config == null || config.chat_changes.mentions, client));
        body.addScrollViewChild(toggle(Key.HIDE_ADVANCEMENTS,
                config != null && config.chat_changes.hideAdvancementMessages, client));
        body.addScrollViewChild(text(Key.DISCORD_MARKER,
                config == null ? "[Discord]" : config.chat_changes.discordLineMarker, client));
        body.addScrollViewChild(number(Key.WIXIE_MINUTES,
                config == null ? 10 : Math.max(1, config.messages.TIME_TO_BROADCAST / 1200), client));
        return body;
    }

    private static UIElement toggle(Key key, boolean on, boolean client) {
        Switch control = new Switch();
        control.setOn(on);
        control.layout(l -> l.width(28).height(14).flexShrink(0));
        control.setOnSwitchChanged(value -> {
            if (client) send(key, String.valueOf(value));
        });
        return tile(key, control);
    }

    private static UIElement number(Key key, int value, boolean client) {
        return text(key, String.valueOf(value), client);
    }

    /** A text field and Save: numbers and the Discord marker. */
    private static UIElement text(Key key, String value, boolean client) {
        UIElement controls = new UIElement();
        controls.layout(l -> l.flexDirection(FlexDirection.ROW).height(ROW_H).gapColumn(3).flexShrink(0));
        TextField field = new TextField();
        field.setText(value == null ? "" : value);
        field.layout(l -> l.width(CONTROL_W).height(ROW_H));
        Button save = new Button();
        save.setText(Component.translatable("gui.roll_mod.hub.moderation.save"));
        save.layout(l -> l.width(34).height(ROW_H));
        save.setOnClick(e -> {
            if (client) send(key, field.getText());
        });
        controls.addChildren(field, save);
        return tile(key, controls);
    }

    private static void send(Key key, String value) {
        PacketDistributor.sendToServer(new ServerSettingPacket(key.name(), value));
        // The client's copy too, so a rebuilt page shows it (in singleplayer this is the server's own).
        if (ModConfigs.MAIN != null) {
            try {
                ServerSettingPacket.apply(ModConfigs.MAIN, key, value);
            } catch (IllegalArgumentException ignored) {
                // Not a number: the server ignores it as well.
            }
        }
    }

    private static UIElement tile(Key key, UIElement control) {
        UIElement tile = new UIElement();
        tile.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(4).gapRow(2).marginBottom(3));
        tile.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        UIElement top = new UIElement();
        top.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).alignItems(AlignItems.CENTER)
                .gapColumn(4));
        Label name = new Label();
        name.setText(Component.translatable("gui.roll_mod.hub.moderation.settings." + key.id()));
        name.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        top.addChildren(name, control);
        Label hint = new Label();
        hint.setText(Component.translatable("gui.roll_mod.hub.moderation.settings." + key.id() + ".tip")
                .withStyle(ChatFormatting.GRAY));
        hint.layout(l -> l.widthPercent(100));
        hint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        tile.addChildren(top, hint);
        return tile;
    }
}
