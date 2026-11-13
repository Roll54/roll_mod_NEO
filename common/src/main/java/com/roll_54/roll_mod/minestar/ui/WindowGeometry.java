package com.roll_54.roll_mod.minestar.ui;

/**
 * Where a player last left an {@link ExpandableWindow}, and how big: a remembered preference, kept
 * by each kind of window in its own file ({@code HubWindowState}, {@code ConfigWindowState}).
 *
 * <p>The stored rectangle is never rewritten to fit the current screen. {@link #applied} returns a
 * clamped copy, so dropping to a smaller resolution or a bigger GUI scale and going back restores the
 * window exactly as it was.
 */
public interface WindowGeometry {

    /** A rectangle in GUI-scaled screen pixels. */
    record Rect(int x, int y, int w, int h) {}

    /** The window to show on a {@code screenW}x{@code screenH} screen. Pure — the preference is left alone. */
    Rect applied(int screenW, int screenH);

    boolean maximized();

    void setMaximized(boolean maximized);

    /** Records where the window now is. Only meaningful while not maximized. */
    void setRect(int x, int y, int w, int h);

    /** Default size, centred, not maximized. */
    void reset();

    /** Writes the file if anything changed since it was read or last written. */
    void save();

    /** Whether the window may be moved and resized. Maximizing always works. */
    default boolean movable() {
        return true;
    }

    /**
     * The usual {@link #applied}: the whole screen when maximized; otherwise the preference shrunk to
     * fit (never below the minimum the screen allows) and pushed back on screen. {@code x < 0} means
     * "never placed", which centres it.
     */
    static Rect fit(int x, int y, int w, int h, boolean maximized, int minW, int minH, int screenW, int screenH) {
        if (maximized) return new Rect(0, 0, screenW, screenH);
        int cw = clamp(w, Math.min(minW, screenW), screenW);
        int ch = clamp(h, Math.min(minH, screenH), screenH);
        int cx = x < 0 ? (screenW - cw) / 2 : clamp(x, 0, screenW - cw);
        int cy = x < 0 ? (screenH - ch) / 2 : clamp(y, 0, screenH - ch);
        return new Rect(cx, cy, cw, ch);
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }
}
