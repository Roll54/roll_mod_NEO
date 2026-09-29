package com.roll_54.roll_mod.items.modulardrill.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.items.modulardrill.ModuleType;
import com.roll_54.roll_mod.network.packet.drill.DrillTrashFilterPacket;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.network.packet.drill.DrillVolumePacket;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The held modular drill's config screen, opened by keybind: the mining-volume editor, and the
 * Trash Filter's list when that module is installed.
 *
 * <p>Per the user's mockup — three columns X/Y/Z, each a +, the current value and a −; the held
 * drill's icon on the right, and energy per block and per whole volume across the bottom. Nothing else:
 * the throttle lives on Ctrl+scroll and the module list on the tooltip.
 *
 * <p>With a Trash Filter installed the screen gets two tabs: Volume (the editor above) and Trash
 * filter — its 18 ghost slots with the player inventory under them, so an item can be picked up and
 * clicked into a slot. Without one the screen is unchanged and has no tabs.
 *
 * <p>Tab switching is a client-side click the server never sees, so it only toggles display; both
 * pages are always built on both sides, and nothing in them ticks or syncs, so the server copy
 * sitting on the first tab costs nothing. Both commits hang off the root's REMOVED, which fires
 * whichever tab is showing.
 *
 * <p><b>Built on both sides</b>, like every LdLib2 UI here — but the numbers are edited in a
 * local copy and committed as one {@link DrillVolumePacket} when the screen closes. They have to
 * be: this menu holds no inventory slots, so the server's component writes on the held stack
 * would not reach the client while it is open, and live labels read stale data. Local state
 * updates the instant a button is clicked; the item catches up at close, clamped server-side.
 */
public final class ModularDrillConfigUI {

    public static final ResourceLocation UI_ID = RollMod.id("drill_config");

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 102;

    private static final int SLOT_SIZE = 18;
    private static final int FILTER_COLUMNS = 9;
    private static final int FILTER_X = (PANEL_WIDTH - FILTER_COLUMNS * SLOT_SIZE) / 2;
    private static final int FILTER_HEADER_Y = 4;
    private static final int FILTER_SLOTS_Y = 14;
    /** Where the inventory starts on the filter page, under the two slot rows. */
    private static final int FILTER_SECTION_HEIGHT = 54;
    private static final int INVENTORY_X = 8;
    /** The inventory grid plus the panel's bottom margin, as the installation table lays it out. */
    private static final int INVENTORY_HEIGHT = 82;

    private static final int TAB_X = 4;
    private static final int TAB_Y = 3;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 16;
    /** Where the pages start: under the tab row. */
    private static final int PAGE_Y = TAB_Y + TAB_HEIGHT;
    /** The taller of the two pages; the panel keeps one size so it does not jump when switching. */
    private static final int PAGE_HEIGHT = Math.max(PANEL_HEIGHT, FILTER_SECTION_HEIGHT + INVENTORY_HEIGHT);

    private static final int COLUMN_X = 14;
    private static final int COLUMN_STEP = 34;
    private static final int COLUMN_WIDTH = 24;
    private static final int HEADER_Y = 8;
    private static final int PLUS_Y = 22;
    private static final int VALUE_Y = 42;
    private static final int MINUS_Y = 56;
    private static final int BUTTON_HEIGHT = 14;
    private static final int LINE_HEIGHT = 10;

    private static final int ICON_X = 126;
    private static final int ICON_Y = 12;
    private static final int ICON_SIZE = 32;
    /**
     * The two EU lines, full width under the columns. They used to sit in a 64px column under the
     * icon, where a translated caption left no room: HIDE wrapping dropped the number itself, so
     * the lines read "EU/…:" and nothing else.
     */
    private static final int EU_X = 8;
    private static final int EU_WIDTH = PANEL_WIDTH - 2 * EU_X;
    private static final int EU_BLOCK_Y = 76;
    private static final int EU_VOLUME_Y = 88;

    private static final int TEXT_PRIMARY = 0xFFE0E0E0;
    /** Minecraft's AQUA — every EU-usage figure wears it, on the tooltip and here alike. */
    private static final int TEXT_ENERGY = 0xFF55FFFF;

    private ModularDrillConfigUI() {
    }

    public static ModularUI createUI(Player player) {
        ItemStack drill = player.getMainHandItem();
        // Decided from the synced component, so both sides build the same tree.
        boolean filter = drill.getItem() instanceof ModularDrillItem
                && DrillModuleHelper.get(drill).has(ModuleType.TRASH_FILTER);
        int height = filter ? PAGE_Y + PAGE_HEIGHT : PANEL_HEIGHT;

        UIElement root = VendorUIHelper.styledPanel(PANEL_WIDTH, height);
        root.layout(l -> l.paddingAll(0));

        if (drill.getItem() instanceof ModularDrillItem item) {
            if (filter) {
                UIElement volumePage = page(root);
                build(volumePage, root, player, drill, item);
                UIElement filterPage = page(root);
                buildFilter(filterPage, root, player, drill);
                filterPage.addChild(VendorUIHelper.playerInventory(INVENTORY_X, FILTER_SECTION_HEIGHT));

                List<UIElement> pages = List.of(volumePage, filterPage);
                List<Tab> tabs = List.of(
                        tab(root, 0, "gui.roll_mod.drill_config.tab.volume"),
                        tab(root, 1, "gui.roll_mod.drill_config.trash_filter"));
                for (int i = 0; i < tabs.size(); i++) {
                    final int index = i;
                    tabs.get(i).addEventListener(UIEvents.CLICK, e -> select(tabs, pages, index));
                }
                select(tabs, pages, 0);
            } else {
                build(root, root, player, drill, item);
            }
        }

        return ModularUI.of(
                UI.of(root, StylesheetManager.MC_MERGED, StylesheetManager.GDP_MERGED), player);
    }

    /** A page under the tab row; its children are laid out from its own top-left corner. */
    private static UIElement page(UIElement root) {
        UIElement page = new UIElement();
        root.addChild(VendorUIHelper.abs(page, 0, PAGE_Y, PANEL_WIDTH, PAGE_HEIGHT));
        return page;
    }

    private static Tab tab(UIElement root, int index, String key) {
        Tab tab = new Tab();
        tab.setText(Component.translatable(key));
        // Tab carries no click listener of its own; TabView normally supplies one.
        root.addChild(VendorUIHelper.abs(tab, TAB_X + index * (TAB_WIDTH + 2), TAB_Y, TAB_WIDTH, TAB_HEIGHT));
        return tab;
    }

    /** Shows one page and latches its tab; the inventory lives on the filter page and goes with it. */
    private static void select(List<Tab> tabs, List<UIElement> pages, int selected) {
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setDisplay(i == selected);
            tabs.get(i).setSelected(i == selected);
        }
    }

    private static void build(UIElement parent, UIElement root, Player player, ItemStack drill,
                              ModularDrillItem item) {
        // The GUI's own copy of the volume. Seeded from the drill when the screen opens, edited
        // freely while it is on, written back once at close. Bounds are baked at open time too —
        // modules cannot change while this screen is up (the table is a different screen).
        DrillModuleHelper.Volume seed = DrillModuleHelper.volume(drill);
        int[] local = {seed.x(), seed.y(), seed.z()};
        int max = DrillModuleHelper.maxEdge(drill);
        long costPerBlock = DrillModuleHelper.costPerBlock(drill, item.voltage());

        Label euVolume = euLine(parent, EU_VOLUME_Y);
        Runnable repaintVolume = () -> euVolume.setText(Component.translatable(
                "gui.roll_mod.drill_config.eu_per_volume",
                EnergyFormatUtils.formatEnergy(costPerBlock * local[0] * local[1] * local[2])));

        axisColumn(parent, 0, "X", local, max, repaintVolume);
        axisColumn(parent, 1, "Y", local, max, repaintVolume);
        axisColumn(parent, 2, "Z", local, max, repaintVolume);

        UIElement icon = new UIElement();
        icon.style(s -> s.backgroundTexture(new ItemStackTexture(drill.copy())));
        parent.addChild(VendorUIHelper.abs(icon, ICON_X, ICON_Y, ICON_SIZE, ICON_SIZE));

        Label euBlock = euLine(parent, EU_BLOCK_Y);
        euBlock.setText(Component.translatable("gui.roll_mod.drill_config.eu_per_block",
                EnergyFormatUtils.formatEnergy(costPerBlock)));
        repaintVolume.run();

        // The commit: one absolute packet when the screen goes away. The listener is attached on
        // both sides (the trees must match), so only the client half acts.
        root.addEventListener(UIEvents.REMOVED, e -> {
            if (player.level().isClientSide) {
                PacketDistributor.sendToServer(
                        new DrillVolumePacket(local[0], local[1], local[2]));
            }
        });
    }

    /**
     * The Trash Filter section: a header and 2×9 ghost slots seeded from the drill. A click with an
     * item sets a one-count copy, an empty-handed click clears; XEI drags write the slots directly.
     * Like the volume, the list is committed once at close — read straight from the slots, so a
     * drag counts too.
     */
    private static void buildFilter(UIElement parent, UIElement root, Player player, ItemStack drill) {
        Label header = new Label();
        header.textStyle(text -> text.textColor(TEXT_PRIMARY).textShadow(true)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        header.setText(Component.translatable("gui.roll_mod.drill_config.trash_filter"));
        parent.addChild(VendorUIHelper.abs(header, FILTER_X, FILTER_HEADER_Y,
                FILTER_COLUMNS * SLOT_SIZE, LINE_HEIGHT));

        ItemContainerContents seed = drill.getOrDefault(
                ComponentsRegistry.TRASH_FILTER.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> initial = NonNullList.withSize(DrillModuleHelper.TRASH_FILTER_SLOTS, ItemStack.EMPTY);
        seed.copyInto(initial);

        List<ItemSlot> slots = new ArrayList<>(DrillModuleHelper.TRASH_FILTER_SLOTS);
        for (int i = 0; i < DrillModuleHelper.TRASH_FILTER_SLOTS; i++) {
            ItemSlot slot = VendorUIHelper.phantomSlot();
            slot.setItem(initial.get(i).copy());
            slot.addEventListener(UIEvents.CLICK, e -> {
                AbstractContainerMenu menu = slot.getModularUI() == null ? null : slot.getModularUI().getMenu();
                if (menu == null) return;
                ItemStack cursor = menu.getCarried();
                slot.setItem(cursor.isEmpty() ? ItemStack.EMPTY : cursor.copyWithCount(1));
                e.stopPropagation();
            });
            int x = FILTER_X + (i % FILTER_COLUMNS) * SLOT_SIZE;
            int y = FILTER_SLOTS_Y + (i / FILTER_COLUMNS) * SLOT_SIZE;
            parent.addChild(VendorUIHelper.abs(slot, x, y, SLOT_SIZE, SLOT_SIZE));
            slots.add(slot);
        }

        root.addEventListener(UIEvents.REMOVED, e -> {
            if (player.level().isClientSide) {
                List<ItemStack> values = new ArrayList<>(slots.size());
                for (ItemSlot slot : slots) {
                    ItemStack value = slot.getValue();
                    values.add(value == null || value.isEmpty() ? ItemStack.EMPTY : value.copyWithCount(1));
                }
                PacketDistributor.sendToServer(new DrillTrashFilterPacket(values));
            }
        });
    }

    /** One mockup column: the axis letter, +, the locally-edited value, −. */
    private static void axisColumn(UIElement root, int axis, String name,
                                   int[] local, int max, Runnable repaintVolume) {
        int x = COLUMN_X + axis * COLUMN_STEP;

        Label header = centeredLabel();
        header.setText(Component.literal(name));
        root.addChild(VendorUIHelper.abs(header, x, HEADER_Y, COLUMN_WIDTH, LINE_HEIGHT));

        Label value = centeredLabel();
        value.setText(Component.literal(String.valueOf(local[axis])));

        Button plus = new Button();
        plus.setText(Component.literal("+"));
        plus.setOnClick(e -> step(axis, +2, local, max, value, repaintVolume));
        root.addChild(VendorUIHelper.abs(plus, x, PLUS_Y, COLUMN_WIDTH, BUTTON_HEIGHT));

        root.addChild(VendorUIHelper.abs(value, x, VALUE_Y, COLUMN_WIDTH, LINE_HEIGHT));

        Button minus = new Button();
        minus.setText(Component.literal("−"));
        minus.setOnClick(e -> step(axis, -2, local, max, value, repaintVolume));
        root.addChild(VendorUIHelper.abs(minus, x, MINUS_Y, COLUMN_WIDTH, BUTTON_HEIGHT));
    }

    /**
     * Steps of two: only odd sizes exist (1, 3, 5, …), the seed is odd and the module's max edge
     * is odd, so ±2 with a plain clamp can never leave the odd rail.
     */
    private static void step(int axis, int delta, int[] local, int max,
                             Label value, Runnable repaintVolume) {
        int next = Mth.clamp(local[axis] + delta, 1, max);
        if (next == local[axis]) return;
        local[axis] = next;
        value.setText(Component.literal(String.valueOf(next)));
        repaintVolume.run();
    }

    private static Label euLine(UIElement root, int y) {
        Label label = new Label();
        label.textStyle(text -> text.textColor(TEXT_ENERGY).textShadow(false)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        root.addChild(VendorUIHelper.abs(label, EU_X, y, EU_WIDTH, LINE_HEIGHT));
        return label;
    }

    private static Label centeredLabel() {
        Label label = new Label();
        label.textStyle(text -> text.textColor(TEXT_PRIMARY).textShadow(true)
                .textAlignHorizontal(Horizontal.CENTER)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        return label;
    }
}
