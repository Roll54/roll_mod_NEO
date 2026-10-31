package com.roll_54.roll_mod.economy.vendingblock.gui.trade;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.economy.vendingblock.gui.components.InterfaceStorageHandler;
import com.roll_54.roll_mod.economy.vendingblock.network.VendorInterfacePacket;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The owner's inventory page: the vending block's ME interface, laid out like AE2's interface screen.
 *
 * <ul>
 *   <li>Config row: what to keep stocked. Click with an item to stock it (as many as held, so at
 *       most a stack, which is all an interface slot holds), right-click to clear. JEI/EMI ghost
 *       drags work too.
 *   <li>Storage row: the interface's storage, which purchases are paid from.
 *   <li>The upgrade slot (crafting or fuzzy card), priority, fuzzy mode and the network status.
 * </ul>
 */
final class VendorInterfacePanel {

  private static final int SLOT = 18;
  /** Gap between the priority label and its field. */
  private static final int LABEL_GAP = 20;

  private VendorInterfacePanel() {}

  static UIElement build(VendorBlockEntity be) {
    UIElement panel = new UIElement();
    var logic = be.getInterfaceLogic();
    var config = logic.getConfig();

    panel.addChild(label(8, 0, 160, Component.translatable("menu.roll_mod.vendor.me.config")));

    for (int i = 0; i < config.size(); i++) {
      int slot = i;
      ItemSlot configSlot = VendorUIHelper.phantomSlot();
      configSlot.setItem(configStack(be, slot), false);
      configSlot.addEventListener(
          UIEvents.TICK,
          e -> {
            ItemStack shown = configStack(be, slot);
            if (!ItemStack.matches(shown, configSlot.getValue())) configSlot.setItem(shown, false);
          });
      configSlot.addEventListener(
          UIEvents.CLICK,
          e -> {
            AbstractContainerMenu menu =
                configSlot.getModularUI() == null ? null : configSlot.getModularUI().getMenu();
            if (menu == null) return;
            ItemStack cursor = menu.getCarried();
            if (e.button == 1) {
              PacketDistributor.sendToServer(
                  VendorInterfacePacket.clearConfig(be.getBlockPos(), slot));
            } else if (!cursor.isEmpty()) {
              PacketDistributor.sendToServer(
                  VendorInterfacePacket.setConfig(
                      be.getBlockPos(), slot, cursor, cursor.getCount()));
            }
            e.stopPropagation();
          });
      // A JEI/EMI ghost drag writes the slot's value directly.
      configSlot.registerValueListener(
          dropped -> {
            if (dropped == null || dropped.isEmpty()) return;
            if (ItemStack.isSameItemSameComponents(dropped, configStack(be, slot))) return;
            PacketDistributor.sendToServer(
                VendorInterfacePacket.setConfig(be.getBlockPos(), slot, dropped, 1));
          });
      configSlot.addEventListener(
          UIEvents.HOVER_TOOLTIPS,
          e -> {
            if (configSlot.getValue().isEmpty()) {
              e.hoverTooltips =
                  new HoverTooltips(
                      List.of(Component.translatable("menu.roll_mod.vendor.me.config.help")),
                      null,
                      null,
                      ItemStack.EMPTY);
            }
          });
      panel.addChild(VendorUIHelper.abs(configSlot, 8 + i * SLOT, 10, SLOT, SLOT));
    }

    // The upgrade slot: one card, crafting or fuzzy, as on an interface.
    ItemSlot upgrade =
        new ItemSlot().bind((IItemHandlerModifiable) logic.getUpgrades().toItemHandler(), 0);
    upgrade.addEventListener(
        UIEvents.HOVER_TOOLTIPS,
        e -> {
          if (logic.getUpgrades().getStackInSlot(0).isEmpty()) {
            e.hoverTooltips =
                new HoverTooltips(
                    List.of(Component.translatable("menu.roll_mod.vendor.me.upgrade")),
                    null,
                    null,
                    ItemStack.EMPTY);
          }
        });
    panel.addChild(VendorUIHelper.abs(upgrade, 178, 10, SLOT, SLOT));

    panel.addChild(label(8, 31, 160, Component.translatable("menu.roll_mod.vendor.me.storage")));
    InterfaceStorageHandler storage = new InterfaceStorageHandler(logic.getStorage());
    for (int i = 0; i < storage.getSlots(); i++) {
      ItemSlot slot = new ItemSlot().bind(storage, i);
      panel.addChild(VendorUIHelper.abs(slot, 8 + i * SLOT, 41, SLOT, SLOT));
    }

    // Priority, as on the interface's priority screen.
    int priorityLabelW = 44;
    int priorityX = 8 + priorityLabelW + LABEL_GAP;
    panel.addChild(
        label(8, 68, priorityLabelW, Component.translatable("menu.roll_mod.vendor.me.priority")));
    TextField priority = new TextField();
    priority.setTextRegexValidator("-?\\d*");
    priority.setText(String.valueOf(logic.getPriority()));
    panel.addChild(VendorUIHelper.abs(priority, priorityX, 64, 34, 16));
    Button setPriority = new Button();
    setPriority.setText(Component.translatable("menu.roll_mod.vendor.me.set"));
    setPriority.setOnClick(
        e ->
            PacketDistributor.sendToServer(
                VendorInterfacePacket.setPriority(
                    be.getBlockPos(), (int) VendorUIHelper.parseLong(priority.getText(), 0))));
    panel.addChild(VendorUIHelper.abs(setPriority, priorityX + 38, 64, 30, 16));

    // Fuzzy mode, only with a fuzzy card in.
    Button fuzzy = new Button();
    fuzzy.setOnClick(
        e -> {
          FuzzyMode[] modes = FuzzyMode.values();
          FuzzyMode current = logic.getConfigManager().getSetting(Settings.FUZZY_MODE);
          FuzzyMode next = modes[(current.ordinal() + 1) % modes.length];
          PacketDistributor.sendToServer(VendorInterfacePacket.setFuzzyMode(be.getBlockPos(), next));
        });
    panel.addChild(VendorUIHelper.abs(fuzzy, 8, 86, 90, 16));

    Label status = new Label();
    panel.addChild(VendorUIHelper.abs(status, 110, 90, 86, 10));

    panel.addEventListener(
        UIEvents.TICK,
        e -> {
          boolean hasFuzzy = logic.getUpgrades().isInstalled(AEItems.FUZZY_CARD);
          fuzzy.setDisplay(hasFuzzy);
          if (hasFuzzy) {
            FuzzyMode mode = logic.getConfigManager().getSetting(Settings.FUZZY_MODE);
            fuzzy.setText(
                Component.translatable(
                    "menu.roll_mod.vendor.me.fuzzy." + mode.name().toLowerCase(Locale.ROOT)));
          }
          status.setText(statusText(be.getMeStatus()));
        });

    return panel;
  }

  /** The config slot's item, its count the amount to keep; empty for an unset (or non-item) slot. */
  private static ItemStack configStack(VendorBlockEntity be, int slot) {
    GenericStack stack = be.getInterfaceLogic().getConfig().getStack(slot);
    return stack != null && stack.what() instanceof AEItemKey key
        ? key.toStack((int) Math.max(1, Math.min(Integer.MAX_VALUE, stack.amount())))
        : ItemStack.EMPTY;
  }

  private static Component statusText(int status) {
    return switch (status) {
      case VendorBlockEntity.ME_ONLINE ->
          Component.translatable("menu.roll_mod.vendor.me.online")
              .withStyle(ChatFormatting.DARK_GREEN);
      case VendorBlockEntity.ME_NO_CHANNEL ->
          Component.translatable("menu.roll_mod.vendor.me.no_channel")
              .withStyle(ChatFormatting.GOLD);
      default ->
          Component.translatable("menu.roll_mod.vendor.me.offline")
              .withStyle(ChatFormatting.RED);
    };
  }

  private static Label label(int x, int y, int w, Component text) {
    Label label = new Label();
    label.setText(text);
    return VendorUIHelper.abs(label, x, y, w, 10);
  }
}
