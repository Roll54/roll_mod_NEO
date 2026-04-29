package com.roll_54.roll_mod.items.armor.geckolib;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.client.gecko.MultiProtectingGraviChestPlateRenderer;
import com.roll_54.roll_mod.data.TwoStateToggleableItem;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForgeMod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import static com.roll_54.roll_mod.registry.ComponentsRegistry.*;
import static net.minecraft.world.level.Level.NETHER;

public class MultiProtectingGraviChestItem extends ArmorItem implements ISimpleEnergyItem, TwoStateToggleableItem, GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Important numbers... I need to remember this number...
    public static long ENERGY_CAPACITY = 2_000_000_000L;



    public MultiProtectingGraviChestItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties.stacksTo(1).rarity(Rarity.EPIC).component(FIRST_STATE_ACTIVATED.get(), false).component(SECOND_STATE_ACTIVATED.get(), false));
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return ENERGY_CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return 500 * 1000;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF4444;
    }
    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = (double) getStoredEnergy(stack) / getEnergyCapacity(stack);
        return (int) (13 * frac);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(RollMod.id("gravichestplate_armor"), 8.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.CHEST
        );

        builder.build();

        if (this.getStoredEnergy(stack) <= 0) {
            return ItemAttributeModifiers.EMPTY;
        }



        if (this.isActivatedFirst(stack)) {
            builder.add(
                    NeoForgeMod.CREATIVE_FLIGHT,
                    new AttributeModifier(RollMod.id("gravichestplate_flight_roll_"), 1, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST
            );
        }

        if (this.isActivatedSecond(stack)) {
            builder.add(
                    AttributeRegistry.SULFUR_ARMOR,
                    new AttributeModifier(RollMod.id("gravichestplate_sulfur_armor"), 16.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST
            );
        }

        return builder.build();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MultiProtectingGraviChestPlateRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null)
                    this.renderer = new MultiProtectingGraviChestPlateRenderer();
                // Defer creation of our renderer then cache it so that it doesn't get instantiated too early

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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide())
            return;
        if (entity instanceof Player player && stack == player.getItemBySlot(EquipmentSlot.CHEST)) {
            if (player.getAbilities().flying && isActivatedFirst(stack)) {
                setStoredEnergy(stack, Math.max(0, getStoredEnergy(stack) - 5000));
            }
            if (isActivatedSecond(stack)){
                setStoredEnergy(stack, Math.max(0, getStoredEnergy(stack) - 10000));
            }
        }
    }

    //todo тут треба доробити тристейт
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("cartridge.destination")
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
            tooltip.add(Component.translatable("cartridge.required_tier")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.translatable("cartridge.fuel_required")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("bukvi"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

}