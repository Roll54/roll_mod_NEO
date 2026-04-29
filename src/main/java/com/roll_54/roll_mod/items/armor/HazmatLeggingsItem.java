package com.roll_54.roll_mod.items.armor;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.TooltipArmorItem;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import com.roll_54.roll_mod.util.TooltipOptions;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.NonNull;

public class HazmatLeggingsItem extends TooltipArmorItem {
    public HazmatLeggingsItem(Type type, Properties props, TooltipOptions opts) {
        super(ModArmorMaterials.HAZMAT_ARMOR, type, props, opts);
    }

    @Override
    public @NonNull ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(
                        AttributeRegistry.SULFUR_ARMOR, new AttributeModifier(
                                RollMod.id("hazmatchestplate_sulfur_armor"),
                                2,
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST
                )

                .add(
                        Attributes.ARMOR, new AttributeModifier(
                                RollMod.id("hazmatchestplate_armor"),
                                2,
                                AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.CHEST
                ).build();

    }
}
