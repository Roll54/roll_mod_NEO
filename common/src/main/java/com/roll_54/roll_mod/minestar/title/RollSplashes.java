package com.roll_54.roll_mod.minestar.title;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.util.List;

/**
 * The title screen's splash: now and then one of the mod's own lines, vanilla's otherwise.
 *
 * <p>The mod's lines are {@code assets/roll_mod/texts/splashes.txt}, one per line, blank lines and {@code #} comments
 * skipped, read through the resource manager so a resource pack can replace them. Without the file (or with it empty)
 * every splash is vanilla's.
 */
final class RollSplashes {

    private static final ResourceLocation FILE = RollMod.id("texts/splashes.txt");
    /** One splash in this many is the mod's own, when it has any. */
    private static final int ONE_IN = 3;
    private static final RandomSource RANDOM = RandomSource.create();

    private RollSplashes() {}

    /** A splash for this showing of the menu; null where vanilla would show none. */
    static @Nullable SplashRenderer pick(Minecraft mc) {
        List<String> own = load(mc);
        if (!own.isEmpty() && RANDOM.nextInt(ONE_IN) == 0) {
            return new SplashRenderer(own.get(RANDOM.nextInt(own.size())));
        }
        return mc.getSplashManager().getSplash();
    }

    private static List<String> load(Minecraft mc) {
        try (BufferedReader reader = mc.getResourceManager().openAsReader(FILE)) {
            return reader.lines().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
