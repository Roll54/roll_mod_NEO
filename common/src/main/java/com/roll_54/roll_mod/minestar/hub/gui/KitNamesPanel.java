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
 * The moderation tab's "Kits" mode: every kit by its id on the left, and the name players see the
 * selected one as on the right.
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

    private String shownKit;
    private int shownVersion = -1;

    /** Fills {@code pane}, one of the moderation tab's stacked panes. */
    KitNamesPanel(UIElement pane) {
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

        editor.addChildren(id, row, preview);
        box.addChildren(empty, editor);
        pane.addChildren(hint, box);
    }

    /** Keeps the editor on the selected kit; the field follows the server unless it is being typed in. */
    void tick() {
        ClientKitNamesCache.Entry kit = find(selected[0]);
        boolean has = kit != null;
        editor.setDisplay(has);
        empty.setDisplay(!has);
        if (!has) {
            shownKit = null;
            return;
        }
        boolean switched = !kit.name().equals(shownKit);
        if (switched || (shownVersion != ClientKitNamesCache.VERSION && !name.isFocused())) {
            shownKit = kit.name();
            shownVersion = ClientKitNamesCache.VERSION;
            id.setText(Component.translatable("gui.roll_mod.hub.moderation.kitNames.id",
                    Component.literal(kit.name()).withStyle(ChatFormatting.GRAY)));
            name.setText(kit.displayName());
        }
        preview.setText(Component.translatable("gui.roll_mod.hub.moderation.kitNames.preview")
                .append(" ").append(LegacyText.display(name.getText().isBlank() ? kit.name() : name.getText())));
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
