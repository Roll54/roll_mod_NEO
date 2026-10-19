package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.letters.GiveCommands;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * How a letter's rewards are drawn, shared by the reader's tab and the staff composer so a letter
 * looks the same being written as being read.
 *
 * <p><b>Item slots here are never bound.</b> {@link VendorUIHelper#phantomSlot()} plus
 * {@code setItem} registers no sync value, which is the only reason slots may be created inside a
 * rebuild at all. A {@code bind(...)} on one of these would hand out a sync id the other side never
 * allocates and silently break every hub tab built after it. The item drawn on a {@code give}
 * command chip is the same kind of unbound phantom slot.
 *
 * <p>A command reward that is a {@code give} shows the item it gives (see {@link GiveCommands}); a
 * product giving several items shows the first, with every one of them listed on hover.
 */
final class LetterRewardChips {

    static final int SLOT = 18;
    /** The starcoin art the rest of the mod already uses for money. */
    private static final IGuiTexture MONEY_ICON = SpriteTexture.of(RollMod.id("textures/item/lp.png"));

    // TODO(art): placeholder — swap for a real command icon when one exists.
    private static final IGuiTexture COMMAND_ICON =
            SpriteTexture.of(RollMod.id("textures/gui/hub/teleportation/settings.png"));
    private static final int COLOR_CHIP = 0x30FFFFFF;

    private LetterRewardChips() {}

    /**
     * A wrapping row of chips, laid out right to left: the first reward sits against the right
     * edge and each next one to its left. {@code onClick} is told which reward was clicked, or is null.
     */
    static UIElement strip(List<LetterReward> rewards, @Nullable Consumer<Integer> onClick,
                           @Nullable Component tip) {
        UIElement strip = new UIElement();
        strip.layout(l -> l.flexDirection(FlexDirection.ROW_REVERSE).flexWrap(FlexWrap.WRAP)
                .widthPercent(100).gapColumn(3).gapRow(3));
        for (int i = 0; i < rewards.size(); i++) {
            final int index = i;
            UIElement chip = chip(rewards.get(i), tip);
            if (onClick != null) {
                chip.addEventListener(UIEvents.CLICK, e -> {
                    onClick.accept(index);
                    e.stopPropagation();
                });
            }
            strip.addChild(chip);
        }
        return strip;
    }

    static UIElement chip(LetterReward reward, @Nullable Component tip) {
        return switch (reward.type()) {
            case ITEM -> {
                ItemSlot slot = VendorUIHelper.phantomSlot();
                slot.setItem(reward.item().copy());
                slot.layout(l -> l.width(SLOT).height(SLOT));
                // Same plate as the money and command chips instead of the vanilla slot frame.
                slot.style(s -> s.background(new ColorRectTexture(COLOR_CHIP)));
                // The slot shows the item's own tooltip, which is the useful one here.
                yield slot;
            }
            case MONEY -> icon(MONEY_ICON, Component.translatable("gui.roll_mod.letters.reward.money",
                    Component.literal(String.valueOf(reward.amount())).withStyle(ChatFormatting.YELLOW),
                    Component.translatable("gui.roll_mod.letters.currency." + reward.currency().id())), tip);
            case COMMAND -> {
                // A labelled command reward is a thing bought, not a command: its name, and how many.
                Component what = reward.label().isEmpty()
                        ? Component.translatable("gui.roll_mod.letters.reward.command",
                                Component.literal("/" + String.join(" ; /", reward.commands())).withStyle(ChatFormatting.AQUA))
                        : Component.translatable("gui.roll_mod.letters.reward.product",
                                Component.literal(reward.label()).withStyle(ChatFormatting.GOLD), reward.repeat());
                List<ItemStack> given = GiveCommands.parse(reward, registries());
                yield given.isEmpty() ? icon(COMMAND_ICON, what, tip) : itemChip(given, what, tip);
            }
        };
    }

    /**
     * A {@code give} reward: the first item it gives, at the count it will arrive in, and on hover the
     * reward's own line, every item it gives, then the item's tooltip.
     */
    private static UIElement itemChip(List<ItemStack> given, Component what, @Nullable Component tip) {
        UIElement chip = new UIElement();
        chip.layout(l -> l.width(SLOT).height(SLOT).paddingAll(1));
        chip.style(s -> s.background(new ColorRectTexture(COLOR_CHIP)));
        ItemSlot slot = VendorUIHelper.phantomSlot();
        slot.setItem(given.get(0).copy());
        slot.layout(l -> l.width(SLOT - 2).height(SLOT - 2));
        slot.style(s -> s.background(IGuiTexture.EMPTY));
        // The chip owns the hover and the click: the composer removes a reward by clicking its chip.
        slot.slotStyle(s -> s.hoverOverlay(IGuiTexture.EMPTY).showItemTooltips(false));
        slot.setAllowHitTest(false);
        chip.addChild(slot);

        List<Component> lines = new ArrayList<>();
        lines.add(what);
        for (ItemStack stack : given) {
            lines.add(Component.literal("  " + stack.getCount() + "× ").withStyle(ChatFormatting.GRAY)
                    .append(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
        }
        if (tip != null) lines.add(tip);
        ItemStack shown = given.get(0);
        chip.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                lines, null, null, shown));
        return chip;
    }

    /**
     * The registries to parse {@code give} items against, from whichever side is drawing: the client
     * level, or the running server. {@code null} before either exists, which just means no preview.
     */
    @Nullable
    private static HolderLookup.Provider registries() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            HolderLookup.Provider client = ClientRegistries.get();
            if (client != null) return client;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    /** Isolated holder for the client-only lookup, as {@code HubUI.ClientReturn} is. */
    private static final class ClientRegistries {
        @Nullable
        static HolderLookup.Provider get() {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null ? null : level.registryAccess();
        }
    }

    /**
     * An icon the size of an item slot, so money and commands line up with items in the strip; what
     * it actually is lives in the hover text.
     */
    private static UIElement icon(IGuiTexture texture, Component what, @Nullable Component tip) {
        UIElement chip = new UIElement();
        chip.layout(l -> l.width(SLOT).height(SLOT).paddingAll(1));
        chip.style(s -> s.background(new ColorRectTexture(COLOR_CHIP)));
        UIElement picture = new UIElement();
        picture.layout(l -> l.width(SLOT - 2).height(SLOT - 2));
        picture.style(s -> s.background(texture));
        chip.addChild(picture);

        List<Component> lines = new ArrayList<>();
        lines.add(what);
        if (tip != null) lines.add(tip);
        chip.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                lines, null, null, ItemStack.EMPTY));
        return chip;
    }
}
