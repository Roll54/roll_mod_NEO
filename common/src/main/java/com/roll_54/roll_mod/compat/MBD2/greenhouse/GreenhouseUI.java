package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;

/**
 * The EV Greenhouse controller's screen: the seed slot and what the machine is doing, nothing else.
 *
 * <pre>
 *   +----------------------------------------------+
 *   |  <machine name>                              |
 *   |  +------+   status                           |
 *   |  | SEED |   stage n / m                      |
 *   |  +------+   EU/t now / max                   |
 *   |             upgrades                         |
 *   |             [=========== stage ===========]  |
 *   |  [ player inventory + hotbar ]               |
 *   +----------------------------------------------+
 * </pre>
 *
 * <p>Bound by id afterwards, as {@code CropManagerUI} explains: the slot by MBD2's item-slot binder
 * ({@code ui:<trait>_0}), the rest by {@link GrowerTraitDefinition#initTraitUI}.
 */
public final class GreenhouseUI {

    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 170;

    private static final int SEED_SIZE = VendorUIHelper.SLOT_SIZE * 2;
    private static final int SEED_X = 10;
    private static final int SEED_Y = 26;
    private static final int INFO_X = 56;
    private static final int INFO_Y = 20;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 86;

    private GreenhouseUI() {}

    public static UITemplate template(ItemSlotCapabilityTraitDefinition seed, GrowerTraitDefinition grower) {
        UIElement root = VendorUIHelper.styledPanel(PANEL_WIDTH, PANEL_HEIGHT);
        root.layout(l -> l.paddingAll(0));

        Label title = new Label();
        title.setText("EV Greenhouse");
        title.setId("ui:machine_name");
        root.addChild(VendorUIHelper.abs(title, 8, 6, 160, 10));

        ItemSlot seedSlot = new ItemSlot();
        seedSlot.setId(seed.uiId() + "_0");
        root.addChild(VendorUIHelper.abs(seedSlot, SEED_X, SEED_Y, SEED_SIZE, SEED_SIZE));

        UIElement info = new UIElement();
        grower.createTraitUITemplate(info);
        root.addChild(VendorUIHelper.abs(info, INFO_X, INFO_Y, GrowerTraitDefinition.WIDTH, GrowerTraitDefinition.HEIGHT));

        root.addChild(VendorUIHelper.playerInventory(INVENTORY_X, INVENTORY_Y));
        return UITemplate.of(root, StylesheetManager.MC_MERGED, StylesheetManager.GDP_MERGED);
    }
}
