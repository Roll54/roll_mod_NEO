package com.roll_54.roll_mod.economy.plot.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.plot.PlotRegistry;
import com.roll_54.roll_mod.minestar.hub.gui.HubSection;
import com.roll_54.roll_mod.economy.plot.PlotSnapshot;
import com.roll_54.roll_mod.economy.plot.client.ClientPlotCache;
import com.roll_54.roll_mod.economy.plot.network.BuyPlotPacket;
import com.roll_54.roll_mod.economy.plot.network.PlotActionPacket;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * The {@code /rollmod plots} GUI. Left: the shop-world map with one semi-transparent overlay per
 * plot — green (buyable &amp; affordable), yellow (buyable &amp; too expensive), blue (yours), red
 * (owned by someone else, not for sale). Right: a context dialog — buy a vacant/listed plot, or,
 * for your own plot, list it for resale / sell it back to the server. Plot data is read from {@link
 * ClientPlotCache} (populated by {@code SyncPlotsPacket}), so the UI does no async work; it
 * rebuilds on tick whenever the cache changes.
 */
public final class PlotPurchaseUI {

  private static final int MARGIN = 4;
  private static final int TITLE_H = 16;

  /** The I button, which fits inside the band TITLE_H already reserves. */
  private static final int INFO = 12;

  /**
   * The dialog never gets narrower than this; the map takes what is left, up to the body's height,
   * scaling the plot overlays with it.
   */
  private static final int DIALOG_MIN_W = 150;

  /** Inside the dialog, around its column of fields. */
  private static final int DIALOG_PAD = 8;

  /**
   * A cool tint, against the auction house's warm one. Translucent, so the hub's background
   * shows through instead of being hidden behind a pale slab.
   */
  private static final int PANEL_COLOR = 0x40AEBAC6;

  private static final int COLOR_AVAILABLE = 0x8000FF00; // transparent green
  private static final int COLOR_UNAFFORDABLE = 0x80FFFF00; // transparent yellow
  private static final int COLOR_MINE = 0x800080FF; // transparent blue
  private static final int COLOR_OWNED = 0x80FF0000; // transparent red

  private PlotPurchaseUI() {}

  /** The element tree. Always a tab of the hub, which owns the one ModularUI. */
  public static UIElement buildRoot(Player player) {
    // Fills the tab box, whatever size the hub window has been given.
    UIElement root = new UIElement();
    root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
        .paddingAll(MARGIN));
    root.style(s -> s.backgroundTexture(new ColorRectTexture(PANEL_COLOR)));

    HubSection.Info section = HubSection.info("plots");

    UIElement titleRow = new UIElement();
    titleRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(TITLE_H)
        .alignItems(AlignItems.CENTER).paddingLeft(4).flexShrink(0));
    Label title = new Label();
    title.setText(Component.translatable("menu.roll_mod.plot.title"));
    title.layout(l -> l.flexGrow(1).height(12));
    section.button().layout(l -> l.width(INFO).height(INFO).flexShrink(0));
    titleRow.addChildren(title, section.button());
    root.addChild(titleRow);

    UIElement body = new UIElement();
    body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
        .gapColumn(MARGIN));

    // Map background; plot overlays are children positioned in percent of the image, so they scale
    // with the map. Starts at the image's own size, which the server's copy keeps.
    UIElement map = new UIElement();
    map.style(s -> s.backgroundTexture(SpriteTexture.of(PlotRegistry.BACKGROUND)));
    map.layout(l -> l.width(PlotRegistry.IMG_W).height(PlotRegistry.IMG_H).flexShrink(0));

    UIElement dialog = new UIElement();
    dialog.style(s -> s.backgroundTexture(new ColorRectTexture(0xFF4A4A4A)));
    dialog.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1)
        .minWidth(DIALOG_MIN_W).heightPercent(100));

    body.addChildren(map, dialog);
    root.addChild(body);

    // The map is as large as the body allows while leaving the dialog its minimum width, keeping
    // the image's aspect. Measured per tick rather than from LAYOUT_CHANGED, so the layout is never
    // changed from inside its own pass, and applied only when it actually changes.
    float[] mapScale = {1f};
    body.addEventListener(UIEvents.TICK, e -> {
      float w = body.getContentWidth() - DIALOG_MIN_W - MARGIN;
      float h = body.getContentHeight();
      if (w <= 0 || h <= 0) return;
      float scale = Math.max(0.25f, Math.min(w / PlotRegistry.IMG_W, h / PlotRegistry.IMG_H));
      if (Math.abs(scale - mapScale[0]) < 0.001f) return;
      mapScale[0] = scale;
      map.layout(l -> l.width((float) Math.floor(PlotRegistry.IMG_W * scale))
          .height((float) Math.floor(PlotRegistry.IMG_H * scale)));
    });

    String[] selected = {null};
    String[] confirmSellFor = {null}; // plot id currently awaiting sell-to-server confirmation
    Runnable[] rebuildDialog = {() -> {}};

    buildOverlays(map, selected, confirmSellFor, rebuildDialog);
    buildDialog(dialog, selected, confirmSellFor, rebuildDialog);

    // Last, so it covers the map and the dialog.
    root.addChild(section.overlay());

    return root;
  }

  /* ------------------------------------------------ overlays ----------------------------------------------------- */

  private static void buildOverlays(
      UIElement map, String[] selected, String[] confirmSellFor, Runnable[] rebuildDialog) {
    String[] sig = {null};
    Runnable rebuild =
        () -> {
          List<PlotSnapshot> plots = ClientPlotCache.PLOTS;
          String newSig = overlaySignature(plots);
          if (newSig.equals(sig[0])) {
            return;
          }
          sig[0] = newSig;

          map.clearAllChildren();
          map.clearLayoutCache();
          for (PlotSnapshot p : plots) {
            UIElement overlay = new UIElement();
            overlay.style(s -> s.backgroundTexture(new ColorRectTexture(colorOf(p))));
            String id = p.id();
            overlay.addEventListener(
                UIEvents.CLICK,
                e -> {
                  selected[0] = id;
                  confirmSellFor[0] = null; // reset any pending confirmation when switching plots
                  rebuildDialog[0].run();
                  e.stopPropagation();
                });
            // Image pixels as a share of the image, so the overlay stays on its plot at any map size.
            overlay.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .leftPercent(100f * p.pixelX() / PlotRegistry.IMG_W)
                .topPercent(100f * p.pixelY() / PlotRegistry.IMG_H)
                .widthPercent(100f * p.pixelW() / PlotRegistry.IMG_W)
                .heightPercent(100f * p.pixelH() / PlotRegistry.IMG_H));
            map.addChild(overlay);
          }
        };
    rebuild.run();
    map.addEventListener(UIEvents.TICK, e -> rebuild.run());
  }

  private static int colorOf(PlotSnapshot p) {
    return switch (p.kind()) {
      case MINE -> COLOR_MINE;
      case OTHER_UNLISTED -> COLOR_OWNED;
      default -> p.affordable() ? COLOR_AVAILABLE : COLOR_UNAFFORDABLE; // VACANT, OTHER_LISTED
    };
  }

  /* ------------------------------------------------- dialog ------------------------------------------------------ */

  private static void buildDialog(
      UIElement dialog, String[] selected, String[] confirmSellFor, Runnable[] rebuildDialog) {
    // A column of flow rows that scrolls when the window is too short for it — the sell
    // confirmation in particular is taller than the dialog at the smallest hub size.
    // flexBasis(0) and COLUMN for the reasons HubSection gives for its own scroller.
    ScrollerView content = new ScrollerView();
    content.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
    content.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
        .verticalScrollDisplay(ScrollDisplay.AUTO)
        .horizontalScrollDisplay(ScrollDisplay.NEVER));
    content.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN)
        .widthPercent(100).paddingAll(DIALOG_PAD)));
    dialog.addChild(content);

    Runnable rebuild =
        () -> {
          content.clearAllScrollViewChildren();
          PlotSnapshot p = find(selected[0]);
          if (p == null) {
            addLabel(content, "menu.roll_mod.plot.select", 4);
            return;
          }
          switch (p.kind()) {
            case MINE -> buildMine(content, p, confirmSellFor, rebuildDialog);
            case OTHER_UNLISTED -> buildOwned(content, p);
            default -> buildBuy(content, p); // VACANT, OTHER_LISTED
          }
        };
    rebuildDialog[0] = rebuild;
    rebuild.run();

    // Live refresh when the selected plot's state changes underfoot (e.g. someone else buys it).
    String[] sig = {""};
    dialog.addEventListener(
        UIEvents.TICK,
        e -> {
          String newSig = dialogSignature(selected[0], confirmSellFor[0], find(selected[0]));
          if (!newSig.equals(sig[0])) {
            sig[0] = newSig;
            rebuild.run();
          }
        });
  }

  /** Vacant plot or someone else's resale listing — show price and a Buy button. */
  private static void buildBuy(ScrollerView content, PlotSnapshot p) {
    if (p.kind() == PlotSnapshot.Kind.OTHER_LISTED) {
      addText(
          content,
          Component.translatable(
              "menu.roll_mod.plot.resaleFrom",
              p.ownerName() == null ? "?" : p.ownerName()),
          2);
    }
    addLabel(content, "menu.roll_mod.plot.buyPrompt", 8);

    UIElement priceRow = new UIElement();
    priceRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(14)
        .alignItems(AlignItems.CENTER).marginTop(8).flexShrink(0));
    Label price = new Label();
    price.setText(Component.literal(EnergyFormatUtils.formatEnergy(p.price())));
    price.layout(l -> l.flexGrow(1).height(12).marginLeft(4));
    priceRow.addChildren(VendorUIHelper.starcoinIcon(14), price);
    content.addScrollViewChild(priceRow);

    if (p.affordable()) {
      Button buy = new Button();
      buy.setText(Component.translatable("menu.roll_mod.plot.buy"));
      String id = p.id();
      buy.setOnClick(e -> PacketDistributor.sendToServer(new BuyPlotPacket(id)));
      content.addScrollViewChild(button(buy, 8));
    } else {
      addLabel(content, "menu.roll_mod.plot.tooExpensive", 8);
    }
  }

  /** Someone else's plot, not for sale. */
  private static void buildOwned(ScrollerView content, PlotSnapshot p) {
    addText(
        content,
        Component.translatable(
            "menu.roll_mod.plot.owned", p.ownerName() == null ? "?" : p.ownerName()),
        4);
  }

  /** The viewer's own plot — resale listing controls plus sell-back-to-server (with a warning). */
  private static void buildMine(
      ScrollerView content, PlotSnapshot p, String[] confirmSellFor, Runnable[] rebuildDialog) {
    String id = p.id();
    addLabel(content, "menu.roll_mod.plot.yours", 0);

    // Resale price input (defaults to the current listing price, else the plot's base price).
    addLabel(content, "menu.roll_mod.plot.resalePriceField", 6);
    long defaultPrice = p.listed() ? p.listingPrice() : p.startingPrice();
    TextField priceField = VendorUIHelper.intField(Long.toString(defaultPrice), t -> {});
    priceField.layout(l -> l.widthPercent(100).height(16).marginTop(2).flexShrink(0));
    content.addScrollViewChild(priceField);

    Button list = new Button();
    list.setText(
        Component.translatable(
            p.listed()
                ? "menu.roll_mod.plot.updateListing"
                : "menu.roll_mod.plot.list"));
    list.setOnClick(
        e -> {
          long base = Math.max(1, VendorUIHelper.parseLong(priceField.getText(), defaultPrice));
          PacketDistributor.sendToServer(PlotActionPacket.list(id, base));
        });
    content.addScrollViewChild(button(list, 4));

    if (p.listed()) {
      addText(
          content,
          Component.translatable(
              "menu.roll_mod.plot.listedFor",
              EnergyFormatUtils.formatEnergy(p.listingPrice())),
          6);

      Button unlist = new Button();
      unlist.setText(Component.translatable("menu.roll_mod.plot.unlist"));
      unlist.setOnClick(e -> PacketDistributor.sendToServer(PlotActionPacket.unlist(id)));
      content.addScrollViewChild(button(unlist, 2));
    }

    // Sell back to the server — two-step, because you only get the base starting price.
    boolean confirming = id.equals(confirmSellFor[0]);
    if (!confirming) {
      Button sell = new Button();
      sell.setText(
          Component.translatable(
              "menu.roll_mod.plot.sellToServer",
              EnergyFormatUtils.formatEnergy(p.startingPrice())));
      sell.setOnClick(
          e -> {
            confirmSellFor[0] = id;
            rebuildDialog[0].run();
          });
      content.addScrollViewChild(button(sell, 16));
    } else {
      addText(
          content,
          Component.translatable(
              "menu.roll_mod.plot.sellWarning",
              EnergyFormatUtils.formatEnergy(p.startingPrice())),
          12);

      Button confirm = new Button();
      confirm.setText(Component.translatable("menu.roll_mod.plot.sellConfirm"));
      confirm.setOnClick(
          e -> {
            PacketDistributor.sendToServer(PlotActionPacket.sellToServer(id));
            confirmSellFor[0] = null;
            rebuildDialog[0].run();
          });
      content.addScrollViewChild(button(confirm, 4));

      Button cancel = new Button();
      cancel.setText(Component.translatable("menu.roll_mod.plot.cancel"));
      cancel.setOnClick(
          e -> {
            confirmSellFor[0] = null;
            rebuildDialog[0].run();
          });
      content.addScrollViewChild(button(cancel, 4));
    }
  }

  /** A full-width button, {@code marginTop} below the row above it. */
  private static Button button(Button button, int marginTop) {
    button.layout(l -> l.widthPercent(100).height(18).marginTop(marginTop).flexShrink(0));
    return button;
  }

  private static void addLabel(ScrollerView parent, String key, int marginTop) {
    addText(parent, Component.translatable(key), marginTop);
  }

  /**
   * Add a label that wraps to the dialog's width and grows as tall as its text needs. Labels
   * auto-size to their content by default, so {@code adaptiveWidth(false)} + {@code WRAP} is what
   * pins them to the width and pushes overflow onto new lines; {@code adaptiveHeight} then takes
   * the height from those lines, which is what lets the rows below follow it down.
   */
  private static void addText(ScrollerView parent, Component text, int marginTop) {
    Label label = new Label();
    label.setText(text);
    label.layout(l -> l.widthPercent(100).marginTop(marginTop).flexShrink(0));
    label.textStyle(s -> s.adaptiveWidth(false).adaptiveHeight(true).textWrap(TextWrap.WRAP));
    parent.addScrollViewChild(label);
  }

  /* -------------------------------------------------- data ------------------------------------------------------- */

  private static PlotSnapshot find(String id) {
    if (id == null) {
      return null;
    }
    for (PlotSnapshot p : ClientPlotCache.PLOTS) {
      if (p.id().equals(id)) {
        return p;
      }
    }
    return null;
  }

  private static String overlaySignature(List<PlotSnapshot> plots) {
    StringBuilder s = new StringBuilder();
    for (PlotSnapshot p : plots) {
      s.append(p.id())
          .append(':')
          .append(p.pixelX())
          .append(',')
          .append(p.pixelY())
          .append(',')
          .append(p.pixelW())
          .append(',')
          .append(p.pixelH())
          .append(':')
          .append(colorOf(p))
          .append(';');
    }
    return s.toString();
  }

  private static String dialogSignature(String selectedId, String confirmSellFor, PlotSnapshot p) {
    if (p == null) {
      return "sel=" + selectedId + ";none";
    }
    return "sel="
        + selectedId
        + ';'
        + p.kind()
        + ';'
        + p.price()
        + ';'
        + p.affordable()
        + ';'
        + p.listed()
        + ';'
        + p.listingPrice()
        + ';'
        + p.ownerName()
        + ";confirm="
        + selectedId.equals(confirmSellFor);
  }
}
