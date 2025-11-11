package com.roll_54.roll_mod.modArmor;

import com.roll_54.roll_mod.modItems.TooltipArmorItem;
import com.roll_54.roll_mod.util.TooltipOptions;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public class HazmatBootsItem extends TooltipArmorItem {

    private static final TagKey<Block> NETHER_SPEED_BLOCKS =
            TagKey.create(Registries.BLOCK, ResourceLocation.parse("roll_mod:nether_speed_blocks"));

    public HazmatBootsItem(Holder<ArmorMaterial> material,
                           ArmorItem.Type type,
                           Item.Properties props,
                           TooltipOptions opts) {
        super(material, type, props, opts);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        if (player.getItemBySlot(EquipmentSlot.FEET) != stack) return;

        BlockPos pos = player.blockPosition().below();
        BlockState state = level.getBlockState(pos);
        if (state.is(NETHER_SPEED_BLOCKS)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    220,
                    2,
                    true,
                    false
            ));
        }
    }
}
