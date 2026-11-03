package com.roll_54.roll_mod.gtmogs;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.mojang.serialization.Codec;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.gtmogs.api.GTMOGSAPI;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.common.CommonInit;
import com.roll_54.roll_mod.gtmogs.common.network.GTNetwork;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.utils.FormattingUtil;
import dev.emi.emi.config.EmiConfig;
import me.shedaniel.rei.api.client.REIRuntime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

/**
 * GregTech Modern Ore Gen Standalone (gtmogs 1.0.8 fork, LGPL-3.0; see META-INF/licenses/gtmogs-LICENSE),
 * built into roll_mod as roll_mod's ore generation. It is not a mod of its own: {@link RollMod} starts it,
 * and everything it names is under {@code roll_mod:} (registries, generator and indicator types, blocks,
 * network payloads, recipe viewer categories).
 */
public class GTMOGS {

    public static final String MOD_ID = RollMod.MODID;
    /**
     * Its config keeps the name it had as a mod of its own ({@code config/gtmogs.yaml}, {@code config.gtmogs.*}
     * lang keys), and so do its save data and client cache, so existing worlds and settings carry over.
     */
    public static final String CONFIG_ID = "gtmogs";
    private static final ResourceLocation TEMPLATE_LOCATION = ResourceLocation.fromNamespaceAndPath(MOD_ID, "");
    public static final Codec<ResourceLocation> GTMOGS_ID = Codec.STRING.comapFlatMap(
            str -> ResourceLocation.read(appendIdString(str)),
            s -> s.getNamespace().equals(MOD_ID) ? s.getPath() : s.toString());

    public static final String NAME = "GTMOGS";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @ApiStatus.Internal
    public static IEventBus gtModBus;

    public GTMOGS(IEventBus modBus) {
        GTMOGSAPI.instance = this;
        GTMOGS.gtModBus = modBus;
        ConfigHolder.init();

        // must be set here because of KubeJS compat
        // trying to read this before the pre-init stage
        CommonInit.init(modBus);

        modBus.addListener(GTNetwork::registerPayloads);
    }

    public static ResourceLocation id(String path) {
        if (path.isBlank()) {
            return TEMPLATE_LOCATION;
        }

        int i = path.indexOf(':');
        if (i > 0) {
            return ResourceLocation.tryParse(path);
        } else if (i == 0) {
            path = path.substring(i + 1);
        }
        // only convert it to camel_case if it has any uppercase to begin with
        if (FormattingUtil.hasUpperCase(path)) {
            path = FormattingUtil.toLowerCaseUnderscore(path);
        }
        return TEMPLATE_LOCATION.withPath(path);
    }

    public static String appendIdString(String id) {
        int i = id.indexOf(':');
        if (i > 0) {
            return id;
        } else if (i == 0) {
            return MOD_ID + id;
        } else {
            return MOD_ID + ":" + id;
        }
    }

    /**
     * @return if we're running in a production environment
     */
    public static boolean isProd() {
        return FMLLoader.isProduction();
    }

    /**
     * @return if we're not running in a production environment
     */
    public static boolean isDev() {
        return !isProd();
    }

    /**
     * @return if we're running data generation
     */
    public static boolean isDataGen() {
        return DatagenModLoader.isRunningDataGen();
    }

    /**
     * A friendly reminder that the server instance is populated on the server side only, so null/side check it!
     * 
     * @return the current minecraft server instance
     */
    public static MinecraftServer getMinecraftServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * @param modId the mod id to check for
     * @return if the mod whose id is {@code modId} is loaded or not
     */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * For async stuff use this, otherwise use {@link GTMOGS isClientSide}
     * 
     * @return if the current thread is the client thread
     */
    @SuppressWarnings("ConstantValue")
    public static boolean isClientThread() {
        return isClientSide() && Minecraft.getInstance() != null && Minecraft.getInstance().isSameThread();
    }

    /**
     * @return if the game is the <strong>PHYSICAL</strong> client, e.g. not a dedicated server.
     * @apiNote Do not use this to check if you're currently on the server thread for side-specific actions!
     *          It does <strong>NOT</strong> work for that. Use {@link #isClientThread()} instead.
     * @see #isClientThread()
     */
    public static boolean isClientSide() {
        return FMLEnvironment.dist.isClient();
    }

    /**
     * This check isn't the same for client and server!
     * 
     * @return if it's safe to access the current instance {@link net.minecraft.world.level.Level Level} on client or if
     *         it's safe to access any level on server.
     */
    public static boolean canGetServerLevel() {
        if (isClientSide()) {
            return Minecraft.getInstance().level != null;
        }
        var server = getMinecraftServer();
        return server != null &&
                !(server.isStopped() || server.isShutdown() || !server.isRunning() || server.isCurrentlySaving());
    }

    /**
     * @return the path to the minecraft instance directory
     */
    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static class Mods {

        public static boolean isAnyRecipeViewerLoaded() {
            return isModLoaded(GTValues.MODID_EMI) || isModLoaded(GTValues.MODID_JEI) ||
                    isModLoaded(GTValues.MODID_REI);
        }

        public static boolean isJEILoaded() {
            return !(isModLoaded(GTValues.MODID_EMI) || isModLoaded(GTValues.MODID_REI)) &&
                    isModLoaded(GTValues.MODID_JEI);
        }

        public static boolean isREILoaded() {
            return isModLoaded(GTValues.MODID_REI) && (!isClientSide() || REIRuntime.getInstance().isOverlayVisible());
        }

        public static boolean isEMILoaded() {
            return isModLoaded(GTValues.MODID_EMI) && (!isClientSide() || EmiConfig.enabled);
        }

        public static boolean isKubeJSLoaded() {
            return isModLoaded(GTValues.MODID_KUBEJS);
        }

        public static boolean isFTBTeamsLoaded() {
            return isModLoaded(GTValues.MODID_FTB_TEAMS);
        }

        public static boolean isHeraclesLoaded() {
            return isModLoaded(GTValues.MODID_HERACLES);
        }

        public static boolean isFTBQuestsLoaded() {
            return isModLoaded(GTValues.MODID_FTB_QUEST);
        }

        public static boolean isArgonautsLoaded() {
            return isModLoaded(GTValues.MODID_ARGONAUTS);
        }
    }
}
