package com.roll_54.roll_mod.minestar;


import com.roll_54.roll_mod.RollMod;
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

import static com.roll_54.roll_mod.init.ItemRegistry.ERROR_ITEM;

@EventBusSubscriber(modid = RollMod.MODID)
public class AutoGiveHandler {

    public static final String ITEM_TO_GIVE = "minecraft:armadillo_scute";
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerLevelTick(ServerTickEvent.Post e) {
        var server = e.getServer();

        tickCounter++;

        // Кожні 20 тік
        if (tickCounter % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            boolean autogive = player.getPersistentData().getBoolean("roll_mod:autogive");

            if (autogive) {

                var id = ResourceLocation.parse(ITEM_TO_GIVE);
                var item = BuiltInRegistries.ITEM.get(id);

                if (item == Items.AIR) {
                    RollMod.LOGGER.error("Item {} does not exist!", ITEM_TO_GIVE);
                    player.getInventory().add(new ItemStack(ERROR_ITEM));
                    return;
                }

                // ----------------------------
                // Додаємо 2 ЛОКАЛІЗОВАНІ РЯДКИ
                // ----------------------------
                ItemStack stack = new ItemStack(item);

                ItemLore lore = new ItemLore(List.of(
                        Component.translatable("item.roll_mod.autogive_line_1"),
                        Component.translatable("item.roll_mod.autogive_line_2")
                ));

                stack.set(DataComponents.LORE, lore);

                // Видати предмет
                player.getInventory().add(stack);

                RollMod.LOGGER.debug("[Autogive] {} -> gave item", player.getGameProfile().getName());
            }else {
                RollMod.LOGGER.debug("[Autogive] {} -> skipped",
                        player.getGameProfile().getName());
            }
        }
    }
}