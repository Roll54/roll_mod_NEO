package com.roll_54.roll_mod.Netherstorm;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = RollMod.MODID)
public class StormDropsHandler {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // лише сервер
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        // лише Nether
        if (level.dimension() != Level.NETHER) return;

        // лише якщо активний шторм
        if (!StormHandler.isStormActive()) return;

        // не для гравців
        if (event.getEntity() instanceof Player) return;

        // повага до gamerule doMobLoot
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        var entity = event.getEntity();

        // --- 1. Спеціальний дроп для «штормових» Wither Skeleton ---
        if (entity.getType() == EntityType.WITHER_SKELETON && entity.getPersistentData().getBoolean("roll_mod:storm_spawn")) {
            ItemStack specialDrop = new ItemStack(Items.NETHERITE_SCRAP, 5);
            ItemEntity itemEntity = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), specialDrop);
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        }

        // --- 2. Глобальний дроп під час шторму ---
        int count = 1 + level.random.nextInt(15);
        Item sulfurDust = BuiltInRegistries.ITEM.get(ResourceLocation.parse("modern_industrialization:sulfur_dust"));
        if (sulfurDust != null) {
            ItemStack stack = new ItemStack(sulfurDust, count);
            ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }
}