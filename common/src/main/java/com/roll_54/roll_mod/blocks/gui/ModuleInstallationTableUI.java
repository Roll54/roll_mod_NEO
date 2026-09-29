package com.roll_54.roll_mod.blocks.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.ModularTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Module Installation Table's GUI: the drill slot, and one row of module slots that are
 * windows into the drill's component (see {@link DrillModuleSlotHandler}).
 *
 * <p><b>Built on both sides.</b> {@code createUI} runs on the dedicated server too, and LdLib2
 * matches slots positionally, so all 7 module slots always exist; the ones a lesser drill does
 * not offer just refuse items server-side, and the TICK repaint greys them as a courtesy.
 */
public final class ModuleInstallationTableUI {

    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 190;

    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int TITLE_WIDTH = 160;
    private static final int LINE_HEIGHT = 10;

    private static final int SLOT_SIZE = 18;
    private static final int DRILL_SLOT_X = 26;
    private static final int DRILL_SLOT_Y = 28;

    private static final int STATUS_X = 52;
    private static final int STATUS_Y = 28;
    private static final int STATUS_WIDTH = 116;

    private static final int MODULE_ROW_X = 19;
    private static final int MODULE_ROW_Y = 62;
    private static final int MODULE_SLOT_STEP = 20;

    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 108;

    private static final int TEXT_PRIMARY = 0xFFE0E0E0;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    /** The plate a slot the current drill does not offer wears. */
    private static final ColorRectTexture LOCKED_SLOT = new ColorRectTexture(0x60000000);

    private ModuleInstallationTableUI() {
    }

    public static ModularUI build(ModuleInstallationTableBlockEntity table, Player player) {
        UIElement root = VendorUIHelper.styledPanel(PANEL_WIDTH, PANEL_HEIGHT);
        root.layout(l -> l.paddingAll(0));

        Label title = new Label();
        title.setText(Component.translatable("block.roll_mod.module_installation_table"));
        title.textStyle(text -> text.textColor(TEXT_PRIMARY).textShadow(true)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        root.addChild(VendorUIHelper.abs(title, TITLE_X, TITLE_Y, TITLE_WIDTH, LINE_HEIGHT));

        ItemSlot drillSlot = new ItemSlot().bind(new SlotItemHandler(table.inventory, 0, 0, 0));
        drillSlot.style(s -> s.backgroundTexture(Sprites.RECT_DARK));
        root.addChild(VendorUIHelper.abs(drillSlot, DRILL_SLOT_X, DRILL_SLOT_Y, SLOT_SIZE, SLOT_SIZE));

        statusLine(root, table);
        moduleRow(root, table);

        root.addChild(VendorUIHelper.playerInventory(INVENTORY_X, INVENTORY_Y));

        return ModularUI.of(
                UI.of(root, StylesheetManager.MC_MERGED, StylesheetManager.GDP_MERGED), player);
    }

    /**
     * "n / m modules" with "complexity c / max" under it, or the hint to put a tool down. Painted once at build and then every
     * tick; the block entity re-syncs on every drill change (see {@code drillChanged}), so the
     * client copy this reads is fresh.
     */
    private static void statusLine(UIElement root, ModuleInstallationTableBlockEntity table) {
        Label status = new Label();
        status.textStyle(text -> text.textColor(TEXT_SECONDARY).textShadow(false)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        // The complexity budget, on the line under it.
        Label complexity = new Label();
        complexity.textStyle(text -> text.textColor(TEXT_SECONDARY).textShadow(false)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        Runnable repaint = () -> {
            ItemStack drill = table.drill();
            if (drill.getItem() instanceof ModularTool item) {
                status.setText(Component.translatable("gui.roll_mod.install_table.slots",
                        DrillModuleHelper.get(drill).modules().size(), item.moduleSlots()));
                int used = DrillModuleHelper.complexity(drill);
                // Red once the budget is spent: that, not the slots, is why modules are refused.
                complexity.setText(Component.translatable("gui.roll_mod.install_table.complexity",
                                used, item.maxComplexity())
                        .withStyle(used >= item.maxComplexity() ? ChatFormatting.RED : ChatFormatting.GRAY));
            } else {
                status.setText(Component.translatable("gui.roll_mod.install_table.empty"));
                complexity.setText(Component.empty());
            }
        };
        repaint.run();
        status.addEventListener(UIEvents.TICK, event -> repaint.run());
        root.addChild(VendorUIHelper.abs(status, STATUS_X, STATUS_Y, STATUS_WIDTH, LINE_HEIGHT));
        root.addChild(VendorUIHelper.abs(complexity, STATUS_X, STATUS_Y + LINE_HEIGHT + 1, STATUS_WIDTH, LINE_HEIGHT));
    }

    private static void moduleRow(UIElement root, ModuleInstallationTableBlockEntity table) {
        DrillModuleSlotHandler handler = new DrillModuleSlotHandler(table);

        for (int i = 0; i < DrillModuleSlotHandler.SLOTS; i++) {
            int index = i;
            ItemSlot slot = new ItemSlot().bind(new SlotItemHandler(handler, index, 0, 0));
            slot.style(s -> s.backgroundTexture(Sprites.RECT_DARK));
            // Courtesy repaint only — the handler is what actually refuses a locked slot.
            slot.addEventListener(UIEvents.TICK, event -> {
                boolean open = table.drill().getItem() instanceof ModularTool item
                        && index < item.moduleSlots();
                slot.style(s -> s.backgroundTexture(open ? Sprites.RECT_DARK : LOCKED_SLOT));
            });
            root.addChild(VendorUIHelper.abs(slot,
                    MODULE_ROW_X + i * MODULE_SLOT_STEP, MODULE_ROW_Y, SLOT_SIZE, SLOT_SIZE));
        }
    }
}
