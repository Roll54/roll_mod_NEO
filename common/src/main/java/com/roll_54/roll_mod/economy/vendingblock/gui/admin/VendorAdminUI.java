package com.roll_54.roll_mod.economy.vendingblock.gui.admin;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.network.BuyModePacket;
import com.roll_54.roll_mod.economy.vendingblock.network.DiscardsPaymentPacket;
import com.roll_54.roll_mod.economy.vendingblock.network.InfiniteInventoryPacket;
import com.roll_54.roll_mod.economy.vendingblock.network.OwnerChangePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Admin/settings UI (opened with the Vendor Key), rebuilt on LdLib2: owner text field, infinite and
 * discard toggles, and the product/facade filter slots. The legacy unused slot 10 is dropped.
 */
public final class VendorAdminUI {

  private VendorAdminUI() {}

  public static ModularUI build(VendorBlockEntity be, Player player) {
    UIElement root = VendorUIHelper.panel(176, 166);

    Label title = new Label();
    title.setText(Component.translatable("menu.roll_mod.admin.settings"));
    root.addChild(VendorUIHelper.abs(title, 8, 6, 160, 10));

    // owner name field
    Label ownerLabel = new Label();
    ownerLabel.setText(Component.translatable("menu.roll_mod.admin.owner"));
    root.addChild(VendorUIHelper.abs(ownerLabel, 8, 20, 60, 10));

    TextField owner = new TextField();
    owner.setText(be.getOwnerUser() == null ? "" : be.getOwnerUser());
    owner.setTextResponder(
        value ->
            PacketDistributor.sendToServer(new OwnerChangePacket(be.getBlockPos(), value.trim())));
    root.addChild(VendorUIHelper.abs(owner, 8, 32, 110, 18));

    // infinite-inventory toggle
    Label infiniteLabel = new Label();
    infiniteLabel.setText(Component.translatable("menu.roll_mod.tooltip.infinite"));
    root.addChild(VendorUIHelper.abs(infiniteLabel, 8, 56, 120, 10));
    Switch infinite = new Switch();
    infinite.setOn(be.isInfinite());
    infinite.setOnSwitchChanged(
        on -> PacketDistributor.sendToServer(new InfiniteInventoryPacket(be.getBlockPos(), on)));
    root.addChild(VendorUIHelper.abs(infinite, 130, 54, 28, 14));

    // discard-payment toggle
    Label discardLabel = new Label();
    discardLabel.setText(Component.translatable("menu.roll_mod.tooltip.discard"));
    root.addChild(VendorUIHelper.abs(discardLabel, 8, 72, 120, 10));
    Switch discard = new Switch();
    discard.setOn(be.isDiscarding());
    discard.setOnSwitchChanged(
        on -> PacketDistributor.sendToServer(new DiscardsPaymentPacket(be.getBlockPos(), on)));
    root.addChild(VendorUIHelper.abs(discard, 130, 70, 28, 14));

    // buy-mode toggle (vendor buys from players instead of selling to them)
    Label buyModeLabel = new Label();
    buyModeLabel.setText(Component.translatable("menu.roll_mod.tooltip.buymode"));
    root.addChild(VendorUIHelper.abs(buyModeLabel, 8, 88, 120, 10));
    Switch buyMode = new Switch();
    buyMode.setOn(be.isBuyMode());
    buyMode.setOnSwitchChanged(
        on -> PacketDistributor.sendToServer(new BuyModePacket(be.getBlockPos(), on)));
    root.addChild(VendorUIHelper.abs(buyMode, 130, 86, 28, 14));

    // facade (block skin)
    Label facadeLabel = new Label();
    facadeLabel.setText(Component.translatable("menu.roll_mod.tooltip.facade"));
    root.addChild(VendorUIHelper.abs(facadeLabel, 8, 106, 60, 10));
    root.addChild(VendorUIHelper.facadeSlot(be, 70, 104));

    return ModularUI.of(UI.of(root), player);
  }
}
