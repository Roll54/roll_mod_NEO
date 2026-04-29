package com.roll_54.roll_mod.data.attribute;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

//Ми маємо мануально додавати теги до ентіті, я їбав...
@EventBusSubscriber(modid = RollMod.MODID)
public class AttributeEvents {

    @SubscribeEvent
    public static void onModifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, AttributeRegistry.SULFUR_ARMOR);
    }
}
