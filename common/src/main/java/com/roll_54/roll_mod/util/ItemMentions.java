package com.roll_54.roll_mod.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code <item:minecraft:diamond>} in a text: the item's name in brackets, in its rarity colour,
 * with the item's own tooltip on hover — the way chat shows an item.
 *
 * <p><b>Operators only.</b> The tag is just text until something written by an operator expands it:
 * letters check their author when they are written ({@code LetterService}, {@code LetterCommand}),
 * JSON text checks the command source ({@code ComponentArgumentMixin}), chat checks the sender
 * ({@code ItemMentionEvents}). A tag from anyone else stays the literal text it was typed as.
 *
 * <p>An id that names no item is left as typed, so a typo shows up rather than vanishing.
 */
public final class ItemMentions {

    /** {@code <item:namespace:path>}; the namespace may be left out for {@code minecraft}. */
    public static final Pattern TAG = Pattern.compile("<item:([a-z0-9_.-]+(?::[a-z0-9_./-]+)?)>");

    private ItemMentions() {}

    /** Whether {@code source} may expand tags: an operator, or the console / a command block. */
    public static boolean allowed(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    /** The ids of every tag in {@code text}, typos included — what "did this edit add one" compares. */
    public static Set<String> ids(@Nullable String text) {
        Set<String> out = new HashSet<>();
        if (text == null || text.isEmpty()) return out;
        Matcher m = TAG.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    public static boolean contains(@Nullable String text) {
        return text != null && text.contains("<item:") && TAG.matcher(text).find();
    }

    /**
     * The slot an item's icon is drawn into: one glyph of {@link #ICON_FONT}, a font whose only
     * provider is a {@code space} advance ({@code assets/roll_mod/font/item_icon.json}). It draws
     * nothing and takes an icon's width; not being a space, it is never a line-break point, so text
     * glued to it wraps together with it. {@code LinkLabel} finds it by this glyph and
     * this font and draws the item from the run's {@code SHOW_ITEM} hover on top.
     */
    public static final String ICON = "\uE000";
    public static final ResourceLocation ICON_FONT = ResourceLocation.fromNamespaceAndPath("roll_mod", "item_icon");

    /** The component a tag becomes as text, {@code [Name]} — for chat and other text nothing draws icons in. */
    public static @Nullable MutableComponent mention(String id) {
        return mention(id, false);
    }

    /**
     * The component a tag becomes, or {@code null} when {@code id} names no item. With {@code icon}
     * it is the {@link #ICON} slot alone — the picture, no name or brackets, the name on hover — and
     * only for text a {@code LinkLabel} draws, since anywhere else the slot would be an empty gap.
     * Without, it is {@code [Name]} in the rarity's style.
     */
    public static @Nullable MutableComponent mention(String id, boolean icon) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return null;
        var item = BuiltInRegistries.ITEM.getOptional(key).orElse(null);
        if (item == null || item == Items.AIR) return null;
        ItemStack stack = new ItemStack(item);
        // The rarity's style modifier rather than its colour, so a modded rarity looks as it does in
        // the item's own tooltip.
        MutableComponent out = icon
                ? Component.literal(ICON).withStyle(s -> s.withFont(ICON_FONT))
                : Component.literal("[").append(stack.getHoverName()).append("]")
                        .withStyle(stack.getRarity().getStyleModifier());
        return out
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack))));
    }

    /**
     * {@code component} with every tag in its literal text expanded, siblings included. Untouched
     * when it holds none, so the common case allocates nothing.
     */
    public static Component expand(Component component) {
        return expand(component, false);
    }

    /** As above; {@code icon} as {@link #mention(String, boolean)} takes it. */
    public static Component expand(Component component, boolean icon) {
        return TextTags.replace(component, TAG, m -> mention(m.group(1), icon));
    }
}
