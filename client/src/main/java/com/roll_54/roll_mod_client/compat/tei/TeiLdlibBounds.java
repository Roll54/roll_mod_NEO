package com.roll_54.roll_mod_client.compat.tei;

import com.roll_54.roll_mod.minestar.hub.gui.LdlibScreenBounds;
import com.roll_54.roll_mod_client.RollModClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Tells TEI where an LdLib screen's window is ({@link LdlibScreenBounds}), so it lays its item list and sidebars out
 * around it instead of over it — the arrows, pins and "Page 1 of 1" were drawn inside machine windows.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TeiLdlibBounds {

    private TeiLdlibBounds() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("tei")) event.enqueueWork(Tei::register);
    }

    /** Kept apart so TEI's classes are only resolved with TEI loaded. */
    private static final class Tei {
        static void register() {
            dev.lorp.tei.client.ExclusionAreas.addBounds("roll_mod", null, screen -> {
                int[] r = LdlibScreenBounds.of(screen, screen.width, screen.height);
                return r == null ? null : new dev.lorp.tei.client.ExclusionAreas.Rect(r[0], r[1], r[2], r[3]);
            });
        }
    }
}
