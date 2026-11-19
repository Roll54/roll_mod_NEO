package com.roll_54.roll_mod.gtmogs;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.common.CommonInit;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.utils.FormattingUtil;
import dev.emi.emi.config.EmiConfig;
import me.shedaniel.rei.api.client.REIRuntime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;


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

    public static final String NAME = "GTMOGS";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @ApiStatus.Internal
    public static IEventBus gtModBus;

    public GTMOGS(IEventBus modBus) {
        GTMOGS.gtModBus = modBus;
        ConfigHolder.init();

        CommonInit.init(modBus);
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

    public static class Mods {

        public static boolean isJEILoaded() {
            return !(isModLoaded(GTValues.MODID_EMI) || isModLoaded(GTValues.MODID_REI)) &&
                    isModLoaded(GTValues.MODID_JEI);
        }

        public static boolean isREILoaded() {
            return isModLoaded(GTValues.MODID_REI) && (!isClientSide() || REIRuntime.getInstance().isOverlayVisible());
        }

        /**
         * roll_mod: also true under Truly Enough Items. TEI runs GTEMIPlugin (the ore vein pages) itself and takes
         * EMI ingredients from the pages' slots, so the slots must report EMI stacks there too. GTJEIPlugin stays
         * off under TEI ({@link #isJEILoaded} is false without the "jei" mod), so the pages are not listed twice.
         * {@code EmiConfig} is EMI's own and not part of TEI, so it is only read with EMI loaded.
         */
        public static boolean isEMILoaded() {
            if (isModLoaded(GTValues.MODID_TEI)) return true;
            return isModLoaded(GTValues.MODID_EMI) && (!isClientSide() || EmiConfig.enabled);
        }
    }
}
