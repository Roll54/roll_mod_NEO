package com.roll_54.roll_mod.items.armor.geckolib;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.TriStateToggleAbleItem;
import com.roll_54.roll_mod.mixin.MIKeyMapInvoker;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
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

public class MultiProtectingGraviChestItem extends ArmorItem implements ISimpleEnergyItem, TriStateToggleAbleItem, GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Important numbers... I need to remember this number...
    public static long ENERGY_CAPACITY = 2_000_000_000L;



    public MultiProtectingGraviChestItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties.stacksTo(1).rarity(Rarity.EPIC)
                .component(FIRST_STATE_ACTIVATED.get(), false)
                .component(SECOND_STATE_ACTIVATED.get(), false)
                .component(THIRD_STATE_ACTIVATED.get(), false));
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

        if (this.getStoredEnergy(stack) <= 0) {
            return builder.build();
        }

        if (this.isActivatedById(stack, 0)) {
            builder.add(
                    NeoForgeMod.CREATIVE_FLIGHT,
                    new AttributeModifier(RollMod.id("gravichestplate_flight"), 1, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST
            );
        }

        if (this.isActivatedById(stack, 1)) {
            builder.add(
                    AttributeRegistry.SULFUR_ARMOR,
                    new AttributeModifier(RollMod.id("gravichestplate_sulfur_armor"), 16.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST
            );
        }

        // State 2: Elytra mode (no special attributes, just animation/visuals)
        if (this.isActivatedById(stack, 2)) {
            // Elytra mode handled through rendering and animation
        }

        return builder.build();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        GeoArmorRendererRegistry.apply(this, consumer);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isActivatedById(stack, 2) && getStoredEnergy(stack) > 10000;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        // energy consumption is handled in the armor tick.
        return true;
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
            // State 0: Flight mode - 5000 energy per tick
            if (player.getAbilities().flying && isActivatedById(stack, 0)) {
                setStoredEnergy(stack, Math.max(0, getStoredEnergy(stack) - 5000));
            }

            // State 1: Sulfur armor mode - 10000 energy per tick
            if (isActivatedById(stack, 1)) {
                setStoredEnergy(stack, Math.max(0, getStoredEnergy(stack) - 10000));
            }

            // State 2: Elytra mode - 20000 energy per tick (when falling/gliding)
            if (isActivatedById(stack, 2) && player.isFallFlying()) {
                setStoredEnergy(stack, Math.max(0, getStoredEnergy(stack) - 20000));
            }

            tryBoost(stack, player);
        }
    }

    public void tryBoost(ItemStack stack, Player player) {
        if (!isActivatedById(stack, 2)) return;
        if (player.onGround()) return;
        if (!MIKeyMapInvoker.invokeIsHoldingUp(player)) return;

        long energy = getStoredEnergy(stack);
        long boostCost = 5000;

        if (energy < boostCost) return;

        setStoredEnergy(stack, energy - boostCost);

        if (player.isFallFlying()) {
            Vec3 look = player.getLookAngle();
            Vec3 velocity = player.getDeltaMovement();

            double maxSpeed = 1.5;
            double attenuation = 0.5;

            player.setDeltaMovement(
                    velocity.scale(attenuation).add(look.scale(maxSpeed))
            );
            player.hurtMarked = true;
        } else {
            Vec3 velocity = player.getDeltaMovement();

            double maxSpeed = 1.0;
            double acceleration = 0.25;

            if (velocity.y < maxSpeed) {
                player.setDeltaMovement(
                        velocity.x,
                        Math.min(maxSpeed, velocity.y + acceleration),
                        velocity.z
                );
                player.hurtMarked = true;
            }

            player.fallDistance = 0;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ObfuscationReflectionHelper.setPrivateValue(
                    ServerGamePacketListenerImpl.class,
                    serverPlayer.connection,
                    0,
                    "aboveGroundTickCount"
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.roll_mod.gravichestplate_state_0")
                    .withStyle(net.minecraft.ChatFormatting.BLUE));
            tooltip.add(Component.translatable("tooltip.roll_mod.gravichestplate_state_0_desc")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));

            tooltip.add(Component.translatable("tooltip.roll_mod.gravichestplate_state_1")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.roll_mod.gravichestplate_state_1_desc")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));

            tooltip.add(Component.translatable("tooltip.roll_mod.gravichestplate_state_2")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.roll_mod.gravichestplate_state_2_desc")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.roll_mod.general_press_shift"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }



}