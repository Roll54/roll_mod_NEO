package com.roll_54.roll_mod_client.util;

import com.roll_54.roll_mod.items.modulardrill.NewItemVisibility;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * While the client hides new items, their tooltip is just the placeholder name: every other line —
 * the item's own, attributes, advanced info, other mods' additions — is dropped. Lowest priority so
 * lines added by anyone else are caught too.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class NewItemTooltipHandler {

    private NewItemTooltipHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!NewItemVisibility.hidden() || !NewItemVisibility.isNew(event.getItemStack())) return;
        List<Component> lines = event.getToolTip();
        if (lines.size() > 1) lines.subList(1, lines.size()).clear();
    }
}
