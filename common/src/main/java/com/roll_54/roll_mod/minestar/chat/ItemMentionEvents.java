package com.roll_54.roll_mod.minestar.chat;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.ItemMentions;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * {@code <item:…>} tags in an operator's chat message, expanded (see {@link ItemMentions}). Anyone
 * else's message goes out as typed. Late, so a handler that rewrites the text first still has its
 * tags expanded.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ItemMentionEvents {

    private ItemMentionEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChat(ServerChatEvent event) {
        if (!event.getPlayer().hasPermissions(2) || !event.getRawText().contains("<item:")) return;
        event.setMessage(ItemMentions.expand(event.getMessage()));
    }
}
