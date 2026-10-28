package com.roll_54.roll_mod.economy.service.gui;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.service.ServiceBlockEntity;
import com.roll_54.roll_mod.economy.service.ServiceMessages;
import com.roll_54.roll_mod.economy.service.ServiceTransaction;
import com.roll_54.roll_mod.economy.service.ServiceType;
import com.roll_54.roll_mod.economy.service.ServiceWorkSlots;
import com.roll_54.roll_mod.economy.service.ServiceXp;
import com.roll_54.roll_mod.economy.service.network.ServiceSettingsPacket;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockRegistry;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The service block's screens, all in one wide frame:
 *
 * <pre>
 * ┌──────────────[icon]──────────────┐
 * │ what it does        │ the service │
 * │ price, stock, owner │ slot, button│
 * ├──────────────────────────────────┤
 * │         player inventory         │
 * └──────────────────────────────────┘
 * </pre>
 *
 * <p>The left column only informs; everything to click is on the right. The icon over the middle is
 * the service's own texture ({@link ServiceType#iconTexture()}), or its item where it has none.
 *
 * <p>Every screen is built on both sides, like the vendor's. Live text reads the block entity on each
 * tick — on the client that is the copy the block's update packet keeps current — and clicks that change
 * anything run on the server ({@code setOnServerClick}), apart from the price field, which sends a
 * {@link ServiceSettingsPacket}.
 */
public final class ServiceUI {

  private static final int W = 300;
  private static final int H = 250;
  /** Left column: x {@value #LEFT_X} to the divider. */
  private static final int LEFT_X = 8;
  private static final int LEFT_W = 136;
  private static final int DIVIDER_X = 150;
  /** Right column: x {@value #RIGHT_X} to the panel's edge. */
  private static final int RIGHT_X = 158;
  private static final int RIGHT_W = 134;
  private static final int TOP = 26;
  /** Where the columns end and the inventory starts. */
  private static final int INVENTORY_Y = 168;
  private static final int LINE = 12;
  private static final int DIVIDER = 0x40FFFFFF;
  private static final int PREVIEW_INTERVAL = 10;

  private ServiceUI() {}

  /* --------------------------------------------- screens -------------------------------------------- */

  /** First open by the owner: pick the one service this block will ever offer. */
  public static ModularUI picker(ServiceBlockEntity be, Player player) {
    Icon icon = Icon.block();
    UIElement root = frame(icon);

    Label title = label(LEFT_X, TOP, LEFT_W, LINE, Component.translatable("gui.roll_mod.service.picker.title"));
    Label description = wrapped(LEFT_X, TOP + LINE + 2, LEFT_W, 44,
        Component.translatable("gui.roll_mod.service.picker.hint").withStyle(ChatFormatting.GRAY));
    Label warning = wrapped(LEFT_X, TOP + LINE + 50, LEFT_W, 20,
        Component.translatable("gui.roll_mod.service.picker.final").withStyle(ChatFormatting.GOLD));
    root.addChildren(title, description, warning);

    // The pick is remembered on each side: the client to show it, the server to apply it.
    ServiceType[] selected = {null};
    Button[] buttons = new Button[ServiceType.values().length];
    for (int i = 0; i < buttons.length; i++) {
      ServiceType type = ServiceType.values()[i];
      Button button = new Button();
      button.setText(type.title());
      button.setOnClick(e -> {
        selected[0] = type;
        title.setText(type.title());
        description.setText(type.description().copy().withStyle(ChatFormatting.GRAY));
        icon.show(type);
        for (int j = 0; j < buttons.length; j++) {
          ServiceType other = ServiceType.values()[j];
          buttons[j].setText(other == type ? other.title().copy().withStyle(ChatFormatting.YELLOW) : other.title());
        }
      });
      button.setOnServerClick(e -> selected[0] = type);
      buttons[i] = VendorUIHelper.abs(button, RIGHT_X, TOP + i * 14, RIGHT_W, 13);
      root.addChild(button);
    }

    Button confirm = new Button();
    confirm.setText(Component.translatable("gui.roll_mod.service.picker.confirm"));
    confirm.setOnServerClick(e -> {
      if (selected[0] == null || !(player instanceof ServerPlayer sp)) return;
      if (be.chooseService(selected[0])) {
        // Reopened once this click is handled, now as the owner's screen for the chosen service. The
        // block's new state goes first: the regular update would only leave at the end of the tick,
        // and a client still seeing no service would build the picker again, unlike the server.
        sp.server.execute(() -> {
          be.sendTo(sp);
          BlockUIMenuType.openUI(sp, be.getBlockPos());
        });
      }
    });
    root.addChild(VendorUIHelper.abs(confirm, LEFT_X, INVENTORY_Y - 20, LEFT_W, 16));
    return finish(root, player);
  }

  /** Anyone but the owner, before a service is picked. */
  public static ModularUI notReady(ServiceBlockEntity be, Player player) {
    UIElement root = frame(Icon.block());
    root.addChild(label(LEFT_X, TOP, LEFT_W, LINE, Component.translatable("block.roll_mod.service_block")));
    root.addChild(wrapped(LEFT_X, TOP + LINE + 2, LEFT_W, 40,
        Component.translatable("gui.roll_mod.service.not_ready").withStyle(ChatFormatting.GRAY)));
    root.addChild(ownerLine(be, TOP + 60));
    return finish(root, player);
  }

  /** The owner's settings: the price and batch size, the stock, and — with the vendor key — unlimited. */
  public static ModularUI owner(ServiceBlockEntity be, Player player, boolean key) {
    ServiceType type = be.getServiceType();
    UIElement root = frame(iconOf(type));
    info(root, be, type);

    // Price per batch, and the batch itself in the service's own measure.
    root.addChild(label(RIGHT_X, TOP, RIGHT_W, LINE,
        Component.translatable("gui.roll_mod.service.owner.price")));
    String[] price = {String.valueOf(be.getPrice())};
    TextField priceField = VendorUIHelper.exprField(price[0], text -> price[0] = text);
    root.addChild(VendorUIHelper.abs(priceField, RIGHT_X, TOP + LINE + 2, RIGHT_W, 14));

    root.addChild(label(RIGHT_X, TOP + 2 * LINE + 8, RIGHT_W, LINE,
        Component.translatable("gui.roll_mod.service.owner.batch." + resource(type))));
    String[] batch = {String.valueOf(be.getBatch())};
    TextField batchField = VendorUIHelper.exprField(batch[0], text -> batch[0] = text);
    root.addChild(VendorUIHelper.abs(batchField, RIGHT_X, TOP + 3 * LINE + 10, RIGHT_W, 14));

    Button set = new Button();
    set.setText(Component.translatable("gui.roll_mod.service.owner.set"));
    set.setOnClick(e -> {
      long newPrice = VendorUIHelper.parseExpr(price[0], -1L);
      long newBatch = VendorUIHelper.parseExpr(batch[0], -1L);
      if (newPrice >= 0 && newBatch >= 1) {
        PacketDistributor.sendToServer(new ServiceSettingsPacket(be.getBlockPos(), newPrice, newBatch));
      }
    });
    if (be.offersFixedBatch()) {
      // Charging shares the row with its mode: fill the item at the price per batch, or sell exactly
      // one batch for exactly the price.
      int half = (RIGHT_W - 2) / 2;
      root.addChild(VendorUIHelper.abs(set, RIGHT_X, TOP + 4 * LINE + 18, half, 16));
      Button mode = new Button();
      Supplier<Component> caption = () -> Component.translatable(be.isFixedBatch()
          ? "gui.roll_mod.service.owner.mode.fixed"
          : "gui.roll_mod.service.owner.mode.fill");
      mode.setText(caption.get());
      mode.addEventListener(UIEvents.TICK, e -> mode.setText(caption.get()));
      mode.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
          java.util.List.of(Component.translatable(be.isFixedBatch()
              ? "gui.roll_mod.service.owner.mode.fixed.tip"
              : "gui.roll_mod.service.owner.mode.fill.tip")), null, null, ItemStack.EMPTY));
      mode.setOnServerClick(e -> be.setFixedBatch(!be.isFixedBatch()));
      root.addChild(VendorUIHelper.abs(mode, RIGHT_X + half + 2, TOP + 4 * LINE + 18, RIGHT_W - half - 2, 16));
    } else {
      root.addChild(VendorUIHelper.abs(set, RIGHT_X, TOP + 4 * LINE + 18, RIGHT_W, 16));
    }

    int y = TOP + 4 * LINE + 38;
    if (type.resource() == ServiceType.Resource.ENERGY) {
      // The machine hull sets the buffer and the cable tier; it stays in the block.
      ItemSlot hull = new ItemSlot().bind(be.getHullSlot(), 0);
      root.addChild(VendorUIHelper.abs(hull, RIGHT_X, y, VendorUIHelper.SLOT_SIZE, VendorUIHelper.SLOT_SIZE));
      Label hullState = label(RIGHT_X + 22, y + 5, RIGHT_W - 22, LINE, Component.empty());
      hullState.addEventListener(UIEvents.TICK, e -> hullState.setText(hullLine(be)));
      root.addChild(hullState);
      y += VendorUIHelper.SLOT_SIZE + 4;
    }
    // Down to the inventory, or to the Unlimited button for a vendor-key holder.
    root.addChild(wrapped(RIGHT_X, y, RIGHT_W, (key ? INVENTORY_Y - 22 : INVENTORY_Y - 4) - y,
        Component.translatable("gui.roll_mod.service.owner.hint." + resource(type)).withStyle(ChatFormatting.GRAY)));

    if (key) {
      Button unlimited = new Button();
      Supplier<Component> caption = () -> Component.translatable(be.isUnlimited()
          ? "gui.roll_mod.service.owner.unlimited.on"
          : "gui.roll_mod.service.owner.unlimited.off");
      unlimited.setText(caption.get());
      unlimited.addEventListener(UIEvents.TICK, e -> unlimited.setText(caption.get()));
      unlimited.setOnServerClick(e -> be.setUnlimited(!be.isUnlimited()));
      root.addChild(VendorUIHelper.abs(unlimited, RIGHT_X, INVENTORY_Y - 20, RIGHT_W, 16));
    }
    return finish(root, player);
  }

  /** What a customer sees: the work slot (or XP buttons), the cost, and the action. */
  public static ModularUI customer(ServiceBlockEntity be, Player player) {
    ServiceType type = be.getServiceType();
    UIElement root = frame(iconOf(type));
    info(root, be, type);
    if (type.usesSlot()) {
      slotService(root, be, player, type);
    } else {
      xpService(root, be, player, type);
    }
    return finish(root, player);
  }

  /* ------------------------------------------ right column ------------------------------------------ */

  private static void slotService(UIElement root, ServiceBlockEntity be, Player player, ServiceType type) {
    // The customer's own slot, one per open screen: never stored in the block, and handed back when
    // the screen ends (see ServiceWorkSlots).
    ItemStackHandler work = new ItemStackHandler(1) {
      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
        return type.accepts(stack);
      }

      @Override
      public int getSlotLimit(int slot) {
        return 1;
      }
    };
    if (player instanceof ServerPlayer sp) ServiceWorkSlots.open(sp, work);

    root.addChild(label(RIGHT_X, TOP, RIGHT_W, LINE,
        Component.translatable("gui.roll_mod.service.slot." + resource(type))));
    ItemSlot slot = new ItemSlot().bind(work, 0);
    root.addChild(VendorUIHelper.abs(slot, RIGHT_X, TOP + LINE + 2, VendorUIHelper.SLOT_SIZE, VendorUIHelper.SLOT_SIZE));
    Label state = label(RIGHT_X + 22, TOP + LINE + 7, RIGHT_W - 22, LINE, Component.empty());
    state.addEventListener(UIEvents.TICK, e -> state.setText(itemState(type, work.getStackInSlot(0))));
    root.addChild(state);

    Label cost = label(RIGHT_X, TOP + 2 * LINE + 14, RIGHT_W, LINE, Component.empty());
    int[] ticks = {0};
    cost.addEventListener(UIEvents.TICK, e -> {
      // A dry run of the whole transfer: twice a second is plenty for a preview.
      if (ticks[0]++ % PREVIEW_INTERVAL != 0) return;
      long cap = ServiceTransaction.cap(be);
      long amount = work.getStackInSlot(0).isEmpty()
          ? 0L
          : ServiceTransaction.move(type, player, be, work, 0L, cap, false);
      cost.setText(amount > 0 && amount < cap
          ? ServiceMessages.partialBatch(type, be).copy().withStyle(ChatFormatting.RED)
          : costLine(type, amount, be));
    });
    root.addChild(cost);

    Button action = new Button();
    action.setText(type.action());
    action.setOnServerClick(e -> {
      if (player instanceof ServerPlayer sp) ServiceTransaction.use(sp, be, work, 0L);
    });
    root.addChild(VendorUIHelper.abs(action, RIGHT_X, TOP + 3 * LINE + 20, RIGHT_W, 16));
  }

  private static void xpService(UIElement root, ServiceBlockEntity be, Player player, ServiceType type) {
    Label level = label(RIGHT_X, TOP, RIGHT_W, LINE, Component.empty());
    level.addEventListener(UIEvents.TICK, e -> level.setText(Component.translatable(
        "gui.roll_mod.service.xp.level", player.experienceLevel, ServiceXp.totalPoints(player))));
    root.addChild(level);

    // The cost of the smallest step, so the price per level is visible before clicking.
    Label cost = label(RIGHT_X, TOP + LINE + 2, RIGHT_W, LINE, Component.empty());
    cost.addEventListener(UIEvents.TICK, e -> {
      long points = type.sells() ? ServiceXp.pointsToGain(player, 1) : ServiceXp.pointsToLose(player, 1);
      cost.setText(costLine(type, ServiceTransaction.move(type, player, be, null, points, Long.MAX_VALUE, false), be));
    });
    root.addChild(cost);

    root.addChild(label(RIGHT_X, TOP + 2 * LINE + 8, RIGHT_W, LINE,
        Component.translatable(type.sells() ? "gui.roll_mod.service.xp.buy" : "gui.roll_mod.service.xp.sell")));
    int[] steps = {1, 5, 10, type.sells() ? 30 : -1};
    int w = (RIGHT_W - 6) / 4;
    for (int i = 0; i < steps.length; i++) {
      int levels = steps[i];
      Button button = new Button();
      button.setText(levels < 0
          ? Component.translatable("gui.roll_mod.service.xp.all")
          : Component.literal((type.sells() ? "+" : "-") + levels));
      button.setOnServerClick(e -> {
        if (!(player instanceof ServerPlayer sp)) return;
        long points = levels < 0
            ? ServiceXp.totalPoints(sp)
            : type.sells() ? ServiceXp.pointsToGain(sp, levels) : ServiceXp.pointsToLose(sp, levels);
        ServiceTransaction.use(sp, be, null, points);
      });
      root.addChild(VendorUIHelper.abs(button, RIGHT_X + i * (w + 2), TOP + 3 * LINE + 12, w, 16));
    }
  }

  /* ------------------------------------------- left column ------------------------------------------ */

  /** Name, description, price, stock and owner. */
  private static void info(UIElement root, ServiceBlockEntity be, ServiceType type) {
    root.addChild(label(LEFT_X, TOP, LEFT_W, LINE, type.title().copy().withStyle(ChatFormatting.BOLD)));
    root.addChild(wrapped(LEFT_X, TOP + LINE + 2, LEFT_W, 36, type.description().copy().withStyle(ChatFormatting.GRAY)));

    int y = TOP + LINE + 42;
    root.addChild(VendorUIHelper.starcoinIcon(LEFT_X, y, 10));
    Label price = label(LEFT_X + 14, y + 1, LEFT_W - 14, LINE, Component.empty());
    price.addEventListener(UIEvents.TICK, e -> price.setText(Component.translatable("gui.roll_mod.service.price",
        VendorUIHelper.priceText(be.getPrice(), "menu.roll_mod.buy.free"), be.batchText())));
    root.addChild(price);

    Label stock = label(LEFT_X, y + LINE + 2, LEFT_W, LINE, Component.empty());
    stock.addEventListener(UIEvents.TICK, e -> stock.setText(stockLine(type, be)));
    root.addChild(stock);

    root.addChild(ownerLine(be, y + 2 * LINE + 4));
  }

  private static Label ownerLine(ServiceBlockEntity be, int y) {
    String owner = be.getOwnerUser() != null ? be.getOwnerUser() : "?";
    return label(LEFT_X, y, LEFT_W, LINE,
        Component.translatable("jade.roll_mod.owner", owner).withStyle(ChatFormatting.GRAY));
  }

  /** "In stock: …" for a sale, "Room for: …" for buy-back, or "Unlimited". */
  private static Component stockLine(ServiceType type, ServiceBlockEntity be) {
    if (be.isUnlimited()) return Component.translatable("gui.roll_mod.service.unlimited").withStyle(ChatFormatting.AQUA);
    if (type.resource() == ServiceType.Resource.FLUID && type.sells()) {
      FluidStack held = be.getTank().getFluid();
      if (held.isEmpty()) return Component.translatable("gui.roll_mod.service.stock.none").withStyle(ChatFormatting.RED);
      return Component.translatable("gui.roll_mod.service.stock.fluid", held.getHoverName(),
          EnergyFormatUtils.formatEnergy(held.getAmount()));
    }
    if (type.resource() == ServiceType.Resource.XP && ServiceBlockEntity.xpFluid() == Fluids.EMPTY) {
      return Component.translatable("gui.roll_mod.service.stock.no_xp_fluid").withStyle(ChatFormatting.RED);
    }
    return type.sells()
        ? Component.translatable("gui.roll_mod.service.stock", ServiceMessages.amount(type, ServiceMessages.stock(type, be)))
        : Component.translatable("gui.roll_mod.service.room", ServiceMessages.amount(type, ServiceMessages.room(type, be)));
  }

  /** "No hull" in red, or the hull's tier and the buffer it gives. */
  private static Component hullLine(ServiceBlockEntity be) {
    var tier = be.hullTier();
    if (tier == null) return Component.translatable("gui.roll_mod.service.hull.none").withStyle(ChatFormatting.RED);
    return Component.translatable("gui.roll_mod.service.hull", tier.shortEnglishName(),
        EnergyFormatUtils.formatEnergyWithUnit(be.getEnergyCapacity()));
  }

  /** "Cost: N" for a sale, "You get: N" for buy-back, with what moves. */
  private static Component costLine(ServiceType type, long amount, ServiceBlockEntity be) {
    if (amount <= 0) return Component.translatable("gui.roll_mod.service.cost.none").withStyle(ChatFormatting.GRAY);
    long cost = ServiceTransaction.cost(amount, be.getPrice(), be.getBatch());
    return Component.translatable(type.sells() ? "gui.roll_mod.service.cost" : "gui.roll_mod.service.payout",
        ServiceMessages.amount(type, amount), cost);
  }

  /** What is in the customer's item now: its energy, or its fluid. */
  private static Component itemState(ServiceType type, ItemStack stack) {
    if (stack.isEmpty()) return Component.empty();
    if (type.resource() == ServiceType.Resource.ENERGY) {
      var grand = stack.getCapability(dev.technici4n.grandpower.api.ILongEnergyStorage.ITEM);
      if (grand != null) {
        return Component.literal(EnergyFormatUtils.formatEnergy(grand.getAmount()) + " / "
            + EnergyFormatUtils.formatEnergyWithUnit(grand.getCapacity()));
      }
      var fe = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
      return fe == null ? Component.empty() : Component.literal(EnergyFormatUtils.formatEnergy(fe.getEnergyStored())
          + " / " + EnergyFormatUtils.formatEnergyWithUnit(fe.getMaxEnergyStored()));
    }
    var fluid = stack.copyWithCount(1).getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
    if (fluid == null || fluid.getTanks() == 0) return Component.empty();
    FluidStack held = fluid.getFluidInTank(0);
    String amounts = EnergyFormatUtils.formatEnergy(held.getAmount()) + " / "
        + EnergyFormatUtils.formatEnergy(fluid.getTankCapacity(0)) + " mB";
    return held.isEmpty() ? Component.literal(amounts) : held.getHoverName().copy().append(" " + amounts);
  }

  /* --------------------------------------------- pieces --------------------------------------------- */

  /** The wide panel: the icon over the middle, the divider between the columns, the inventory below. */
  private static UIElement frame(Icon icon) {
    UIElement root = VendorUIHelper.styledPanel(W, H);
    root.layout(l -> l.paddingAll(0));
    UIElement divider = new UIElement();
    divider.style(s -> s.backgroundTexture(new ColorRectTexture(DIVIDER)));
    root.addChild(VendorUIHelper.abs(divider, DIVIDER_X, TOP, 1, INVENTORY_Y - TOP - 6));
    int x = (W - VendorUIHelper.SLOT_SIZE) / 2;
    root.addChild(VendorUIHelper.abs(icon.slot, x, 4, VendorUIHelper.SLOT_SIZE, VendorUIHelper.SLOT_SIZE));
    root.addChild(VendorUIHelper.abs(icon.texture, x + 1, 5, 16, 16));
    return root;
  }

  private static Icon iconOf(ServiceType type) {
    Icon icon = new Icon();
    icon.show(type);
    return icon;
  }

  /**
   * The icon over the middle: the service's texture where it has one, otherwise its item. Both sit in
   * the same place and only one is shown, so the picker can switch between them as the owner clicks.
   */
  private static final class Icon {
    final ItemSlot slot = VendorUIHelper.phantomSlot();
    final UIElement texture = new UIElement();

    /** The service block itself, before a service is picked. */
    static Icon block() {
      Icon icon = new Icon();
      icon.slot.setItem(new ItemStack(BlockRegistry.SERVICE.get()));
      icon.texture.setDisplay(false);
      return icon;
    }

    void show(ServiceType type) {
      ResourceLocation sprite = type.iconTexture();
      slot.setDisplay(sprite == null);
      texture.setDisplay(sprite != null);
      if (sprite != null) {
        texture.style(s -> s.backgroundTexture(SpriteTexture.of(sprite)));
      } else {
        slot.setItem(type.icon());
      }
    }
  }

  private static ModularUI finish(UIElement root, Player player) {
    root.addChild(VendorUIHelper.playerInventory((W - 162) / 2, INVENTORY_Y));
    return ModularUI.of(RollUi.of(player, root), player);
  }

  private static Label label(int x, int y, int w, int h, Component text) {
    Label label = new Label();
    label.setText(text);
    label.textStyle(t -> t.textWrap(TextWrap.HIDE));
    return VendorUIHelper.abs(label, x, y, w, h);
  }

  private static Label wrapped(int x, int y, int w, int h, Component text) {
    Label label = new Label();
    label.setText(text);
    label.textStyle(t -> t.textWrap(TextWrap.WRAP));
    return VendorUIHelper.abs(label, x, y, w, h);
  }

  private static String resource(ServiceType type) {
    return type.resource().name().toLowerCase(java.util.Locale.ROOT);
  }
}
