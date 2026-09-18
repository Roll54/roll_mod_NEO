package com.roll_54.roll_mod.economy.vendingblock.gui.display;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Owner-facing display block UI on LdLib2: a real product slot (slot 0) and a facade filter (slot
 * 1).
 */
public final class DisplayUI {

  private DisplayUI() {}

  public static ModularUI build(DisplayBlockEntity be, Player player) {
    UIElement root = VendorUIHelper.panel(176, 166);

    Label title = new Label();
    title.setText(Component.translatable("menu.roll_mod.display.settings"));
    root.addChild(VendorUIHelper.abs(title, 8, 6, 160, 10));

    // real product slot (slot 0)
    ItemSlot product = new ItemSlot().bind(be.inventory, 0);
    root.addChild(VendorUIHelper.abs(product, 62, 35, 18, 18));

    // facade filter (slot 1)
    root.addChild(VendorUIHelper.displayFacadeSlot(be, 1, 98, 35));

    root.addChild(VendorUIHelper.playerInventory(8, 84));

    return ModularUI.of(UI.of(root), player);
  }
}
