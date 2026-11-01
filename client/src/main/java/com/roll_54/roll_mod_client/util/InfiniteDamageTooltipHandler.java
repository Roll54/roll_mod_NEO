package com.roll_54.roll_mod_client.util;

import com.roll_54.roll_mod.items.modularsaber.ModularSaberItem;
import com.roll_54.roll_mod.items.modularsaber.SaberVoltage;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * The IV saber kills in one hit ({@code SaberModuleEvents#onOneShot}), so its attack damage line reads
 * "∞ Attack Damage" instead of the attribute's number. Lowest priority, so the line is swapped after
 * any other mod has reformatted it.
 */
@EventBusSubscriber(modid = RollModClient.MODID)
public class InfiniteDamageTooltipHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModularSaberItem saber)
                || saber.voltage() != SaberVoltage.IV || !saber.isLive(stack)) return;

        String attackDamage = Component.translatable(Attributes.ATTACK_DAMAGE.value().getDescriptionId()).getString();
        List<Component> tooltip = event.getToolTip();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().contains(attackDamage)) {
                tooltip.set(i, Component.literal(" ∞ ")
                        .append(Component.translatable(Attributes.ATTACK_DAMAGE.value().getDescriptionId()))
                        .withStyle(ChatFormatting.DARK_GREEN));
                return;
            }
        }
    }
}
