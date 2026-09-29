package com.roll_54.roll_mod.economy.vendingblock.gui.auction;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
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
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The {@code /rollmod ah} GUI. A single {@link ModularUI} hosting four sub-panels toggled via
 * {@code setDisplay} (browse grid, listing detail, create form, collection). Data is read from
 * {@link ClientAuctionCache} on the client and {@link AuctionData} on the (logical) server, since
 * {@code createUI} runs on both sides. All mutating actions are addressed by listing UUID through
 * {@link AuctionActionPacket}/{@link CreateListingPacket}, so client-side filtering/pagination
 * never has to match the server's element tree.
 *
 * <p>Laid out in flex rows and columns against whatever size the hub window has, not in pixels:
 * the browse grid gains columns and rows as the window grows, and the other three panels scroll
 * when it is too small for them.
 */
public final class AuctionUI {

  private static final int EDGE = 8;          // panel-edge inset
  private static final int INSET = EDGE * 2;  // content inset, one step further in
  private static final int HEADER_H = 24;     // title row and its two buttons

  private static final int SLOT = VendorUIHelper.SLOT_SIZE;
  private static final int LINE_H = 12;
  private static final int FIELD_H = 16;
  private static final int BTN_H = 18;
  /**
   * Wide enough for the longest sort label — uk_ua's "Скоро завершаться" needs ~100px, and a
   * Button spends 8 of its width on padding and margins. The search field takes the rest of the row.
   */
  private static final int SORT_W = 120;
  private static final int HEAD_BTN_W = 56;

  /** The I button. */
  private static final int INFO = 12;

  private static final int GRID_X = 12;
  private static final int PAGER_H = 24;

  /**
   * The grid's size before it has been laid out — the default hub window's — and on the server's
   * copy of the tree, which never is. After that the grid measures itself every tick.
   */
  private static final int DEFAULT_COLS = (HubUI.DEFAULT_CONTENT_W - 2 * GRID_X) / SLOT;
  private static final int DEFAULT_ROWS =
      (HubUI.DEFAULT_CONTENT_H - HEADER_H - 22 - PAGER_H) / SLOT;

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
    // Fills the tab box, but with no background of its own: the hub already paints the window frame
    // and the dark content panel behind every tab, and a panel here only washed over them.
    UIElement root = new UIElement();
    root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100));

    HubSection.Info section = HubSection.info("auction");

    UIElement browse = new UIElement();
    UIElement detail = new UIElement();
    UIElement create = new UIElement();
    UIElement collection = new UIElement();
    // The four share the body; only one is displayed, and a hidden one leaves the layout.
    for (UIElement panel : List.of(browse, detail, create, collection)) {
      panel.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
          .flexBasis(0).flexGrow(1));
    }

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

    // Title on the left, then the two panel buttons, then the I button in the far corner where
    // every other section has it.
    UIElement header = row(HEADER_H);
    header.layout(l -> l.paddingHorizontal(EDGE).flexShrink(0));

    Label title = new Label();
    title.setText(Component.translatable("menu.roll_mod.ah.title"));
    title.layout(l -> l.flexGrow(1).height(LINE_H));
    title.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

    Button createBtn = new Button();
    createBtn.setText(Component.translatable("menu.roll_mod.ah.create"));
    createBtn.setOnClick(e -> select.accept(2));
    createBtn.layout(l -> l.width(HEAD_BTN_W).height(FIELD_H).marginRight(4));

    Button collectionBtn = new Button();
    collectionBtn.setText(Component.translatable("menu.roll_mod.ah.collection"));
    collectionBtn.setOnClick(e -> select.accept(3));
    collectionBtn.layout(l -> l.width(HEAD_BTN_W).height(FIELD_H).marginRight(4));

    section.button().layout(l -> l.width(INFO).height(INFO).flexShrink(0));
    header.addChildren(title, createBtn, collectionBtn, section.button());
    root.addChild(header);

    UUID[] selected = {null};
    Runnable[] rebuildDetail = {() -> {}};

    buildBrowse(player, browse, select, selected, rebuildDetail);
    buildDetail(player, detail, select, selected, rebuildDetail);
    buildCreate(player, create, select);
    buildCollection(player, collection, select);

    select.accept(0);

    UIElement body = new UIElement();
    body.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
        .flexBasis(0).flexGrow(1));
    body.addChildren(browse, detail, create, collection);
    root.addChild(body);
    // Last, so it covers whichever of the four panels is on screen.
    root.addChild(section.overlay());
    // The player inventory lives on the hub window, under the content box.
    return root;
  }

  /* -------------------------------------------------- layout ----------------------------------------------------- */

  /** A full-width row of fixed height, children centred vertically. */
  private static UIElement row(int height) {
    UIElement row = new UIElement();
    row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(height)
        .alignItems(AlignItems.CENTER).flexShrink(0));
    return row;
  }

  /** A full-width single line of text. */
  private static Label line(Component text, int marginTop) {
    Label label = new Label();
    label.setText(text);
    label.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(marginTop).flexShrink(0));
    label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
    return label;
  }

  /**
   * A vertical scroller that takes the rest of its column. flexBasis(0) for the reason HubSection
   * gives: LdLib's flex-basis defaults to auto, so a grow alone leaves the scroller as tall as its
   * content and it never scrolls. COLUMN explicitly, or the measured height never exceeds the view.
   */
  private static ScrollerView scroller() {
    ScrollerView scroller = new ScrollerView();
    scroller.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
    scroller.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
        .verticalScrollDisplay(ScrollDisplay.AUTO)
        .horizontalScrollDisplay(ScrollDisplay.NEVER));
    scroller.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN)
        .widthPercent(100)));
    return scroller;
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
    // Listings per page as of the last rebuild, so a resize keeps the first listing on screen.
    int[] pageSize = {DEFAULT_COLS * DEFAULT_ROWS};

    panel.layout(l -> l.paddingHorizontal(GRID_X));

    UIElement toolbar = row(14);
    toolbar.layout(l -> l.marginTop(2));

    TextField search = new TextField();
    search.setText("");
    search.setTextResponder(
        t -> {
          filter[0] = t;
          page[0] = 0;
        });
    search.layout(l -> l.flexGrow(1).height(14).marginRight(4));

    Button sortBtn = new Button();
    sortBtn.setText(sortLabel(sort[0]));
    sortBtn.setOnClick(
        e -> {
          sort[0] = (sort[0] + 1) % 3;
          sortBtn.setText(sortLabel(sort[0]));
          page[0] = 0;
        });
    sortBtn.layout(l -> l.width(SORT_W).height(14).flexShrink(0));
    toolbar.addChildren(search, sortBtn);

    // Wraps plain slot-sized cells, so a wider window is simply more columns. Clipped, because a
    // row that no longer fits after a shrink is dropped at the next rebuild, not before.
    UIElement grid = new UIElement();
    grid.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1).marginTop(6)
        .flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP)
        .alignContent(AlignContent.FLEX_START));
    grid.setOverflowVisible(false);

    UIElement pager = row(PAGER_H);

    Button prev = new Button();
    prev.setText(Component.literal("<"));
    prev.setOnClick(
        e -> {
          if (page[0] > 0) page[0]--;
        });
    prev.layout(l -> l.width(20).height(FIELD_H).flexShrink(0));

    Label pageLabel = new Label();
    pageLabel.layout(l -> l.flexGrow(1).height(LINE_H));
    pageLabel.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER)
        .textAlignVertical(Vertical.CENTER));

    Button next = new Button();
    next.setText(Component.literal(">"));
    next.layout(l -> l.width(20).height(FIELD_H).flexShrink(0));
    pager.addChildren(prev, pageLabel, next);

    panel.addChildren(toolbar, grid, pager);

    String[] sig = {null};
    Runnable rebuild =
        () -> {
          // Measured every time rather than on LAYOUT_CHANGED: the tree must never be rebuilt from
          // inside a layout pass, and this already runs once a tick. Cells hold no sync value, so a
          // column count that differs between client and server — the server's copy is never laid
          // out — costs nothing; the two already page through different lists.
          int cols = cells(grid.getContentWidth(), DEFAULT_COLS);
          int rows = cells(grid.getContentHeight(), DEFAULT_ROWS);
          int size = cols * rows;
          if (size != pageSize[0]) {
            page[0] = page[0] * pageSize[0] / size;
            pageSize[0] = size;
          }

          List<AuctionListing> list = filtered(player, filter[0], sort[0]);
          int pages = Math.max(1, (list.size() + size - 1) / size);
          if (page[0] >= pages) page[0] = pages - 1;
          int start = page[0] * size;
          int end = Math.min(list.size(), start + size);

          StringBuilder s =
              new StringBuilder()
                  .append(cols)
                  .append('x')
                  .append(rows)
                  .append('/')
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
            itemSlot.layout(l2 -> l2.width(SLOT).height(SLOT).flexShrink(0));
            grid.addChild(itemSlot);
          }
          pageLabel.setText(
              Component.translatable("menu.roll_mod.ah.page", page[0] + 1, pages));
        };

    next.setOnClick(
        e -> {
          List<AuctionListing> list = filtered(player, filter[0], sort[0]);
          int pages = Math.max(1, (list.size() + pageSize[0] - 1) / pageSize[0]);
          if (page[0] < pages - 1) page[0]++;
        });

    rebuild.run();
    panel.addEventListener(UIEvents.TICK, e -> rebuild.run());
  }

  /** How many whole slots fit along {@code length}; the default until the grid has a size. */
  private static int cells(float length, int fallback) {
    return length <= 0 ? fallback : Math.max(1, (int) (length / SLOT));
  }

  /* -------------------------------------------------- detail ----------------------------------------------------- */

  private static void buildDetail(
      Player player,
      UIElement panel,
      IntConsumer select,
      UUID[] selected,
      Runnable[] rebuildDetail) {
    ScrollerView content = scroller();
    content.viewContainer(c -> c.layout(l -> l.paddingHorizontal(INSET).paddingTop(16)));

    UIElement footer = row(FIELD_H + 8);
    footer.layout(l -> l.paddingHorizontal(EDGE));
    Button back = new Button();
    back.setText(Component.translatable("menu.roll_mod.ah.back"));
    back.setOnClick(e -> select.accept(0));
    back.layout(l -> l.width(60).height(FIELD_H));
    footer.addChild(back);

    panel.addChildren(content, footer);

    Runnable rebuild =
        () -> {
          content.clearAllScrollViewChildren();
          AuctionListing l = findById(player, selected[0]);
          if (l == null) {
            content.addScrollViewChild(line(Component.translatable("menu.roll_mod.ah.notFound"), 0));
            return;
          }

          UIElement itemRow = row(SLOT);
          ItemSlot item = VendorUIHelper.phantomSlot();
          item.setItem(l.item.copy());
          item.layout(l2 -> l2.width(SLOT).height(SLOT).flexShrink(0).marginRight(6));
          Label name = new Label();
          name.setText(Component.literal(l.item.getCount() + "x ").append(l.item.getHoverName()));
          name.layout(l2 -> l2.flexGrow(1).height(LINE_H));
          name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
          itemRow.addChildren(item, name);
          content.addScrollViewChild(itemRow);

          content.addScrollViewChild(
              line(Component.translatable("menu.roll_mod.ah.seller", l.sellerName), 10));

          Label time = line(remainingText(l), 4);
          time.addEventListener(UIEvents.TICK, e -> time.setText(remainingText(l)));
          content.addScrollViewChild(time);

          if (!l.bidding) {
            content.addScrollViewChild(line(
                Component.translatable(
                    "menu.roll_mod.ah.price",
                    EnergyFormatUtils.formatEnergy(l.fixedPrice)),
                8));

            Button buy = new Button();
            buy.setText(Component.translatable("menu.roll_mod.buy.button"));
            buy.setOnClick(
                e -> {
                  PacketDistributor.sendToServer(
                      AuctionActionPacket.of(AuctionActionPacket.Action.BUY, l.id));
                  select.accept(0);
                });
            buy.layout(l2 -> l2.width(90).height(BTN_H));
            UIElement actions = row(BTN_H);
            actions.layout(l2 -> l2.marginTop(12));
            actions.addChild(buy);
            content.addScrollViewChild(actions);
          } else {
            content.addScrollViewChild(line(
                Component.translatable(
                    l.hasBids()
                        ? "menu.roll_mod.ah.currentBid"
                        : "menu.roll_mod.ah.startBid",
                    EnergyFormatUtils.formatEnergy(l.displayPrice())),
                8));

            long minNext = l.minNextBid(1);
            TextField bidField = VendorUIHelper.intField(Long.toString(minNext), t -> {});
            bidField.layout(l2 -> l2.width(70).height(FIELD_H).marginRight(6));

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
            placeBid.layout(l2 -> l2.width(70).height(BTN_H));

            UIElement bidRow = row(BTN_H);
            bidRow.layout(l2 -> l2.marginTop(10));
            bidRow.addChildren(bidField, placeBid);
            content.addScrollViewChild(bidRow);
          }

          // Buyout (bidding only) and cancel (own listing only) share the last row.
          UIElement lastRow = row(BTN_H);
          lastRow.layout(l2 -> l2.marginTop(4));
          if (l.bidding && l.buyout > 0) {
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
            buyout.layout(l2 -> l2.width(120).height(BTN_H).marginRight(8));
            lastRow.addChild(buyout);
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
            cancel.layout(l2 -> l2.width(74).height(BTN_H));
            lastRow.addChild(cancel);
          }
          content.addScrollViewChild(lastRow);
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
    // One column of rows, in reading order. It used to be a scatter of absolute rectangles, which is
    // how the Select item button ended up underneath the bidding switch and the price labels on top
    // of the picker hint — in LdLib2 a later sibling wins both the paint and the click. Flow layout
    // cannot overlap. It scrolls, because with the inventory shown under it the form may get less
    // height than it needs in a small window.
    ScrollerView form = scroller();
    form.viewContainer(c -> c.layout(l -> l.paddingHorizontal(INSET).paddingTop(8)));
    panel.addChild(form);

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
    pick.layout(l -> l.width(SLOT).height(SLOT).flexShrink(0).marginRight(6));

    // "Select item": arms the picker, then the next thing the player lifts off their inventory
    // becomes the listing's item.
    //
    // It watches the cursor rather than listening on the inventory slots because those are real
    // menu slots — a click on one is handled by vanilla's slotClicked, and LdLib's ItemSlot exposes
    // no hook to intercept it. Observing what ends up on the cursor needs no interception at all,
    // and the stack is only copied, so the player still puts it straight back.
    boolean[] picking = {false};

    Label pickHint = line(Component.translatable("menu.roll_mod.ah.pick.idle"), 6);

    Button selectItem = new Button();
    selectItem.setText(Component.translatable("menu.roll_mod.ah.pick.button"));
    selectItem.setOnClick(e -> picking[0] = true);
    selectItem.layout(l -> l.width(PICK_BTN_W).height(BTN_H).flexShrink(0).marginLeft(8));

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

    TextField qty = VendorUIHelper.intField("1", t -> {});
    qty.layout(l -> l.width(40).height(FIELD_H).flexShrink(0));

    // The bidding switch keeps the far end of the row; the spacer pushes it there.
    UIElement spacer = new UIElement();
    spacer.layout(l -> l.flexGrow(1));
    Label typeLabel = new Label();
    typeLabel.setText(Component.translatable("menu.roll_mod.ah.bidding"));
    typeLabel.layout(l -> l.width(48).height(LINE_H).flexShrink(0).marginRight(4));
    typeLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
    Switch type = new Switch();
    type.setOn(false);
    type.layout(l -> l.width(28).height(14).flexShrink(0));

    UIElement itemRow = row(BTN_H);
    itemRow.addChildren(pick, qty, selectItem, spacer, typeLabel, type);

    // Fixed-price fields. The two groups are mutually exclusive and take the same place in the
    // column: a hidden one leaves the layout.
    UIElement fixedGroup = new UIElement();
    fixedGroup.layout(l -> l.flexDirection(FlexDirection.COLUMN).marginTop(8).flexShrink(0));
    Label priceLabel = fieldLabel(Component.translatable("menu.roll_mod.ah.priceField"), 120);
    TextField priceField = VendorUIHelper.intField("1", t -> {});
    priceField.layout(l -> l.width(100).height(FIELD_H).marginTop(2));
    fixedGroup.addChildren(priceLabel, priceField);

    // Bidding fields: start and buyout side by side.
    UIElement bidGroup = new UIElement();
    bidGroup.layout(l -> l.flexDirection(FlexDirection.ROW).marginTop(8).flexShrink(0));
    UIElement startCol = new UIElement();
    startCol.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(110));
    Label startLabel = fieldLabel(Component.translatable("menu.roll_mod.ah.startField"), 90);
    TextField startField = VendorUIHelper.intField("1", t -> {});
    startField.layout(l -> l.width(90).height(FIELD_H).marginTop(2));
    startCol.addChildren(startLabel, startField);
    UIElement buyoutCol = new UIElement();
    buyoutCol.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(110));
    Label buyoutLabel = fieldLabel(Component.translatable("menu.roll_mod.ah.buyoutField"), 110);
    TextField buyoutField = VendorUIHelper.intField("0", t -> {});
    buyoutField.layout(l -> l.width(90).height(FIELD_H).marginTop(2));
    buyoutCol.addChildren(buyoutLabel, buyoutField);
    bidGroup.addChildren(startCol, buyoutCol);
    bidGroup.setDisplay(false);

    type.setOnSwitchChanged(
        on -> {
          fixedGroup.setDisplay(!on);
          bidGroup.setDisplay(on);
        });

    UIElement daysGroup = new UIElement();
    daysGroup.layout(l -> l.flexDirection(FlexDirection.COLUMN).marginTop(8).flexShrink(0));
    Label daysLabel = fieldLabel(Component.translatable("menu.roll_mod.ah.daysField"), 120);
    TextField daysField = VendorUIHelper.intField("10", t -> {});
    daysField.layout(l -> l.width(50).height(FIELD_H).marginTop(2));
    daysGroup.addChildren(daysLabel, daysField);

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
    list.layout(l -> l.width(90).height(BTN_H).marginRight(8));

    Button back = new Button();
    back.setText(Component.translatable("menu.roll_mod.ah.back"));
    back.setOnClick(e -> select.accept(0));
    back.layout(l -> l.width(90).height(FIELD_H));

    UIElement buttons = row(BTN_H);
    buttons.layout(l -> l.marginTop(10).marginBottom(4));
    buttons.addChildren(list, back);

    form.addScrollViewChildren(itemRow, pickHint, fixedGroup, bidGroup, daysGroup, buttons);
  }

  private static Label fieldLabel(Component text, int width) {
    Label label = new Label();
    label.setText(text);
    label.layout(l -> l.width(width).height(10));
    return label;
  }

  /* ------------------------------------------------ collection --------------------------------------------------- */

  private static void buildCollection(Player player, UIElement panel, IntConsumer select) {
    ScrollerView scroller = scroller();
    scroller.layout(l -> l.marginTop(4).marginHorizontal(EDGE).widthAuto());
    panel.addChild(scroller);

    int[] lastCount = {-1};
    Runnable rebuild =
        () -> {
          List<ItemStack> claims = claims(player);
          scroller.clearAllScrollViewChildren();
          for (ItemStack stack : claims) {
            if (stack.isEmpty()) continue;
            UIElement row = row(SLOT);
            row.layout(l -> l.marginBottom(2));
            ItemSlot slot = VendorUIHelper.phantomSlot();
            slot.setItem(stack.copy());
            slot.layout(l -> l.width(18).height(18).flexShrink(0));
            row.addChild(slot);
            Label label = new Label();
            label.setText(Component.literal(stack.getCount() + "x ").append(stack.getHoverName()));
            label.layout(l -> l.flexGrow(1).height(12).marginLeft(6));
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
    claimAll.layout(l -> l.width(90).height(FIELD_H).marginRight(10));

    Button back = new Button();
    back.setText(Component.translatable("menu.roll_mod.ah.back"));
    back.setOnClick(e -> select.accept(0));
    back.layout(l -> l.width(60).height(FIELD_H));

    UIElement footer = row(FIELD_H + 10);
    footer.layout(l -> l.paddingHorizontal(EDGE));
    footer.addChildren(claimAll, back);
    panel.addChild(footer);
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
