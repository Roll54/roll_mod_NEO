package com.roll_54.roll_mod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {@code <tag:…>} markers in the literal text of a component tree — what
 * {@link ItemMentions} and the Hueri statue's player tags both do.
 */
public final class TextTags {

    private TextTags() {}

    /**
     * {@code component} with every match of {@code tag} in its literal text, siblings included,
     * replaced by what {@code replacement} makes of it; a {@code null} replacement leaves that match
     * as typed. Untouched when nothing matches, so the common case allocates nothing.
     *
     * <p>A tagged literal becomes an empty component with the original style, the text split into
     * children under it, so the pieces around a tag inherit that style exactly as the whole did.
     */
    public static Component replace(Component component, Pattern tag,
                                    Function<Matcher, @Nullable Component> replacement) {
        if (!containsAnywhere(component, tag)) return component;
        return rebuild(component, tag, replacement);
    }

    private static boolean containsAnywhere(Component component, Pattern tag) {
        if (component.getContents() instanceof PlainTextContents plain && tag.matcher(plain.text()).find()) return true;
        for (Component sibling : component.getSiblings()) {
            if (containsAnywhere(sibling, tag)) return true;
        }
        return false;
    }

    private static MutableComponent rebuild(Component component, Pattern tag,
                                            Function<Matcher, @Nullable Component> replacement) {
        ComponentContents contents = component.getContents();
        MutableComponent out;
        if (contents instanceof PlainTextContents plain && tag.matcher(plain.text()).find()) {
            out = Component.empty().withStyle(component.getStyle());
            String text = plain.text();
            Matcher m = tag.matcher(text);
            int last = 0;
            while (m.find()) {
                Component replaced = replacement.apply(m);
                if (replaced == null) continue;
                if (m.start() > last) out.append(Component.literal(text.substring(last, m.start())));
                out.append(replaced);
                last = m.end();
            }
            if (last < text.length()) out.append(Component.literal(text.substring(last)));
        } else {
            out = MutableComponent.create(contents).withStyle(component.getStyle());
        }
        for (Component sibling : component.getSiblings()) out.append(rebuild(sibling, tag, replacement));
        return out;
    }
}
