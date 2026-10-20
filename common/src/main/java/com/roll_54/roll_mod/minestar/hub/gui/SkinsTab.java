package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache;
import com.roll_54.roll_mod.network.packet.skin.SkinActionPacket;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * The cosmetic skins tab: the player in the middle wearing every skin they have chosen, and one
 * tile per slot around them — head and sword on the left, pickaxe and axe on the right. A tile opens
 * a picker of every skin for its slot; owned ones apply, locked ones say how to get them.
 *
 * <p>Like {@link RadioTab}, the server keeps the choice: a pick only sends a {@link SkinActionPacket},
 * and what is shown follows {@link ClientItemSkinCache}, so a refused change never sticks. No sync
 * values; the tree is the same on both sides and only the client's copy ever sends anything.
 *
 * <p>Its bookmark shows only while {@code hub.skinsTab} is on in the roll_mod config.
 */
final class SkinsTab {

    private static final int GAP = 6;
    private static final int TILE_W = 76;
    private static final int TILE_ICON = 36;
    private static final int PICKER_W = 210;
    private static final int PICKER_H = 180;
    private static final int ROW_H = 22;
    private static final int ROW_ICON = 18;
    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_HOVER = 0x50FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    private SkinsTab() {}

    static UIElement build(Player player) {
        boolean client = player.level().isClientSide();

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));

        HubSection.Info section = HubSection.info("skins");
        Picker picker = new Picker(player);
        // The 3D draws land on top of everything, so they stand down while anything covers them —
        // the picker only as the in-hub fallback; as a desktop window it covers nothing here.
        BooleanSupplier covered = () -> section.overlay().isDisplayed() || picker.popup.root().isDisplayed();

        // The tool in the doll's main hand: the one whose skin changed last, the sword until then.
        // Remembered in the client's hub state, so it is still in hand the next time the hub opens.
        SkinCategory[] held = {client ? savedHeld() : SkinCategory.SWORD};

        Map<SkinCategory, Tile> tiles = new EnumMap<>(SkinCategory.class);
        for (SkinCategory slot : SkinCategory.values()) {
            tiles.put(slot, new Tile(slot, covered));
        }
        for (Tile tile : tiles.values()) {
            tile.root.addEventListener(UIEvents.CLICK, e -> picker.open(tile.slot));
        }
        Runnable markHeld = () -> tiles.values().forEach(t -> t.select(t.slot == held[0]));
        markHeld.run();

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1).gapColumn(GAP));

        UIElement frame = new UIElement();
        frame.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        frame.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        SkinDollElement doll = new SkinDollElement(covered, () -> held[0]);
        doll.layout(l -> l.widthPercent(100).heightPercent(100));
        frame.addChild(doll);

        body.addChildren(column(tiles.get(SkinCategory.HELMET), tiles.get(SkinCategory.SWORD)), frame,
                column(tiles.get(SkinCategory.PICKAXE), tiles.get(SkinCategory.AXE)));

        root.addChildren(HubSection.header("skins", section), body, section.overlay(), picker.popup.root());

        int[] shown = {-1};
        // What the last tick saw, to tell which slot a sync changed; null until the first one.
        @SuppressWarnings("unchecked")
        Map<SkinCategory, ResourceLocation>[] before = new Map[1];
        root.addEventListener(UIEvents.TICK, e -> {
            if (!client || shown[0] == ClientItemSkinCache.VERSION) return;
            shown[0] = ClientItemSkinCache.VERSION;
            Map<SkinCategory, ResourceLocation> active = ClientItemSkinCache.activeFor(player.getUUID());
            tiles.values().forEach(t -> t.show(active.get(t.slot)));
            if (before[0] != null) {
                SkinCategory changed = changedTool(before[0], active, held[0]);
                if (changed != held[0]) {
                    held[0] = changed;
                    markHeld.run();
                    HubWindowState state = HubWindowState.get();
                    state.setSplit(HELD_KEY, changed.ordinal());
                    state.save();
                }
            }
            before[0] = active;
            if (picker.popup.isOpen()) picker.rebuild();
        });

        return root;
    }

    /** The client hub state's key for the tool in the doll's hand, stored as its ordinal. */
    private static final String HELD_KEY = "skins.held";

    private static SkinCategory savedHeld() {
        SkinCategory[] all = SkinCategory.values();
        int saved = HubWindowState.get().split(HELD_KEY, SkinCategory.SWORD.ordinal());
        SkinCategory slot = saved >= 0 && saved < all.length ? all[saved] : SkinCategory.SWORD;
        return slot == SkinCategory.HELMET ? SkinCategory.SWORD : slot;
    }

    /**
     * The tool to hold after a sync: a tool slot that gained or switched its skin, else {@code held}
     * — unless {@code held}'s own skin was taken off, which puts the sword back in hand.
     */
    private static SkinCategory changedTool(Map<SkinCategory, ResourceLocation> before,
                                            Map<SkinCategory, ResourceLocation> after, SkinCategory held) {
        for (SkinCategory slot : SkinCategory.values()) {
            if (slot == SkinCategory.HELMET) continue;
            ResourceLocation now = after.get(slot);
            if (now != null && !now.equals(before.get(slot))) return slot;
        }
        return after.containsKey(held) || !before.containsKey(held) ? held : SkinCategory.SWORD;
    }

    private static UIElement column(Tile top, Tile bottom) {
        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(TILE_W).heightPercent(100).flexShrink(0)
                .gapRow(GAP).justifyContent(AlignContent.CENTER));
        column.addChildren(top.root, bottom.root);
        return column;
    }

    private static Component slotName(SkinCategory slot) {
        return Component.translatable("gui.roll_mod.hub.skins.slot." + slot.getSerializedName());
    }

    /** One slot: its picture as the player has it, the slot's name, and the chosen skin's. */
    private static final class Tile {
        final SkinCategory slot;
        final UIElement root = new UIElement();
        private final Label skinName = new Label();

        Tile(SkinCategory slot, BooleanSupplier covered) {
            this.slot = slot;
            root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(3).gapRow(2)
                    .alignItems(AlignItems.CENTER));
            select(false);
            root.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("gui.roll_mod.hub.skins.tile.tip")), null, null, ItemStack.EMPTY));

            SkinDollElement.Icon icon = SkinDollElement.Icon.slot(slot, covered);
            icon.layout(l -> l.width(TILE_ICON).height(TILE_ICON));
            icon.setAllowHitTest(false);

            Label name = label(slotName(slot).copy().withStyle(ChatFormatting.GRAY));
            show(null);
            skinName.layout(l -> l.widthPercent(100).height(10));
            skinName.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.HIDE));
            skinName.setAllowHitTest(false);

            root.addChildren(icon, name, skinName);
        }

        void show(@Nullable ResourceLocation active) {
            ItemSkinDefinition skin = active == null ? null : ItemSkinRegistry.get(active);
            skinName.setText(skin == null
                    ? Component.translatable("gui.roll_mod.hub.skins.none").withStyle(ChatFormatting.DARK_GRAY)
                    : skin.displayName().copy().withStyle(ChatFormatting.AQUA));
        }

        void select(boolean on) {
            root.style(s -> s.background(new ColorRectTexture(on ? COLOR_ROW_SELECTED : COLOR_ROW)));
        }

        private static Label label(Component text) {
            Label label = new Label();
            label.setText(text);
            label.layout(l -> l.widthPercent(100).height(10));
            label.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.HIDE));
            label.setAllowHitTest(false);
            return label;
        }
    }

    /**
     * Every skin for one slot, "None" first, then owned, then locked. An owned one applies and
     * closes; a locked one is only selected, and its unlock text fills the bottom of the window.
     */
    private static final class Picker {
        final HubSection.Floating popup;

        private final Player player;
        private final Label caption = new Label();
        private final ScrollerView list = new ScrollerView();
        private final Label lockedTitle = new Label();
        private final Label lockedHow = new Label();
        private SkinCategory slot = SkinCategory.HELMET;
        @Nullable private ResourceLocation chosenLocked;

        Picker(Player player) {
            this.player = player;
            // A desktop window of its own, centred on the screen and free of the hub's bounds; the
            // in-hub panel, centred in the tab, where the platform gives no second window.
            this.popup = HubSection.popup(Component.translatable("gui.roll_mod.hub.skins.picker"),
                    PICKER_W, PICKER_H, this::fill).windowed();
        }

        void open(SkinCategory slot) {
            this.slot = slot;
            chosenLocked = null;
            rebuild();
            popup.open();
        }

        private void fill(UIElement body) {
            body.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(3));

            caption.layout(l -> l.widthPercent(100).height(10).flexShrink(0));
            caption.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

            list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
            list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                    .verticalScrollDisplay(ScrollDisplay.AUTO)
                    .horizontalScrollDisplay(ScrollDisplay.NEVER));
            list.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
            list.viewPort(v -> v.layout(l -> l.paddingAll(0)));

            for (Label label : List.of(lockedTitle, lockedHow)) {
                // Inset 2px from the left; the title also 2px from the top, which moves the block down.
                label.layout(l -> l.widthPercent(100).flexShrink(0).paddingLeft(2)
                        .paddingTop(label == lockedTitle ? 2 : 0));
                label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
                label.setDisplay(false);
            }

            body.addChildren(caption, list, lockedTitle, lockedHow);
        }

        void rebuild() {
            caption.setText(slotName(slot).copy().withStyle(ChatFormatting.YELLOW));
            list.clearAllScrollViewChildren();
            list.clearLayoutCache();

            Set<ResourceLocation> owned = ClientItemSkinCache.unlocked();
            ResourceLocation active = ClientItemSkinCache.activeFor(player.getUUID()).get(slot);
            List<ItemSkinDefinition> unlocked = new ArrayList<>();
            List<ItemSkinDefinition> locked = new ArrayList<>();
            for (ItemSkinDefinition skin : ItemSkinRegistry.all()) {
                if (skin.category() != slot) continue;
                (owned.contains(skin.skinId()) ? unlocked : locked).add(skin);
            }
            // Unlocked while the window was up — it is no longer the locked one being explained.
            if (chosenLocked != null && owned.contains(chosenLocked)) chosenLocked = null;

            if (unlocked.isEmpty() && locked.isEmpty()) {
                Label none = new Label();
                none.setText(Component.translatable("gui.roll_mod.hub.skins.empty").withStyle(ChatFormatting.GRAY));
                none.layout(l -> l.widthPercent(100));
                none.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
                list.addScrollViewChild(none);
            } else {
                list.addScrollViewChild(row(null, false, active == null));
                for (ItemSkinDefinition skin : unlocked) {
                    list.addScrollViewChild(row(skin, false, skin.skinId().equals(active)));
                }
                for (ItemSkinDefinition skin : locked) {
                    list.addScrollViewChild(row(skin, true, skin.skinId().equals(chosenLocked)));
                }
            }
            showLocked();
        }

        /** {@code skin} null is the "None" row. */
        private UIElement row(@Nullable ItemSkinDefinition skin, boolean isLocked, boolean highlighted) {
            int base = highlighted ? COLOR_ROW_SELECTED : COLOR_ROW;
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).marginBottom(2)
                    .paddingHorizontal(HubSection.ROW_INSET).gapColumn(4).alignItems(AlignItems.CENTER));
            row.style(s -> s.background(new ColorRectTexture(base)));
            row.addEventListener(UIEvents.MOUSE_ENTER, e -> row.style(s -> s.background(new ColorRectTexture(COLOR_ROW_HOVER))));
            row.addEventListener(UIEvents.MOUSE_LEAVE, e -> row.style(s -> s.background(new ColorRectTexture(base))));
            row.addEventListener(UIEvents.CLICK, e -> pick(skin, isLocked));

            UIElement icon = skin == null ? new UIElement() : SkinDollElement.Icon.skin(skin, isLocked, () -> false);
            icon.layout(l -> l.width(ROW_ICON).height(ROW_ICON).flexShrink(0));
            icon.setAllowHitTest(false);

            Component name = skin == null
                    ? Component.translatable("gui.roll_mod.hub.skins.none").withStyle(ChatFormatting.GRAY)
                    : skin.displayName().copy().withStyle(isLocked ? ChatFormatting.DARK_GRAY
                            : highlighted ? ChatFormatting.AQUA : ChatFormatting.WHITE);
            Label label = new Label();
            label.setText(name);
            label.layout(l -> l.flexGrow(1).height(ROW_H));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            label.setAllowHitTest(false);

            row.addChildren(icon, label);
            return row;
        }

        private void pick(@Nullable ItemSkinDefinition skin, boolean isLocked) {
            if (skin != null && isLocked) {
                chosenLocked = skin.skinId();
                rebuild();
                return;
            }
            PacketDistributor.sendToServer(skin == null
                    ? SkinActionPacket.clear(slot)
                    : SkinActionPacket.set(slot, skin.skinId()));
            popup.close();
        }

        private void showLocked() {
            ItemSkinDefinition skin = chosenLocked == null ? null : ItemSkinRegistry.get(chosenLocked);
            lockedTitle.setDisplay(skin != null);
            lockedHow.setDisplay(skin != null);
            if (skin == null) return;
            lockedTitle.setText(Component.translatable("gui.roll_mod.hub.skins.locked", skin.displayName())
                    .withStyle(ChatFormatting.RED));
            // A skin with no unlock text of its own gets the general one.
            String key = skin.translationKey() + ".unlock";
            lockedHow.setText(Component.translatable(Language.getInstance().has(key)
                    ? key : "gui.roll_mod.hub.skins.locked.default").withStyle(ChatFormatting.GRAY));
        }
    }
}
