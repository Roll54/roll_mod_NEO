package com.roll_54.roll_mod.economy.vendingblock.gui.buyer;

import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.Position;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.transaction.VendorBlockTransaction;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.network.BuyModePacket;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;

/**
 * Buyer view: a scrollable list of positions. Each row is laid out left→right: item · starcoin ·
 * price · "N left" · [Buy].
 */
public final class VendorBuyerUI {

  private VendorBuyerUI() {}

  private static final int PANEL_WIDTH = 256;
  private static final int ROW_WIDTH = 232;

  public static ModularUI build(VendorBlockEntity be, Player player) {
    // The theme's panel surface (hub settings), not a fixed light-grey plate. Children are placed
    // absolutely, so the theme's own padding is dropped.
    UIElement root = VendorUIHelper.styledPanel(PANEL_WIDTH, 180);
    root.layout(l -> l.paddingAll(0));

    Label title = new Label();
    title.setText(
        Component.translatable(
            be.isBuyMode()
                ? "menu.roll_mod.sell.title"
                : "menu.roll_mod.buy.title"));
    root.addChild(VendorUIHelper.abs(title, 8, 6, 130, 10));

    // No buy-mode switch here: this is the customer's screen, and only the owner (or the vendor
    // key) may flip it — BuyModePacket refuses anyone else. The owner's switch is in the settings
    // menu of VendorTradeUI.

    ScrollerView scroller = new ScrollerView();
    scroller.viewContainer(c -> c.layout(l -> l.flexDirection(YogaFlexDirection.COLUMN)));
    scroller.horizontalScroller(s -> s.setDisplay(false)); // vertical-only; rows are sized to fit
    VendorUIHelper.abs(scroller, 6, 18, PANEL_WIDTH - 12, 154);

    List<Position> positions = be.getPositions();
    for (int i = 0; i < positions.size(); i++) {
      scroller.addScrollViewChild(row(be, player, positions.get(i), i));
    }
    root.addChild(scroller);

    return ModularUI.of(RollUi.of(player, root), player);
  }

  private static UIElement row(VendorBlockEntity be, Player player, Position position, int index) {
    UIElement row = new UIElement();
    row.layout(
        l ->
            l.width(ROW_WIDTH)
                .height(20)
                .flexDirection(YogaFlexDirection.ROW)
                .alignItems(YogaAlign.CENTER)
                .marginBottom(2)
                .paddingLeft(2));

    ItemSlot item = VendorUIHelper.phantomSlot();
    item.setItem(position.displayStack());
    item.layout(l -> l.width(18).height(18));
    row.addChild(item);

    UIElement coin = new UIElement();
    coin.style(s -> s.backgroundTexture(SpriteTexture.of(VendorUIHelper.STARCOIN)));
    coin.layout(l -> l.width(10).height(10).marginLeft(8));
    row.addChild(coin);

    Label price = new Label();
    price.setText(VendorUIHelper.priceText(position.price(), "menu.roll_mod.buy.free"));
    price.layout(l -> l.width(60).height(12).marginLeft(4));
    row.addChild(price);

    // Buy mode deliberately hides the per-position count ("how many you can bring").
    if (!be.isBuyMode()) {
      Label left = new Label();
      left.setText(leftText(be, position));
      left.addEventListener(UIEvents.TICK, e -> left.setText(leftText(be, position)));
      left.layout(l -> l.width(70).height(12).marginLeft(4));
      row.addChild(left);
    }

    Button buy = new Button();
    if (be.isBuyMode()) {
      buy.setText(Component.translatable("menu.roll_mod.sell.button"));
      buy.setOnServerClick(e -> VendorBlockTransaction.sell(be.getLevel(), player, be, index));
    } else {
      buy.setText(Component.translatable("menu.roll_mod.buy.button"));
      buy.setOnServerClick(e -> VendorBlockTransaction.purchase(be.getLevel(), player, be, index));
    }
    buy.layout(l -> l.width(44).height(16).marginLeft(8));
    row.addChild(buy);

    return row;
  }

  private static Component leftText(VendorBlockEntity be, Position position) {
    String amount = be.isInfinite() ? "∞" : String.valueOf(be.purchasesLeft(position));
    return Component.translatable("menu.roll_mod.buy.left", amount);
  }
}
