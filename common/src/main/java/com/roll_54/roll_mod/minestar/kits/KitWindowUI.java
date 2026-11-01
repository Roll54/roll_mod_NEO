package com.roll_54.roll_mod.minestar.kits;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.curios.CuriosCompat.CurioRow;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.hub.gui.KitWindowReturn;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

/**
 * A kit laid out as the player's own inventory screen: armour down the left, the three main rows,
 * the hotbar under a small gap, the offhand after it, then the curio rows. The moderator's editor
 * has their own inventory underneath to drag items from; a player's preview is read-only and has
 * none.
 *
 * <p><b>Built on both sides from the same {@link KitWindowLayout}</b>, as the moderation inventory
 * window is: the server binds the slots to the session's handlers, the client binds the same number
 * of slots, in the same order, to empty placeholders that the menu's sync then fills.
 */
public final class KitWindowUI {

    public static final ResourceLocation UI_ID = RollMod.id("kit_window");

    private static final int WIDTH = 236;
    private static final int PAD = 8;
    private static final int SLOT = 18;
    private static final int LINE = 10;
    private static final int BUTTON_W = 70;
    private static final int BUTTON_H = 14;
    private static final int CONTENT_Y = 38;
    private static final int GROUP_GAP = 4;
    private static final int PLAYER_INV_W = 162;
    private static final int PLAYER_INV_H = 76;

    /** Head to feet, top row down, in the main handler's numbering (see {@link KitSlots}). */
    private static final int[] ARMOUR = {39, 38, 37, 36};

    private static final int TEXT = 0xFFE0E0E0;
    private static final int TEXT_DIM = 0xFFAAAAAA;
    private static final int WARN = 0xFFFFAA00;

    private KitWindowUI() {}

    /** @param session the server's session; {@code null} on the client, whose slots are placeholders. */
    static ModularUI createUI(Player player, KitWindowLayout layout, @Nullable KitWindows.Session session) {
        int contentBottom = contentHeight(layout);
        int inventoryY = contentBottom + 6;
        int height = layout.editable() ? inventoryY + PLAYER_INV_H + 6 : contentBottom + 6;

        UIElement root = VendorUIHelper.styledPanel(WIDTH, height);
        root.layout(l -> l.paddingAll(0));

        header(root, layout);
        slots(root, layout, session);

        if (layout.editable()) root.addChild(VendorUIHelper.playerInventory((WIDTH - PLAYER_INV_W) / 2, inventoryY));
        return ModularUI.of(RollUi.of(player, root), player);
    }

    private static void header(UIElement root, KitWindowLayout layout) {
        Label title = label(LegacyText.display(layout.title().isBlank() ? layout.kit() : layout.title()), TEXT, true);
        root.addChild(VendorUIHelper.abs(title, PAD, 8, WIDTH - PAD * 2 - BUTTON_W - 4, LINE));

        Label hint = label(Component.translatable(layout.editable()
                ? "gui.roll_mod.kit.window.editing" : "gui.roll_mod.kit.window.preview"),
                layout.editable() ? WARN : TEXT_DIM, false);
        root.addChild(VendorUIHelper.abs(hint, PAD, 22, WIDTH - PAD * 2 - BUTTON_W - 4, LINE));

        // Back where the window was opened from: the moderation tab's Kits mode, or the home tab.
        boolean editor = layout.editable();
        Button back = new Button();
        back.setText(Component.translatable(editor ? "gui.roll_mod.kit.editor.back" : "gui.roll_mod.kit.window.back"));
        back.setOnClick(e -> KitWindowReturn.back(editor));
        root.addChild(VendorUIHelper.abs(back, WIDTH - PAD - BUTTON_W, 6, BUTTON_W, BUTTON_H));
    }

    private static void slots(UIElement root, KitWindowLayout layout, @Nullable KitWindows.Session session) {
        IItemHandler main = session != null ? session.main : new ItemStackHandler(KitSlots.MAIN_SLOTS);
        boolean editable = layout.editable();

        int gridX = PAD + SLOT + 4;
        for (int row = 0; row < 4; row++) {
            int y = CONTENT_Y + row * SLOT + (row == 3 ? 4 : 0);
            root.addChild(slot(main, ARMOUR[row], editable, 1, PAD, y));
            int first = row < 3 ? 9 + row * 9 : 0;
            for (int i = 0; i < 9; i++) {
                root.addChild(slot(main, first + i, editable, 64, gridX + i * SLOT, y));
            }
        }
        root.addChild(slot(main, KitSlots.OFFHAND, editable, 64, gridX + 9 * SLOT + 4, CONTENT_Y + 3 * SLOT + 4));

        // Curio rows wrap like words: each is its name over its slots, as wide as it needs.
        int x = PAD;
        int y = CONTENT_Y + 4 * SLOT + 8;
        for (int r = 0; r < layout.curios().size(); r++) {
            CurioRow row = layout.curios().get(r);
            int w = groupWidth(row);
            if (x > PAD && x + w > WIDTH - PAD) {
                x = PAD;
                y += LINE + SLOT + GROUP_GAP;
            }
            Label name = label(Component.translatable("curios.identifier." + row.id()), TEXT_DIM, false);
            root.addChild(VendorUIHelper.abs(name, x, y, w, LINE - 1));
            IItemHandler handler = session != null ? session.curios.get(r) : new ItemStackHandler(row.size());
            for (int i = 0; i < row.size(); i++) {
                root.addChild(slot(handler, i, editable, 1, x + i * SLOT, y + LINE));
            }
            x += w + GROUP_GAP;
        }
    }

    private static int groupWidth(CurioRow row) {
        return Math.max(row.size(), 3) * SLOT;
    }

    /** Where the slots end; the same walk as {@link #slots}, without the slots. */
    private static int contentHeight(KitWindowLayout layout) {
        int y = CONTENT_Y + 4 * SLOT + 8;
        if (layout.curios().isEmpty()) return y;
        int x = PAD;
        for (CurioRow row : layout.curios()) {
            int w = groupWidth(row);
            if (x > PAD && x + w > WIDTH - PAD) {
                x = PAD;
                y += LINE + SLOT + GROUP_GAP;
            }
            x += w + GROUP_GAP;
        }
        return y + LINE + SLOT;
    }

    private static ItemSlot slot(IItemHandler handler, int index, boolean editable, int max, int x, int y) {
        ItemSlot slot = new ItemSlot().bind(new KitSlot(handler, index, editable, max));
        return VendorUIHelper.abs(slot, x, y, SLOT, SLOT);
    }

    private static Label label(Component text, int color, boolean shadow) {
        Label label = new Label();
        // The colour on the text too: a theme's label rule can outrank the label's own, never the component's.
        label.setText(Component.empty().withColor(color & 0xFFFFFF).append(text));
        label.textStyle(t -> t.textColor(color).textShadow(shadow).adaptiveWidth(false).textWrap(TextWrap.HIDE));
        return label;
    }

    /**
     * A kit window slot. A preview locks it on both sides, so the client does not even try a move the
     * server would refuse. {@code max} is the most one slot holds: 1 for armour and curios.
     */
    private static final class KitSlot extends SlotItemHandler {

        private final boolean editable;
        private final int max;

        KitSlot(IItemHandler handler, int index, boolean editable, int max) {
            super(handler, index, 0, 0);
            this.editable = editable;
            this.max = max;
        }

        @Override
        public boolean mayPickup(Player player) {
            return editable && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return editable && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return Math.min(max, super.getMaxStackSize());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(max, Math.min(stack.getMaxStackSize(), getItemHandler().getSlotLimit(getSlotIndex())));
        }
    }
}
