package com.roll_54.roll_mod.minestar.debug;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TESTING ONLY: makes game loading take longer on purpose, so the loading screen (the
 * {@code earlywindow} jar: slides, crossfades, captions, bars) can be watched for as long as needed.
 *
 * <p>{@code config/roll_mod/loading_screen_test.json}:
 * <pre>{ "testDelaySeconds": 0, "testReloadDelaySeconds": 0 }</pre>
 * <ul>
 *   <li>{@code testDelaySeconds} above 0: roll_mod's construction waits that many seconds with its own
 *       progress bar first in the list, so the delay is visible as a loading task (the early window);</li>
 *   <li>{@code testReloadDelaySeconds} above 0: every resource reload — the startup's and F3+T's —
 *       waits that long in one of its steps ({@link LoadingScreenTestReloadDelay}), for the loading
 *       wipe's hold and its meteorite tools.</li>
 * </ul>
 * Both capped at 600. The file is created with both at 0 — off — when missing. Physical client only:
 * a server never waits. Keep them at 0 in anything shipped to players.
 */
public final class LoadingScreenTestDelay {

    private static final String KEY = "testDelaySeconds";
    static final String RELOAD_KEY = "testReloadDelaySeconds";
    private static final int MAX_SECONDS = 600;

    private LoadingScreenTestDelay() {}

    public static void runIfConfigured() {
        if (!FMLEnvironment.dist.isClient()) return;
        int seconds = seconds(KEY);
        if (seconds <= 0) return;

        RollMod.LOGGER.warn("[LoadingScreenTest] delaying loading by {} s ({} in {}) — testing only", seconds, KEY, file());
        ProgressMeter bar = StartupNotificationManager.prependProgressBar("roll_mod: loading-screen test delay", seconds);
        try {
            for (int i = 0; i < seconds; i++) {
                bar.label("Test delay: " + (seconds - i) + " s left");
                Thread.sleep(1000L);
                bar.increment();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            bar.complete();
            StartupNotificationManager.popBar(bar);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("loading_screen_test.json");
    }

    /** One of the delays, 0..{@link #MAX_SECONDS}; 0 when unset or unreadable. Creates the file, all off, when missing. */
    static int seconds(String key) {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            try {
                Files.createDirectories(file.getParent());
                JsonObject json = new JsonObject();
                json.addProperty("_comment", "TESTING ONLY. Seconds the game waits while loading (testDelaySeconds) "
                        + "and in every resource reload (testReloadDelaySeconds), to look at the loading screens. 0 = off.");
                json.addProperty(KEY, 0);
                json.addProperty(RELOAD_KEY, 0);
                MinestarFiles.write(file, json);
            } catch (Exception e) {
                RollMod.LOGGER.warn("[LoadingScreenTest] could not create {}", file, e);
            }
            return 0;
        }
        JsonObject json = MinestarFiles.readObject(file);
        try {
            return json.has(key) ? Math.max(0, Math.min(json.get(key).getAsInt(), MAX_SECONDS)) : 0;
        } catch (Exception e) {
            RollMod.LOGGER.warn("[LoadingScreenTest] {} in {} is not a number; no delay", key, file);
            return 0;
        }
    }
}
