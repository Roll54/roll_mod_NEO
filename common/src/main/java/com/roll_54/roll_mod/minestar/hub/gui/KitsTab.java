package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.kits.ClientKitCache;
import com.roll_54.roll_mod.minestar.kits.KitSlots;
import com.roll_54.roll_mod.network.packet.KitWindowPackets;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.List;

/**
 * The kits as a hub tab of their own, as on Eden_LD — they used to share the home tab's side column with the quick
 * teleports. The same {@link KitsPanel}, given the tab's whole width, and Preview opens a window inside the hub
 * (what the kit holds, laid out as an inventory) instead of a screen of its own, which closed and reopened the hub.
 */
final class KitsTab {

    private static final int SLOT = 18;
    private static final int GAP = 4;
    /** Armour column, a gap, nine columns, a gap, the offhand. */
    private static final int GRID_W = SLOT + GAP + 9 * SLOT + GAP + SLOT;
    private static final int GRID_H = 4 * SLOT + GAP;
    private static final int PREVIEW_W = GRID_W + 24;
    private static final int PREVIEW_H = GRID_H + 90;
    /** Head to feet, top row down. */
    private static final int[] ARMOUR = {39, 38, 37, 36};

    private KitsTab() {}

    static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));
        HubSection.Info section = HubSection.info("kits");
        UIElement panel = KitsPanel.build(true);
        panel.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));

        UIElement[] content = {null};
        HubSection.Floating preview = HubSection.popup(Component.translatable("gui.roll_mod.hub.kits.preview"),
                PREVIEW_W, PREVIEW_H, body -> content[0] = body);
        // The server answers a Preview with what the kit holds; each answer fills the window and opens it.
        int[] seen = {ClientKitCache.PREVIEW_VERSION};
        root.addEventListener(UIEvents.TICK, e -> {
            if (ClientKitCache.PREVIEW_VERSION == seen[0]) return;
            seen[0] = ClientKitCache.PREVIEW_VERSION;
            KitWindowPackets.Contents kit = ClientKitCache.PREVIEW;
            if (kit == null || content[0] == null) return;
            fill(content[0], kit);
            preview.open();
        });

        root.addChildren(HubSection.header("kits", section), panel, preview.root(), section.overlay());
        return root;
    }

    private static void fill(UIElement content, KitWindowPackets.Contents kit) {
        content.clearAllChildren();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(3));

        Label title = new Label();
        title.setText(LegacyText.display(kit.title()));
        title.layout(l -> l.widthPercent(100).height(10));
        title.textStyle(t -> t.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
        Label often = new Label();
        often.setText(cooldownLine(kit.cooldownTicks()).copy().withStyle(ChatFormatting.GRAY));
        often.layout(l -> l.widthPercent(100).height(10));
        often.textStyle(t -> t.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
        content.addChildren(title, often);
        if (!kit.description().isBlank()) {
            Label description = new Label();
            description.setText(LegacyText.display(kit.description()).copy().withStyle(ChatFormatting.GRAY));
            description.layout(l -> l.widthPercent(100));
            description.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            content.addChild(description);
        }

        ItemStack[] bySlot = new ItemStack[KitSlots.MAIN_SLOTS];
        for (int i = 0; i < kit.slots().size(); i++) {
            int slot = kit.slots().get(i);
            if (slot >= 0 && slot < bySlot.length) bySlot[slot] = kit.items().get(i);
        }
        UIElement grid = new UIElement();
        grid.layout(l -> l.width(GRID_W).height(GRID_H).marginTop(3).flexShrink(0));
        int gridX = SLOT + GAP;
        for (int row = 0; row < 4; row++) {
            int y = row * SLOT + (row == 3 ? GAP : 0);
            grid.addChild(slot(bySlot[ARMOUR[row]], 0, y));
            int first = row < 3 ? 9 + row * 9 : 0;
            for (int i = 0; i < 9; i++) grid.addChild(slot(bySlot[first + i], gridX + i * SLOT, y));
        }
        grid.addChild(slot(bySlot[KitSlots.OFFHAND], gridX + 9 * SLOT + GAP, 3 * SLOT + GAP));
        content.addChild(grid);
    }

    /** One read-only slot: the stack, its count, and the item's own tooltip on hover. */
    private static UIElement slot(ItemStack stack, int x, int y) {
        UIElement slot = new UIElement();
        slot.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(x).top(y).width(SLOT).height(SLOT));
        slot.style(s -> s.background(new GuiTextureGroup(new ColorRectTexture(0x40000000),
                new ColorBorderTexture(1, 0x50FFFFFF))));
        if (stack == null || stack.isEmpty()) return slot;
        UIElement icon = new UIElement();
        icon.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(1).top(1).width(16).height(16));
        icon.style(s -> s.background(new ItemStackTexture(stack)));
        slot.addChild(icon);
        if (stack.getCount() > 1) {
            Label count = new Label();
            count.setText(Component.literal(String.valueOf(stack.getCount())));
            count.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(9).width(SLOT - 1).height(9));
            count.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT).textColor(0xFFFFFFFF).textShadow(true));
            slot.addChild(count);
        }
        slot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(stack.getHoverName()), null, null, stack));
        return slot;
    }

    /** How often the kit can be had: once, every so long, or any time. */
    private static Component cooldownLine(long ticks) {
        if (ticks < 0) return Component.translatable("gui.roll_mod.hub.kits.preview.once");
        if (ticks > 0) return Component.translatable("gui.roll_mod.hub.kits.preview.every", Durations.format(ticks * 50L));
        return Component.translatable("gui.roll_mod.hub.kits.preview.anytime");
    }
}
