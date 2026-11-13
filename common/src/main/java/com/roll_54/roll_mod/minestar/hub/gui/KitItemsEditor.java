package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.kits.HubKitEditor;
import com.roll_54.roll_mod.minestar.kits.KitSlots;
import com.roll_54.roll_mod.network.packet.KitNamePackets;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

/**
 * The moderation tab's kit items editor, as a window inside the hub (it used to be a screen of its own, which closed
 * and reopened the hub): the kit laid out like a player's inventory — armour, three rows, hotbar, offhand — in real
 * slots, with the hub's own inventory shown under it while it is open. Closing it, by Save or any other way, saves the
 * kit on the server ({@link HubKitEditor}).
 *
 * <p>Built with the tab on both sides, so its slots line up on the menu; the server fills them when a kit is loaded.
 */
final class KitItemsEditor {

    private static final int SLOT = 18;
    private static final int GAP = 4;
    private static final int GRID_W = SLOT + GAP + 9 * SLOT + GAP + SLOT;
    private static final int GRID_H = 4 * SLOT + GAP;
    private static final int W = GRID_W + 24;
    private static final int H = GRID_H + 70;
    /** Head to feet, top row down, in the kit's slot numbering ({@link KitSlots}). */
    private static final int[] ARMOUR = {39, 38, 37, 36};

    final HubSection.Floating popup;
    private final Label caption = new Label();
    private final Consumer<Boolean> inventory;
    private boolean wasOpen;

    KitItemsEditor(Player player, Consumer<Boolean> inventory) {
        this.inventory = inventory;
        IItemHandler handler = HubKitEditor.handler(player);
        popup = HubSection.popup(Component.translatable("gui.roll_mod.hub.moderation.kits.editItems"), W, H, body -> {
            body.layout(l -> l.alignItems(AlignItems.CENTER).gapRow(4));
            caption.layout(l -> l.widthPercent(100).height(10).flexShrink(0));
            caption.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

            UIElement grid = new UIElement();
            grid.layout(l -> l.width(GRID_W).height(GRID_H).flexShrink(0));
            int gridX = SLOT + GAP;
            for (int row = 0; row < 4; row++) {
                int y = row * SLOT + (row == 3 ? GAP : 0);
                grid.addChild(slot(player, handler, ARMOUR[row], 1, 0, y));
                int first = row < 3 ? 9 + row * 9 : 0;
                for (int i = 0; i < 9; i++) grid.addChild(slot(player, handler, first + i, 64, gridX + i * SLOT, y));
            }
            grid.addChild(slot(player, handler, KitSlots.OFFHAND, 64, gridX + 9 * SLOT + GAP, 3 * SLOT + GAP));

            Label hint = new Label();
            hint.setText(Component.translatable("gui.roll_mod.kit.window.editing").withStyle(ChatFormatting.GOLD));
            hint.layout(l -> l.widthPercent(100).flexShrink(0));
            hint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

            Button save = new Button();
            save.setText(Component.translatable("gui.roll_mod.hub.moderation.save"));
            save.layout(l -> l.width(80).height(14).flexShrink(0));
            HubSection.centreCaption(save);
            save.setOnClick(e -> close());
            body.addChildren(caption, grid, hint, save);
        }).noCloseButton();
        // Saves however it closes: Save, a tab change, another window opening.
        popup.root().addEventListener(UIEvents.TICK, e -> sync());
    }

    private static ItemSlot slot(Player player, IItemHandler handler, int index, int max, int x, int y) {
        ItemSlot slot = new ItemSlot().bind(HubKitEditor.slot(player, handler, index, max));
        return VendorUIHelper.abs(slot, x, y, SLOT, SLOT);
    }

    private void close() {
        popup.close();
    }

    /** Opens on {@code kit}; the caller has asked the server to load it. */
    void open(String kit) {
        caption.setText(Component.literal(kit).withStyle(ChatFormatting.BOLD));
        HubKitEditor.clientOpen = true;
        popup.open();
        sync();
    }

    /** Called by the tab every frame as well, since a closed window gets no ticks of its own. */
    void sync() {
        boolean open = popup.isOpen();
        if (open == wasOpen) return;
        wasOpen = open;
        inventory.accept(open);
        if (!open) {
            HubKitEditor.clientOpen = false;
            PacketDistributor.sendToServer(new KitNamePackets.Edit(KitNamePackets.Edit.Action.ITEMS_SAVE, "", 0L));
        }
    }
}
