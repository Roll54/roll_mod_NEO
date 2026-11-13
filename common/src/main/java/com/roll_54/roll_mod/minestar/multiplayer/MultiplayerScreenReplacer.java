package com.roll_54.roll_mod.minestar.multiplayer;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.mixin.client.JoinMultiplayerScreenAccessor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Swaps vanilla's server list for {@link RollMultiplayerScreen} whenever one is about to open.
 *
 * <p>Vanilla builds the list from many places — the title screen, the safety warning, a disconnect
 * screen's Back, its own Refresh — so it is swapped as it opens rather than at each caller. The new
 * screen returns to whatever the vanilla one would have. Only vanilla's own class is swapped: a mod's
 * subclass of it is left alone.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class MultiplayerScreenReplacer {

    private MultiplayerScreenReplacer() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen().getClass() == JoinMultiplayerScreen.class) {
            event.setNewScreen(new RollMultiplayerScreen(
                    ((JoinMultiplayerScreenAccessor) event.getNewScreen()).roll_mod$getLastScreen()));
        }
    }
}
