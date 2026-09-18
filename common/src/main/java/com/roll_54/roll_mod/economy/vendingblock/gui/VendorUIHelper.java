package com.roll_54.roll_mod.economy.vendingblock.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.components.FilterValidation;
import com.roll_54.roll_mod.economy.vendingblock.network.FilterSlotUpdatePacket;
import com.roll_54.roll_mod.economy.vendingblock.network.SetFacadePacket;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaPositionType;

/** Shared building blocks for the LdLib2 vendor/display UIs. Coordinates are panel-local pixels. */
public final class VendorUIHelper {

  public static final int PANEL_COLOR = 0xFFC6C6C6;
  public static final int SLOT_SIZE = 18;
  public static final ResourceLocation STARCOIN =
      RollMod.id("textures/gui/container/starcoin.png");

  private VendorUIHelper() {}

  public static UIElement panel(int width, int height) {
    return panel(width, height, PANEL_COLOR);
  }

  /**
   * A panel in a colour of its own. The hub shows several of these screens as tabs, and a tint
   * apiece is what tells them apart at a glance.
   */
  public static UIElement panel(int width, int height, int color) {
    UIElement root = new UIElement();
    root.style(s -> s.backgroundTexture(new ColorRectTexture(color)));
    root.layout(l -> l.width(width).height(height));
    return root;
  }

  public static <T extends UIElement> T abs(T el, int x, int y, int w, int h) {
    el.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(x).top(y).width(w).height(h));
    return el;
  }

  /**
   * A phantom (ghost) item slot. {@link ItemSlot#xeiPhantom()} pulls in client-only XEI rendering,
   * so it is only applied on the client; {@code createUI} also runs on the dedicated server.
   */
  public static ItemSlot phantomSlot() {
    ItemSlot slot = new ItemSlot();
    if (FMLEnvironment.dist.isClient()) {
      slot.xeiPhantom();
    }
    return slot;
  }

  /** A starcoin currency icon element of the given size. */
  public static UIElement starcoinIcon(int x, int y, int size) {
    UIElement icon = new UIElement();
    icon.style(s -> s.backgroundTexture(SpriteTexture.of(STARCOIN)));
    return abs(icon, x, y, size, size);
  }

  /** An integer-only text field; {@code responder} receives the raw text on each edit. */
  public static TextField intField(
      int x, int y, int w, int h, String initial, Consumer<String> responder) {
    TextField field = new TextField();
    field.setTextRegexValidator("\\d*");
    field.setText(initial);
    field.setTextResponder(responder);
    return abs(field, x, y, w, h);
  }

  public static long parseLong(String text, long fallback) {
    if (text == null || text.isEmpty()) return fallback;
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  public static Component priceText(long price, String freeKey) {
    return price == 0L
        ? Component.translatable(freeKey)
        : Component.literal(
            com.roll_54.roll_mod.economy.util.EnergyFormatUtils.formatEnergy(price));
  }

  /** Standard player inventory grid; auto-binds to the viewing player when added to the UI. */
  public static InventorySlots playerInventory(int x, int y) {
    InventorySlots inv = new InventorySlots();
    inv.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(x).top(y));
    return inv;
  }

  /**
   * Vendor facade phantom slot (admin): click sets/clears the block skin via {@link
   * SetFacadePacket}.
   */
  public static ItemSlot facadeSlot(VendorBlockEntity be, int x, int y) {
    ItemSlot slot = phantomSlot();
    slot.setItem(be.getFacade().copy());
    slot.addEventListener(UIEvents.TICK, e -> slot.setItem(be.getFacade().copy()));
    slot.addEventListener(
        UIEvents.CLICK,
        e -> {
          AbstractContainerMenu menu =
              slot.getModularUI() == null ? null : slot.getModularUI().getMenu();
          if (menu == null) return;
          ItemStack cursor = menu.getCarried();
          ItemStack facade = cursor.isEmpty() ? ItemStack.EMPTY : cursor.copyWithCount(1);
          PacketDistributor.sendToServer(new SetFacadePacket(be.getBlockPos(), facade));
          slot.setItem(facade.copy());
          e.stopPropagation();
        });
    return abs(slot, x, y, SLOT_SIZE, SLOT_SIZE);
  }

  /**
   * Facade filter slot for the display block (full-block + facade-blacklist validated, count 1).
   */
  public static ItemSlot displayFacadeSlot(DisplayBlockEntity be, int slotIndex, int x, int y) {
    ItemSlot slot = phantomSlot();
    slot.setItem(be.inventory.getStackInSlot(slotIndex).copy());
    slot.addEventListener(
        UIEvents.TICK, e -> slot.setItem(be.inventory.getStackInSlot(slotIndex).copy()));
    slot.addEventListener(
        UIEvents.CLICK,
        e -> {
          AbstractContainerMenu menu =
              slot.getModularUI() == null ? null : slot.getModularUI().getMenu();
          if (menu == null) return;
          ItemStack cursor = menu.getCarried();
          ItemStack result;
          if (cursor.isEmpty()) {
            result = ItemStack.EMPTY;
          } else if (FilterValidation.isBlacklistedFacade(cursor, be.getLevel(), be.getBlockPos())
              || !FilterValidation.isFullBlock(cursor, be.getLevel(), be.getBlockPos())) {
            return;
          } else {
            result = cursor.copyWithCount(1);
          }
          PacketDistributor.sendToServer(
              new FilterSlotUpdatePacket(be.getBlockPos(), slotIndex, result));
          slot.setItem(result.copy());
          e.stopPropagation();
        });
    return abs(slot, x, y, SLOT_SIZE, SLOT_SIZE);
  }
}
