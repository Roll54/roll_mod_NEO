package com.roll_54.roll_mod.minestar.letters;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a letter wears in the list, as the one string the compose form types and the file stores.
 *
 * <ul>
 *   <li>blank — the default envelope, {@code textures/gui/hub/letters/letter(_new).png};</li>
 *   <li>an item id, e.g. {@code minecraft:diamond} or {@code create:wrench} — any item any mod in the
 *       pack registers, drawn with its own model;</li>
 *   <li>a texture path ending in {@code .png}, e.g. {@code create:textures/item/wrench.png} — any
 *       file in any mod's or resource pack's assets, blitted as it is.</li>
 * </ul>
 *
 * <p>The server checks an item id against the registry, since both sides share it. A texture path
 * can only be checked where the assets are, so the client falls back to the envelope for a file it
 * does not have.
 */
public final class LetterIcon {

    public static final int MAX = 256;

    private LetterIcon() {}

    /** The icon as stored: trimmed, and blank if it names nothing usable. */
    public static String clean(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.isEmpty() || text.length() > MAX) return "";
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null) return "";
        if (isTexture(id)) return id.toString();
        return item(id).isEmpty() ? "" : id.toString();
    }

    public static boolean isTexture(ResourceLocation id) {
        return id.getPath().endsWith(".png");
    }

    /** The item an icon names, or {@link ItemStack#EMPTY} for blank, a texture, or an unknown id. */
    public static ItemStack item(ResourceLocation id) {
        if (isTexture(id)) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(id)
                .filter(item -> item != Items.AIR)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }
}
