package com.roll_54.roll_mod.minestar;


import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ModConfigs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

import static com.roll_54.roll_mod.data.RMMAttachment.AUTO_GIVE;
import static com.roll_54.roll_mod.init.ItemRegistry.ERROR_ITEM;

@EventBusSubscriber(modid = RollMod.MODID)
public class AutoGiveHandler {

    private static int tickCounter = 0;


    @SubscribeEvent
    public static void onServerLevelTick(ServerTickEvent.Post e) {
        var server = e.getServer();

        tickCounter++;

        if (tickCounter % ModConfigs.MAIN.TIME_TO_GIVE != 0) return; // every 10 minutes

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean autoGive = player.getData(AUTO_GIVE.get());

            if (autoGive) {

                var itemId = ModConfigs.MAIN.autogiveItem;
                var id = ResourceLocation.parse(itemId);
                var item = BuiltInRegistries.ITEM.get(id);

                if (item == Items.AIR) {
                    RollMod.LOGGER.error("Item {} does not exist!", itemId);
                    player.getInventory().add(new ItemStack(ERROR_ITEM));
                    return;
                }

                ItemStack stack = new ItemStack(item);

                ItemLore lore = new ItemLore(List.of(
                        Component.translatable("item.roll_mod.autogive_line_1"),
                        Component.translatable("item.roll_mod.autogive_line_2")
                ));

                stack.set(DataComponents.LORE, lore);

                player.getInventory().add(stack);
            }
        }

    }
}