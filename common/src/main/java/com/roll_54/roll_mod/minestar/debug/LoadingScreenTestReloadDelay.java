package com.roll_54.roll_mod.minestar.debug;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * TESTING ONLY: {@code testReloadDelaySeconds} in {@code config/roll_mod/loading_screen_test.json}
 * (see {@link LoadingScreenTestDelay}) makes every client resource reload that much longer — a step
 * that just waits, off the main thread — so the loading wipe's hold and its meteorite tools can be
 * watched. Read at each reload; 0, the default, adds nothing.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class LoadingScreenTestReloadDelay implements PreparableReloadListener {

    private LoadingScreenTestReloadDelay() {}

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new LoadingScreenTestReloadDelay());
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        int seconds = LoadingScreenTestDelay.seconds(LoadingScreenTestDelay.RELOAD_KEY);
        return CompletableFuture.runAsync(() -> {
                    if (seconds <= 0) return;
                    RollMod.LOGGER.warn("[LoadingScreenTest] delaying this resource reload by {} s — testing only", seconds);
                    try {
                        Thread.sleep(seconds * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenRunAsync(() -> {}, gameExecutor);
    }

    @Override
    public String getName() {
        return "roll_mod loading-screen test delay";
    }
}
