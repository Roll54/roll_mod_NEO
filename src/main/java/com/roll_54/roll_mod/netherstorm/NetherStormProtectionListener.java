package com.roll_54.roll_mod.netherstorm;


import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import static com.roll_54.roll_mod.data.ModTags.STORM_PROTECTIVE_TAG;
import static com.roll_54.roll_mod.data.RMMAttachment.STORM_PROTECTED;
import static com.roll_54.roll_mod.netherstorm.StormHandler.isStormActive;

@EventBusSubscriber(modid = RollMod.MODID)
public final class NetherStormProtectionListener {

    private NetherStormProtectionListener() {}

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent e) {

        if (!(e.getEntity() instanceof ServerPlayer player)) return;

        ResourceKey<Level> to = e.getDimension();

        if (to != Level.NETHER) return;

        boolean hasFullSet = true;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack piece = player.getItemBySlot(slot);
                if (!piece.is(STORM_PROTECTIVE_TAG)) {
                    hasFullSet = false;
                    break;
                }
            }
        }

        boolean hasEffect = player.hasEffect(ModEffects.SULFUR_RESISTANCE);
        boolean hasPrimeProtection = player.hasData(STORM_PROTECTED);

        if (hasFullSet || hasEffect || !isStormActive() || hasPrimeProtection) return;

        e.setCanceled(true);

        // 💬 Повідомлення ГРАВЦЮ, ВІД ЙОГО ІМЕНІ
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