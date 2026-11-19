package com.roll_54.roll_mod.minestar.creative;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import com.roll_54.roll_mod.minestar.ui.ExpandableWindow;
import com.roll_54.roll_mod.minestar.ui.SavedWindowState;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The creative inventory, rebuilt in LdLib in the hub's style: a window (moved, resized and maximized
 * like the hub's, remembered in {@code creative_window.json}), every creative tab as an icon down the
 * left, a search bar over the item grid, and the hotbar — or, on the inventory tab, the whole survival
 * inventory with armor, offhand and the destroy slot — at the bottom.
 *
 * <p>It edits the player's own inventory the way vanilla's creative screen does: the slot is set on the
 * client and sent with {@code handleCreativeModeItemAdd}; items dropped outside the window go out with
 * {@code handleCreativeModeItemDrop}. The carried stack is this screen's own.
 *
 * <p>Clicks: left takes a full stack (one more if you already hold it), right takes one, middle a full
 * stack, shift-left puts a full stack straight into the inventory; clicking the grid while holding
 * something deletes it. In inventory slots: left swaps or merges, right halves or places one, shift-left
 * deletes. The destroy slot deletes what you hold; shift-click it to empty the inventory. Number keys
 * over the grid put that item in the hotbar slot; the save/load-hotbar keys + a number work as vanilla's.
 * Search: plain text by name and tooltip, {@code #} for tags — across every item.
 *
 * <p>Colours: the hub theme, except that slots and the window background are vanilla grey under the
 * Ore theme (and the vanilla-looking MC theme).
 */
@OnlyIn(Dist.CLIENT)
public final class RollCreativeScreen extends ModularUIScreen {

    private static final int SLOT = 18;
    private static final int TAB = 22;
    private static final int PADDING = 6;
    private static final int BAR_H = 16;
    /** Inventory menu slots: armor 5..8 (head first), main 9..35, hotbar 36..44, offhand 45. */
    private static final int HOTBAR_FIRST = 36;
    private static final int OFFHAND = 45;

    private static CreativeModeTab lastTab;

    private final Minecraft mc = Minecraft.getInstance();
    private final ExpandableWindow window;
    private final Label title = new Label();
    private final TextField search = new TextField();
    private final ScrollerView tabColumn = new ScrollerView();
    private final ItemGrid grid = new ItemGrid();
    private final InventoryView inventory = new InventoryView();

    private CreativeModeTab tab;
    private final List<ItemStack> items = new ArrayList<>();
    private ItemStack carried = ItemStack.EMPTY;
    /** What the mouse is over this frame, for the tooltip and the number keys. */
    private ItemStack hovered = ItemStack.EMPTY;
    private boolean hoveredInGrid;

    private RollCreativeScreen(UIElement root, ExpandableWindow window) {
        super(ModularUI.of(UI.of(root, sheets(), size -> {
            window.fit((int) size.getWidth(), (int) size.getHeight());
            return size;
        })), Component.translatable("itemGroup.inventory"));
        this.window = window;
        rebuildTabContents();
        build(root);
        setTab(lastTab != null && lastTab.shouldDisplay() ? lastTab : CreativeModeTabs.searchTab());
    }

    public RollCreativeScreen() {
        this(new UIElement(), new ExpandableWindow(state(), state().defaultW, state().defaultH, state().minW, state().minH));
    }

    private static SavedWindowState state() {
        return SavedWindowState.get("creative_window.json", 340, 250, 250, 200);
    }

    /** The hub theme, plus vanilla grey for the window under Ore. */
    private static List<Stylesheet> sheets() {
        List<Stylesheet> out = new ArrayList<>(RollUi.sheets());
        if (greyLook()) out.add(Stylesheet.parse(".creative_window { background: built-in(ui-mc:RECT); }"));
        return out;
    }

    private static boolean greyLook() {
        UiTheme theme = UiThemeState.get();
        return theme == UiTheme.ORE || theme.palette() == null;
    }

    /* ------------------------------------------------------- tabs ------------------------------------------------------- */

    /**
     * What the search indexes were last built from: the search tab's item list (a new collection each
     * time the tabs are rebuilt) and the connection they belong to.
     */
    private static Object indexedItems;
    private static Object indexedConnection;

    /**
     * Tab contents for this world, and the search indexes over them.
     *
     * <p>Vanilla only builds the creative search indexes when {@code tryRebuildTabContents} reports that
     * it rebuilt the tabs. Recipe viewers call that themselves — TEI's item index does, at load — so by
     * the time the creative screen opens it reports "nothing to do" and the indexes are never built:
     * every search comes back empty (vanilla's own creative screen has the same problem with TEI). So
     * the indexes are built here whenever the items they were built from have changed, whoever rebuilt
     * the tabs.
     */
    private void rebuildTabContents() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        FeatureFlagSet features = player.connection.enabledFeatures();
        boolean permissions = player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get();
        HolderLookup.Provider registries = mc.level.registryAccess();
        CreativeModeTabs.tryRebuildTabContents(features, permissions, registries);
        Object items = CreativeModeTabs.searchTab().getDisplayItems();
        if (items == indexedItems && player.connection == indexedConnection) return;
        indexedItems = items;
        indexedConnection = player.connection;
        SessionSearchTrees trees = player.connection.searchTrees();
        CreativeModeTabs.allTabs().stream().filter(CreativeModeTab::hasSearchBar).forEach(t -> {
            List<ItemStack> list = List.copyOf(t.getDisplayItems());
            trees.updateCreativeTooltips(registries, list, CreativeModeTabSearchRegistry.getNameSearchKey(t));
            trees.updateCreativeTags(list, CreativeModeTabSearchRegistry.getTagSearchKey(t));
        });
    }

    /** The search tab holds every item: it is the "All" group here. */
    private static Component tabName(CreativeModeTab t) {
        return t.getType() == CreativeModeTab.Type.SEARCH ? Component.translatable("gui.roll_mod.creative.all") : t.getDisplayName();
    }

    private static List<CreativeModeTab> visibleTabs() {
        // Vanilla's search tab is shown as "All", first in the column, rather than among the rest.
        return CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(t -> t.shouldDisplay() && t.getType() != CreativeModeTab.Type.SEARCH).toList();
    }

    private void setTab(CreativeModeTab next) {
        tab = next;
        lastTab = next;
        title.setText(tabName(next));
        boolean inventoryTab = next.getType() == CreativeModeTab.Type.INVENTORY;
        grid.setDisplay(!inventoryTab);
        inventory.setDisplay(inventoryTab);
        // Nothing to search on the inventory tab.
        search.setDisplay(!inventoryTab);
        grid.scrollRow = 0;
        refreshItems();
    }

    /** The grid's contents: search results across every item while there is a query, else the tab's. */
    private void refreshItems() {
        items.clear();
        String query = search.getValue() == null ? "" : search.getValue().trim();
        if (!query.isEmpty()) {
            items.addAll(searchAll(query));
        } else if (tab.getType() == CreativeModeTab.Type.HOTBAR) {
            HotbarManager hotbars = mc.getHotbarManager();
            for (int i = 0; i < 9; i++) {
                Hotbar hotbar = hotbars.get(i);
                if (hotbar.isEmpty() || mc.level == null) {
                    for (int j = 0; j < 9; j++) {
                        if (j != i) {
                            items.add(ItemStack.EMPTY);
                            continue;
                        }
                        ItemStack hint = new ItemStack(net.minecraft.world.item.Items.PAPER);
                        hint.set(DataComponents.CREATIVE_SLOT_LOCK, net.minecraft.util.Unit.INSTANCE);
                        hint.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo",
                                mc.options.keySaveHotbarActivator.getTranslatedKeyMessage(),
                                mc.options.keyHotbarSlots[i].getTranslatedKeyMessage()));
                        items.add(hint);
                    }
                } else {
                    items.addAll(hotbar.load(mc.level.registryAccess()));
                }
            }
        } else {
            items.addAll(tab.getDisplayItems());
        }
        grid.scrollRow = Math.min(grid.scrollRow, grid.maxRow());
    }

    /** Vanilla's creative search over every item: {@code #tag} by tag, anything else by name and tooltip. */
    private List<ItemStack> searchAll(String query) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return List.of();
        CreativeModeTab all = CreativeModeTabs.searchTab();
        SessionSearchTrees trees = connection.searchTrees();
        SearchTree<ItemStack> tree;
        String text = query;
        if (text.startsWith("#")) {
            text = text.substring(1);
            tree = trees.creativeTagSearch(CreativeModeTabSearchRegistry.getTagSearchKey(all));
        } else {
            tree = trees.creativeNameSearch(CreativeModeTabSearchRegistry.getNameSearchKey(all));
        }
        return tree.search(text.toLowerCase(Locale.ROOT));
    }

    /* ------------------------------------------------------ layout ------------------------------------------------------ */

    private void build(UIElement root) {
        root.layout(l -> l.widthPercent(100).heightPercent(100));
        // A click on the bare screen beside the window throws the held stack out, as vanilla's does.
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.target == root && !carried.isEmpty()) {
                mc.gameMode.handleCreativeModeItemDrop(e.button == 1 ? carried.split(1) : carried);
                if (e.button != 1) carried = ItemStack.EMPTY;
            }
        });

        window.layout(l -> l.flexDirection(FlexDirection.COLUMN).paddingAll(PADDING).gapRow(4));
        window.addClass("panel_bg");
        window.addClass("creative_window");
        root.addChild(window);

        UIElement titleBar = new UIElement();
        titleBar.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(BAR_H).flexShrink(0).gapColumn(2));
        title.layout(l -> l.flexGrow(1).heightPercent(100));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE).textShadow(true));
        titleBar.addChildren(title,
                windowButton(() -> window.maximized() ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE, window::toggleMaximize),
                windowButton(() -> Icons.WINDOW_CLOSE, this::onClose));
        window.dragBy(titleBar);
        window.dragBy(title);
        window.addChild(titleBar);

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).flexGrow(1).flexBasis(0).gapColumn(4));

        // The tabs, as icons down the left, like the hub's bookmarks on its left side.
        // Exactly the icons' width, so tabs-to-items is the same 4px gap as search-to-items.
        tabColumn.layout(l -> l.width(TAB).flexShrink(0).heightPercent(100));
        tabColumn.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.NEVER)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        tabColumn.style(s -> s.background(IGuiTexture.EMPTY));
        tabColumn.viewPort(v -> v.layout(l -> l.paddingAll(0)).style(s -> s.background(IGuiTexture.EMPTY)));
        tabColumn.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(2)));
        tabColumn.addScrollViewChild(new TabIcon(CreativeModeTabs.searchTab()));
        for (CreativeModeTab t : visibleTabs()) tabColumn.addScrollViewChild(new TabIcon(t));

        UIElement main = new UIElement();
        main.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100).gapRow(4));
        search.setText("", false);
        search.layout(l -> l.height(14).widthPercent(100).flexShrink(0));
        search.style(s -> s.tooltips(Component.translatable("gui.roll_mod.creative.search.tip")));
        search.setTextResponder(s -> {
            // Searching from the inventory or saved-hotbar tab shows the results, not those tabs.
            if (!s.isBlank() && tab.getType() != CreativeModeTab.Type.CATEGORY && tab.getType() != CreativeModeTab.Type.SEARCH) {
                setTab(CreativeModeTabs.searchTab());
            } else {
                grid.scrollRow = 0;
                refreshItems();
            }
        });
        // An explicit 4px under the search bar — the same gap as between the tab icons and the items.
        // (The column's row gap does not separate the search field from what follows it.)
        grid.layout(l -> l.flexGrow(1).flexBasis(0).widthPercent(100).marginTop(4));
        inventory.layout(l -> l.flexGrow(1).flexBasis(0).widthPercent(100).marginTop(4));
        HotbarRow hotbar = new HotbarRow();
        hotbar.layout(l -> l.height(SLOT + 2).widthPercent(100).flexShrink(0));
        main.addChildren(search, grid, inventory, hotbar);

        body.addChildren(tabColumn, main);
        window.addChild(body);
    }

    private static UIElement windowButton(java.util.function.Supplier<IGuiTexture> icon, Runnable action) {
        UIElement button = new UIElement();
        button.layout(l -> l.width(BAR_H - 2).height(BAR_H - 2).flexShrink(0));
        button.style(s -> s.background(IGuiTexture.dynamic(icon)));
        button.addEventListener(UIEvents.CLICK, e -> action.run());
        return button;
    }

    /* ------------------------------------------------------ clicks ------------------------------------------------------ */

    private void clickGrid(ItemStack stack, int button) {
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, stack)) {
            carried = ItemStack.EMPTY; // the picker takes back whatever you drop on it
            return;
        }
        if (stack.isEmpty() || stack.has(DataComponents.CREATIVE_SLOT_LOCK)) return;
        int max = stack.getMaxStackSize();
        if (button == 0 && Screen.hasShiftDown()) {
            int target = firstFreeSlot();
            if (target >= 0) setSlot(target, stack.copyWithCount(max));
        } else if (button == 0) {
            // Left click always picks up a full stack, also onto one of the same item already held.
            carried = stack.copyWithCount(max);
        } else if (button == 1) {
            // Right click: one, or one more onto the same item held.
            if (!carried.isEmpty()) carried.grow(carried.getCount() < max ? 1 : 0);
            else carried = stack.copyWithCount(1);
        } else if (button == 2) {
            carried = stack.copyWithCount(max);
        }
    }

    private void clickSlot(int id, int button) {
        Slot slot = mc.player.inventoryMenu.getSlot(id);
        ItemStack in = slot.getItem();
        if (button == 2) {
            if (!in.isEmpty()) carried = in.copyWithCount(in.getMaxStackSize());
            return;
        }
        if (button == 0 && Screen.hasShiftDown()) {
            setSlot(id, ItemStack.EMPTY);
            return;
        }
        boolean same = !carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, in);
        if (button == 0) {
            if (carried.isEmpty()) {
                carried = in.copy();
                setSlot(id, ItemStack.EMPTY);
            } else if (same) {
                int move = Math.min(in.getMaxStackSize() - in.getCount(), carried.getCount());
                setSlot(id, in.copyWithCount(in.getCount() + move));
                carried.shrink(move);
            } else {
                ItemStack old = in.copy();
                setSlot(id, carried.copy());
                carried = old;
            }
        } else if (button == 1) {
            if (carried.isEmpty()) {
                int half = (in.getCount() + 1) / 2;
                carried = in.copyWithCount(half);
                setSlot(id, in.copyWithCount(in.getCount() - half));
            } else if (in.isEmpty() || same && in.getCount() < in.getMaxStackSize()) {
                setSlot(id, carried.copyWithCount((in.isEmpty() ? 0 : in.getCount()) + 1));
                carried.shrink(1);
            }
        }
    }

    private void clickDestroy() {
        if (!carried.isEmpty()) {
            carried = ItemStack.EMPTY;
        } else if (Screen.hasShiftDown()) {
            for (int id = 5; id <= OFFHAND; id++) setSlot(id, ItemStack.EMPTY);
        }
    }

    /** First empty hotbar slot, then the main inventory; -1 when full. As menu slot ids. */
    private int firstFreeSlot() {
        for (int i = 0; i < 9; i++) if (mc.player.inventoryMenu.getSlot(HOTBAR_FIRST + i).getItem().isEmpty()) return HOTBAR_FIRST + i;
        for (int id = 9; id < HOTBAR_FIRST; id++) if (mc.player.inventoryMenu.getSlot(id).getItem().isEmpty()) return id;
        return -1;
    }

    /** Vanilla's creative edit: set here, tell the server. */
    private void setSlot(int id, ItemStack stack) {
        mc.player.inventoryMenu.getSlot(id).set(stack);
        mc.gameMode.handleCreativeModeItemAdd(stack, id);
    }

    /* ------------------------------------------------------- keys ------------------------------------------------------- */

    /** Whether the search bar has focus, so TEI's key shortcuts stay off while typing in it. */
    public boolean isTyping() {
        return search.isFocused();
    }

    /** The item under the mouse (grid or inventory), for TEI's R / U / favorite keys; empty when none. */
    public ItemStack hoveredStack() {
        return hovered;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = search.isFocused();
        // TEI's R / U on whatever item is under the mouse. TEI now handles these itself on this screen
        // (TeiCreativeScreenMixin); kept as the fallback for when its key event does not reach it first.
        if (!typing && com.roll_54.roll_mod.compat.tei.TeiBridge.handleKey(keyCode, scanCode, hovered)) return true;
        if (!typing) {
            for (int i = 0; i < 9; i++) {
                if (!mc.options.keyHotbarSlots[i].matches(keyCode, scanCode)) continue;
                boolean save = mc.options.keySaveHotbarActivator.isDown();
                boolean load = mc.options.keyLoadHotbarActivator.isDown();
                if (save || load) {
                    CreativeModeInventoryScreen.handleHotbarLoadOrSave(mc, i, load, save);
                    if (tab.getType() == CreativeModeTab.Type.HOTBAR) refreshItems();
                    return true;
                }
                if (hoveredInGrid && !hovered.isEmpty() && !hovered.has(DataComponents.CREATIVE_SLOT_LOCK)) {
                    setSlot(HOTBAR_FIRST + i, hovered.copyWithCount(hovered.getMaxStackSize()));
                    return true;
                }
            }
            if (mc.options.keyInventory.matches(keyCode, scanCode)) {
                onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        state().save();
        super.onClose();
    }

    /* ----------------------------------------------------- drawing ----------------------------------------------------- */

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hovered = ItemStack.EMPTY;
        hoveredInGrid = false;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!carried.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            graphics.pose().popPose();
        } else if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, Screen.getTooltipFromItem(mc, hovered), hovered.getTooltipImage(), hovered,
                    mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final ItemStack BARRIER = new ItemStack(net.minecraft.world.item.Items.BARRIER);
    private static final ItemStack CHEST = new ItemStack(net.minecraft.world.item.Items.CHEST);

    /** A slot-shaped button with an item for its icon; its tooltip while hovered and nothing is held. */
    private void iconButton(GuiGraphics g, GUIContext context, int x, int y, ItemStack icon, String tip) {
        boolean over = inside(context, x, y);
        g.flush();
        drawSlot(g, x, y, over);
        drawItem(g, icon, x, y);
        g.flush();
        if (over && carried.isEmpty()) {
            hovered = ItemStack.EMPTY;
            g.renderComponentTooltip(font, List.of(Component.translatable(tip)), context.mouseX, context.mouseY);
        }
    }

    /** Curios' screen, as its button on vanilla's creative inventory opens it; the held stack goes along. */
    private void openCurios() {
        ItemStack held = carried;
        carried = ItemStack.EMPTY;
        com.roll_54.roll_mod.compat.curios.CuriosClientAccess.openCurios(held);
    }

    private void clearHotbar() {
        for (int i = 0; i < 9; i++) setSlot(HOTBAR_FIRST + i, ItemStack.EMPTY);
    }

    /** Back to the survival inventory tab: armor, offhand and the whole inventory. */
    private void showInventoryTab() {
        search.setText("", false);
        setTab(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY));
    }

    /** One slot: vanilla grey bevel under the grey look, the theme's colours otherwise. */
    private static void drawSlot(GuiGraphics g, int x, int y, boolean highlight) {
        UiTheme.Palette p = UiThemeState.get().palette();
        boolean grey = greyLook() || p == null;
        int dark = grey ? 0xFF373737 : p.line();
        int light = grey ? 0xFFFFFFFF : p.line();
        int inner = grey ? 0xFF8B8B8B : p.raised();
        g.fill(x, y, x + SLOT - 1, y + SLOT - 1, dark);
        g.fill(x + 1, y + 1, x + SLOT, y + SLOT, light);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, inner);
        if (highlight) g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x80FFFFFF);
    }

    private void drawItem(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        g.renderItem(stack, x + 1, y + 1);
        g.renderItemDecorations(font, stack, x + 1, y + 1);
    }

    private static boolean inside(GUIContext c, int x, int y) {
        return c.mouseX >= x && c.mouseX < x + SLOT && c.mouseY >= y && c.mouseY < y + SLOT;
    }

    /* ------------------------------------------------------ pieces ------------------------------------------------------ */

    /** A tab's icon; the selected one framed. */
    private final class TabIcon extends UIElement {
        private final CreativeModeTab icon;

        TabIcon(CreativeModeTab tab) {
            this.icon = tab;
            layout(l -> l.width(TAB).height(TAB).flexShrink(0));
            style(s -> s.tooltips(tabName(tab)));
            addEventListener(UIEvents.MOUSE_DOWN, e -> {
                if (e.button != 0) return;
                search.setText("", false);
                setTab(icon);
            });
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            int x = Math.round(getPositionX()), y = Math.round(getPositionY());
            GuiGraphics g = context.graphics;
            g.flush();
            UiTheme.Palette p = UiThemeState.get().palette();
            int accent = p != null && !greyLook() ? p.accent() : 0xFFFFFFFF;
            g.fill(x, y, x + TAB, y + TAB, icon == tab ? accent : 0xFF373737);
            g.fill(x + 1, y + 1, x + TAB - 1, y + TAB - 1, icon == tab ? 0xFFC6C6C6 : (isHover() ? 0xFFA0A0A0 : 0xFF8B8B8B));
            g.renderItem(icon.getIconItem(), x + 3, y + 3);
            g.flush();
        }
    }

    /** The tab's items (or search results), as many columns as fit; the mouse wheel scrolls rows. */
    private final class ItemGrid extends UIElement {
        int scrollRow;
        /** The scrollbar is being dragged: it follows the mouse while the left button is held. */
        boolean draggingBar;

        ItemGrid() {
            addEventListener(UIEvents.MOUSE_WHEEL, e -> {
                scrollRow = Math.max(0, Math.min(maxRow(), scrollRow - (int) Math.signum(e.deltaY)));
                e.stopPropagation();
            });
            addEventListener(UIEvents.MOUSE_DOWN, this::onClick);
        }

        int columns() {
            // Saved hotbars are rows of nine.
            if (tab != null && tab.getType() == CreativeModeTab.Type.HOTBAR && (search.getValue() == null || search.getValue().isBlank())) return 9;
            return Math.max(1, (int) ((getSizeWidth() - 6) / SLOT));
        }

        int rows() {
            return Math.max(1, (int) (getSizeHeight() / SLOT));
        }

        int maxRow() {
            int total = (items.size() + columns() - 1) / columns();
            return Math.max(0, total - rows());
        }

        private void onClick(UIEvent e) {
            // The scrollbar column, right of the slots: jump there and drag from it.
            if (e.button == 0 && maxRow() > 0 && e.x >= getPositionX() + columns() * SLOT) {
                draggingBar = true;
                scrollTo(e.y);
                e.stopPropagation();
                return;
            }
            int col = (int) ((e.x - getPositionX()) / SLOT);
            int row = (int) ((e.y - getPositionY()) / SLOT);
            if (col < 0 || col >= columns() || row < 0 || row >= rows()) return;
            int index = (scrollRow + row) * columns() + col;
            clickGrid(index < items.size() ? items.get(index) : ItemStack.EMPTY, e.button);
        }

        /** Scrolls so the thumb's middle sits at {@code mouseY}, as far as it goes. */
        private void scrollTo(double mouseY) {
            int max = maxRow();
            if (max <= 0) return;
            int rows = rows();
            float barH = rows * SLOT;
            float thumbH = Math.max(8, barH * rows / (rows + max));
            float t = (float) ((mouseY - getPositionY() - thumbH / 2) / Math.max(1f, barH - thumbH));
            scrollRow = Math.round(Math.max(0f, Math.min(1f, t)) * max);
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            if (draggingBar) {
                if (Minecraft.getInstance().mouseHandler.isLeftPressed()) scrollTo(context.mouseY);
                else draggingBar = false;
            }
            int x0 = Math.round(getPositionX()), y0 = Math.round(getPositionY());
            int cols = columns(), rows = rows();
            scrollRow = Math.min(scrollRow, maxRow());
            GuiGraphics g = context.graphics;
            g.flush();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = x0 + c * SLOT, y = y0 + r * SLOT;
                    int index = (scrollRow + r) * cols + c;
                    ItemStack stack = index < items.size() ? items.get(index) : ItemStack.EMPTY;
                    boolean over = inside(context, x, y);
                    drawSlot(g, x, y, over);
                    drawItem(g, stack, x, y);
                    if (over && !stack.isEmpty()) {
                        hovered = stack;
                        hoveredInGrid = true;
                    }
                }
            }
            // A thin scrollbar on the right while there is more than fits.
            int max = maxRow();
            if (max > 0) {
                int barX = x0 + cols * SLOT + 2, barH = rows * SLOT;
                int thumbH = Math.max(8, barH * rows / (rows + max));
                int thumbY = y0 + (barH - thumbH) * scrollRow / max;
                g.fill(barX, y0, barX + 3, y0 + barH, 0x60000000);
                g.fill(barX, thumbY, barX + 3, thumbY + thumbH, draggingBar ? 0xFFFFFFFF : 0xFFC6C6C6);
            }
            g.flush();
        }
    }

    /** The hotbar under the grid; after it the destroy slot, "clear hotbar" and "inventory tab". */
    private final class HotbarRow extends UIElement {
        HotbarRow() {
            addEventListener(UIEvents.MOUSE_DOWN, e -> {
                int x = (int) (e.x - getPositionX()), y = (int) (e.y - getPositionY());
                if (y < 1 || y >= 1 + SLOT) return;
                if (x >= 0 && x < 9 * SLOT) clickSlot(HOTBAR_FIRST + x / SLOT, e.button);
                else if (x >= destroyX() && x < destroyX() + SLOT) clickDestroy();
                else if (x >= clearX() && x < clearX() + SLOT && e.button == 0) clearHotbar();
                else if (x >= inventoryX() && x < inventoryX() + SLOT && e.button == 0) showInventoryTab();
                else if (curios && x >= curiosX() && x < curiosX() + SLOT && e.button == 0) openCurios();
            });
        }

        int destroyX() {
            return 9 * SLOT + 8;
        }

        int clearX() {
            return destroyX() + SLOT + 2;
        }

        int inventoryX() {
            return clearX() + SLOT + 2;
        }

        /** Curios' button, replacing the one Curios puts on vanilla's creative inventory. */
        final boolean curios = com.roll_54.roll_mod.compat.curios.CuriosCompat.loaded();

        int curiosX() {
            return inventoryX() + SLOT + 2;
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            int x0 = Math.round(getPositionX()), y0 = Math.round(getPositionY()) + 1;
            GuiGraphics g = context.graphics;
            g.flush();
            for (int i = 0; i < 9; i++) {
                int x = x0 + i * SLOT;
                ItemStack stack = mc.player.inventoryMenu.getSlot(HOTBAR_FIRST + i).getItem();
                boolean over = inside(context, x, y0);
                drawSlot(g, x, y0, over);
                drawItem(g, stack, x, y0);
                if (over && !stack.isEmpty()) hovered = stack;
            }
            int dx = x0 + destroyX();
            drawSlot(g, dx, y0, inside(context, dx, y0));
            g.flush();
            // The destroy slot's mark.
            Icons.DELETE.draw(g, context.mouseX, context.mouseY, dx + 2, y0 + 2, 14, 14, context.partialTick);
            iconButton(g, context, x0 + clearX(), y0, BARRIER, "gui.roll_mod.creative.clear_hotbar");
            iconButton(g, context, x0 + inventoryX(), y0, CHEST, "gui.roll_mod.creative.inventory_tab");
            if (curios) {
                int cx = x0 + curiosX();
                boolean over = inside(context, cx, y0);
                g.flush();
                drawSlot(g, cx, y0, over);
                com.roll_54.roll_mod.compat.curios.CuriosClientAccess.drawButton(g, cx + 1, y0 + 1, 16, 16, over);
                g.flush();
                if (over && carried.isEmpty()) {
                    hovered = ItemStack.EMPTY;
                    g.renderComponentTooltip(font, List.of(Component.translatable("gui.roll_mod.creative.curios")),
                            context.mouseX, context.mouseY);
                }
            }
            if (inside(context, dx, y0) && carried.isEmpty()) {
                hovered = ItemStack.EMPTY;
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.roll_mod.creative.destroy"),
                        Component.translatable("gui.roll_mod.creative.destroy.shift")), context.mouseX, context.mouseY);
            }
        }
    }

    /** The survival inventory: armor, the player, offhand, the 27 main slots. */
    private final class InventoryView extends UIElement {
        /** Menu slot id at each local position, filled in as it draws. */
        private final List<int[]> slots = new ArrayList<>();

        InventoryView() {
            addEventListener(UIEvents.MOUSE_DOWN, e -> {
                int x = (int) (e.x - getPositionX()), y = (int) (e.y - getPositionY());
                for (int[] s : slots) {
                    if (x >= s[1] && x < s[1] + SLOT && y >= s[2] && y < s[2] + SLOT) {
                        clickSlot(s[0], e.button);
                        return;
                    }
                }
            });
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            int x0 = Math.round(getPositionX()), y0 = Math.round(getPositionY());
            GuiGraphics g = context.graphics;
            slots.clear();
            for (int i = 0; i < 4; i++) slots.add(new int[]{5 + i, 0, i * SLOT});
            slots.add(new int[]{OFFHAND, 76, 3 * SLOT});
            for (int id = 9; id < HOTBAR_FIRST; id++) {
                int i = id - 9;
                slots.add(new int[]{id, (i % 9) * SLOT, 4 * SLOT + 6 + (i / 9) * SLOT});
            }
            g.flush();
            for (int[] s : slots) {
                int x = x0 + s[1], y = y0 + s[2];
                ItemStack stack = mc.player.inventoryMenu.getSlot(s[0]).getItem();
                boolean over = inside(context, x, y);
                drawSlot(g, x, y, over);
                if (stack.isEmpty()) {
                    // The armor and offhand outlines, as vanilla shows them on empty slots.
                    var icon = mc.player.inventoryMenu.getSlot(s[0]).getNoItemIcon();
                    if (icon != null) {
                        g.blit(x + 1, y + 1, 0, 16, 16, mc.getTextureAtlas(icon.getFirst()).apply(icon.getSecond()));
                    }
                }
                drawItem(g, stack, x, y);
                if (over && !stack.isEmpty()) hovered = stack;
            }
            // The player, between the armor and the offhand.
            g.fill(x0 + SLOT + 2, y0, x0 + 74, y0 + 4 * SLOT - 1, 0xFF000000);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x0 + SLOT + 2, y0, x0 + 74, y0 + 4 * SLOT - 1,
                    30, 0.0625f, context.mouseX, context.mouseY, mc.player);
            g.flush();
        }
    }
}
