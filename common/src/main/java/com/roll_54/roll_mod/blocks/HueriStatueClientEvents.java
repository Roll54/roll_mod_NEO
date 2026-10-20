package com.roll_54.roll_mod.blocks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Hueri statue's look-at hint: "Right-click to read" over the hotbar while the crosshair is on
 * a statue this player has not opened yet. Re-sent once a second, so it stays up for as long as
 * they look and fades on its own once they look away.
 */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class HueriStatueClientEvents {

    private static final int HINT_EVERY = 20;
    private static int cooldown;

    private HueriStatueClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (cooldown > 0) cooldown--;
        if (ClientHueriStatue.seen || cooldown > 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.screen != null) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (!minecraft.level.getBlockState(hit.getBlockPos()).is(BlockRegistry.HUERI_STATUE.get())) return;
        minecraft.gui.setOverlayMessage(Component.translatable("hint.roll_mod.hueri_statue.read")
                .withStyle(ChatFormatting.GOLD), false);
        cooldown = HINT_EVERY;
    }

    /** Back to the "nothing to show" default, so the next server starts from what it says. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHueriStatue.seen = true;
        ClientHueriStatue.PLAYERS.clear();
    }
}
