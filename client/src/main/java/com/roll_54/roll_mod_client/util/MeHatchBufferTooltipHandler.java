package com.roll_54.roll_mod_client.util;

import com.roll_54.roll_mod.compat.mi.machines.me.MeHatches;
import com.roll_54.roll_mod_client.RollModClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * An ME item output hatch broken while ME could not take its buffer carries that buffer on the item; this
 * lists it ({@link MeHatches#appendBufferTooltip}).
 */
@EventBusSubscriber(modid = RollModClient.MODID)
public class MeHatchBufferTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        MeHatches.appendBufferTooltip(event.getItemStack(), event.getToolTip());
    }
}
