package com.roll_54.roll_mod.mixin.aae;

import com.roll_54.roll_mod.compat.aae.AaeOxygenCard;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.util.NanoSuitUtils;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.common.registry.ModDataManagers;
import earth.terrarium.adastra.common.tags.ModFluidTags;
import earth.terrarium.common_storage_lib.context.ItemContext;
import earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.fluid.util.FluidProvider;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.resources.fluid.util.FluidAmounts;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.armors.QuantumChestplate;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.roll_54.roll_mod.netherstorm.StormHandler.isPlayerProtectedFromStormWithOutArmor;

/**
 * Gives Advanced AE's quantum chestplate the same Ad Astra oxygen tank as the nano chestplate
 * ({@code NanoArmorMixin}): 10 B, or 40 B with the oxygen tank card installed
 * ({@link NanoSuitUtils#quantumTankSize}). The capacity is read per call, so installing or toggling the card takes
 * effect at once. Filled by the Oxygen Loader like any space suit; drained here while worn.
 */
@Mixin(QuantumChestplate.class)
public abstract class QuantumChestplateOxygenMixin implements FluidProvider.Item {

    @Override
    public CommonStorage<FluidResource> getFluids(ItemStack stack, ItemContext context) {
        return new SimpleFluidStorage(
                context,
                ModDataManagers.FLUID_CONTENTS.componentType(),
                1,
                FluidAmounts.toPlatformAmount(NanoSuitUtils.quantumTankSize(stack))
        ).filter(0, f -> f.is(ModFluidTags.OXYGEN));
    }

    /** The chestplate takes the oxygen tank card in AAE's card slot, like its own cards. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void roll_mod$acceptOxygenCard(Item.Properties properties, CallbackInfo ci) {
        List<UpgradeType> possible = ((QuantumArmorBase) (Object) this).getPossibleUpgrades();
        if (AaeOxygenCard.TYPE != null && !possible.contains(AaeOxygenCard.TYPE)) {
            possible.add(AaeOxygenCard.TYPE);
        }
    }

    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void roll_mod$oxygenTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected,
                                     CallbackInfo ci) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        // A card installed by the first version (a bare roll_mod component) becomes a real AAE upgrade.
        if (stack.has(ComponentsRegistry.OXYGEN_TANK_CARD.get()) && AaeOxygenCard.TYPE != null) {
            stack.remove(ComponentsRegistry.OXYGEN_TANK_CARD.get());
            ((QuantumArmorBase) (Object) this).applyUpgrade(stack, AaeOxygenCard.TYPE);
        }
        // inventoryTick also runs for a chestplate lying in the inventory; only a worn one breathes.
        if (living.getItemBySlot(EquipmentSlot.CHEST) != stack) return;
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        living.setTicksFrozen(0);
        if (living.tickCount % 12 != 0 || !roll_mod$hasOxygen(stack)) return;

        if (!OxygenApi.API.hasOxygen(living)) {
            roll_mod$consumeOxygen(stack, 1L);
        }
        if (living.isEyeInFluid(FluidTags.WATER)) {
            roll_mod$consumeOxygen(stack, 1L);
            living.setAirSupply(Math.min(living.getMaxAirSupply(), living.getAirSupply() + 40));
        }
        if (living instanceof ServerPlayer serverPlayer && !isPlayerProtectedFromStormWithOutArmor(serverPlayer)) {
            roll_mod$consumeOxygen(stack, 3L);
        }
    }

    @Unique
    private boolean roll_mod$hasOxygen(ItemStack stack) {
        CommonStorage<FluidResource> container = new ModifyOnlyContext(stack).find(FluidApi.ITEM);
        return container != null && container.getAmount(0) > FluidAmounts.toPlatformAmount(1L);
    }

    @Unique
    private void roll_mod$consumeOxygen(ItemStack stack, long amount) {
        CommonStorage<FluidResource> container = new ModifyOnlyContext(stack).find(FluidApi.ITEM);
        if (container != null) {
            container.extract(container.getResource(0), FluidAmounts.toPlatformAmount(amount), false);
        }
    }
}
