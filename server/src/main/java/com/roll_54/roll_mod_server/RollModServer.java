package com.roll_54.roll_mod_server;

import com.roll_54.roll_mod.minestar.deathlog.DeathLogService;
import com.roll_54.roll_mod.minestar.friends.FriendsProvider;
import com.roll_54.roll_mod_server.minestar.PrimeManager;
import com.roll_54.roll_mod.RollMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for the dedicated-server jar. All server features register themselves
 * through {@code @EventBusSubscriber(modid = RollModServer.MODID)} game-bus handlers,
 * so the constructor only needs to exist.
 *
 * <p>The Minestar mod and DimThreads are optional: their classes here are only compiled when their jars are in
 * {@code server/jars} (server/build.gradle), and only touched when they are also loaded at runtime ({@link #optional}).
 */
@Mod(value = RollModServer.MODID, dist = Dist.DEDICATED_SERVER)
public final class RollModServer {
    public static final String MODID = "roll_mod_server";

    public RollModServer(ModContainer container) {
        // The hub's friends tab lives in :common, which cannot see the Minestar client; this jar can.
        if (present("ua.com.minestar.Minestar")) {
            Object provider = optional("com.roll_54.roll_mod_server.minestar.MinestarFriendsProvider");
            if (provider instanceof FriendsProvider friends) FriendsProvider.set(friends);
        }
        // Prime keeps a player's inventory through death; the death log marks those deaths so a
        // moderator is warned that restoring one would hand out a second copy.
        DeathLogService.setKeepsInventory(PrimeManager::hasPrime);
        // DimThreads ticks the dimensions on their own threads; the per-level crash guard goes with them.
        if (present("me.srrapero720.dimthread.api.DimThreadApi") || net.neoforged.fml.ModList.get().isLoaded("dimthread")) {
            try {
                Class.forName("com.roll_54.roll_mod_server.crash.DimThreadsGuard").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException | LinkageError e) {
                RollMod.LOGGER.info("[{}] DimThreads crash guard not available: {}", MODID, e.toString());
            }
        }
        RollMod.LOGGER.info("[{}] init complete.", MODID);
    }

    /** Whether {@code className} can be loaded: its mod is installed. */
    public static boolean present(String className) {
        try {
            Class.forName(className, false, RollModServer.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** A new instance of an optional class (built only with its jar), or null when it is not in this build. */
    public static Object optional(String className) {
        try {
            return Class.forName(className).getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            RollMod.LOGGER.info("[{}] {} not available: {}", MODID, className, e.toString());
            return null;
        }
    }
}
