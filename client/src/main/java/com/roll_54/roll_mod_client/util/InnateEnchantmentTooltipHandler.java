package com.roll_54.roll_mod_client.util;

import com.roll_54.roll_mod.items.InnateEnchantments;
import com.roll_54.roll_mod_client.RollModClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Innate enchantments add to the applied ones, so an enchanted bismuth bronze / black bronze / black
 * steel tool must show the summed level, not the component's. Vanilla adds the component's lines after
 * {@code appendHoverText}, so the swap has to happen on the finished tooltip.
 */
@EventBusSubscriber(modid = RollModClient.MODID)
public class InnateEnchantmentTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof InnateEnchantments.Carrier carrier) {
            carrier.innateEnchantments().fixAppliedTooltipLines(event.getItemStack(), event.getToolTip());
        }
    }
}
