package com.roll_54.roll_mod.economy.plot.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.plot.PlotRegistry;
import com.roll_54.roll_mod.minestar.hub.gui.HubSection;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.economy.plot.PlotSnapshot;
import com.roll_54.roll_mod.economy.plot.client.ClientPlotCache;
import com.roll_54.roll_mod.economy.plot.network.BuyPlotPacket;
import com.roll_54.roll_mod.economy.plot.network.PlotActionPacket;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

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
  private static final int MAP_TOP = MARGIN + TITLE_H;
  private static final int DIALOG_X = MARGIN + PlotRegistry.IMG_W + MARGIN;
  // The panel fills the hub's content box. The map is a fixed 240px image pinned at MAP_TOP, so the
  // dialog takes whatever width is left over rather than the other way round.
  /**
   * A cool tint, against the auction house's warm one. Translucent, so the hub's background
   * shows through instead of being hidden behind a pale slab.
   */
  private static final int PANEL_COLOR = 0x40AEBAC6;

  private static final int ROOT_W = HubUI.CONTENT_W;
  private static final int ROOT_H = HubUI.CONTENT_H;
  private static final int DIALOG_W = ROOT_W - DIALOG_X - MARGIN;
  private static final int FIELD_W = DIALOG_W - 16;

  private static final int COLOR_AVAILABLE = 0x8000FF00; // transparent green
  private static final int COLOR_UNAFFORDABLE = 0x80FFFF00; // transparent yellow
  private static final int COLOR_MINE = 0x800080FF; // transparent blue
  private static final int COLOR_OWNED = 0x80FF0000; // transparent red

  private PlotPurchaseUI() {}

  /** The element tree. Always a tab of the hub, which owns the one ModularUI. */
  public static UIElement buildRoot(Player player) {
    UIElement root = VendorUIHelper.panel(ROOT_W, ROOT_H, PANEL_COLOR);

    HubSection.Info section = HubSection.info("plots");

    Label title = new Label();
    title.setText(Component.translatable("menu.roll_mod.plot.title"));
    root.addChild(VendorUIHelper.abs(title, MARGIN + 4, MARGIN + 2, PlotRegistry.IMG_W, 12));
    // TITLE_H already reserves this band, so the button costs the map no room.
    root.addChild(VendorUIHelper.abs(section.button(), ROOT_W - MARGIN - INFO, MARGIN + 2, INFO, INFO));

    // Map background; plot overlays are children positioned in image pixels (1px = 1block).
    UIElement map = new UIElement();
    map.style(s -> s.backgroundTexture(SpriteTexture.of(PlotRegistry.BACKGROUND)));
    VendorUIHelper.abs(map, MARGIN, MAP_TOP, PlotRegistry.IMG_W, PlotRegistry.IMG_H);
    root.addChild(map);

    UIElement dialog = new UIElement();
    dialog.style(s -> s.backgroundTexture(new ColorRectTexture(0xFF4A4A4A)));
    VendorUIHelper.abs(dialog, DIALOG_X, MAP_TOP, DIALOG_W, PlotRegistry.IMG_H);
    root.addChild(dialog);

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
            map.addChild(
                VendorUIHelper.abs(overlay, p.pixelX(), p.pixelY(), p.pixelW(), p.pixelH()));
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
    UIElement content = new UIElement();
    dialog.addChild(VendorUIHelper.abs(content, 0, 0, DIALOG_W, PlotRegistry.IMG_H));

    Runnable rebuild =
        () -> {
          content.clearAllChildren();
          content.clearLayoutCache();
          PlotSnapshot p = find(selected[0]);
          if (p == null) {
            addLabel(content, "menu.roll_mod.plot.select", 8, 12, 40);
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
  private static void buildBuy(UIElement content, PlotSnapshot p) {
    if (p.kind() == PlotSnapshot.Kind.OTHER_LISTED) {
      addText(
          content,
          Component.translatable(
              "menu.roll_mod.plot.resaleFrom",
              p.ownerName() == null ? "?" : p.ownerName()),
          8,
          10,
          FIELD_W,
          24);
    }
    addLabel(content, "menu.roll_mod.plot.buyPrompt", 8, 38, 48);

    Label price = new Label();
    price.setText(Component.literal(EnergyFormatUtils.formatEnergy(p.price())));
    content.addChild(VendorUIHelper.abs(price, 26, 92, FIELD_W - 18, 12));
    content.addChild(VendorUIHelper.starcoinIcon(8, 91, 14));

    if (p.affordable()) {
      Button buy = new Button();
      buy.setText(Component.translatable("menu.roll_mod.plot.buy"));
      String id = p.id();
      buy.setOnClick(e -> PacketDistributor.sendToServer(new BuyPlotPacket(id)));
      content.addChild(VendorUIHelper.abs(buy, 8, 112, FIELD_W, 18));
    } else {
      addLabel(content, "menu.roll_mod.plot.tooExpensive", 8, 112, 24);
    }
  }

  /** Someone else's plot, not for sale. */
  private static void buildOwned(UIElement content, PlotSnapshot p) {
    addText(
        content,
        Component.translatable(
            "menu.roll_mod.plot.owned", p.ownerName() == null ? "?" : p.ownerName()),
        8,
        12,
        FIELD_W,
        40);
  }

  /** The viewer's own plot — resale listing controls plus sell-back-to-server (with a warning). */
  private static void buildMine(
      UIElement content, PlotSnapshot p, String[] confirmSellFor, Runnable[] rebuildDialog) {
    String id = p.id();
    addLabel(content, "menu.roll_mod.plot.yours", 8, 8, 12);

    // Resale price input (defaults to the current listing price, else the plot's base price).
    addLabel(content, "menu.roll_mod.plot.resalePriceField", 8, 24, 22);
    long defaultPrice = p.listed() ? p.listingPrice() : p.startingPrice();
    TextField priceField =
        VendorUIHelper.intField(8, 48, FIELD_W, 16, Long.toString(defaultPrice), t -> {});
    content.addChild(priceField);

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
    content.addChild(VendorUIHelper.abs(list, 8, 68, FIELD_W, 18));

    if (p.listed()) {
      addText(
          content,
          Component.translatable(
              "menu.roll_mod.plot.listedFor",
              EnergyFormatUtils.formatEnergy(p.listingPrice())),
          8,
          90,
          FIELD_W,
          12);

      Button unlist = new Button();
      unlist.setText(Component.translatable("menu.roll_mod.plot.unlist"));
      unlist.setOnClick(e -> PacketDistributor.sendToServer(PlotActionPacket.unlist(id)));
      content.addChild(VendorUIHelper.abs(unlist, 8, 104, FIELD_W, 18));
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
      content.addChild(VendorUIHelper.abs(sell, 8, 150, FIELD_W, 18));
    } else {
      addText(
          content,
          Component.translatable(
              "menu.roll_mod.plot.sellWarning",
              EnergyFormatUtils.formatEnergy(p.startingPrice())),
          8,
          120,
          FIELD_W,
          60);

      Button confirm = new Button();
      confirm.setText(Component.translatable("menu.roll_mod.plot.sellConfirm"));
      confirm.setOnClick(
          e -> {
            PacketDistributor.sendToServer(PlotActionPacket.sellToServer(id));
            confirmSellFor[0] = null;
            rebuildDialog[0].run();
          });
      content.addChild(VendorUIHelper.abs(confirm, 8, 184, FIELD_W, 18));

      Button cancel = new Button();
      cancel.setText(Component.translatable("menu.roll_mod.plot.cancel"));
      cancel.setOnClick(
          e -> {
            confirmSellFor[0] = null;
            rebuildDialog[0].run();
          });
      content.addChild(VendorUIHelper.abs(cancel, 8, 206, FIELD_W, 18));
    }
  }

  private static void addLabel(UIElement parent, String key, int x, int y, int h) {
    addText(parent, Component.translatable(key), x, y, FIELD_W - (x - 8), h);
  }

  /**
   * Add a label that wraps to the given width instead of overflowing the box. Labels auto-size to
   * their content by default, so {@code adaptiveWidth(false)} + {@code WRAP} is what pins them to
   * the box width and pushes overflow onto new lines.
   */
  private static void addText(UIElement parent, Component text, int x, int y, int w, int h) {
    Label label = new Label();
    label.setText(text);
    label.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.WRAP));
    parent.addChild(VendorUIHelper.abs(label, x, y, w, h));
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
