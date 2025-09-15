package com.roll_54.roll_mod.netherstorm;

import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Roll_mod.MODID)
public class StormDropsHandler {

    @SubscribeEvent
    public static void onLivingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        var entity = event.getEntity();

        // Тільки для наших «штормових» Wither Skeleton
//        if (entity.getType() == EntityType.WITHER_SKELETON && entity.getPersistentData().getBoolean("roll_mod:storm_spawn")) {
        if (entity.getType() == EntityType.WITHER_SKELETON && entity.getPersistentData().getBoolean("roll_mod:storm_spawn")) {


            ServerLevel level = (ServerLevel) entity.level();
            // Приклад: дропнути ваш предмет/інгредієнт (поставте свій)
            ItemStack extra = new ItemStack(Items.NETHERITE_SCRAP, 5); // замініть на свій Item

            var itemEntity = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), extra);
            itemEntity.setDefaultPickUpDelay();

            event.getDrops().add(itemEntity);
        }
    }
}