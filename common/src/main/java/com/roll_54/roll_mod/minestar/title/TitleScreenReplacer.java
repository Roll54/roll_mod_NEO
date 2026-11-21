package com.roll_54.roll_mod.minestar.title;

import com.roll_54.roll_mod.RollMod;
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Set;

/**
 * Swaps vanilla's title screen for {@link RollTitleScreen} every time one is about to open.
 *
 * <p>Every way back to the menu — the first screen after loading, leaving a world, a disconnect,
 * Back from a screen that was handed a fresh {@code new TitleScreen()} as its parent — goes through
 * {@code Minecraft.setScreen}, which posts {@link ScreenEvent.Opening} before the screen is added or
 * initialised, so the vanilla one is never shown. Replaces the pack's FancyMenu title layout.
 *
 * <p>Fzzy Config hands out the mod list's Config buttons the first time a vanilla {@link TitleScreen}
 * initialises — which, with the swap, never happens, so every Fzzy mod (this one included) had its
 * Config button greyed out. The first swap does that registration instead, the way Fzzy would.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class TitleScreenReplacer {

    private static boolean fzzyScreensRegistered;

    private TitleScreenReplacer() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) {
            registerFzzyConfigScreens();
            event.setNewScreen(RollTitleScreen.create());
        }
    }

    /** Fzzy's own {@code NetworkEventsClient.registerConfigs}, minus its "is this a TitleScreen" check. */
    private static void registerFzzyConfigScreens() {
        if (fzzyScreensRegistered) return;
        fzzyScreensRegistered = true;
        try {
            ClientConfigRegistry.INSTANCE.start$fzzy_config();
            Set<String> scopes = ClientConfigRegistry.INSTANCE.getScreenScopes$fzzy_config();
            ModList.get().forEachModInOrder(container -> {
                String modId = container.getModId();
                if (!scopes.contains(modId) || container.getCustomExtension(IConfigScreenFactory.class).isPresent()) return;
                IConfigScreenFactory factory = (mod, parent) -> {
                    Screen screen = ClientConfigRegistry.INSTANCE.provideScreen$fzzy_config(modId);
                    return screen != null ? screen : parent;
                };
                container.registerExtensionPoint(IConfigScreenFactory.class, (java.util.function.Supplier<IConfigScreenFactory>) () -> factory);
            });
        } catch (Throwable t) {
            // A Fzzy update renaming these internals must cost the Config buttons, not the menu.
            RollMod.LOGGER.warn("Could not register Fzzy Config screens for the mod list", t);
        }
    }
}
