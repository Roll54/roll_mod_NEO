package com.roll_54.roll_mod.minestar;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Random;
@EventBusSubscriber(modid = RollMod.MODID)
public class RandomMessageHandler {

    private static int tickCounter = 0;
    private static final Random random = new Random();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        tickCounter++;
        if (tickCounter >= MyConfig.INSTANCE.messages.TIME_TO_BROADCAST) {
            tickCounter = 0;
            broadcastRandomMessage(e.getServer());
        }
    }

    private static void broadcastRandomMessage(MinecraftServer server) {

        List<String> messages = MyConfig.INSTANCE.messages.broadcastMessages;
        if (messages != null && !messages.isEmpty()) {
            String messageKey = messages.get(random.nextInt(messages.size()));
            if (messageKey != null && !messageKey.isBlank()) {
                server.getPlayerList().broadcastSystemMessage(Component.translatable(messageKey), false);
            }
        }
    }
}
