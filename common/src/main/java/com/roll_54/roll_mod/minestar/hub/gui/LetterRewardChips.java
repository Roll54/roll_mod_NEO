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
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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
 * allocates and silently break every hub tab built after it.
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
            case COMMAND -> icon(COMMAND_ICON, Component.translatable("gui.roll_mod.letters.reward.command",
                    Component.literal("/" + reward.command()).withStyle(ChatFormatting.AQUA)), tip);
        };
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
