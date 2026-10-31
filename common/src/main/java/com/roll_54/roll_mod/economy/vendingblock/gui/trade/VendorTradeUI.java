package com.roll_54.roll_mod.economy.vendingblock.gui.trade;

import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.Position;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.network.AddPositionPacket;
import com.roll_54.roll_mod.economy.vendingblock.network.BuyModePacket;
import com.roll_54.roll_mod.economy.vendingblock.network.RemovePositionPacket;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;

/**
 * The owner's screen: two pages — the trades (positions) and the inventory (storage) — and a gear
 * button in the corner whose menu switches between them and holds the buy-mode switch. A single
 * player inventory at the bottom is shared by both pages, so items can be grabbed onto the cursor to
 * assign a new position, and moved in and out of storage. The positions list live-refreshes when the
 * position count changes.
 *
 * <p>The buy-mode switch lives here, with the owner, and not on the customer's screen: only the
 * owner or the vendor key may flip it, which {@code BuyModePacket} enforces on the server.
 */
public final class VendorTradeUI {

  private static final int WIDTH = 200;
  private static final int GEAR = 16;
  private static final int MENU_W = 124;
  private static final int MENU_ROW_H = 16;
  /** Over the pages, which the menu drops down across. */
  private static final int MENU_Z = 10;

  private static final int PAGE_TRADES = 0;
  private static final int PAGE_INVENTORY = 1;

  private VendorTradeUI() {}

  public static ModularUI build(VendorBlockEntity be, Player player) {
    // The theme's panel surface (hub settings), not a fixed light-grey plate. Children are placed
    // absolutely, so the theme's own padding is dropped.
    UIElement root = VendorUIHelper.styledPanel(WIDTH, 232);
    root.layout(l -> l.paddingAll(0));

    UIElement positionsPanel = positionsPanel(be);
    UIElement storagePanel = VendorInterfacePanel.build(be);
    VendorUIHelper.abs(positionsPanel, 0, 26, WIDTH, 118);
    VendorUIHelper.abs(storagePanel, 0, 26, WIDTH, 118);
    storagePanel.setDisplay(false);
    int[] page = {PAGE_TRADES};

    // The gear: opens the settings menu below it.
    Button gear = new Button();
    gear.noText().addPreIcon(IGuiTexture.dynamic(() -> Icons.SETTINGS));
    VendorUIHelper.abs(gear, 6, 5, GEAR, GEAR);
    gear.layout(l -> l.paddingAll(1));
    gear.addEventListener(
        UIEvents.HOVER_TOOLTIPS,
        e ->
            e.hoverTooltips =
                new HoverTooltips(
                    List.of(Component.translatable("menu.roll_mod.vendor.settings")),
                    null,
                    null,
                    ItemStack.EMPTY));

    // Which page, and which way the vendor trades — the menu is closed most of the time, so the
    // title is where the state is read.
    Label title = new Label();
    VendorUIHelper.abs(title, 6 + GEAR + 6, 9, WIDTH - GEAR - 18, 10);

    /* ----------------------------------------- settings menu ----------------------------------------- */

    UIElement menu = new UIElement();
    menu.addClass("panel_bg");
    VendorUIHelper.abs(menu, 6, 5 + GEAR + 2, MENU_W, 3 * MENU_ROW_H + 4 * 3);
    menu.layout(l -> l.flexDirection(YogaFlexDirection.COLUMN).paddingAll(3).gapAll(3));
    menu.style(s -> s.zIndex(MENU_Z));
    menu.setDisplay(false);
    // A press on the menu's own background must not reach the page under it.
    menu.addEventListener(UIEvents.MOUSE_DOWN, UIEvent::stopPropagation);

    Runnable showTrades =
        () -> {
          page[0] = PAGE_TRADES;
          positionsPanel.setDisplay(true);
          storagePanel.setDisplay(false);
          menu.setDisplay(false);
        };
    Runnable showInventory =
        () -> {
          page[0] = PAGE_INVENTORY;
          positionsPanel.setDisplay(false);
          storagePanel.setDisplay(true);
          menu.setDisplay(false);
        };


    Button trades = new Button();
    trades.setText(Component.translatable("menu.roll_mod.vendor.menu.trades"));
    trades.layout(l -> l.width(MENU_W - 6).height(MENU_ROW_H));
    trades.setOnClick(e -> showTrades.run());

    Button inventory = new Button();
    inventory.setText(Component.translatable("menu.roll_mod.vendor.menu.inventory"));
    inventory.layout(l -> l.width(MENU_W - 6).height(MENU_ROW_H));
    inventory.setOnClick(e -> showInventory.run());

    UIElement buyRow = new UIElement();
    buyRow.layout(
        l ->
            l.flexDirection(YogaFlexDirection.ROW)
                .alignItems(YogaAlign.CENTER)
                .width(MENU_W - 6)
                .height(MENU_ROW_H));
    Label buyLabel = new Label();
    buyLabel.setText(Component.translatable("menu.roll_mod.tooltip.buymode"));
    buyLabel.layout(l -> l.flexGrow(1).height(10));
    Switch buyMode = new Switch();
    buyMode.setOn(be.isBuyMode());
    buyMode.layout(l -> l.width(28).height(14));
    buyMode.setOnSwitchChanged(
        on -> PacketDistributor.sendToServer(new BuyModePacket(be.getBlockPos(), on)));
    buyRow.addChildren(buyLabel, buyMode);

    menu.addChildren(trades, inventory, buyRow);
    gear.setOnClick(e -> menu.setDisplay(!menu.isDisplayed()));

    // The switch and title follow the block, so a change made elsewhere — the vendor key's admin
    // screen, or the server refusing — shows here too. From the root, which always ticks; the menu
    // stops ticking while it is hidden.
    boolean[] shownBuyMode = {!be.isBuyMode()};
    int[] shownPage = {-1};
    root.addEventListener(
        UIEvents.TICK,
        e -> {
          boolean buying = be.isBuyMode();
          if (buying != shownBuyMode[0] || page[0] != shownPage[0]) {
            shownBuyMode[0] = buying;
            shownPage[0] = page[0];
            if (buyMode.isOn() != buying) buyMode.setOn(buying, false);
            title.setText(
                page[0] == PAGE_INVENTORY
                    ? Component.translatable("menu.roll_mod.vendor.title.inventory")
                    : Component.translatable(
                        buying
                            ? "menu.roll_mod.vendor.title.buying"
                            : "menu.roll_mod.vendor.title.selling"));
          }
        });

    root.addChildren(gear, title, positionsPanel, storagePanel);
    root.addChild(VendorUIHelper.playerInventory(19, 150));
    // Last, so it draws and takes clicks above everything it drops across.
    root.addChild(menu);
    return ModularUI.of(RollUi.of(player, root), player);
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
}
