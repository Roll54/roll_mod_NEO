package com.roll_54.roll_mod.items.armor;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.TooltipArmorItem;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import com.roll_54.roll_mod.util.TooltipOptions;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.NonNull;

public class HazmatBootsItem extends TooltipArmorItem {

// todo, згадати, що це взагалі таке і як це використати.
//    private static final TagKey<Block> NETHER_SPEED_BLOCKS =
//            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "nether_speed_blocks"));

    public HazmatBootsItem(Holder<ArmorMaterial> material,
                           ArmorItem.Type type,
                           Item.Properties props,
                           TooltipOptions opts) {
        super(material, type, props, opts);
    }

// ХУЙНЯ!!!! НІЯКОГО ТІКА, ТРЕБА АТРИБУТИ
//    @Override
//    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
//        if (level.isClientSide) return;
//        if (!(entity instanceof Player player)) return;
//
//        if (player.getItemBySlot(EquipmentSlot.FEET) != stack) return;
//
//        BlockPos pos = player.blockPosition().below();
//        BlockState state = level.getBlockState(pos);
//        if (state.is(NETHER_SPEED_BLOCKS)) {
//            player.addEffect(new MobEffectInstance(
//                    MobEffects.MOVEMENT_SPEED,
//                    220,
//                    2,
//                    true,
//                    false
//            ));
//        }
//    }

    @Override
    public @NonNull ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(
                        AttributeRegistry.SULFUR_ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "gravichestplate_flight") //todo назвать по вумному.
                                //MI.id("gravichestplate_flight") стара фігня
                                , 1.5, AttributeModifier.Operation.

                                //ADD_MULTIPLIED_BASE
                                ADD_VALUE
                        ),
                        EquipmentSlotGroup.FEET)
                .build();

    }
}
