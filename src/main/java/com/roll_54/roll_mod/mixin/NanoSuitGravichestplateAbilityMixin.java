package com.roll_54.roll_mod.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.swedz.extended_industrialization.item.nanosuit.NanoSuitArmorItem;
import net.swedz.extended_industrialization.item.nanosuit.ability.NanoSuitGravichestplateAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NanoSuitGravichestplateAbility.class)
public class NanoSuitGravichestplateAbilityMixin {

    /**
     * @author roll_54
     * @reason Changes the default capacity from 16,777,216 to 1.000,000,000.
     */
    @Overwrite
    public long overrideEnergyCapacity() {
        return 1 << 30;
    }

    /**
     * @author roll_54
     * @reason Prevents flying when the armor runs out of energy.
     */
    @Overwrite
    public void tick(NanoSuitArmorItem item, LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        boolean hasEnergy = item.hasEnergy(stack);
        boolean isActive = item.isActivated(stack);

        // Якщо є енергія і гравець літає — споживаємо енергію
        if (hasEnergy && isActive && player.getAbilities().flying) {
            long stored = item.getStoredEnergy(stack);

            if (stored >= 1024L) {
                item.tryUseEnergy(stack, 1024L);
            } else {
                // енергії замало — зупиняємо політ
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.roll_mod.gravity_chestplate.warning")
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        true
                );
            }
        }
//
//        // Якщо енергії вже нема — відключаємо політ
//        if ((!hasEnergy || !isActive) && player.getAbilities().flying) {
//            player.getAbilities().flying = false;
//            player.onUpdateAbilities();
//        }
    }
}