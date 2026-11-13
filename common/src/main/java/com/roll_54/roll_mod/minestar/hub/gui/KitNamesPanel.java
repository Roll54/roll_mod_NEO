package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.kits.ClientKitNamesCache;
import com.roll_54.roll_mod.minestar.kits.Kit;
import com.roll_54.roll_mod.network.packet.KitNamePackets;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The moderation tab's "Kits" mode: every kit by its id on the left; on the right the name players see the
 * selected one as, its items (edited as a chest, {@code KitEditorMenu}), its cooldown, and delete; under
 * that, a new kit.
 *
 * <p>A kit's id is what its file, its permission node and {@code /rollmod kit} use, so it stays a
 * plain lowercase word; the display name is free text with {@code &} colours, kept in the kit's file
 * ({@code displayName}). Blank shows the id again.
 *
 * <p>Fed by {@link ClientKitNamesCache}, which only those who may rename kits are sent. The mode is
 * offered in the drop-down only to them; the server checks again on every rename.
 */
final class KitNamesPanel {

    private static final int ROW_H = 18;
    private static final int SAVE_W = 40;
    private static final int CREATE_W = 60;
    /** The captions in front of fields ("Cooldown:", "Id:"), so the fields line up. */
    private static final int LABEL_W = 64;
    /** One-click cooldowns, in the format the field takes. */
    private static final String[] PRESETS = {"0", "30m", "1h", "1d", "7d"};
    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    /** The kit being edited, by id; picked from the moderation tab's left list. */
    final String[] selected = {null};

    private final Label id = new Label();
    private final TextField name = new TextField();
    private final Label preview = new Label();
    private final UIElement editor = new UIElement();
    private final Label empty = new Label();
    private final Label info = new Label();
    private final TextField cooldown = new TextField();
    /** The kit's description, shown on its card (as on Eden, plus descriptions). */
    private final TextField description = new TextField();
    private final Button delete = new Button();
    private final TextField newName = new TextField();
    private final TextField newCooldown = new TextField();
    private final Label status = new Label();
    /** The kit the delete button has asked about once; the next click on it deletes. */
    @Nullable
    private String deleteArmed;

    private String shownKit;
    private int shownVersion = -1;
    /** The name the preview button was last captioned with. */
    private String shownPreview;

    /** Fills {@code pane}, one of the moderation tab's stacked panes. */
    KitNamesPanel(UIElement pane, HubConfirm confirmer, java.util.function.Consumer<String> editItems) {
        Label hint = new Label();
        hint.setText(Component.translatable("gui.roll_mod.hub.moderation.kitNames.hint").withStyle(ChatFormatting.GRAY));
        hint.layout(l -> l.widthPercent(100).flexShrink(0));
        hint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        UIElement box = new UIElement();
        box.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1)
                .paddingAll(4).gapRow(4));
        box.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        empty.setText(Component.translatable("gui.roll_mod.hub.moderation.kitNames.pick").withStyle(ChatFormatting.GRAY));
        empty.layout(l -> l.widthPercent(100));
        empty.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        editor.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(4));

        id.layout(l -> l.widthPercent(100).height(ROW_H));
        id.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(4)
                .alignItems(AlignItems.CENTER));
        name.setText("");
        name.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H - 2));
        Button save = new Button();
        save.setText(Component.translatable("gui.roll_mod.hub.moderation.save"));
        save.layout(l -> l.width(SAVE_W).height(ROW_H - 2).flexShrink(0).paddingAll(0));
        HubSection.centreCaption(save);
        save.setOnClick(e -> {
            if (selected[0] == null) return;
            String text = name.getText();
            if (text.length() > Kit.MAX_DISPLAY) text = text.substring(0, Kit.MAX_DISPLAY);
            PacketDistributor.sendToServer(new KitNamePackets.Rename(selected[0], text));
        });
        row.addChildren(name, save);

        // What players will see, live as it is typed.
        preview.layout(l -> l.widthPercent(100));
        preview.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        // How many stacks and how long the wait, as the server has them now.
        info.layout(l -> l.widthPercent(100).height(ROW_H).flexShrink(0));
        info.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        // The items, as a chest: the server opens it; closing it saves and comes back here.
        UIElement itemsRow = row();
        itemsRow.addChild(button("gui.roll_mod.hub.moderation.kits.editItems", -1, () -> {
            if (selected[0] == null) return;
            PacketDistributor.sendToServer(new KitNamePackets.Edit(KitNamePackets.Edit.Action.ITEMS, selected[0], 0L));
            editItems.accept(selected[0]);
        }));

        UIElement cooldownRow = row();
        cooldown.setText("");
        cooldown.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H - 2));
        cooldownRow.addChildren(label(Component.translatable("gui.roll_mod.hub.moderation.kits.cooldown"), LABEL_W),
                cooldown, button("gui.roll_mod.hub.moderation.save", SAVE_W, () -> saveCooldown(cooldown.getText())));

        // One click for the usual waits: sets the field and saves.
        UIElement presetRow = row();
        for (String preset : PRESETS) {
            presetRow.addChild(literalButton(preset, () -> {
                cooldown.setText(preset);
                saveCooldown(preset);
            }));
        }

        // Asks first, in the hub's confirm window.
        delete.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.delete"));
        delete.layout(l -> l.flexGrow(1).height(ROW_H - 2).paddingAll(0));
        HubSection.centreCaption(delete);
        UIElement deleteRow = row();
        deleteRow.addChild(delete);
        delete.setOnClick(e -> {
            String kit = selected[0];
            if (kit == null) return;
            confirmer.ask(Component.translatable("gui.roll_mod.hub.moderation.kits.deleteAsk", kit),
                    Component.translatable("gui.roll_mod.hub.moderation.kits.delete"), () -> {
                        PacketDistributor.sendToServer(new KitNamePackets.Edit(KitNamePackets.Edit.Action.DELETE, kit, 0L));
                        if (kit.equals(selected[0])) selected[0] = null;
                    });
        });

        // The description players read on the kit's card; & colour codes work.
        UIElement descriptionRow = row();
        description.setText("");
        description.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H - 2));
        descriptionRow.addChildren(label(Component.translatable("gui.roll_mod.hub.moderation.kits.description"), LABEL_W),
                description, button("gui.roll_mod.hub.moderation.save", SAVE_W, () -> {
                    if (selected[0] == null) return;
                    String text = description.getText();
                    if (text.length() > Kit.MAX_DESCRIPTION) text = text.substring(0, Kit.MAX_DESCRIPTION);
                    PacketDistributor.sendToServer(new KitNamePackets.Describe(selected[0], text));
                }));

        // A one-time kit, as on Eden: claimed once and never again.
        presetRow.addChild(button("gui.roll_mod.hub.moderation.kits.once", -1, () -> {
            if (selected[0] == null) return;
            cooldown.setText("");
            PacketDistributor.sendToServer(new KitNamePackets.Edit(KitNamePackets.Edit.Action.COOLDOWN, selected[0], Kit.ONCE));
        }));

        editor.addChildren(id, row, preview, info, descriptionRow, itemsRow, cooldownRow, presetRow, deleteRow);

        // A new kit: an id, a cooldown, then its (empty) chest to fill.
        UIElement create = new UIElement();
        create.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(4).marginTop(6).flexShrink(0));
        Label createTitle = label(Component.translatable("gui.roll_mod.hub.moderation.kits.new"), -1);
        UIElement nameRow = row();
        newName.setText("");
        newName.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H - 2));
        nameRow.addChildren(label(Component.translatable("gui.roll_mod.hub.moderation.kits.id"), LABEL_W), newName);
        UIElement createRow = row();
        newCooldown.setText("0");
        newCooldown.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H - 2));
        Button createButton = button("gui.roll_mod.hub.moderation.kits.create", CREATE_W, () -> {
            String kitName = Kit.clampName(newName.getText());
            long ticks = ticks(newCooldown.getText());
            if (!Kit.validName(kitName)) {
                status.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.badName").withStyle(ChatFormatting.RED));
                return;
            }
            if (ticks < 0) {
                status.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.badCooldown").withStyle(ChatFormatting.RED));
                return;
            }
            status.setText(Component.empty());
            PacketDistributor.sendToServer(new KitNamePackets.Edit(KitNamePackets.Edit.Action.CREATE, kitName, ticks));
            editItems.accept(kitName);
            newName.setText("");
            selected[0] = kitName;
        });
        createRow.addChildren(label(Component.translatable("gui.roll_mod.hub.moderation.kits.cooldown"), LABEL_W),
                newCooldown, createButton);
        Label createHint = new Label();
        createHint.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.newHint").withStyle(ChatFormatting.GRAY));
        createHint.layout(l -> l.widthPercent(100).flexShrink(0));
        createHint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        status.layout(l -> l.widthPercent(100).flexShrink(0));
        status.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        create.addChildren(createTitle, nameRow, createRow, createHint, status);

        box.addChildren(empty, editor, create);
        pane.addChildren(hint, box);
    }

    /**
     * A cooldown as typed — {@code 30m}, {@code 1d12h}, a bare number as minutes, {@code 0} for none
     * ({@link Durations#parse}) — in ticks; {@code -1} when it does not parse.
     */
    private static long ticks(String text) {
        long millis = Durations.parse(text);
        return millis < 0 ? -1L : millis / 50L;
    }

    /** Sends a typed cooldown, or says what is wrong with it. */
    private void saveCooldown(String text) {
        if (selected[0] == null) return;
        long ticks = ticks(text);
        if (ticks < 0) {
            status.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.badCooldown").withStyle(ChatFormatting.RED));
            return;
        }
        status.setText(Component.empty());
        PacketDistributor.sendToServer(new KitNamePackets.Edit(KitNamePackets.Edit.Action.COOLDOWN, selected[0], ticks));
    }

    /** The kit whose items chest was just opened from here: the tab comes back to it ({@link #takeReturnTo}). */
    @Nullable
    private static String returnTo;

    /** For {@code ModerationTab}, as it is built again after the chest closed: the kit to show, once. */
    @Nullable
    static String takeReturnTo() {
        String kit = returnTo;
        returnTo = null;
        return kit;
    }

    /** A row of one height; children keep their own widths unless they grow. */
    private static UIElement row() {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).flexShrink(0)
                .gapColumn(4).alignItems(AlignItems.CENTER));
        return row;
    }

    /** A one-line label; {@code width} -1 takes the whole row, anything else is a fixed caption width. */
    private static Label label(Component text, int width) {
        Label label = new Label();
        label.setText(text);
        label.layout(l -> {
            if (width < 0) {
                l.widthPercent(100);
            } else {
                l.width(width);
            }
            l.height(ROW_H).flexShrink(0);
        });
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        return label;
    }

    /** A small button showing {@code caption} as it is (the cooldown presets). */
    private static Button literalButton(String caption, Runnable action) {
        Button button = new Button();
        button.setText(Component.literal(caption));
        button.layout(l -> l.flexGrow(1).height(ROW_H - 2).paddingAll(0));
        HubSection.centreCaption(button);
        button.setOnClick(e -> action.run());
        return button;
    }

    /** A button with a translated caption; {@code width} -1 stretches it. */
    private static Button button(String key, int width, Runnable action) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> {
            if (width < 0) {
                l.flexGrow(1).height(ROW_H - 2);
            } else {
                l.width(width).height(ROW_H - 2).flexShrink(0);
            }
            l.paddingAll(0);
        });
        HubSection.centreCaption(button);
        button.setOnClick(e -> action.run());
        return button;
    }

    /** Keeps the editor on the selected kit; the field follows the server unless it is being typed in. */
    void tick() {
        ClientKitNamesCache.Entry kit = find(selected[0]);
        boolean has = kit != null;
        if (editor.isDisplayed() != has) editor.setDisplay(has);
        if (empty.isDisplayed() == has) empty.setDisplay(!has);
        if (!has) {
            shownKit = null;
            return;
        }
        boolean switched = !kit.name().equals(shownKit);
        if (switched) {
            // The delete button forgets its question, and the last kit's errors go.
            deleteArmed = null;
            delete.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.delete"));
            status.setText(Component.empty());
        }
        if (switched || (shownVersion != ClientKitNamesCache.VERSION && !name.isFocused() && !cooldown.isFocused()
                && !description.isFocused())) {
            shownKit = kit.name();
            shownVersion = ClientKitNamesCache.VERSION;
            id.setText(Component.translatable("gui.roll_mod.hub.moderation.kitNames.id",
                    Component.literal(kit.name()).withStyle(ChatFormatting.GRAY)));
            name.setText(kit.displayName());
            cooldown.setText(kit.cooldownTicks() < 0 ? "" : Durations.formatInput(kit.cooldownTicks() * 50L));
            description.setText(kit.description());
            info.setText(Component.translatable("gui.roll_mod.hub.moderation.kits.info", kit.itemCount(),
                    kit.cooldownTicks() < 0 ? Component.translatable("gui.roll_mod.hub.moderation.kits.onceShort")
                            : Component.literal(kit.cooldownTicks() > 0 ? Durations.formatInput(kit.cooldownTicks() * 50L) : "—")));
        }
        // Only when the name changed: a setText every tick re-parsed the colour codes and re-measured the label,
        // which is what made this mode lag.
        String previewName = name.getText().isBlank() ? kit.name() : name.getText();
        if (!previewName.equals(shownPreview)) {
            shownPreview = previewName;
            preview.setText(Component.translatable("gui.roll_mod.hub.moderation.kitNames.preview")
                    .append(" ").append(LegacyText.display(previewName)));
        }
    }

    /** The left list in Kits mode: ids whose id or name matches {@code filter}. */
    static void rebuildList(ScrollerView list, String filter, String[] selected) {
        list.clearAllScrollViewChildren();
        List<ClientKitNamesCache.Entry> kits = matching(filter);
        if (kits.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.hub.kits.none").withStyle(ChatFormatting.GRAY));
            none.layout(l -> l.widthPercent(100).height(ROW_H));
            list.addScrollViewChild(none);
            return;
        }
        for (ClientKitNamesCache.Entry kit : kits) {
            boolean isSelected = kit.name().equals(selected[0]);
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).marginBottom(2)
                    .paddingHorizontal(HubSection.ROW_INSET).gapColumn(4).alignItems(AlignItems.CENTER));
            row.style(s -> s.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            row.addEventListener(UIEvents.CLICK, e -> selected[0] = kit.name());

            Label label = new Label();
            label.setText(Component.empty()
                    .append(LegacyText.display(kit.displayName().isBlank() ? kit.name() : kit.displayName()))
                    .append(Component.literal("  " + kit.name()).withStyle(ChatFormatting.GRAY)));
            label.layout(l -> l.flexGrow(1).heightPercent(100));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            row.addChild(label);
            list.addScrollViewChild(row);
        }
    }

    private static List<ClientKitNamesCache.Entry> matching(String filter) {
        if (filter.isEmpty()) return ClientKitNamesCache.KITS;
        List<ClientKitNamesCache.Entry> out = new ArrayList<>();
        for (ClientKitNamesCache.Entry kit : ClientKitNamesCache.KITS) {
            if (kit.name().toLowerCase().contains(filter)
                    || LegacyText.plain(kit.displayName()).toLowerCase().contains(filter)) {
                out.add(kit);
            }
        }
        return out;
    }

    @Nullable
    private static ClientKitNamesCache.Entry find(@Nullable String kit) {
        if (kit == null) return null;
        for (ClientKitNamesCache.Entry entry : ClientKitNamesCache.KITS) {
            if (entry.name().equals(kit)) return entry;
        }
        return null;
    }
}
