package com.roll_54.roll_mod.economy.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/** Utility class for RGB color formatting and gradient text generation. */
public final class RgbFormatting {

  private RgbFormatting() {}

  /**
   * Builds a gradient-colored component from the given text. Non-space characters are colored with
   * a smooth gradient from startColor to endColor. Spaces are preserved without gradient stepping.
   *
   * @param text the text to apply gradient to
   * @param startColor the starting RGB color (0xRRGGBB)
   * @param endColor the ending RGB color (0xRRGGBB)
   * @return a MutableComponent with gradient colors applied
   */
  public static MutableComponent buildGradientComponent(String text, int startColor, int endColor) {
    MutableComponent root = Component.empty();

    if (text == null || text.isEmpty()) {
      return root;
    }

    int nonSpaceCount = countNonSpaceCharacters(text);
    int charIndex = 0;

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      // Preserve spaces (no gradient step)
      if (c == ' ') {
        root.append(Component.literal(" "));
        continue;
      }

      // Calculate position along the gradient (0.0 to 1.0)
      float position = (float) charIndex / Math.max(1, nonSpaceCount - 1);

      // Interpolate the RGB value
      int rgb = interpolateRgb(startColor, endColor, position);

      root.append(
          Component.literal(String.valueOf(c)).withStyle(s -> s.withColor(TextColor.fromRgb(rgb))));

      charIndex++;
    }

    return root;
  }

  /**
   * Counts the number of non-space characters in the given text.
   *
   * @param text the text to count
   * @return the count of non-space characters (minimum 1)
   */
  private static int countNonSpaceCharacters(String text) {
    int count = 0;

    for (char c : text.toCharArray()) {
      if (c != ' ') count++;
    }

    return Math.max(1, count);
  }

  /**
   * Interpolates between two RGB colors based on a ratio.
   *
   * @param startColor the starting RGB color (0xRRGGBB)
   * @param endColor the ending RGB color (0xRRGGBB)
   * @param ratio the interpolation ratio (0.0 to 1.0)
   * @return the interpolated RGB color
   */
  public static int interpolateRgb(int startColor, int endColor, float ratio) {
    // Extract RGB components from start color
    int r1 = (startColor >> 16) & 0xFF;
    int g1 = (startColor >> 8) & 0xFF;
    int b1 = startColor & 0xFF;

    // Extract RGB components from end color
    int r2 = (endColor >> 16) & 0xFF;
    int g2 = (endColor >> 8) & 0xFF;
    int b2 = endColor & 0xFF;

    // Interpolate each component
    int r = (int) (r1 + (r2 - r1) * ratio);
    int g = (int) (g1 + (g2 - g1) * ratio);
    int b = (int) (b1 + (b2 - b1) * ratio);

    // Combine back into RGB integer
    return (r << 16) | (g << 8) | b;
  }
}
