package com.roll_54.roll_mod.fluid;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * How a fluid's name is coloured, everywhere the name shows: the fluid itself (tanks, JEI, tooltips),
 * its liquid block and its bucket. MI colours its fluid and bucket names with the fluid colour
 * ({@code MIFluidBlock#getName}, {@code MIBucketItem#getName}); this is the same, plus gradients.
 */
@FunctionalInterface
public interface FluidNameStyle {

    MutableComponent apply(Component name);

    /** The whole name in one colour, raised to MI's minimum luminance so dark fluids stay legible. */
    static FluidNameStyle solid(int rgb) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(minLuminance(rgb)));
        return name -> name.copy().setStyle(style);
    }

    /**
     * A left-to-right gradient from {@code fromRgb} to {@code toRgb}, one colour per character. The name
     * is resolved to a string first, so this follows whatever language the name is shown in.
     */
    static FluidNameStyle gradient(int fromRgb, int toRgb) {
        return name -> {
            String text = name.getString();
            MutableComponent out = Component.empty();
            int length = text.codePointCount(0, text.length());
            int index = 0;
            for (int offset = 0; offset < text.length(); ) {
                int codePoint = text.codePointAt(offset);
                float t = length <= 1 ? 0f : (float) index / (length - 1);
                out.append(Component.literal(new String(Character.toChars(codePoint)))
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(lerp(fromRgb, toRgb, t)))));
                offset += Character.charCount(codePoint);
                index++;
            }
            return out;
        };
    }

    /** MI's {@code FluidHelper.getColorMinLuminance}: scales a colour up until its luminance is at least 0.3. */
    static int minLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        double lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255d;
        if (lum >= 0.3) {
            return rgb & 0xFFFFFF;
        }
        if (lum == 0) {
            return 0x4C4C4C;
        }
        r = Math.min((int) (r * 0.3 / lum), 255);
        g = Math.min((int) (g * 0.3 / lum), 255);
        b = Math.min((int) (b * 0.3 / lum), 255);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
