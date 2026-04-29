package com.roll_54.roll_mod.items.armor.geckolib;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.client.gecko.HazmatHelmetRenderer;
import com.roll_54.roll_mod.items.TooltipArmorItem;
import com.roll_54.roll_mod.items.armor.ModArmorMaterials;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import com.roll_54.roll_mod.util.TooltipOptions;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.NonNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class HazmatHelmetItem extends TooltipArmorItem implements GeoItem {


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HazmatHelmetItem(Properties properties, TooltipOptions opts) {
        super(ModArmorMaterials.HAZMAT_ARMOR, Type.HELMET, properties, opts);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HazmatHelmetRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null)
                    this.renderer = new HazmatHelmetRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @Override
    public @NonNull ItemAttributeModifiers getDefaultAttributeModifiers(@NonNull ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        builder.add(
                AttributeRegistry.SULFUR_ARMOR,
                new AttributeModifier(RollMod.id("hazmat_helmet_sulfur_armor"), 6.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD
        );
        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(RollMod.id("hazmat_helmet_armor"), 4.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD
        );




        return builder.build();
    }
}
