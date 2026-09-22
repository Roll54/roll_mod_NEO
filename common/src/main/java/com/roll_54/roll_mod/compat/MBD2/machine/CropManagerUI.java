package com.roll_54.roll_mod.compat.MBD2.machine;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.MBD2.crops.CropHarvesterTraitDefinition;
import com.roll_54.roll_mod.compat.MBD2.energy.MIEnergyTraitDefinition;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * The Crop Manager Mk2's default GUI, built as an LdLib2 element tree.
 *
 * <pre>
 *   +----------------------------------------------+
 *   |  Crop Manager Mk2                            |
 *   |   [][][][][][]           +--------+          |
 *   |   [][][][][][]           |  HERB  |          |
 *   |   [][][][][][]           +--------+          |
 *   |  [========  x/y EU  ========]                |
 *   |  [ player inventory + hotbar ]               |
 *   +----------------------------------------------+
 * </pre>
 *
 * <p>Nothing here binds to a machine. MBD2 does the binding afterwards by <em>id</em>: each trait
 * definition's {@code initTraitUI} re-finds its widgets with {@code selectRegex("^" + uiId() + "_\\d+$")}
 * for slots, or {@code selectId(uiId())} for bars, and {@code MBDMachine.bindMachineUI} picks up
 * {@code ui:machine_name}. That is why the ids are derived from {@link IUIProviderTrait#uiId()} rather
 * than written out — if a layout is later re-authored in the F4 editor it binds the same way.
 */
public final class CropManagerUI {

    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 184;

    private static final int SLOT = VendorUIHelper.SLOT_SIZE;
    private static final int GRID_COLUMNS = 6;
    private static final int GRID_ROWS = 3;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 20;
    /** The herbicide slot is drawn at double size, which is what makes it read as "the big one". */
    private static final int HERBICIDE_SIZE = SLOT * 2;
    private static final int HERBICIDE_X = 130;
    /** Vertically centred on the grid: the grid spans y 20..74, so its midline is 47. */
    private static final int HERBICIDE_Y = GRID_Y + (GRID_ROWS * SLOT - HERBICIDE_SIZE) / 2;
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 82;
    private static final int ENERGY_WIDTH = 160;
    private static final int ENERGY_HEIGHT = 14;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 102;
    private static final int GEAR_SIZE = 16;
    /** An existing 16x16 gear in this mod's assets; nothing new is introduced for it. */
    private static final ResourceLocation GEAR_ICON =
            RollMod.id("textures/gui/hub/teleportation/settings.png");
    private static final int SETTINGS_WIDTH = 104;
    private static final int SETTINGS_HEIGHT = 78;
    /** Above the slots so the panel, which overhangs the main window, is drawn over anything behind it. */
    private static final int SETTINGS_Z = 200;
    /** Light enough to read on the gdp plate, which is nearly black. */
    private static final int TEXT_PRIMARY = 0xFFE0E0E0;

    private CropManagerUI() {}

    public static UITemplate template(ItemSlotCapabilityTraitDefinition output,
                                     ItemSlotCapabilityTraitDefinition herbicide,
                                     MIEnergyTraitDefinition energy,
                                     CropHarvesterTraitDefinition harvester) {
        // styledPanel, not panel: an inline background would outrank the stylesheet and the gdp
        // plate would never be seen. paddingAll(0) because .panel_bg carries padding-all: 5, and
        // every child below is placed at an absolute pixel coordinate measured without it.
        UIElement root = VendorUIHelper.styledPanel(PANEL_WIDTH, PANEL_HEIGHT);
        root.layout(l -> l.paddingAll(0));
        // The settings window hangs off the right edge of the panel; without this it is clipped away.
        root.style(style -> style.overflowVisible(true));

        Label title = new Label();
        title.setText("Crop Manager Mk2");
        title.setId("ui:machine_name");
        title.textStyle(text -> text.textColor(TEXT_PRIMARY).textShadow(true));
        root.addChild(VendorUIHelper.abs(title, 8, 6, 160, 10));

        for (int index = 0; index < GRID_COLUMNS * GRID_ROWS; index++) {
            ItemSlot slot = new ItemSlot();
            slot.setId(slotId(output, index));
            int x = GRID_X + (index % GRID_COLUMNS) * SLOT;
            int y = GRID_Y + (index / GRID_COLUMNS) * SLOT;
            root.addChild(VendorUIHelper.abs(slot, x, y, SLOT, SLOT));
        }

        ItemSlot herbicideSlot = new ItemSlot();
        herbicideSlot.setId(slotId(herbicide, 0));
        root.addChild(VendorUIHelper.abs(herbicideSlot, HERBICIDE_X, HERBICIDE_Y, HERBICIDE_SIZE, HERBICIDE_SIZE));

        // The energy trait supplies its own bar widget (already carrying id `ui:<name>`), so its
        // initTraitUI keeps working and the "x/y EU" label stays live.
        UIElement energyHolder = new UIElement();
        energy.createTraitUITemplate(energyHolder);
        root.addChild(VendorUIHelper.abs(energyHolder, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT));

        root.addChild(VendorUIHelper.playerInventory(INVENTORY_X, INVENTORY_Y));

        root.addChild(settingsPanel(harvester));
        root.addChild(gearButton(harvester));

        // mc first for the controls it styles (gdp defines none of them), gdp last so its panel and
        // border rules win the tie — the order LDLib2's own editor layers this sheet in.
        return UITemplate.of(root, StylesheetManager.MC_MERGED, StylesheetManager.GDP_MERGED);
    }

    /**
     * The collapsed-by-default settings window. It sits <em>outside</em> the main panel, flush against
     * its right edge, which is why the root needs {@code overflowVisible}.
     *
     * <p>Its contents come from {@link CropHarvesterTraitDefinition#createTraitUITemplate(UIElement)}
     * so the hand-built layout and any editor-generated one stay identical, and so the switch ids
     * match what {@code initTraitUI} binds against.
     */
    private static UIElement settingsPanel(CropHarvesterTraitDefinition harvester) {
        UIElement panel = VendorUIHelper.styledPanel(SETTINGS_WIDTH, SETTINGS_HEIGHT);
        panel.setId(harvester.settingId(CropHarvesterTraitDefinition.SETTINGS_PANEL));
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PANEL_WIDTH).top(0).width(SETTINGS_WIDTH).height(SETTINGS_HEIGHT)
                .paddingAll(0));
        panel.style(style -> style.zIndex(SETTINGS_Z));
        // Declarative default; initTraitUI forces it collapsed again on the live UI, since a listener
        // cannot be attached here (see gearButton) and the two must not disagree on open state.
        panel.setDisplay(false);

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.crop_manager.settings"));
        title.textStyle(text -> text.textColor(TEXT_PRIMARY).textShadow(true));
        panel.addChild(VendorUIHelper.abs(title, 4, 4, SETTINGS_WIDTH - 8, 10));

        UIElement rows = new UIElement();
        rows.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(0).top(16).width(SETTINGS_WIDTH).height(SETTINGS_HEIGHT - 16));
        harvester.createTraitUITemplate(rows);
        panel.addChild(rows);

        return panel;
    }

    /**
     * Gear in the title bar. It carries only an id here — the click is attached later, on the live UI,
     * by {@link CropHarvesterTraitDefinition#initTraitUI}, because this template is serialised to NBT
     * and rebuilt before it is ever shown, which drops any listener set at build time.
     */
    private static Button gearButton(CropHarvesterTraitDefinition harvester) {
        Button gear = new Button();
        gear.setId(harvester.settingId(CropHarvesterTraitDefinition.SETTINGS_TOGGLE));
        // Just the 16x16 icon: noText() drops the label slot, and pointing all three button states at
        // the same sprite removes the default frame, so the control is the PNG and nothing else.
        gear.noText();
        SpriteTexture icon = SpriteTexture.of(GEAR_ICON);
        gear.buttonStyle(style -> style.baseTexture(icon).hoverTexture(icon).pressedTexture(icon));
        return VendorUIHelper.abs(gear, PANEL_WIDTH - GEAR_SIZE - 4, 4, GEAR_SIZE, GEAR_SIZE);
    }

    /** {@code ui:<trait name>_<slot index>} — the id MBD2's item-slot binder looks for. */
    private static String slotId(IUIProviderTrait definition, int index) {
        return definition.uiId() + "_" + index;
    }
}
