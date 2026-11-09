package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.network.packet.MeHatchConfigPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/**
 * The ME hatches' screens (LDLib2), built on both sides like every LDLib2 block screen.
 *
 * <p><b>Input hatch</b>, laid out like an ME interface: a config grid (what to keep stocked) above a stock
 * grid (what is stocked), both {@value MeHatches#INPUT_SLOTS} slots.
 * <ul>
 *   <li>Click a config slot with an item, or for fluids a filled container, to stock it (as many as held,
 *       or the container's mB); right-click to clear; JEI/EMI ghost drags work too.</li>
 *   <li>Click with an empty hand to select the slot, then type an exact amount and press Set.</li>
 * </ul>
 * The config reaches the client with the block entity's sync; the stock through the menu.
 *
 * <p><b>Output hatch</b>: its {@value MeHatches#OUTPUT_SLOTS} buffer slots or tanks, read-only, and whether
 * ME is taking the output.
 */
public final class MeHatchUI {

    private static final int WIDTH = 176;
    private static final int SLOT = 18;
    private static final int COLUMNS = 8;
    private static final int GRID_X = (WIDTH - COLUMNS * SLOT) / 2;

    private MeHatchUI() {}

    /* ---------------------------------------------------- input ----------------------------------------------------- */

    public static ModularUI input(MeInputHatchBlockEntity hatch, Player player) {
        boolean client = player.level().isClientSide();
        boolean fluid = hatch.isFluid();
        UIElement root = root(238);
        header(root, hatch);

        int[] selected = {-1};
        TextField amount = new TextField();
        amount.setTextRegexValidator("\\d*");
        Label hint = new Label();

        root.addChild(label(7, 28, 100, Component.translatable("menu.roll_mod.me_hatch.config")));
        for (int i = 0; i < MeHatches.INPUT_SLOTS; i++) {
            int slot = i;
            Runnable select = () -> {
                selected[0] = slot;
                GenericStack current = hatch.getConfig().getStack(slot);
                amount.setText(current == null ? "" : String.valueOf(current.amount()));
            };
            UIElement configSlot = fluid ? fluidConfigSlot(hatch, slot, select) : itemConfigSlot(hatch, slot, select);
            root.addChild(VendorUIHelper.abs(configSlot, gridX(slot), 38 + gridY(slot), SLOT, SLOT));
        }

        root.addChild(label(7, 76, 100, Component.translatable("menu.roll_mod.me_hatch.stock")));
        GenericStackDisplayHandler stock =
                new GenericStackDisplayHandler(MeHatches.INPUT_SLOTS, client ? null : hatch::getStock);
        for (int i = 0; i < MeHatches.INPUT_SLOTS; i++) {
            root.addChild(VendorUIHelper.abs(displaySlot(stock, i), gridX(i), 86 + gridY(i), SLOT, SLOT));
        }

        // Label 7..85 fits "Кількість, мБ" (~69 px); the button only holds "ОК"/"Set", so it gives up the width.
        root.addChild(label(7, 127, 78, Component.translatable(
                fluid ? "menu.roll_mod.me_hatch.amount_mb" : "menu.roll_mod.me_hatch.amount")));
        root.addChild(VendorUIHelper.abs(amount, 87, 123, 56, 14));
        Button set = new Button();
        set.setText(Component.translatable("menu.roll_mod.me_hatch.set"));
        set.setOnClick(e -> {
            if (selected[0] < 0) return;
            long value = VendorUIHelper.parseLong(amount.getText(), -1);
            if (value < 0) return;
            PacketDistributor.sendToServer(MeHatchConfigPacket.setAmount(hatch.getBlockPos(), selected[0], value));
        });
        root.addChild(VendorUIHelper.abs(set, 145, 123, 24, 14));
        root.addChild(VendorUIHelper.abs(hint, 7, 141, WIDTH - 14, 10));
        hint.addEventListener(UIEvents.TICK, e -> hint.setText(hintText(hatch, selected[0])));

        root.addChild(VendorUIHelper.playerInventory(7, 154));
        return ModularUI.of(RollUi.of(player, root), player);
    }

    private static UIElement itemConfigSlot(MeInputHatchBlockEntity hatch, int slot, Runnable select) {
        ItemSlot configSlot = VendorUIHelper.phantomSlot();
        configSlot.setItem(configItem(hatch, slot), false);
        configSlot.addEventListener(UIEvents.TICK, e -> {
            ItemStack shown = configItem(hatch, slot);
            if (!ItemStack.matches(shown, configSlot.getValue())) configSlot.setItem(shown, false);
        });
        configSlot.addEventListener(UIEvents.CLICK, e -> {
            ItemStack cursor = carried(configSlot);
            if (cursor == null) return;
            if (e.button == 1) {
                PacketDistributor.sendToServer(MeHatchConfigPacket.clear(hatch.getBlockPos(), slot));
            } else if (cursor.isEmpty()) {
                select.run();
            } else {
                AEItemKey key = AEItemKey.of(cursor);
                if (key != null) {
                    PacketDistributor.sendToServer(
                            MeHatchConfigPacket.set(hatch.getBlockPos(), slot, key, cursor.getCount()));
                }
            }
            e.stopPropagation();
        });
        // A JEI/EMI ghost drag writes the slot's value directly.
        configSlot.registerValueListener(dropped -> {
            if (dropped == null || dropped.isEmpty()) return;
            if (ItemStack.isSameItemSameComponents(dropped, configItem(hatch, slot))) return;
            AEItemKey key = AEItemKey.of(dropped);
            if (key != null) PacketDistributor.sendToServer(MeHatchConfigPacket.set(hatch.getBlockPos(), slot, key, 1));
        });
        configSlot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            if (configSlot.getValue().isEmpty()) e.hoverTooltips = tooltip(Component.translatable("menu.roll_mod.me_hatch.config.help.item", MeHatches.ITEM_INPUT_CAPACITY));
        });
        return configSlot;
    }

    private static UIElement fluidConfigSlot(MeInputHatchBlockEntity hatch, int slot, Runnable select) {
        FluidSlot configSlot = new FluidSlot();
        // Client-only XEI rendering, as VendorUIHelper.phantomSlot does for items.
        if (FMLEnvironment.dist.isClient()) configSlot.xeiPhantom();
        configSlot.setFluid(configFluid(hatch, slot), false);
        configSlot.addEventListener(UIEvents.TICK, e -> {
            FluidStack shown = configFluid(hatch, slot);
            if (!FluidStack.matches(shown, configSlot.getValue())) configSlot.setFluid(shown, false);
        });
        configSlot.addEventListener(UIEvents.CLICK, e -> {
            ItemStack cursor = carried(configSlot);
            if (cursor == null) return;
            if (e.button == 1) {
                PacketDistributor.sendToServer(MeHatchConfigPacket.clear(hatch.getBlockPos(), slot));
            } else if (cursor.isEmpty()) {
                select.run();
            } else {
                FluidUtil.getFluidContained(cursor).ifPresent(contained -> {
                    AEFluidKey key = AEFluidKey.of(contained);
                    if (key != null) {
                        PacketDistributor.sendToServer(
                                MeHatchConfigPacket.set(hatch.getBlockPos(), slot, key, contained.getAmount()));
                    }
                });
            }
            e.stopPropagation();
        });
        configSlot.registerValueListener(dropped -> {
            if (dropped == null || dropped.isEmpty()) return;
            if (FluidStack.isSameFluidSameComponents(dropped, configFluid(hatch, slot))) return;
            AEFluidKey key = AEFluidKey.of(dropped);
            if (key != null) {
                long mb = dropped.getAmount() > 0 ? dropped.getAmount() : AEFluidKey.AMOUNT_BUCKET;
                PacketDistributor.sendToServer(MeHatchConfigPacket.set(hatch.getBlockPos(), slot, key, mb));
            }
        });
        configSlot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            if (configSlot.getValue().isEmpty()) e.hoverTooltips = tooltip(Component.translatable("menu.roll_mod.me_hatch.config.help.fluid"));
        });
        return configSlot;
    }

    /** The config slot's item, its count the amount to keep; empty when unset. */
    private static ItemStack configItem(MeInputHatchBlockEntity hatch, int slot) {
        GenericStack stack = hatch.getConfig().getStack(slot);
        return stack != null && stack.what() instanceof AEItemKey key
                ? key.toStack((int) Math.max(1, Math.min(Integer.MAX_VALUE, stack.amount())))
                : ItemStack.EMPTY;
    }

    /** The config slot's fluid, its amount the mB to keep; empty when unset. */
    private static FluidStack configFluid(MeInputHatchBlockEntity hatch, int slot) {
        GenericStack stack = hatch.getConfig().getStack(slot);
        return stack != null && stack.what() instanceof AEFluidKey key
                ? key.toStack((int) Math.max(1, Math.min(Integer.MAX_VALUE, stack.amount())))
                : FluidStack.EMPTY;
    }

    private static Component hintText(MeInputHatchBlockEntity hatch, int selected) {
        if (selected < 0) return Component.translatable("menu.roll_mod.me_hatch.select").withStyle(ChatFormatting.GRAY);
        AEKey key = hatch.getConfig().getKey(selected);
        Component what = key == null
                ? Component.translatable("menu.roll_mod.me_hatch.empty")
                : key.getDisplayName();
        return Component.translatable("menu.roll_mod.me_hatch.selected", selected + 1, what);
    }

    /* ---------------------------------------------------- output ---------------------------------------------------- */

    /**
     * @param contents the buffer slot or tank {@code i} on the server ({@code null} when empty)
     * @param blocked  whether the last flush left something behind, read from the client's block entity
     */
    public static ModularUI output(MeHatchBlockEntity hatch, Player player, IntFunction<GenericStack> contents,
                                   BooleanSupplier blocked) {
        boolean client = player.level().isClientSide();
        UIElement root = root(160);
        header(root, hatch);

        root.addChild(label(7, 30, 160, Component.translatable("menu.roll_mod.me_hatch.buffer")));
        GenericStackDisplayHandler buffer =
                new GenericStackDisplayHandler(MeHatches.OUTPUT_SLOTS, client ? null : contents);
        int x0 = (WIDTH - MeHatches.OUTPUT_SLOTS * SLOT) / 2;
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            root.addChild(VendorUIHelper.abs(displaySlot(buffer, i), x0 + i * SLOT, 40, SLOT, SLOT));
        }

        Label state = new Label();
        root.addChild(VendorUIHelper.abs(state, 7, 62, WIDTH - 14, 10));
        state.addEventListener(UIEvents.TICK, e -> state.setText(blocked.getAsBoolean()
                ? Component.translatable("menu.roll_mod.me_hatch.buffering").withStyle(ChatFormatting.GOLD)
                : Component.translatable("menu.roll_mod.me_hatch.flushing", MeHatches.FLUSH_INTERVAL / 20)
                        .withStyle(ChatFormatting.GRAY)));

        root.addChild(VendorUIHelper.playerInventory(7, 76));
        return ModularUI.of(RollUi.of(player, root), player);
    }

    /* ---------------------------------------------------- shared ---------------------------------------------------- */

    private static UIElement root(int height) {
        // The theme's panel surface; children are placed absolutely, so its padding is dropped.
        UIElement root = VendorUIHelper.styledPanel(WIDTH, height);
        root.layout(l -> l.paddingAll(0));
        return root;
    }

    /**
     * The hatch name, and the ME status on its own row below it: 176 px cannot fit both side by side ("ME Fluid
     * Output Hatch" alone is ~120 px, the Ukrainian names wider still). Content starts at y = 28.
     */
    private static void header(UIElement root, MeHatchBlockEntity hatch) {
        root.addChild(label(7, 6, WIDTH - 14, hatch.getBlockState().getBlock().getName()));
        Label status = new Label();
        root.addChild(VendorUIHelper.abs(status, 7, 16, WIDTH - 14, 10));
        status.addEventListener(UIEvents.TICK, e -> status.setText(MeHatchBlockEntity.statusText(hatch.getMeStatus())));
    }

    /** A read-only slot showing an AE2 stack, with its exact amount on hover. */
    private static ItemSlot displaySlot(GenericStackDisplayHandler handler, int slot) {
        ItemSlot itemSlot = new ItemSlot().bind(handler, slot);
        itemSlot.slotStyle(style -> style.acceptQuickMove(false));
        itemSlot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            GenericStack shown = handler.getShown(slot);
            if (shown == null) return;
            e.hoverTooltips = new HoverTooltips(List.of(
                    shown.what().getDisplayName(),
                    Component.literal(shown.what().formatAmount(shown.amount(), AmountFormat.FULL))
                            .withStyle(ChatFormatting.GRAY)),
                    null, null, ItemStack.EMPTY);
        });
        return itemSlot;
    }

    private static int gridX(int slot) {
        return GRID_X + (slot % COLUMNS) * SLOT;
    }

    private static int gridY(int slot) {
        return (slot / COLUMNS) * SLOT;
    }

    /** The cursor stack of the open menu, {@code null} before the element is in one. */
    private static @Nullable ItemStack carried(UIElement element) {
        AbstractContainerMenu menu = element.getModularUI() == null ? null : element.getModularUI().getMenu();
        return menu == null ? null : menu.getCarried();
    }

    private static HoverTooltips tooltip(Component line) {
        return new HoverTooltips(List.of(line), null, null, ItemStack.EMPTY);
    }

    private static Label label(int x, int y, int w, Component text) {
        Label label = new Label();
        label.setText(text);
        return VendorUIHelper.abs(label, x, y, w, 10);
    }
}
