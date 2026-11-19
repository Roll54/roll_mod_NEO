package com.roll_54.roll_mod.minestar.creative;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Swaps vanilla's creative inventory for {@link RollCreativeScreen} as it opens. Vanilla's constructor
 * has already pointed the player's open menu at its item picker; that is put back to the player's own
 * inventory, which is what the LdLib screen edits.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class CreativeScreenReplacer {

    private CreativeScreenReplacer() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen().getClass() != CreativeModeInventoryScreen.class) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.containerMenu = mc.player.inventoryMenu;
        event.setNewScreen(new RollCreativeScreen());
    }
}
