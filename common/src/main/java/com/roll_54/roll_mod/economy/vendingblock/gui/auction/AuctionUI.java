package com.roll_54.roll_mod.economy.vendingblock.gui.auction;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionData;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionListing;
import com.roll_54.roll_mod.economy.vendingblock.client.ClientAuctionCache;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.hub.gui.HubSection;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.economy.vendingblock.network.AuctionActionPacket;
import com.roll_54.roll_mod.economy.vendingblock.network.CreateListingPacket;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;

/**
 * The {@code /rollmod ah} GUI. A single {@link ModularUI} hosting four sub-panels toggled via
 * {@code setDisplay} (browse grid, listing detail, create form, collection). Data is read from
 * {@link ClientAuctionCache} on the client and {@link AuctionData} on the (logical) server, since
 * {@code createUI} runs on both sides. All mutating actions are addressed by listing UUID through
 * {@link AuctionActionPacket}/{@link CreateListingPacket}, so client-side filtering/pagination
 * never has to match the server's element tree.
 */
public final class AuctionUI {

  // The panel fills the hub's content box. Everything below is derived from it, so re-sizing the
  // hub re-sizes the auction house with it — this screen used to be ~45 loose magic numbers.
  private static final int PANEL_W = HubUI.CONTENT_W;
  private static final int PANEL_H = HubUI.CONTENT_H;

  private static final int EDGE = 8;          // panel-edge inset
  private static final int INSET = EDGE * 2;  // content inset, one step further in
  private static final int HEADER_H = 24;     // title row and its two buttons
  private static final int BODY_H = PANEL_H - HEADER_H;

  private static final int SLOT = VendorUIHelper.SLOT_SIZE;
  private static final int LINE_H = 12;
  private static final int FIELD_H = 16;
  private static final int BTN_H = 18;
  /**
   * Wide enough for the longest sort label — uk_ua's "Скоро завершаться" needs ~100px, and a
   * Button spends 8 of its width on padding and margins. The search field's width is derived
   * from this, so the two always add up to the panel.
   */
  private static final int SORT_W = 120;
  private static final int HEAD_BTN_W = 56;

  /** The I button, and the width the header buttons move left to clear it. */
  private static final int INFO = 12;
  private static final int INFO_SLOT = INFO + 4;

  private static final int GRID_X = 12;
  private static final int GRID_Y = 22;
  private static final int PAGER_H = 24;

  /** The browse grid fills whatever room the panel leaves, rather than a fixed 12x6. */
  private static final int COLS = (PANEL_W - 2 * GRID_X) / SLOT;
  private static final int ROWS = (BODY_H - GRID_Y - PAGER_H) / SLOT;
  private static final int PAGE_SIZE = COLS * ROWS;

  /** A full-width line inside a content panel. */
  private static final int TEXT_W = PANEL_W - 2 * INSET;
  private static final int PAGER_Y = BODY_H - PAGER_H;

  // The create form's rows. It used to be a scatter of literals, which is how the Select item
  // button ended up underneath the bidding switch and the price labels on top of the picker hint —
  // in LdLib2 a later sibling wins both the paint and the click, so overlapping rectangles are not
  // a cosmetic problem. The form only reaches y 150 of the 226 available, so the rows are spread.
  private static final int ROW_ITEM = 8; // picker slot, quantity, Select item, bidding switch
  private static final int ROW_HINT = 32; // what is currently picked
  private static final int ROW_PRICE = 52; // whichever of the two price groups is showing
  private static final int ROW_DAYS = 90; // duration
  // Kept clear of the player inventory, which the hub overlays across the bottom ~70px of the
  // content box while this form is open — the Back button must not end up underneath it.
  private static final int ROW_LIST = 104; // the List button
  private static final int ROW_BACK = 128;

  /** Fits "Вибрати предмет" (~88px) with the Button's own padding on top. */
  private static final int PICK_BTN_W = 110;

  private AuctionUI() {}

  /** The element tree, with no interest in the player inventory. */
  public static UIElement buildRoot(Player player) {
    return buildRoot(player, visible -> {});
  }

  /**
   * The element tree.
   *
   * @param showInventory asks the hub to show or hide the player inventory. Only the create form
   *     needs it: an item is chosen by clicking a stack onto the cursor and then onto the phantom
   *     slot, and a hidden inventory slot cannot be clicked at all — so the form would be unusable
   *     without it, and it is clutter everywhere else.
   */
  public static UIElement buildRoot(Player player, Consumer<Boolean> showInventory) {
    // Sized, but with no background of its own: the hub already paints the window frame and the
    // dark content panel behind every tab, and a panel here only washed over them.
    UIElement root = new UIElement();
    root.layout(l -> l.width(PANEL_W).height(PANEL_H));

    HubSection.Info section = HubSection.info("auction");

    Label title = new Label();
    title.setText(Component.translatable("menu.roll_mod.ah.title"));
    root.addChild(VendorUIHelper.abs(title, EDGE, 6, 200, LINE_H));

    UIElement browse = new UIElement();
    UIElement detail = new UIElement();
    UIElement create = new UIElement();
    UIElement collection = new UIElement();
    VendorUIHelper.abs(browse, 0, HEADER_H, PANEL_W, BODY_H);
    VendorUIHelper.abs(detail, 0, HEADER_H, PANEL_W, BODY_H);
    VendorUIHelper.abs(create, 0, HEADER_H, PANEL_W, BODY_H);
    VendorUIHelper.abs(collection, 0, HEADER_H, PANEL_W, BODY_H);

    IntConsumer select =
        idx -> {
          browse.setDisplay(idx == 0);
          detail.setDisplay(idx == 1);
          create.setDisplay(idx == 2);
          collection.setDisplay(idx == 3);
          // The create form is the only place an item has to be picked up, so it is the only place
          // the inventory is on screen.
          showInventory.accept(idx == 2);
        };

    // Both buttons shift left by INFO_SLOT to clear the I button, which takes the far corner so
    // it sits where every other section's does.
    Button createBtn = new Button();
    createBtn.setText(Component.translatable("menu.roll_mod.ah.create"));
    createBtn.setOnClick(e -> select.accept(2));
    root.addChild(VendorUIHelper.abs(
        createBtn, PANEL_W - EDGE - 2 * HEAD_BTN_W - 4 - INFO_SLOT, 4, HEAD_BTN_W, FIELD_H));

    Button collectionBtn = new Button();
    collectionBtn.setText(Component.translatable("menu.roll_mod.ah.collection"));
    collectionBtn.setOnClick(e -> select.accept(3));
    root.addChild(VendorUIHelper.abs(
        collectionBtn, PANEL_W - EDGE - HEAD_BTN_W - INFO_SLOT, 4, HEAD_BTN_W, FIELD_H));

    root.addChild(VendorUIHelper.abs(section.button(), PANEL_W - EDGE - INFO, 6, INFO, INFO));

    UUID[] selected = {null};
    Runnable[] rebuildDetail = {() -> {}};

    buildBrowse(player, browse, select, selected, rebuildDetail);
    buildDetail(player, detail, select, selected, rebuildDetail);
    buildCreate(player, create, select);
    buildCollection(player, collection, select);

    select.accept(0);

    root.addChildren(browse, detail, create, collection);
    // Last, so it covers whichever of the four panels is on screen.
    root.addChild(section.overlay());
    // The player inventory lives on the hub root, so it is visible from every tab.
    return root;
  }

  /* -------------------------------------------------- browse ----------------------------------------------------- */

  private static void buildBrowse(
      Player player,
      UIElement panel,
      IntConsumer select,
      UUID[] selected,
      Runnable[] rebuildDetail) {
    String[] filter = {""};
    int[] sort = {0};
    int[] page = {0};

    TextField search = new TextField();
    search.setText("");
    search.setTextResponder(
        t -> {
          filter[0] = t;
          page[0] = 0;
        });
    panel.addChild(VendorUIHelper.abs(search, EDGE, 2, PANEL_W - 2 * EDGE - SORT_W - 4, 14));

    Button sortBtn = new Button();
    sortBtn.setText(sortLabel(sort[0]));
    sortBtn.setOnClick(
        e -> {
          sort[0] = (sort[0] + 1) % 3;
          sortBtn.setText(sortLabel(sort[0]));
          page[0] = 0;
        });
    panel.addChild(VendorUIHelper.abs(sortBtn, PANEL_W - EDGE - SORT_W, 2, SORT_W, 14));

    UIElement grid = new UIElement();
    panel.addChild(VendorUIHelper.abs(grid, GRID_X, GRID_Y, COLS * SLOT, ROWS * SLOT));

    Label pageLabel = new Label();
    panel.addChild(VendorUIHelper.abs(pageLabel, GRID_X + 28, PAGER_Y + 4, PANEL_W - 2 * (GRID_X + 28), LINE_H));

    Button prev = new Button();
    prev.setText(Component.literal("<"));
    prev.setOnClick(
        e -> {
          if (page[0] > 0) page[0]--;
        });
    panel.addChild(VendorUIHelper.abs(prev, GRID_X, PAGER_Y + 2, 20, FIELD_H));

    Button next = new Button();
    next.setText(Component.literal(">"));
    panel.addChild(VendorUIHelper.abs(next, PANEL_W - GRID_X - 20, PAGER_Y + 2, 20, FIELD_H));

    String[] sig = {null};
    Runnable rebuild =
        () -> {
          List<AuctionListing> list = filtered(player, filter[0], sort[0]);
          int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
          if (page[0] >= pages) page[0] = pages - 1;
          int start = page[0] * PAGE_SIZE;
          int end = Math.min(list.size(), start + PAGE_SIZE);

          StringBuilder s =
              new StringBuilder()
                  .append(page[0])
                  .append('/')
                  .append(sort[0])
                  .append('/')
                  .append(filter[0])
                  .append('|');
          for (int i = start; i < end; i++) {
            AuctionListing l = list.get(i);
            s.append(l.id)
                .append(':')
                .append(l.item.getCount())
                .append(':')
                .append(l.displayPrice())
                .append(';');
          }
          String newSig = s.toString();
          if (newSig.equals(sig[0])) return;
          sig[0] = newSig;

          grid.clearAllChildren();
          grid.clearLayoutCache();
          for (int i = start; i < end; i++) {
            AuctionListing l = list.get(i);
            int slot = i - start;
            ItemSlot itemSlot = VendorUIHelper.phantomSlot();
            itemSlot.setItem(l.item.copy());
            UUID lid = l.id;
            itemSlot.addEventListener(
                UIEvents.CLICK,
                e -> {
                  selected[0] = lid;
                  rebuildDetail[0].run();
                  select.accept(1);
                  e.stopPropagation();
                });
            grid.addChild(
                VendorUIHelper.abs(itemSlot, (slot % COLS) * SLOT, (slot / COLS) * SLOT, SLOT, SLOT));
          }
          pageLabel.setText(
              Component.translatable("menu.roll_mod.ah.page", page[0] + 1, pages));
        };

    next.setOnClick(
        e -> {
          List<AuctionListing> list = filtered(player, filter[0], sort[0]);
          int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
          if (page[0] < pages - 1) page[0]++;
        });

    rebuild.run();
    panel.addEventListener(UIEvents.TICK, e -> rebuild.run());
  }

  /* -------------------------------------------------- detail ----------------------------------------------------- */

  private static void buildDetail(
      Player player,
      UIElement panel,
      IntConsumer select,
      UUID[] selected,
      Runnable[] rebuildDetail) {
    UIElement content = new UIElement();
    panel.addChild(VendorUIHelper.abs(content, 0, 0, PANEL_W, BODY_H - 26));

    Button back = new Button();
    back.setText(Component.translatable("menu.roll_mod.ah.back"));
    back.setOnClick(e -> select.accept(0));
    panel.addChild(VendorUIHelper.abs(back, EDGE, BODY_H - 24, 60, FIELD_H));

    Runnable rebuild =
        () -> {
          content.clearAllChildren();
          content.clearLayoutCache();
          AuctionListing l = findById(player, selected[0]);
          if (l == null) {
            Label gone = new Label();
            gone.setText(Component.translatable("menu.roll_mod.ah.notFound"));
            content.addChild(VendorUIHelper.abs(gone, INSET, 16, TEXT_W, LINE_H));
            return;
          }

          ItemSlot item = VendorUIHelper.phantomSlot();
          item.setItem(l.item.copy());
          content.addChild(VendorUIHelper.abs(item, INSET, 16, SLOT, SLOT));

          Label name = new Label();
          name.setText(Component.literal(l.item.getCount() + "x ").append(l.item.getHoverName()));
          content.addChild(VendorUIHelper.abs(name, INSET + SLOT + 6, 18, TEXT_W - SLOT - 6, LINE_H));

          Label seller = new Label();
          seller.setText(Component.translatable("menu.roll_mod.ah.seller", l.sellerName));
          content.addChild(VendorUIHelper.abs(seller, INSET, 44, TEXT_W, LINE_H));

          Label time = new Label();
          time.setText(remainingText(l));
          time.addEventListener(UIEvents.TICK, e -> time.setText(remainingText(l)));
          content.addChild(VendorUIHelper.abs(time, INSET, 60, TEXT_W, LINE_H));

          if (!l.bidding) {
            Label price = new Label();
            price.setText(
                Component.translatable(
                    "menu.roll_mod.ah.price",
                    EnergyFormatUtils.formatEnergy(l.fixedPrice)));
            content.addChild(VendorUIHelper.abs(price, INSET, 80, TEXT_W, LINE_H));

            Button buy = new Button();
            buy.setText(Component.translatable("menu.roll_mod.buy.button"));
            buy.setOnClick(
                e -> {
                  PacketDistributor.sendToServer(
                      AuctionActionPacket.of(AuctionActionPacket.Action.BUY, l.id));
                  select.accept(0);
                });
            content.addChild(VendorUIHelper.abs(buy, INSET, 104, 90, BTN_H));
          } else {
            Label bidLabel = new Label();
            bidLabel.setText(
                Component.translatable(
                    l.hasBids()
                        ? "menu.roll_mod.ah.currentBid"
                        : "menu.roll_mod.ah.startBid",
                    EnergyFormatUtils.formatEnergy(l.displayPrice())));
            content.addChild(VendorUIHelper.abs(bidLabel, INSET, 80, TEXT_W, LINE_H));

            long minNext = l.minNextBid(1);
            TextField bidField =
                VendorUIHelper.intField(INSET, 102, 70, FIELD_H, Long.toString(minNext), t -> {});
            content.addChild(bidField);

            Button placeBid = new Button();
            placeBid.setText(Component.translatable("menu.roll_mod.ah.bid"));
            placeBid.setOnClick(
                e -> {
                  long amount = VendorUIHelper.parseLong(bidField.getText(), 0);
                  if (amount > 0) {
                    PacketDistributor.sendToServer(
                        new AuctionActionPacket(AuctionActionPacket.Action.BID, l.id, amount));
                  }
                });
            content.addChild(VendorUIHelper.abs(placeBid, INSET + 76, 102, 70, BTN_H));

            if (l.buyout > 0) {
              Button buyout = new Button();
              buyout.setText(
                  Component.translatable(
                      "menu.roll_mod.ah.buyout",
                      EnergyFormatUtils.formatEnergy(l.buyout)));
              buyout.setOnClick(
                  e -> {
                    PacketDistributor.sendToServer(
                        AuctionActionPacket.of(AuctionActionPacket.Action.BUYOUT, l.id));
                    select.accept(0);
                  });
              content.addChild(VendorUIHelper.abs(buyout, INSET, 124, 120, BTN_H));
            }
          }

          if (l.sellerId.equals(player.getUUID())) {
            Button cancel = new Button();
            cancel.setText(Component.translatable("menu.roll_mod.ah.cancel"));
            cancel.setOnClick(
                e -> {
                  PacketDistributor.sendToServer(
                      AuctionActionPacket.of(AuctionActionPacket.Action.CANCEL, l.id));
                  select.accept(0);
                });
            content.addChild(VendorUIHelper.abs(cancel, INSET + 128, 124, 74, BTN_H));
          }
        };
    rebuildDetail[0] = rebuild;

    // Live-refresh the detail when the underlying listing's bid/price changes.
    String[] sig = {""};
    panel.addEventListener(
        UIEvents.TICK,
        e -> {
          AuctionListing l = findById(player, selected[0]);
          String newSig =
              selected[0] + ":" + (l == null ? "none" : (l.currentBid + "/" + l.displayPrice()));
          if (!newSig.equals(sig[0])) {
            sig[0] = newSig;
            rebuild.run();
          }
        });
  }

  /* -------------------------------------------------- create ----------------------------------------------------- */

  private static void buildCreate(Player player, UIElement panel, IntConsumer select) {
    ItemSlot pick = VendorUIHelper.phantomSlot();
    // Clicking the slot with something on the cursor still works, for anyone who already knows the
    // trick; the Select item button below is the discoverable version of the same thing.
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
    panel.addChild(VendorUIHelper.abs(pick, INSET, ROW_ITEM, SLOT, SLOT));

    // "Select item": arms the picker, then the next thing the player lifts off their inventory
    // becomes the listing's item.
    //
    // It watches the cursor rather than listening on the inventory slots because those are real
    // menu slots — a click on one is handled by vanilla's slotClicked, and LdLib's ItemSlot exposes
    // no hook to intercept it. Observing what ends up on the cursor needs no interception at all,
    // and the stack is only copied, so the player still puts it straight back.
    boolean[] picking = {false};

    Label pickHint = new Label();
    pickHint.setText(Component.translatable("menu.roll_mod.ah.pick.idle"));
    pickHint.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
    panel.addChild(VendorUIHelper.abs(pickHint, INSET, ROW_HINT, TEXT_W, LINE_H));

    Button selectItem = new Button();
    selectItem.setText(Component.translatable("menu.roll_mod.ah.pick.button"));
    selectItem.setOnClick(e -> picking[0] = true);
    // Beside the quantity field rather than at the right edge: the bidding label and switch live
    // there, and being added later they took every click that landed on this button.
    panel.addChild(
        VendorUIHelper.abs(selectItem, INSET + SLOT + 6 + 40 + 8, ROW_ITEM, PICK_BTN_W, BTN_H));

    panel.addEventListener(
        UIEvents.TICK,
        e -> {
          AbstractContainerMenu menu =
              panel.getModularUI() == null ? null : panel.getModularUI().getMenu();
          if (menu == null) return;

          if (picking[0] && !menu.getCarried().isEmpty()) {
            pick.setItem(menu.getCarried().copyWithCount(1));
            picking[0] = false;
          }

          ItemStack chosen = pick.getValue();
          pickHint.setText(
              picking[0]
                  ? Component.translatable("menu.roll_mod.ah.pick.active")
                      .withStyle(ChatFormatting.YELLOW)
                  : chosen.isEmpty()
                      ? Component.translatable("menu.roll_mod.ah.pick.idle")
                      : Component.translatable("menu.roll_mod.ah.pick.chosen", chosen.getHoverName())
                          .withStyle(ChatFormatting.GREEN));
        });

    TextField qty =
        VendorUIHelper.intField(INSET + SLOT + 6, ROW_ITEM + 2, 40, FIELD_H, "1", t -> {});
    panel.addChild(qty);

    Label typeLabel = new Label();
    typeLabel.setText(Component.translatable("menu.roll_mod.ah.bidding"));
    panel.addChild(
        VendorUIHelper.abs(typeLabel, PANEL_W - INSET - 48 - 32, ROW_ITEM + 2, 48, LINE_H));
    Switch type = new Switch();
    type.setOn(false);
    panel.addChild(VendorUIHelper.abs(type, PANEL_W - INSET - 28, ROW_ITEM, 28, 14));

    // Fixed-price fields
    UIElement fixedGroup = new UIElement();
    // Below the hint, not over it. The two groups deliberately share one rectangle — they are
    // mutually exclusive — but at y 34 they also covered pickHint, so in bidding mode the start
    // and buyout labels printed straight over the "what you picked" line.
    VendorUIHelper.abs(fixedGroup, 0, ROW_PRICE, PANEL_W, 32);
    Label priceLabel = new Label();
    priceLabel.setText(Component.translatable("menu.roll_mod.ah.priceField"));
    fixedGroup.addChild(VendorUIHelper.abs(priceLabel, INSET, 0, 120, 10));
    TextField priceField = VendorUIHelper.intField(INSET, 12, 100, FIELD_H, "1", t -> {});
    fixedGroup.addChild(priceField);
    panel.addChild(fixedGroup);

    // Bidding fields
    UIElement bidGroup = new UIElement();
    VendorUIHelper.abs(bidGroup, 0, ROW_PRICE, PANEL_W, 32);
    Label startLabel = new Label();
    startLabel.setText(Component.translatable("menu.roll_mod.ah.startField"));
    bidGroup.addChild(VendorUIHelper.abs(startLabel, INSET, 0, 90, 10));
    TextField startField = VendorUIHelper.intField(INSET, 12, 90, FIELD_H, "1", t -> {});
    bidGroup.addChild(startField);
    Label buyoutLabel = new Label();
    buyoutLabel.setText(Component.translatable("menu.roll_mod.ah.buyoutField"));
    bidGroup.addChild(VendorUIHelper.abs(buyoutLabel, INSET + 110, 0, 110, 10));
    TextField buyoutField = VendorUIHelper.intField(INSET + 110, 12, 90, FIELD_H, "0", t -> {});
    bidGroup.addChild(buyoutField);
    panel.addChild(bidGroup);
    bidGroup.setDisplay(false);

    type.setOnSwitchChanged(
        on -> {
          fixedGroup.setDisplay(!on);
          bidGroup.setDisplay(on);
        });

    Label daysLabel = new Label();
    daysLabel.setText(Component.translatable("menu.roll_mod.ah.daysField"));
    panel.addChild(VendorUIHelper.abs(daysLabel, INSET, ROW_DAYS, 120, 10));
    TextField daysField =
        VendorUIHelper.intField(INSET, ROW_DAYS + 12, 50, FIELD_H, "10", t -> {});
    panel.addChild(daysField);

    Button list = new Button();
    list.setText(Component.translatable("menu.roll_mod.ah.list"));
    list.setOnClick(
        e -> {
          ItemStack picked = pick.getValue();
          if (picked == null || picked.isEmpty()) return;
          int quantity = (int) Math.max(1, VendorUIHelper.parseLong(qty.getText(), 1));
          ItemStack toList = picked.copyWithCount(quantity);
          boolean bidding = type.isOn();
          long fixed = Math.max(0, VendorUIHelper.parseLong(priceField.getText(), 0));
          long startPrice = Math.max(0, VendorUIHelper.parseLong(startField.getText(), 0));
          long buyout = Math.max(0, VendorUIHelper.parseLong(buyoutField.getText(), 0));
          int days = (int) Math.max(1, VendorUIHelper.parseLong(daysField.getText(), 1));
          PacketDistributor.sendToServer(
              new CreateListingPacket(toList, bidding, fixed, startPrice, buyout, days));
          pick.setItem(ItemStack.EMPTY);
          select.accept(0);
        });
    panel.addChild(VendorUIHelper.abs(list, INSET + 110, ROW_LIST, 90, BTN_H));

    Button back = new Button();
    back.setText(Component.translatable("menu.roll_mod.ah.back"));
    back.setOnClick(e -> select.accept(0));
    panel.addChild(VendorUIHelper.abs(back, INSET + 110, ROW_BACK, 90, FIELD_H));
  }

  /* ------------------------------------------------ collection --------------------------------------------------- */

  private static void buildCollection(Player player, UIElement panel, IntConsumer select) {
    ScrollerView scroller = new ScrollerView();
    scroller.viewContainer(c -> c.layout(l -> l.flexDirection(YogaFlexDirection.COLUMN)));
    scroller.horizontalScroller(s -> s.setDisplay(false));
    VendorUIHelper.abs(scroller, EDGE, 4, PANEL_W - 2 * EDGE, BODY_H - 34);
    panel.addChild(scroller);

    int[] lastCount = {-1};
    Runnable rebuild =
        () -> {
          List<ItemStack> claims = claims(player);
          scroller.clearAllScrollViewChildren();
          for (ItemStack stack : claims) {
            if (stack.isEmpty()) continue;
            UIElement row = new UIElement();
            row.layout(
                l ->
                    l.width(PANEL_W - 2 * EDGE - 16)
                        .height(SLOT)
                        .flexDirection(YogaFlexDirection.ROW)
                        .alignItems(YogaAlign.CENTER)
                        .marginBottom(2));
            ItemSlot slot = VendorUIHelper.phantomSlot();
            slot.setItem(stack.copy());
            slot.layout(l -> l.width(18).height(18));
            row.addChild(slot);
            Label label = new Label();
            label.setText(Component.literal(stack.getCount() + "x ").append(stack.getHoverName()));
            label.layout(l -> l.width(180).height(12).marginLeft(6));
            row.addChild(label);
            scroller.addScrollViewChild(row);
          }
          lastCount[0] = claims.size();
        };
    rebuild.run();
    scroller.addEventListener(
        UIEvents.TICK,
        e -> {
          if (claims(player).size() != lastCount[0]) rebuild.run();
        });

    Button claimAll = new Button();
    claimAll.setText(Component.translatable("menu.roll_mod.ah.claimAll"));
    claimAll.setOnClick(e -> PacketDistributor.sendToServer(AuctionActionPacket.claim()));
    panel.addChild(VendorUIHelper.abs(claimAll, EDGE, BODY_H - 26, 90, FIELD_H));

    Button back = new Button();
    back.setText(Component.translatable("menu.roll_mod.ah.back"));
    back.setOnClick(e -> select.accept(0));
    panel.addChild(VendorUIHelper.abs(back, EDGE + 100, BODY_H - 26, 60, FIELD_H));
  }

  /* -------------------------------------------------- data ------------------------------------------------------- */

  private static List<AuctionListing> listings(Player player) {
    if (player.level().isClientSide) return new ArrayList<>(ClientAuctionCache.LISTINGS);
    return new ArrayList<>(AuctionData.get(player.getServer()).getListings());
  }

  private static List<ItemStack> claims(Player player) {
    if (player.level().isClientSide) return new ArrayList<>(ClientAuctionCache.CLAIMS);
    return new ArrayList<>(AuctionData.get(player.getServer()).getClaims(player.getUUID()));
  }

  private static AuctionListing findById(Player player, UUID id) {
    if (id == null) return null;
    for (AuctionListing l : listings(player)) {
      if (l.id.equals(id)) return l;
    }
    return null;
  }

  private static List<AuctionListing> filtered(Player player, String filter, int sort) {
    String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
    List<AuctionListing> out = new ArrayList<>();
    for (AuctionListing l : listings(player)) {
      if (l.item.isEmpty()) continue;
      if (!f.isEmpty() && !l.item.getHoverName().getString().toLowerCase(Locale.ROOT).contains(f))
        continue;
      out.add(l);
    }
    switch (sort) {
      case 1 -> out.sort(Comparator.comparingLong(AuctionListing::displayPrice));
      case 2 -> out.sort(Comparator.comparingLong(l -> l.expiresAtMs));
      default -> out.sort(Comparator.comparingLong((AuctionListing l) -> l.createdAtMs).reversed());
    }
    return out;
  }

  private static Component sortLabel(int sort) {
    return switch (sort) {
      case 1 -> Component.translatable("menu.roll_mod.ah.sort.price");
      case 2 -> Component.translatable("menu.roll_mod.ah.sort.time");
      default -> Component.translatable("menu.roll_mod.ah.sort.newest");
    };
  }

  private static Component remainingText(AuctionListing l) {
    long ms = l.remainingMs(System.currentTimeMillis());
    long minutes = ms / 60_000L;
    long days = minutes / (60 * 24);
    long hours = (minutes / 60) % 24;
    long mins = minutes % 60;
    String time;
    if (days > 0) time = days + "d " + hours + "h";
    else if (hours > 0) time = hours + "h " + mins + "m";
    else time = mins + "m";
    return Component.translatable("menu.roll_mod.ah.endsIn", time);
  }
}
