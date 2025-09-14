package com.roll_54.roll_mod.ModArmor;

import com.roll_54.roll_mod.ModItems.TooltipArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazmatBootsItem extends TooltipArmorItem {

    public HazmatBootsItem(Holder<ArmorMaterial> material,
                           ArmorItem.Type type,
                           Item.Properties props,
                           int tooltipLines,
                           Integer nameColorHex,
                           Integer loreColorHex) {
        super(material, type, props, tooltipLines, nameColorHex, loreColorHex);
    }
    //todo Код неробочий, треба правити.
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        // перевіряємо що саме ці чоботи вдягнені у слот FEET
        if (player.getItemBySlot(EquipmentSlot.FEET) == stack) {
            // додаємо Speed III (рівень 2, бо рівні починаються з 0)
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    220, // 11 секунд, щоб ефект не зникав між оновленнями
                    2,   // amplifier=2 -> Speed III
                    true,
                    false
            ));
        }
    }
}
