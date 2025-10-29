package com.roll_54.roll_mod.util;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = RollMod.MODID)
public class VanillaTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // 🎨 THIS IS FUCKING SHIT. BUT I DONT HAVE ANY ANOTHER WAY!!!!!!!!!
        if (stack.is(Items.NETHER_STAR)) {
            event.getToolTip().set(0,
                    Component.translatable(stack.getDescriptionId())
                            .withStyle(style -> style.withColor(0xedd080))
            );
        }
    }
}