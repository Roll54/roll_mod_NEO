package com.roll_54.roll_mod_client.util;

import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Shows an item's minimal price (economy/minprice) under its name: what one costs at least on the
 * auction or in a shop block, and the whole stack when there is more than one. Nothing for items
 * without a floor, filled buckets (the server leaves them out; JEI shows the fluid instead), or
 * while the server has the module off.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class MinPriceTooltipHandler {

    private static final boolean TEI = net.neoforged.fml.ModList.get().isLoaded("tei");

    private MinPriceTooltipHandler() {}

    /** Kept apart so TEI's classes are only resolved with TEI loaded. */
    private static final class Tei {
        static boolean isStandIn(ItemStack stack) {
            return dev.lorp.tei.client.ModIngredientStacks.isCarrier(stack);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // TEI's stand-ins for effects and other non-item ingredients: not things that can be sold.
        if (TEI && Tei.isStandIn(stack)) return;
        double unit = ClientMinPrices.unit(stack);
        if (unit <= 0) return;
        Component line = stack.getCount() > 1
                ? Component.translatable("tooltip.roll_mod.minprice.stack", ClientMinPrices.format(unit),
                        stack.getCount(), ClientMinPrices.format(unit * stack.getCount()))
                : Component.translatable("tooltip.roll_mod.minprice.item", ClientMinPrices.format(unit));
        int at = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(at, line.copy().withStyle(ChatFormatting.GOLD));
        // A filled bucket or an MI tank: what the container and the fluid in it each add.
        double[] parts = ClientMinPrices.fluidParts(stack);
        if (parts != null) {
            event.getToolTip().add(at + 1, Component.translatable("tooltip.roll_mod.minprice.fluid_container",
                    ClientMinPrices.format(parts[0]), ClientMinPrices.format(parts[1])).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Another server may not have the module, or have other prices. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMinPrices.clear();
    }
}
