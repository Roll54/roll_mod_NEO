package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.letters.LetterIcon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a letter's {@link LetterIcon}: the envelope from {@code textures/gui/hub/letters/}, or
 * whatever item or PNG the staff chose.
 *
 * <p>The envelope has an unread version with the mark painted in, {@code letter_new.png}. A custom
 * icon has no such twin, so an unread one wears the hub's {@link HubBadge} over its corner instead.
 *
 * <p>An item is drawn by an unbound phantom {@link ItemSlot} rather than an {@code ItemStackTexture}:
 * these icons sit in scrolling lists, and an item texture draws outside the UI batch, on top of the
 * list's edge — see {@code PlayerHomesTab}. The slot is never bound, for the reason
 * {@link LetterRewardChips} gives.
 */
final class LetterIcons {

    static final int SIZE = 16;

    static final IGuiTexture LETTER = SpriteTexture.of(RollMod.id("textures/gui/hub/letters/letter.png"));
    static final IGuiTexture LETTER_NEW = SpriteTexture.of(RollMod.id("textures/gui/hub/letters/letter_new.png"));

    /** Asked once per path: a missing file would draw the pink-and-black checkerboard. */
    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();

    private LetterIcons() {}

    /** The envelope, marked or not. */
    static IGuiTexture envelope(boolean unread) {
        return unread ? LETTER_NEW : LETTER;
    }

    /**
     * A {@link #SIZE}-square icon. It never takes clicks or shows a tooltip of its own, so it can sit
     * inside a clickable row.
     */
    static UIElement element(String icon, boolean unread) {
        UIElement box = new UIElement();
        box.layout(l -> l.width(SIZE).height(SIZE).flexShrink(0));
        box.setAllowHitTest(false);

        ResourceLocation id = icon == null || icon.isBlank() ? null : ResourceLocation.tryParse(icon);
        ItemStack stack = id == null ? ItemStack.EMPTY : LetterIcon.item(id);
        boolean custom;
        if (!stack.isEmpty()) {
            box.addChild(slot(stack, false));
            custom = true;
        } else if (id != null && LetterIcon.isTexture(id) && exists(id)) {
            box.style(s -> s.background(SpriteTexture.of(id)));
            custom = true;
        } else {
            box.style(s -> s.background(envelope(unread)));
            custom = false;
        }

        if (custom && unread) {
            UIElement badge = new UIElement();
            badge.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(-1).top(-1)
                    .width(HubBadge.SIZE).height(HubBadge.SIZE));
            badge.style(s -> s.background(HubBadge.texture()).zIndex(1));
            badge.setAllowHitTest(false);
            box.addChild(badge);
        }
        return box;
    }

    /** A bare slot, no frame and no hover wash; {@code interactive} keeps its clicks and tooltip. */
    static ItemSlot slot(ItemStack stack, boolean interactive) {
        ItemSlot slot = VendorUIHelper.phantomSlot();
        slot.setItem(stack.copyWithCount(1));
        slot.layout(l -> l.width(SIZE).height(SIZE));
        slot.style(s -> s.background(IGuiTexture.EMPTY));
        slot.slotStyle(s -> s.hoverOverlay(IGuiTexture.EMPTY).showItemTooltips(interactive));
        slot.setAllowHitTest(interactive);
        return slot;
    }

    static boolean exists(ResourceLocation png) {
        return PRESENT.computeIfAbsent(png, p -> net.minecraft.client.Minecraft.getInstance()
                .getResourceManager().getResource(p).isPresent());
    }
}
