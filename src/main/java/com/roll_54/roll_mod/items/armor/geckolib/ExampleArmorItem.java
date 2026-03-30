package com.roll_54.roll_mod.items.armor.geckolib;

import com.roll_54.roll_mod.client.GeckoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class ExampleArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public ExampleArmorItem(Holder<ArmorMaterial> armorMaterial, ArmorItem.Type type, Properties properties) {
        super(armorMaterial, type, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeckoArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null)
                    this.renderer = new GeckoArmorRenderer();
                // Defer creation of our renderer then cache it so that it doesn't get instantiated too early

                return this.renderer;
            }
        });
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 1, state -> {
            // Apply our generic idle animation.
            // Whether it plays or not is decided down below.
            //   state.setAnimation(DefaultAnimations.IDLE);

            state.setAnimation(RawAnimation.begin().thenLoop("gecko_armor"));
            // Let's gather some data from the state to use below
            // This is the entity that is currently wearing/holding the item
            Entity entity = state.getData(DataTickets.ENTITY);

            // We'll just have ArmorStands always animate, so we can return here
            if (entity instanceof ArmorStand || !(entity instanceof LivingEntity owner))
                return PlayState.CONTINUE;

            // For this example, we only want the animation to play if the entity is wearing all pieces of the armor
            // Let's collect the armor pieces the entity is currently wearing
//            Set<Item> wornArmor = new ObjectOpenHashSet<>();
//
//            for (ItemStack stack : owner.getArmorSlots()) {
//                // We can stop immediately if any of the slots are empty
//                if (stack.isEmpty())
//                    return PlayState.STOP;
//
//                wornArmor.add(stack.getItem());
//            }

            /*
            if (entity instanceof LivingEntity livingEntity) {
                // Check if the entity is in the air (not on the ground)
                if (!livingEntity.onGround()) {
                    // Replace "your_flying_animation" with the actual name of your flying animation
                    state.setAnimation(RawAnimation.begin().thenLoop("your_flying_animation"));
                } else {
                    // Apply our generic idle animation.
                    state.setAnimation(RawAnimation.begin().thenLoop("gecko_armor"));
                }
                return PlayState.CONTINUE;
            }
            */
            return PlayState.CONTINUE;
        }));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
