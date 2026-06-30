package com.roll_54.roll_mod_server.util;


import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * Blocks players from travelling to The End until they have completed the vanilla
 * "Eye Spy" advancement ({@code minecraft:story/follow_ender_eye}), which is granted
 * for entering a stronghold.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class EndAdvancementGate {

    // "Eye Spy" — видається за вхід до фортеці (stronghold)
    private static final ResourceLocation IN_STRONGHOLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "story/follow_ender_eye");

    private EndAdvancementGate() {}

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (event.getDimension() != Level.END) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        AdvancementHolder holder = server.getAdvancements().get(IN_STRONGHOLD);
        if (holder == null) return; // fail open — не блокуємо, якщо ачівку не знайдено

        if (player.getAdvancements().getOrStartProgress(holder).isDone()) return;

        event.setCanceled(true);
        player.sendSystemMessage(
                Component.translatable("message.roll_mod.end_locked")
                        .withStyle(ChatFormatting.YELLOW)
        );
    }
}
