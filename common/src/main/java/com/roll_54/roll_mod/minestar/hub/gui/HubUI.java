package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.math.Size;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.moderation.ClientPlayerStatusCache;
import com.roll_54.roll_mod.economy.plot.gui.PlotPurchaseUI;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.gui.auction.AuctionUI;
import com.roll_54.roll_mod.minestar.dailytasks.gui.DailyTasksUI;
import com.roll_54.roll_mod.minestar.hub.HubCommand;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.appliedenergistics.yoga.YogaPositionType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The player hub: one screen holding the home panel and every other player-facing screen as a tab.
 *
 * <p>This is now the only way any of these screens open — {@code /rollmod ah}, {@code /rollmod
 * plots} and the daily-tasks key all land here on the right tab, and the old standalone UI ids are
 * gone.
 *
 * <h2>Why the tabs are not LdLib2 {@code TabView}s</h2>
 *
 * {@code TabView} hides unselected tabs with {@code setDisplay(false)}, and a hidden element stops
 * ticking — {@code screenTick}/{@code serverTick} only recurse into displayed children. Every screen
 * embedded here does all of its work in a {@code UIEvents.TICK} listener, and {@link DailyTasksUI}'s
 * server→client bindings only poll while displayed. Worse, tab selection is a client-side click the
 * server never sees, so the server's copy of the tree would sit on the first tab forever and never
 * poll the daily-task suppliers at all.
 *
 * <p>So tabs are hidden by collapsing their slot to {@code height(0)} with overflow clipped, which
 * keeps the whole tree displayed and ticking on both sides. The {@link Tab} elements in the header
 * are used purely for their look and their latched selected state.
 *
 * <p>{@code createUI} runs on <em>both</em> sides and the bindings inside the tabs are positional, so
 * the tree must be built identically everywhere: every tab is always constructed, in a fixed order,
 * with nothing conditional on dist, permission or player state. New tabs append.
 */
public final class HubUI {

    public static final ResourceLocation UI_ID = RollMod.id("hub");

    /**
     * Stamped on the root element, so a client-side screen handler can tell the hub apart from every
     * other LdLib screen the mod opens — see {@link HubReturn}. An element id, not a sync value, so
     * it costs the tree nothing and is identical on both sides.
     */
    public static final String ROOT_ID = "roll_mod_hub";

    /** The box every tab is sized to. Changing it resizes all of them. */
    public static final int CONTENT_W = 460;
    public static final int CONTENT_H = 250;

    private static final int PADDING = 6;
    private static final int TAB_H = 22;
    private static final int TAB_W = 22;
    private static final int TAB_ICON = 16;
    private static final int TAB_GAP = 2;

    /** The leave-the-hub button in the window's top-right corner. Square, like the tab icons. */
    private static final int BACK_BTN = 16;

    /**
     * Where that button sits, measured from the window's own edges rather than from the tab strip.
     * Deliberately inside {@link #PADDING}: the button overhangs the content column and sits against
     * the frame, which is what keeps it clear of the right-most bookmark.
     */
    private static final int BACK_INSET_X = 3;
    private static final int BACK_INSET_Y = 8;

    /** Above the header strip and the content box, both of which reach into its corner. */
    private static final int BACK_Z = 2;

    /**
     * Breathing room at the top of the content box, so an embedded screen does not start flush
     * against the tab strip. The box grows by the same amount rather than eating into it, so no tab
     * loses its bottom 6px.
     */
    private static final int GUI_TOP_INSET = 6;

    /** Where the player's inventory sits under the content box: three rows, a gap, and the hotbar. */
    private static final int INVENTORY_H = 3 * VendorUIHelper.SLOT_SIZE + 5 + VendorUIHelper.SLOT_SIZE;

    /**
     * The whole window. Always this size, inventory shown or not — see {@link #buildRoot}.
     *
     * <p>The inventory is deliberately <em>not</em> in this sum. It is laid out absolutely, pinned
     * to the bottom of the window and drawn over the foot of the content box, so showing it neither
     * grows the window nor pushes anything down. Reserving a row for it instead made the window
     * 68px taller for every tab, to hold something only the auction's create form ever asks for.
     */
    private static final int ROOT_W = CONTENT_W + 2 * PADDING;
    private static final int ROOT_H = 290 + GUI_TOP_INSET;

    private static final int COLOR_CONTENT = 0x40000000;

    /**
     * The bookmarks, as boxes closed on all four sides.
     *
     * <p>Not one of LdLib2's {@code TAB_*} sprites, because none of them can be: every tab sprite in
     * every LdLib atlas is deliberately open on its bottom edge, which is what lets a vanilla
     * bookmark bleed into the panel under it. {@code RECT_THIN} is the closed box LdLib2 itself
     * reaches for when it wants a tab that is a box — see {@code ResourceView tab:host} in its
     * {@code mc.lss}.
     *
     * <p>Built from the atlas rather than taken from {@link MCSprites#RECT_THIN}. {@code
     * MCSprites.init} reflectively replaces every one of that class's texture fields at
     * resource-load time, so holding one in a field of our own is a race with load order and cannot
     * be recoloured; {@link MCSprites#MC} is a plain {@code ResourceLocation} and is left alone.
     */
    private static final int TAB_SPRITE_U = 48;
    private static final int TAB_SPRITE_V = 0;
    private static final int TAB_SPRITE = 16;
    private static final int TAB_SPRITE_BORDER = 3;

    /**
     * How far the box is dimmed. {@code RECT_THIN}'s face is the near-white vanilla panel
     * (0xC6C6C6), which against this window reads as a bright plate; at 0x70 it becomes part of the
     * dark UI. Same dimming the bookmark sprite needed before it.
     */
    private static final int TAB_DIM = 0xFF707070;

    private static IGuiTexture tabBox() {
        return SpriteTexture.of(MCSprites.MC)
                .setSprite(TAB_SPRITE_U, TAB_SPRITE_V, TAB_SPRITE, TAB_SPRITE)
                .setBorder(TAB_SPRITE_BORDER)
                .setColor(TAB_DIM);
    }

    /** Every tab that is not the current one, and the same box under the cursor. */
    private static final IGuiTexture TAB_BASE = tabBox();

    /**
     * The selected bookmark: the same box, taking the content section's own colour, so the tab reads
     * as belonging to the panel it opens onto. That is the whole of the selected cue now — with the
     * box closed at the bottom there is no seam left to leave open.
     *
     * <p>The fill goes <em>over</em> the box, not under it. {@code RECT_THIN}'s face is opaque and
     * {@code Tab} draws its style texture across the element's full bounds, so anything painted
     * beneath would simply be covered.
     */
    private static final IGuiTexture TAB_SELECTED =
            new GuiTextureGroup(tabBox(), new ColorRectTexture(COLOR_CONTENT));

    /**
     * One tab.
     *
     * <p>{@code id} keys the hover name, {@code gui.roll_mod.hub.tab.<id>}. {@code icon} is a
     * drawable rather than a file name, so a tab is free to carry something other than the mod's own
     * artwork — see {@link #icon(String)}. A vanilla item works, through an {@code ItemStackTexture},
     * but that stretches to the element box and draws outside the UI's batch, always on top; every
     * tab wears a PNG now.
     * {@code content} runs on both sides, and receives a callback the tab can use to ask for the
     * player inventory; only the auction does.
     *
     * <p>{@code visible} decides whether the <em>bookmark</em> shows, never whether the content is
     * built — the content always is, for everyone, see the class javadoc. It is read client-side
     * every frame, so it must be a cheap cache read.
     */
    private record HubTab(String id, IGuiTexture icon,
                          BiFunction<Player, Consumer<Boolean>, UIElement> content,
                          BooleanSupplier visible, BooleanSupplier attention) {

        HubTab(String id, IGuiTexture icon, BiFunction<Player, Consumer<Boolean>, UIElement> content) {
            this(id, icon, content, () -> true, () -> false);
        }

        /** Always shown, badged while {@code attention} holds. */
        static HubTab flagged(String id, IGuiTexture icon,
                              BiFunction<Player, Consumer<Boolean>, UIElement> content,
                              BooleanSupplier attention) {
            return new HubTab(id, icon, content, () -> true, attention);
        }

        IGuiTexture texture() {
            return icon;
        }

        Component title() {
            return Component.translatable("gui." + RollMod.MODID + ".hub.tab." + id);
        }
    }

    /**
     * The mod's own artwork for a tab: a 16x16 PNG at
     * {@code assets/roll_mod/textures/gui/hub/<name>.png}. No item, no model, no registry entry —
     * {@link SpriteTexture} blits the file directly.
     *
     * <p>The argument is the file's base name, and {@link RollMod#id} is handed the <em>full</em>
     * resource path: {@code textures/} prefix and {@code .png} suffix included. That is not
     * decoration. A sprite location is a plain file path here, not an atlas key, so
     * {@code roll_mod:gui/hub/home.png} points at a file that does not exist — and a missing sprite
     * draws the pink-and-black checkerboard with nothing in the log to say why. Everywhere else in
     * the mod that reaches for a PNG spells it the same way; see {@code DailyTaskIcon.Texture}.
     *
     * <p>The name is passed rather than derived from the tab id because the two do not line up:
     * the {@code home} tab wears {@code main.png} and the {@code homes} tab wears {@code home.png}.
     */
    private static IGuiTexture icon(String name) {
        return SpriteTexture.of(RollMod.id("textures/gui/hub/" + name + ".png"));
    }

    /**
     * The tabs, in <em>tree</em> order. <b>Append only</b> — the bindings inside these subtrees are
     * addressed by position, so inserting in the middle would shift them. Where they appear in the
     * header is {@link #DISPLAY_ORDER}, which is deliberately a different order.
     */
    private static final List<HubTab> TABS = List.of(
            HubTab.flagged("home", icon("main"), (player, inventory) -> HomeTab.build(player),
                    HubBadge::homePending),
            HubTab.flagged("daily_tasks", icon("daily/daily_tasks"),
                    (player, inventory) -> DailyTasksUI.buildRoot(player), HubBadge::dailyPending),
            // The only tab that wants the inventory, and only while its create form is open.
            // Still an item: hub/ has no auction artwork yet.
            HubTab.flagged("auction", icon("auction"), AuctionUI::buildRoot, HubBadge::auctionPending),
            new HubTab("warps", icon("warp"), (player, inventory) -> WarpTab.build(player)),
            new HubTab("plots", icon("land_market"),
                    (player, inventory) -> PlotPurchaseUI.buildRoot(player)),
            HubTab.flagged("homes", icon("home"),
                    (player, inventory) -> PlayerHomesTab.build(player), HubBadge::homeInvitePending),
            // tp_icon, not the tpa arrow the rows wear: the tab is the whole section — asking,
            // answering and the argument-less trips — and the row button's own icon on the tab read
            // as if the tab were just the one action.
            HubTab.flagged("tpa", icon("teleportation/tp_icon"),
                    (player, inventory) -> TpaTab.build(player), HubBadge::tpaPending),
            // TODO(art): placeholder icon — drop a real textures/gui/hub/moderation.png in and
            // change the name here; nothing else needs to move.
            new HubTab("moderation", icon("warp/admin"),
                    ModerationTab::build,
                    () -> ClientModerationCache.ALLOWED, HubBadge::moderationPending),
            // The envelope carries its own mark while something is unread (letter_new.png), so this
            // bookmark takes no HubBadge on top — it would be the same mark twice.
            new HubTab("letters",
                    IGuiTexture.dynamic(() -> LetterIcons.envelope(ClientPlayerStatusCache.UNREAD_LETTERS > 0)),
                    (player, inventory) -> LettersTab.build(player)));

    /**
     * Header order, as indices into {@link #TABS}. Purely visual: the content slots are still built
     * in {@code TABS} order, so no positional binding moves, and homes can sit second without
     * having to be inserted second. {@link #indexOf} keeps returning tree indices, which is what
     * every caller passes to {@code HubCommand.open}.
     */
    private static final List<Integer> DISPLAY_ORDER = List.of(0, 5, 6, 1, 2, 3, 4, 8, 7);

    static {
        // Appending a tab and forgetting this list would silently hide it; fail at class-load
        // instead, where the cause is obvious.
        if (DISPLAY_ORDER.size() != TABS.size() || Set.copyOf(DISPLAY_ORDER).size() != TABS.size()) {
            throw new IllegalStateException("HubUI.DISPLAY_ORDER must be a permutation of TABS");
        }
    }

    private static final int HOME = indexOf("home");

    /** Index of a tab by id, for the commands that open the hub on a particular one. */
    public static int indexOf(String id) {
        for (int i = 0; i < TABS.size(); i++) {
            if (TABS.get(i).id().equals(id)) return i;
        }
        return 0;
    }

    /**
     * Forces a tab index into range. The hub key sends the client's remembered tab across the wire,
     * so the number arriving at {@code HubCommand.open} is whatever a client chose to send.
     */
    public static int clampTab(int tab) {
        return tab < 0 || tab >= TABS.size() ? 0 : tab;
    }

    /**
     * The tab the hub was on when it last closed, so the hub key can reopen it there.
     *
     * <p>Client state, and only ever written behind a {@link Dist#CLIENT} check — the dedicated
     * server builds this same tree for every player and would otherwise scribble one player's tab
     * over another's. On the client there is one screen and one local player, so one field is the
     * whole of it. Never persisted: "or if none saved, the first tab" is exactly what a fresh
     * {@code 0} means, so a restart starts on the home tab.
     */
    private static int lastTab = 0;

    /** The remembered tab, or {@code 0} if the hub has not been closed yet this session. */
    public static int lastTab() {
        return lastTab;
    }

    private HubUI() {}

    public static ModularUI createUI(Player player) {
        // The size argument sets LdLib's own wrapper element, the one the inspector calls __ROOT__,
        // which is the parent of everything built below. Without a provider here that wrapper is
        // never given a size at all and just hugs its content. The lambda receives the screen size;
        // ignoring it keeps the hub a fixed size at every resolution.
        return ModularUI.of(UI.of(buildRoot(player), screen -> Size.of(ROOT_W, ROOT_H)), player);
    }

    public static UIElement buildRoot(Player player) {
        ServerPlayer server = player instanceof ServerPlayer sp ? sp : null;

        // Which tab the command asked for. Selection is client state, but the request is made on the
        // server, so it crosses as a binding and the client acts on it once, on its first tick.
        SimpleBinding<Integer> requestedTab = DataBindingBuilder
                .intValS2C((java.util.function.Supplier<Integer>)
                        () -> server == null ? 0 : HubCommand.requestedTab(server))
                .initialValue(0).build();

        // Fixed, and sized as though the inventory were always there. A hidden element leaves the
        // layout completely, so without this the whole window shrank by the inventory's height the
        // moment it hid — and grew again when the auction's create form asked for it back.
        UIElement root = new UIElement();
        root.setId(ROOT_ID);
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).paddingAll(PADDING)
                .width(ROOT_W).height(ROOT_H));
        root.style(s -> s.background(MCSprites.BORDER));
        root.addSyncValue(requestedTab.getSyncValue());

        // Flush against the panel rather than sunk into it: the tabs are closed boxes now, so they
        // have no open foot to hide and nothing to be continuous with.
        UIElement header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW).height(TAB_H).gapColumn(TAB_GAP));
        // Unclipped, so a tab can never be trimmed by its own row. It also gates hit-testing, so
        // turning it off would newly clip clicks to the header's content rect.
        //
        // No z-index, deliberately. The header used to be lifted above its later siblings, from back
        // when the tabs hung 4px past it and had to paint over the panel. They no longer do — and the
        // header still spans the full width of the window's top band, which is where the back button
        // now lives, so a z-index here buries that button under a transparent strip: drawn beneath
        // it, and unclickable, because hit-testing follows the same order.
        header.setOverflowVisible(true);

        // A fixed box, so the window does not resize as tabs are switched: collapsed slots
        // contribute no height and the box decides the size for all of them.
        UIElement content = new UIElement();
        content.layout(l -> l.width(CONTENT_W).height(CONTENT_H + GUI_TOP_INSET)
                .paddingTop(GUI_TOP_INSET));
        content.style(s -> s.background(new ColorRectTexture(COLOR_CONTENT)));
        content.setOverflowVisible(false);

        // One inventory for the whole hub, off unless a tab asks for it. Hiding it is what keeps it
        // out of the way: a hidden ItemSlot can never be the hovered element, so LdLib's isHovering
        // override refuses every click on it. The slots themselves stay registered on the menu, so
        // nothing is lost and shift-clicking from elsewhere still finds them.
        // Absolute, so it costs the column nothing: pinned to the bottom of the window, overlapping
        // the foot of the content box. The auction's create form is the only thing that asks for
        // it, and that form ends well above where this starts.
        UIElement inventory = new UIElement();
        inventory.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).bottom(PADDING).width(CONTENT_W).height(INVENTORY_H));
        inventory.addChild(VendorUIHelper.playerInventory(
                (CONTENT_W - 9 * VendorUIHelper.SLOT_SIZE) / 2, 0));
        inventory.setDisplay(false);

        // Each tab's request is remembered rather than obeyed immediately, because a tab that is not
        // on screen must not be able to open the inventory, and because switching away used to
        // discard the request outright: the auction's create form stayed displayed with the
        // inventory hidden, and a hidden ItemSlot refuses every click, so the item picker could
        // never be used again until the form was closed and reopened.
        int[] currentTab = {0};
        boolean[] wantsInventory = new boolean[TABS.size()];

        List<UIElement> slots = new ArrayList<>(TABS.size());
        for (int i = 0; i < TABS.size(); i++) {
            final int index = i;
            UIElement slot = new UIElement();
            slot.layout(l -> l.width(CONTENT_W));
            // Clipped, because a collapsed slot's child keeps its own fixed height.
            slot.setOverflowVisible(false);
            Consumer<Boolean> ask = wanted -> {
                wantsInventory[index] = wanted;
                if (currentTab[0] == index) inventory.setDisplay(wanted);
            };
            slot.addChild(TABS.get(index).content().apply(player, ask));
            slots.add(slot);
            content.addChild(slot);
        }

        // Applies the arriving tab's standing inventory request.
        IntConsumer onSelected = idx -> {
            currentTab[0] = idx;
            inventory.setDisplay(idx >= 0 && idx < wantsInventory.length && wantsInventory[idx]);
        };

        // Indexed by TREE index, filled in display order — so `tabs.get(n)` still lines up with
        // `slots.get(n)` while the header renders in whatever order DISPLAY_ORDER asks for.
        List<Tab> tabs = new ArrayList<>(Collections.nCopies(TABS.size(), (Tab) null));
        List<UIElement> badges = new ArrayList<>(Collections.nCopies(TABS.size(), (UIElement) null));
        for (int displayed : DISPLAY_ORDER) {
            final int index = displayed;
            HubTab spec = TABS.get(index);
            Tab tab = new Tab();
            // Icon only: the caption is dropped and the name moves to the hover tooltip.
            tab.text.setDisplay(false);
            // Square, and exactly the header's height. The 3px padding leaves a 16x16 content box,
            // which is the icon's own size, so the icon centres itself on both axes.
            tab.layout(l -> l.width(TAB_W).height(TAB_H).paddingAll(3));
            // Start hidden if it is not for this viewer, rather than showing until the first TICK
            // below catches up — that gap is long enough to see the bookmark blink.
            if (FMLEnvironment.dist == Dist.CLIENT && !spec.visible().getAsBoolean()) {
                tab.setDisplay(false);
            }

            UIElement icon = new UIElement();
            icon.layout(l -> l.width(TAB_ICON).height(TAB_ICON));
            icon.style(s -> s.background(spec.texture()));
            tab.addChild(icon);

            // The attention mark, over the icon's top-right corner. Absolute, so it costs the
            // bookmark no layout; toggled by the client-only TICK below, which is safe for the same
            // reasons hiding the bookmark itself is — no sync value, no TICK of its own.
            UIElement badge = new UIElement();
            badge.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(1).top(1)
                    .width(HubBadge.SIZE).height(HubBadge.SIZE));
            badge.style(s -> s.background(HubBadge.texture()).zIndex(1));
            badge.setDisplay(false);
            tab.addChild(badge);
            badges.set(index, badge);

            // Tooltips are collected by walking up from the hovered element, so this fires with the
            // cursor over the icon too.
            tab.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    List.of(spec.title()), null, null, ItemStack.EMPTY));
            // Base and hover are the same box; only selection changes the look. See TAB_SELECTED.
            tab.tabStyle(s -> s.baseTexture(TAB_BASE)
                    .hoverTexture(TAB_BASE)
                    .selectedTexture(TAB_SELECTED));
            // Tab carries no click listener of its own; TabView normally supplies one.
            tab.addEventListener(UIEvents.CLICK, e -> select(tabs, slots, onSelected, index));

            tabs.set(index, tab);
            header.addChild(tab);
        }

        // Header first, panel under it. Nothing overlaps any more, so the order is just the order
        // they are read in.
        root.addChildren(header, content);

        root.addChild(inventory);

        // The way out, pinned to the window rather than parked at the end of the tab row — see
        // back(). Last, and lifted by a z-index of its own, so it stays a button on top of the
        // window rather than something the header's band draws over. Added unconditionally and on
        // both sides, like everything else in this tree: only its click behaviour is client-side,
        // and an element that existed on one side only would shift every positional binding after
        // it — see the class javadoc.
        root.addChild(back());

        select(tabs, slots, onSelected, 0);

        // The section's explanation is about the section, so it goes when the screen holding it does.
        // On the window route it is an always-on-top OS window, which would otherwise be left sitting
        // over the game with nothing behind it to explain. REMOVED reaches every element in the tree
        // from ModularUI.onRemoved; HubSection.closeInfo is a no-op off the client, where the server
        // copy of this tree also gets it.
        root.addEventListener(UIEvents.REMOVED, e -> {
            // Remembered on the way out rather than on every selection: select() also runs on the
            // server copy of this tree, and while the hub is open the screen itself is the record of
            // which tab is showing. See lastTab.
            if (FMLEnvironment.dist == Dist.CLIENT) lastTab = currentTab[0];
            HubSection.closeInfo();
        });

        // Honour the tab the command asked for, once, as soon as the value arrives.
        boolean[] applied = {false};
        // Bookmarks whose tab is not for this viewer. setDisplay is safe on these, and only these:
        // a bookmark carries no sync value and no TICK listener, and is not an ancestor of its
        // content slot, which lives under `content`. A hidden flex child also drops its gap, so the
        // row closes up rather than leaving a hole.
        // Client only: the dedicated server has no client caches to ask, and its copy's bookmarks
        // are never drawn.
        root.addEventListener(UIEvents.TICK, e -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            // Being on the home tab is seeing the mute it shows, which clears its mark here and on
            // the inventory button.
            if (currentTab[0] == HOME) ClientPlayerStatusCache.acknowledgeStanding();
            for (int i = 0; i < TABS.size(); i++) {
                Tab tab = tabs.get(i);
                boolean visible = TABS.get(i).visible().getAsBoolean();
                if (tab.isDisplayed() != visible) tab.setDisplay(visible);
                boolean flagged = visible && TABS.get(i).attention().getAsBoolean();
                UIElement badge = badges.get(i);
                if (badge != null && badge.isDisplayed() != flagged) badge.setDisplay(flagged);
                // Standing on a tab that just went away: back to the first one.
                if (!visible && currentTab[0] == i) select(tabs, slots, onSelected, 0);
            }
        });

        root.addEventListener(UIEvents.TICK, e -> {
            if (applied[0]) return;
            Integer wanted = requestedTab.getSyncValue().getValue();
            if (wanted == null) return;
            applied[0] = true;
            if (wanted > 0 && wanted < slots.size()) {
                select(tabs, slots, onSelected, wanted);
            }
        });

        return root;
    }

    /**
     * The button that leaves the hub for the player's inventory, pushed to the far end of the header
     * by an auto left margin and centred in it by auto margins above and below.
     *
     * <p>Does the same thing as the inventory key, and for the same reason: the hub is opened from
     * the inventory, so the inventory is where leaving it should land you. See {@link HubReturn}.
     */
    private static UIElement back() {
        UIElement back = new UIElement();
        // Anchored to the window, not to the tab strip. As a flow child of the header its position
        // was whatever the tabs left over — an auto margin measured from the last bookmark, needing a
        // negative margin to escape the row's edge. Absolute against the root measures from the
        // window itself, so the button holds its corner however many tabs there are and whatever the
        // header does. The inventory is pinned the same way, for the same reason.
        back.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(BACK_INSET_X + 4).top(BACK_INSET_Y).width(BACK_BTN).height(BACK_BTN));
        // Above the header strip it shares the window's top band with, so it draws as a button over
        // the background rather than under it — and, because hit-testing follows the same order, so
        // the click actually reaches it.
        back.style(s -> s.background(icon("main_back")).zIndex(BACK_Z));
        back.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui." + RollMod.MODID + ".hub.back.tip")),
                null, null, ItemStack.EMPTY));
        // The dist check keeps HubReturn — and the client-only screen classes it reaches — off a
        // dedicated server, where this tree is also built. The click itself cannot fire there, but
        // the reference would still resolve.
        back.addEventListener(UIEvents.CLICK, e -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientReturn.toInventory();
        });
        return back;
    }

    /** Isolated holder for the client-only return, as {@code HubSection} does for its window. */
    private static final class ClientReturn {
        static void toInventory() {
            HubReturn.toInventory();
        }
    }

    /**
     * Shows one tab and collapses the rest. Height rather than {@code setDisplay}, deliberately —
     * see the class javadoc: a hidden element stops ticking, which would freeze every embedded
     * screen and starve the daily-tasks bindings on the server.
     */
    private static void select(List<Tab> tabs, List<UIElement> slots,
                               IntConsumer onSelected, int selected) {
        // Whatever explanation was open belonged to the section being left. Unconditional rather than
        // only on a real change: re-selecting the current tab is the one case where leaving it open
        // would be defensible, and it is not worth the state to tell apart.
        HubSection.closeInfo();
        for (int i = 0; i < slots.size(); i++) {
            final boolean shown = i == selected;
            slots.get(i).layout(l -> l.height(shown ? CONTENT_H : 0));
            Tab tab = i < tabs.size() ? tabs.get(i) : null;
            if (tab != null) tab.setSelected(shown);
        }
        // The arriving tab decides whether the inventory is shown, from the request it made last
        // time. Blanket-hiding here instead is what stranded the auction's create form.
        onSelected.accept(selected);
    }
}
