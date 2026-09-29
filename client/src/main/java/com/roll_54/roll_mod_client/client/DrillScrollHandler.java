package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.network.packet.drill.DrillThrottlePacket;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ctrl + mouse scroll on a held modular drill adjusts its speed throttle.
 *
 * <p>The event is cancelled so the hotbar does not also scroll — Ctrl+scroll means the throttle
 * and nothing else while a modular drill is in hand. The delta travels as a relative nudge and
 * the server clamps it, so a scroll burst cannot race the drill into an illegal value.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class DrillScrollHandler {

    /** Throttle percent per scroll notch. */
    private static final int STEP = 5;

    private DrillScrollHandler() {}

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) return;
        if (!Screen.hasControlDown()) return;
        if (!(minecraft.player.getMainHandItem().getItem() instanceof ModularDrillItem)) return;

        int notches = (int) Math.signum(event.getScrollDeltaY());
        if (notches == 0) return;

        event.setCanceled(true);
        PacketDistributor.sendToServer(new DrillThrottlePacket(notches * STEP));
    }
}
