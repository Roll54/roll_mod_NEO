package com.roll_54.roll_mod.items.electricItems.orescanner.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.gtmogs.OreMaterials;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.items.electricItems.orescanner.ClientOreScanCache;
import com.roll_54.roll_mod.items.electricItems.orescanner.OreScanResult;
import com.roll_54.roll_mod.minestar.hub.gui.MarqueeLabel;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ore scanner's screen, 600 x 400: the scanned chunks on the left ({@link OreChunkMap}), a search field and the
 * materials found, with their totals, on the right. Everything shown comes from {@link ClientOreScanCache}, which
 * the scan packet fills just before the screen opens, so no LDLib sync values are involved and the server's copy
 * of the tree is just an empty frame.
 */
public final class OreScannerUI {

    public static final ResourceLocation UI_ID = RollMod.id("ore_scanner");

    /** The panel's full size; it shrinks to fit smaller screens (high GUI scales), keeping {@link #MARGIN} free. */
    private static final int MAX_W = 600;
    private static final int MAX_H = 400;
    private static final int MARGIN = 8;
    private static final int PAD = 10;
    private static final int TOP = 20;
    /** The map gives up width before the list drops below this. */
    private static final int MIN_LIST_W = 150;
    private static final int ROW_H = 16;
    private static final int SWATCH = 8;
    private static final int COUNT_W = 54;

    private OreScannerUI() {}

    public static ModularUI createUI(Player player) {
        UIElement root = VendorUIHelper.styledPanel(MAX_W, MAX_H);
        root.layout(l -> l.paddingAll(0));

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.ore_scanner.title"));
        title.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(PAD).top(5).height(12));
        root.addChild(title);

        String[] filter = {""};
        // A material clicked in the list: the map shows only it until it is clicked again.
        String[] locked = {null};
        OreChunkMap map = new OreChunkMap(() -> ClientOreScanCache.LAST,
                material -> locked[0] != null ? material.equals(locked[0]) : matches(material, filter[0]));
        map.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(PAD).top(TOP));
        root.addChild(map);

        UIElement side = new UIElement();
        side.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).top(TOP)
                .flexDirection(FlexDirection.COLUMN).gapRow(4));
        root.addChild(side);

        TextField search = new TextField();
        search.setText("");
        search.layout(l -> l.widthPercent(100).height(ROW_H));
        search.setTextResponder(text -> filter[0] = text == null ? "" : text.strip().toLowerCase());
        side.addChild(search);

        ScrollerView list = new ScrollerView();
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100)));
        side.addChild(list);

        // Rebuilt when a new scan arrives, the search changes or a material is (un)locked; the map redraws every
        // frame anyway. A new scan drops the lock.
        String[] shown = {null};
        int[] sequence = {-1};
        root.addEventListener(UIEvents.TICK, e -> {
            if (sequence[0] != ClientOreScanCache.SEQUENCE) {
                sequence[0] = ClientOreScanCache.SEQUENCE;
                locked[0] = null;
            }
            String signature = sequence[0] + "|" + filter[0] + "|" + locked[0];
            if (signature.equals(shown[0])) return;
            shown[0] = signature;
            rebuildList(list, ClientOreScanCache.LAST, filter[0], locked);
        });

        // Laid out for the full size here (all the server's copy ever gets), then re-fitted to the screen on every
        // screen init — opening, resizing the window, changing the GUI scale — and centred by LDLib.
        fit(root, title, map, side, MAX_W, MAX_H);
        return ModularUI.of(RollUi.of(player, root, screen -> {
            int w = Math.min(MAX_W, screen.width - 2 * MARGIN);
            int h = Math.min(MAX_H, screen.height - 2 * MARGIN);
            fit(root, title, map, side, w, h);
            return Size.of(w, h);
        }), player);
    }

    /** Square map on the left, as big as the height allows while the list keeps {@link #MIN_LIST_W}; list fills the rest. */
    private static void fit(UIElement root, Label title, OreChunkMap map, UIElement side, int w, int h) {
        int mapSize = Math.max(0, Math.min(h - TOP - PAD, w - 3 * PAD - MIN_LIST_W));
        int sideLeft = 2 * PAD + mapSize;
        root.layout(l -> l.width(w).height(h));
        title.layout(l -> l.width(w - 2 * PAD));
        map.layout(l -> l.width(mapSize).height(mapSize));
        side.layout(l -> l.left(sideLeft).width(w - sideLeft - PAD).height(h - TOP - PAD));
    }

    private static void rebuildList(ScrollerView list, OreScanResult scan, String filter, String[] locked) {
        list.clearAllScrollViewChildren();
        Map<String, Integer> totals = new LinkedHashMap<>();
        if (scan != null) {
            for (OreScanResult.Chunk chunk : scan.chunks()) chunk.totals().forEach((m, c) -> totals.merge(m, c, Integer::sum));
        }
        List<Map.Entry<String, Integer>> rows = totals.entrySet().stream()
                .filter(e -> matches(e.getKey(), filter))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();
        if (rows.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable(scan == null || totals.isEmpty()
                    ? "gui.roll_mod.ore_scanner.no_ores" : "gui.roll_mod.ore_scanner.no_match"));
            none.textStyle(t -> t.textColor(0xFFAAAAAA));
            none.layout(l -> l.widthPercent(100).height(ROW_H));
            list.addScrollViewChild(none);
            return;
        }
        for (Map.Entry<String, Integer> row : rows) list.addScrollViewChild(row(row.getKey(), row.getValue(), locked));
    }

    private static UIElement row(String material, int count, String[] locked) {
        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).height(ROW_H).flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER).gapColumn(4).paddingAll(1));
        if (material.equals(locked[0])) {
            row.style(s -> s.backgroundTexture(new ColorRectTexture(0x50000000 | (color(material) & 0xFFFFFF))));
        }
        row.addEventListener(UIEvents.CLICK, e -> locked[0] = material.equals(locked[0]) ? null : material);
        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(material.equals(locked[0])
                        ? "gui.roll_mod.ore_scanner.unlock_tip" : "gui.roll_mod.ore_scanner.lock_tip")),
                null, null, ItemStack.EMPTY));

        UIElement swatch = new UIElement();
        swatch.layout(l -> l.width(SWATCH).height(SWATCH).flexShrink(0));
        swatch.style(s -> s.backgroundTexture(new ColorRectTexture(color(material))));
        row.addChild(swatch);

        // Scrolls sideways while the row is hovered, if the name is too long for it.
        MarqueeLabel name = new MarqueeLabel(materialName(material), row::isSelfOrChildHover, color(material));
        name.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H - 2));
        row.addChild(name);

        Label amount = new Label();
        amount.setText(Component.literal(Integer.toString(count)));
        amount.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT).textAlignVertical(Vertical.CENTER)
                .textColor(0xFFFFFFFF));
        amount.layout(l -> l.width(COUNT_W).height(ROW_H - 2).flexShrink(0));
        row.addChild(amount);
        return row;
    }

    private static boolean matches(String material, String filter) {
        return filter.isEmpty() || materialName(material).getString().toLowerCase().contains(filter);
    }

    /** {@code material.roll_mod.<id>}, written by {@code OreLangProvider}. */
    static MutableComponent materialName(String material) {
        return Component.translatable("material.roll_mod." + material);
    }

    /** The material's colour from {@link OreDefinition#hexColor()}, opaque; grey for an unknown id. */
    static int color(String material) {
        OreDefinition def = OreMaterials.byId(material);
        if (def == null) return 0xFF808080;
        return 0xFF000000 | Integer.parseInt(def.hexColor().substring(1), 16);
    }
}
