package com.roll_54.roll_mod.Netherstorm;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = RollMod.MODID)
public class StormGlobalDrops {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // тільки сервер
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        // тільки Nether
        if (level.dimension() != Level.NETHER) return;

        // тільки коли шторм активний
        if (!StormHandler.isStormActive()) return;

        // не для гравців
        if (event.getEntity() instanceof Player) return;

        // поважаємо gamerule doMobLoot
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        int count = 1 + level.random.nextInt(15);

// шукаємо предмет у реєстрі за ID
        Item scrap = BuiltInRegistries.ITEM.get(ResourceLocation.parse("modern_industrialization:sulfur_dust"));
        if (scrap != null) {
            ItemStack stack = new ItemStack(scrap, count);

            ItemEntity drop = new ItemEntity(
                    level,
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    stack
            );
            drop.setDefaultPickUpDelay();

            event.getDrops().add(drop);
        }
    }
}