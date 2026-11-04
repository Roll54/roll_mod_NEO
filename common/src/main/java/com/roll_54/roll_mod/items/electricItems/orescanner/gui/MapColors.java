package com.roll_54.roll_mod.items.electricItems.orescanner.gui;

import com.roll_54.roll_mod.util.color.Ciede2000;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ore colours as the scanner map draws them. A material colour within ΔE2000 {@value #MIN_DELTA} of the map's empty
 * cells ({@link OreChunkMap#EMPTY}) would vanish into them, so it is drawn lighter instead: same a* and b* (hue and
 * chroma), L* raised until it sits about {@value #TARGET_DELTA} ΔE from the background. Map only — the list and
 * tooltips keep the true colour. Client side.
 */
final class MapColors {

    private static final double MIN_DELTA = 15.0;
    private static final double TARGET_DELTA = 17.5;
    private static final double MAX_DELTA = 20.0;

    private static final Map<Integer, Integer> CACHE = new ConcurrentHashMap<>();

    private MapColors() {}

    /** {@code argb}, or a lighter shade of it if it is too close to the map background. */
    static int forMap(int argb) {
        return CACHE.computeIfAbsent(argb, MapColors::compute);
    }

    private static int compute(int argb) {
        int background = OreChunkMap.EMPTY & 0xFFFFFF;
        int rgb = argb & 0xFFFFFF;
        if (Ciede2000.deltaE(rgb, background) >= MIN_DELTA) return argb;

        double[] lab = Ciede2000.lab(rgb);
        double[] bg = Ciede2000.lab(background);
        // The background is dark, so lighter is the way out; ΔE grows with L* from here, so bisect for the target.
        double low = Math.max(lab[0], bg[0]);
        double high = 100.0;
        int best = rgb;
        for (int i = 0; i < 24; i++) {
            double mid = (low + high) / 2.0;
            int candidate = Ciede2000.rgb(new double[]{mid, lab[1], lab[2]});
            double delta = Ciede2000.deltaE(candidate, background);
            if (delta >= MIN_DELTA) best = candidate;
            if (delta >= MIN_DELTA && delta <= MAX_DELTA && Math.abs(delta - TARGET_DELTA) < 0.5) break;
            if (delta < TARGET_DELTA) low = mid;
            else high = mid;
        }
        return (argb & 0xFF000000) | best;
    }
}
