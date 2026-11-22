package com.roll_54.roll_mod.minestar.title;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The server's own splash lines under the logo, mixed in with vanilla's: one time in three the title screen shows
 * one of these instead of a vanilla one. Add a line here to add a splash.
 */
public final class RollSplashes {

    private static final List<String> LINES = List.of(
            "Букви",
            "Станьте кіборгом і не отримайте кіберпсихоз!",
            "У всьому винна русня",
            "Чіпуньк");

    private RollSplashes() {}

    public static SplashRenderer pick(Minecraft mc) {
        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            return new SplashRenderer(LINES.get(ThreadLocalRandom.current().nextInt(LINES.size())));
        }
        return mc.getSplashManager().getSplash();
    }
}
