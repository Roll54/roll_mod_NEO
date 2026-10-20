package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.network.packet.WarpActionPacket;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The moderation tab's warp editor, shown in place of the warp's details while "Edit" is on: name,
 * description and price, moving it to where the moderator stands, and — on official warps — the
 * quest a visitor's team must have finished before it opens (see {@code QuestGate}).
 *
 * <p>Built once and refilled from the selected warp, so typing survives the tab's rebuilds; the
 * fields only follow the server when the warp changes and nobody is typing in them. Every action is
 * re-checked server-side by {@code WarpActionPacket}.
 */
final class WarpEditor {

    private static final int ROW_H = 16;
    private static final int GAP = 3;
    private static final int PRICE_W = 60;
    private static final int COLOR_PANEL = 0x40000000;

    final UIElement box = new UIElement();
    final QuestPicker picker;

    private final TextField name = new TextField();
    private final TextField description = new TextField();
    private final TextField price = VendorUIHelper.intField("0", t -> {});
    private final Label quest = new Label();
    private final UIElement questRow = new UIElement();
    private final Label questHint = new Label();

    @Nullable private Warp warp;
    private String shown;

    WarpEditor() {
        picker = new QuestPicker(id -> {
            if (warp != null) PacketDistributor.sendToServer(WarpActionPacket.setQuest(warp.id(), id));
        });

        box.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1)
                .paddingAll(6).gapRow(GAP));
        box.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        name.setText("");
        name.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H));
        hint(name, "gui.roll_mod.hub.moderation.warps.edit.name");
        price.layout(l -> l.width(PRICE_W).height(ROW_H).flexShrink(0));
        hint(price, "gui.roll_mod.hub.moderation.warps.edit.price");
        UIElement first = row();
        first.addChildren(name, price);

        description.setText("");
        description.layout(l -> l.widthPercent(100).height(ROW_H));
        hint(description, "gui.roll_mod.hub.moderation.warps.edit.description");

        UIElement buttons = row();
        buttons.addChildren(
                button("gui.roll_mod.hub.moderation.warps.edit.save", () -> PacketDistributor.sendToServer(
                        WarpActionPacket.edit(warp.id(), name.getText(), description.getText(),
                                Math.max(0L, VendorUIHelper.parseLong(price.getText(), 0))))),
                button("gui.roll_mod.hub.moderation.warps.edit.move", () ->
                        PacketDistributor.sendToServer(WarpActionPacket.moveHere(warp.id()))));

        Label questTitle = new Label();
        questTitle.setText(Component.translatable("gui.roll_mod.hub.moderation.warps.quest.title"));
        questTitle.layout(l -> l.widthPercent(100).height(12).marginTop(4));
        questTitle.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        quest.layout(l -> l.widthPercent(100));
        quest.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        questRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(GAP));
        questRow.addChildren(
                button("gui.roll_mod.hub.moderation.warps.quest.pick", picker::open),
                button("gui.roll_mod.hub.moderation.warps.quest.clear", () ->
                        PacketDistributor.sendToServer(WarpActionPacket.setQuest(warp.id(), 0))));

        questHint.setText(Component.translatable("gui.roll_mod.hub.moderation.warps.quest.officialOnly")
                .withStyle(ChatFormatting.GRAY));
        questHint.layout(l -> l.widthPercent(100));
        questHint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        box.addChildren(first, description, buttons, questTitle, quest, questRow, questHint);
    }

    /** Points the editor at {@code current}, refilling the fields when it changed and nobody is typing. */
    void tick(@Nullable Warp current) {
        warp = current;
        if (current == null) return;
        String signature = current.id() + "/" + current.name() + "/" + current.description() + "/"
                + current.price() + "/" + current.requiredQuest() + "/" + current.approval().id();
        if (signature.equals(shown)) return;
        boolean switched = shown == null || !shown.startsWith(current.id().toString());
        boolean typing = name.isFocused() || description.isFocused() || price.isFocused();
        if (typing && !switched) return;
        shown = signature;

        name.setText(current.name());
        description.setText(current.description());
        price.setText(Long.toString(current.price()));

        quest.setText(current.requiredQuest() == 0
                ? Component.translatable("gui.roll_mod.hub.moderation.warps.quest.none").withStyle(ChatFormatting.GRAY)
                : QuestGate.title(true, current.requiredQuest()).copy().withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("  " + QuestGate.code(current.requiredQuest()))
                                .withStyle(ChatFormatting.DARK_GRAY)));
        // A quest gates official warps only; the server refuses it on any other.
        boolean official = current.isAdmin();
        questRow.setDisplay(official);
        questHint.setDisplay(!official);
    }

    private static UIElement row() {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(GAP)
                .alignItems(AlignItems.CENTER));
        return row;
    }

    private Button button(String key, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.flexGrow(1).height(ROW_H));
        button.setOnClick(e -> {
            if (warp != null) onClick.run();
        });
        return button;
    }

    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }
}
