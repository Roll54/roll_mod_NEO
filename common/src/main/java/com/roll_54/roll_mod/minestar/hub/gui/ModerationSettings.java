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
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.minestar.leaderboard.LeaderboardChapters;
import com.roll_54.roll_mod.network.packet.ServerSettingPacket;
import com.roll_54.roll_mod.network.packet.ServerSettingPacket.Key;
import com.roll_54.roll_mod.registry.ModConfigs;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

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

    static UIElement build(boolean client, QuestPicker chapterPicker) {
        ScrollerView body = FriendsTab.scroller();
        body.layout(l -> l.heightPercent(100));
        MyConfig config = ModConfigs.MAIN;

        body.addScrollViewChild(section("gui.roll_mod.hub.moderation.settings.section.chat"));
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

        body.addScrollViewChild(section("gui.roll_mod.hub.moderation.settings.section.leaderboard"));
        body.addScrollViewChild(chapters(config, client, chapterPicker));
        return body;
    }

    /** A section's caption, 3px clear of its band's left edge like the tiles' text. */
    private static com.lowdragmc.lowdraglib2.gui.ui.elements.Label section(String key) {
        var label = FriendsTab.note(key);
        label.layout(l -> l.paddingLeft(3).paddingRight(3));
        return label;
    }

    /**
     * The quest stage board's stages: a row per stage, highest first — the quest it is tied to, the name the board
     * shows it by, ▲▼ to reorder and ✕ to drop it — and "Add quest", which opens the book's quests to pick from. Order
     * changes, additions and removals are sent at once; typed names with Save.
     */
    private static UIElement chapters(MyConfig config, boolean client, QuestPicker picker) {
        List<LeaderboardChapters.Stage> stages = new ArrayList<>(
                LeaderboardChapters.parse(config == null ? "" : config.hub.leaderboardChapters));
        List<TextField> names = new ArrayList<>();

        UIElement tile = new UIElement();
        tile.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(4).gapRow(2).marginBottom(3));
        tile.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.moderation.settings.leaderboard_chapters"));
        title.layout(l -> l.widthPercent(100).height(ROW_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        Label hint = new Label();
        hint.setText(Component.translatable("gui.roll_mod.hub.moderation.settings.leaderboard_chapters.tip")
                .withStyle(ChatFormatting.GRAY));
        hint.layout(l -> l.widthPercent(100));
        hint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        UIElement rows = new UIElement();
        rows.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(2));

        Runnable[] rebuild = {null};
        // Takes the typed names into the list, so a name moves with its stage. Only before the list changes: after a
        // swap the fields still sit in the old order, and reading them again put each name back on the old place.
        Runnable takeNames = () -> {
            for (int i = 0; i < stages.size() && i < names.size(); i++) {
                stages.set(i, new LeaderboardChapters.Stage(stages.get(i).chapter(),
                        LeaderboardChapters.clean(names.get(i).getText())));
            }
        };
        Runnable sendList = () -> {
            if (client) send(Key.LEADERBOARD_CHAPTERS, LeaderboardChapters.write(stages));
        };
        Runnable sendAll = () -> {
            takeNames.run();
            sendList.run();
        };
        rebuild[0] = () -> {
            rows.clearAllChildren();
            names.clear();
            if (stages.isEmpty()) {
                Label none = new Label();
                none.setText(Component.translatable("gui.roll_mod.hub.moderation.settings.leaderboard_chapters.none")
                        .withStyle(ChatFormatting.GRAY));
                none.layout(l -> l.widthPercent(100).height(ROW_H));
                none.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
                rows.addChild(none);
            }
            for (int i = 0; i < stages.size(); i++) {
                int at = i;
                LeaderboardChapters.Stage stage = stages.get(i);
                UIElement row = new UIElement();
                row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                        .alignItems(AlignItems.CENTER).gapColumn(3));
                Label place = new Label();
                place.setText(Component.literal((at + 1) + ".").withStyle(ChatFormatting.GRAY));
                place.layout(l -> l.width(14).heightPercent(100).flexShrink(0));
                place.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
                Label chapter = new Label();
                // The quest it is tied to; a stage saved when stages were chapters shows the chapter.
                chapter.setText(QuestGate.title(true, stage.chapter()).getString().equals(QuestGate.code(stage.chapter()))
                        ? QuestGate.chapterTitle(true, stage.chapter()) : QuestGate.title(true, stage.chapter()));
                chapter.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                chapter.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
                TextField name = new TextField();
                name.setText(stage.label());
                name.layout(l -> l.width(CONTROL_W).height(ROW_H).flexShrink(0));
                names.add(name);
                row.addChildren(place, chapter, name,
                        small("▲", at > 0, () -> {
                            takeNames.run();
                            java.util.Collections.swap(stages, at, at - 1);
                            sendList.run();
                            rebuild[0].run();
                        }),
                        small("▼", at < stages.size() - 1, () -> {
                            takeNames.run();
                            java.util.Collections.swap(stages, at, at + 1);
                            sendList.run();
                            rebuild[0].run();
                        }),
                        small("✕", true, () -> {
                            takeNames.run();
                            stages.remove(at);
                            sendList.run();
                            rebuild[0].run();
                        }));
                rows.addChild(row);
            }
        };
        rebuild[0].run();

        UIElement actions = new UIElement();
        actions.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(3));
        Button add = new Button();
        add.setText(Component.translatable("gui.roll_mod.hub.moderation.settings.leaderboard_chapters.add"));
        add.layout(l -> l.flexGrow(1).height(ROW_H).paddingAll(0));
        HubSection.centreCaption(add);
        Button save = new Button();
        save.setText(Component.translatable("gui.roll_mod.hub.moderation.save"));
        // Wide enough for "Зберегти" with 3px clear of the frame on each side.
        save.layout(l -> l.width(62).height(ROW_H).paddingAll(0).flexShrink(0));
        HubSection.centreCaption(save);
        save.setOnClick(e -> sendAll.run());
        // New stages go to the bottom: the lowest. ▲ lifts them.
        picker.setOnPick(id -> {
            if (stages.size() >= LeaderboardChapters.MAX_STAGES) return;
            if (stages.stream().anyMatch(s -> s.chapter() == id)) return;
            takeNames.run();
            stages.add(new LeaderboardChapters.Stage(id, ""));
            sendList.run();
            rebuild[0].run();
        });
        add.setOnClick(e -> picker.open());
        actions.addChildren(add, save);

        tile.addChildren(title, hint, rows, actions);
        return tile;
    }

    private static Button small(String glyph, boolean active, Runnable action) {
        // ▲▼ as the theme's drop-down chevron, like the selectors'.
        if ("▲".equals(glyph) || "▼".equals(glyph)) return HubIcons.arrow("▲".equals(glyph), active, ROW_H, action);
        Button button = new Button();
        button.setText(Component.literal(glyph));
        button.layout(l -> l.width(ROW_H).height(ROW_H).paddingAll(0).flexShrink(0));
        HubSection.centreCaption(button);
        button.setActive(active);
        button.setOnClick(e -> action.run());
        return button;
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
        // Wide enough for 3px either side of "Save", the caption 1px low to sit in the middle of the bevelled face.
        save.layout(l -> l.width(62).height(ROW_H).paddingAll(0).flexShrink(0));
        HubSection.centreCaption(save);
        save.text.layout(l -> l.marginTop(1));
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
        // Unclipped: on AE2 and Ore a button's face reaches a pixel past its box (CLAUDE.md, UI rules).
        top.setOverflowVisible(true);
        control.setOverflowVisible(true);
        Label hint = new Label();
        hint.setText(Component.translatable("gui.roll_mod.hub.moderation.settings." + key.id() + ".tip")
                .withStyle(ChatFormatting.GRAY));
        // 3px under the controls' boxes (the row gap is 2).
        hint.layout(l -> l.widthPercent(100).marginTop(1));
        hint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        tile.addChildren(top, hint);
        return tile;
    }
}
