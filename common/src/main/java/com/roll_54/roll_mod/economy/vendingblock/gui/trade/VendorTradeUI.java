package com.roll_54.roll_mod.economy.vendingblock.gui.trade;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.Position;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorStorage;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.gui.components.BigSlotItemHandler;
import com.roll_54.roll_mod.economy.vendingblock.network.AddPositionPacket;
import com.roll_54.roll_mod.economy.vendingblock.network.RemovePositionPacket;
import com.roll_54.roll_mod.economy.vendingblock.network.StorageLockPacket;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;

/**
 * Owner hub: two buttons ("See positions" / "Storage") toggle two pre-built panels via {@code
 * setDisplay}. A single player inventory at the bottom is shared by both panels (so items can be
 * grabbed onto the cursor to assign a new position, and moved in/out of storage). The positions
 * list live-refreshes when the position count changes.
 */
public final class VendorTradeUI {

  private VendorTradeUI() {}

  public static ModularUI build(VendorBlockEntity be, Player player) {
    UIElement root = VendorUIHelper.panel(200, 232);

    UIElement positionsPanel = positionsPanel(be);
    UIElement storagePanel = storagePanel(be);
    VendorUIHelper.abs(positionsPanel, 0, 26, 200, 118);
    VendorUIHelper.abs(storagePanel, 0, 26, 200, 118);
    storagePanel.setDisplay(false);

    Button positionsBtn = new Button();
    positionsBtn.setText(Component.translatable("menu.roll_mod.positions.button"));
    positionsBtn.setOnClick(
        e -> {
          positionsPanel.setDisplay(true);
          storagePanel.setDisplay(false);
        });
    VendorUIHelper.abs(positionsBtn, 8, 6, 90, 16);

    Button storageBtn = new Button();
    storageBtn.setText(Component.translatable("menu.roll_mod.storage.button"));
    storageBtn.setOnClick(
        e -> {
          positionsPanel.setDisplay(false);
          storagePanel.setDisplay(true);
        });
    VendorUIHelper.abs(storageBtn, 102, 6, 90, 16);

    root.addChildren(positionsBtn, storageBtn, positionsPanel, storagePanel);
    root.addChild(VendorUIHelper.playerInventory(19, 150));
    return ModularUI.of(UI.of(root), player);
  }

  private static UIElement positionsPanel(VendorBlockEntity be) {
    UIElement panel = new UIElement();

    // existing positions, live-refreshed when the count changes
    ScrollerView list = new ScrollerView();
    list.viewContainer(c -> c.layout(l -> l.flexDirection(YogaFlexDirection.COLUMN)));
    list.horizontalScroller(s -> s.setDisplay(false));
    VendorUIHelper.abs(list, 6, 2, 188, 68);
    int[] lastCount = {-1};
    Runnable rebuild =
        () -> {
          list.clearAllScrollViewChildren();
          List<Position> ps = be.getPositions();
          for (int i = 0; i < ps.size(); i++)
            list.addScrollViewChild(positionRow(be, ps.get(i), i));
          lastCount[0] = ps.size();
        };
    rebuild.run();
    list.addEventListener(
        UIEvents.TICK,
        e -> {
          if (be.getPositions().size() != lastCount[0]) rebuild.run();
        });
    panel.addChild(list);

    // create form. The pick slot's value is set either by a manual cursor-click (handler below)
    // or by an XEI/JEI ghost-drag (which writes the slot value directly), so submit reads it
    // straight from the slot rather than a tracked variable.
    ItemSlot pick = VendorUIHelper.phantomSlot();
    pick.addEventListener(
        UIEvents.CLICK,
        e -> {
          AbstractContainerMenu menu =
              pick.getModularUI() == null ? null : pick.getModularUI().getMenu();
          if (menu == null) return;
          ItemStack cursor = menu.getCarried();
          pick.setItem(cursor.isEmpty() ? ItemStack.EMPTY : cursor.copyWithCount(1));
          e.stopPropagation();
        });
    panel.addChild(VendorUIHelper.abs(pick, 6, 78, 18, 18));

    TextField amount = VendorUIHelper.intField(28, 79, 34, 16, "1", t -> {});
    panel.addChild(amount);
    TextField price = VendorUIHelper.intField(66, 79, 46, 16, "0", t -> {});
    panel.addChild(price);

    Button submit = new Button();
    submit.setText(Component.translatable("menu.roll_mod.positions.submit"));
    submit.setOnClick(
        e -> {
          ItemStack item = pick.getValue();
          if (item == null || item.isEmpty()) return;
          int amt = (int) Math.max(1, VendorUIHelper.parseLong(amount.getText(), 1));
          long pr = Math.max(0, VendorUIHelper.parseLong(price.getText(), 0));
          PacketDistributor.sendToServer(
              new AddPositionPacket(be.getBlockPos(), item.copyWithCount(1), amt, pr));
          pick.setItem(ItemStack.EMPTY);
        });
    panel.addChild(VendorUIHelper.abs(submit, 116, 79, 60, 16));

    return panel;
  }

  private static UIElement positionRow(VendorBlockEntity be, Position position, int index) {
    UIElement row = new UIElement();
    row.layout(
        l ->
            l.width(180)
                .height(18)
                .flexDirection(YogaFlexDirection.ROW)
                .alignItems(YogaAlign.CENTER)
                .marginBottom(2));

    ItemSlot item = VendorUIHelper.phantomSlot();
    item.setItem(position.displayStack());
    item.layout(l -> l.width(18).height(18));
    row.addChild(item);

    Label info = new Label();
    info.setText(
        Component.translatable(
            "menu.roll_mod.positions.entry",
            position.amount(),
            com.roll_54.roll_mod.economy.util.EnergyFormatUtils.formatEnergy(position.price())));
    info.layout(l -> l.width(120).height(12).marginLeft(6));
    row.addChild(info);

    Button delete = new Button();
    delete.setText(Component.literal("X"));
    delete.setOnClick(
        e -> PacketDistributor.sendToServer(new RemovePositionPacket(be.getBlockPos(), index)));
    delete.layout(l -> l.width(16).height(16).marginLeft(4));
    row.addChild(delete);

    return row;
  }

  private static final int LOCK_ON_COLOR = 0xFFCC3030;
  private static final int LOCK_OFF_COLOR = 0x90303030;

  private static UIElement storagePanel(VendorBlockEntity be) {
    UIElement panel = new UIElement();

    for (int i = 0; i < VendorStorage.SLOTS; i++) {
      int index = i;
      int x = 8 + (i % 9) * 18;
      int y = 4 + (i / 9) * 18;

      ItemSlot slot = new ItemSlot().bind(new BigSlotItemHandler(be.storage, i, 0, 0));
      panel.addChild(VendorUIHelper.abs(slot, x, y, 18, 18));

      // lock toggle: a small corner indicator (red = locked), on top of the slot
      UIElement lock = new UIElement();
      setLockColor(lock, be.storage.isLocked(i));
      lock.addEventListener(UIEvents.MOUSE_DOWN, UIEvent::stopPropagation);
      lock.addEventListener(
          UIEvents.CLICK,
          e -> {
            boolean newLocked = !be.storage.isLocked(index);
            PacketDistributor.sendToServer(
                new StorageLockPacket(be.getBlockPos(), index, newLocked));
            setLockColor(lock, newLocked);
            e.stopPropagation();
          });
      lock.addEventListener(UIEvents.TICK, e -> setLockColor(lock, be.storage.isLocked(index)));
      panel.addChild(VendorUIHelper.abs(lock, x + 12, y + 1, 5, 5));
    }

    return panel;
  }

  private static void setLockColor(UIElement lock, boolean locked) {
    lock.style(
        s -> s.backgroundTexture(new ColorRectTexture(locked ? LOCK_ON_COLOR : LOCK_OFF_COLOR)));
  }
}
