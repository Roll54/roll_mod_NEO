package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod.minestar.CleanDropConfig;
import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

@EventBusSubscriber(modid = RollModServer.MODID)
public class CleanDrop {

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        double radius = CleanDropConfig.RADIUS_CLEAN.getAsDouble();
        int maxAllowed = CleanDropConfig.MIN_ITEMS.getAsInt();
        List<String> targetTags = (List<String>) CleanDropConfig.REMOVE_ITEMS.get();

        AABB area = new AABB(pos).inflate(radius);
        List<ItemEntity> itemsInRange = level.getEntitiesOfClass(ItemEntity.class, area);

        List<ItemEntity> taggedItems = itemsInRange.stream()
                .filter(itemEntity -> {
                    net.minecraft.world.item.ItemStack stack = itemEntity.getItem();

                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (targetTags.contains(itemId)) return true;

                    return stack.getTags().anyMatch(tagKey -> {
                        String tagName = tagKey.location().toString();
                        return targetTags.contains(tagName) || targetTags.contains("#" + tagName);
                    });
                })
                .toList();


        if (taggedItems.size() > maxAllowed) {
            for (ItemEntity item : taggedItems) {
                item.discard();
            }
        }
    }

}