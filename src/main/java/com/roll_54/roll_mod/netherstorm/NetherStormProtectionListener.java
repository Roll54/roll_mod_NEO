package com.roll_54.roll_mod.netherstorm;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import static com.roll_54.roll_mod.netherstorm.StormHandler.isPlayerProtectedFromStorm;

@EventBusSubscriber(modid = RollMod.MODID)
public final class NetherStormProtectionListener {

    private NetherStormProtectionListener() {}

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ResourceKey<Level> to = event.getDimension();

        if (to != Level.NETHER) return;

        if (isPlayerProtectedFromStorm(player)) {
            return;
        } else {
            event.setCanceled(true);
        }


        // todo ланг кеї...
        Component msg = Component.literal(player.getName().getString() + ": ")
                .withStyle(ChatFormatting.WHITE)
                .append(
                        Component.literal("Не варто йти в незер без ")
                                .withStyle(ChatFormatting.WHITE)
                )
                .append(
                        Component.literal("Сіркостійкого костюму")
                                .withStyle(ChatFormatting.YELLOW)
                )
                .append(
                        Component.literal(" бо я помру")
                                .withStyle(ChatFormatting.WHITE)
                );

        player.sendSystemMessage(msg);
    }
}