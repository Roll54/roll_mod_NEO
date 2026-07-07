package com.roll_54.roll_mod.mixin.adstra;

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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.swedz.extended_industrialization.item.nanosuit.NanoSuitArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grafts Ad Astra's oxygen-tank behaviour onto Extended Industrialization's nano suit chest piece.
 * The suit exposes a single-slot oxygen fluid tank (via {@link FluidProvider.Item}) and drains it each
 * tick to keep the wearer supplied with air, mirroring {@code SpaceSuitItem}. The suit keeps its own
 * energy bar; the tank is not shown on the durability bar.
 */
@Mixin(NanoSuitArmorItem.class)
public abstract class NanoArmorMixin implements FluidProvider.Item {

    // NanoSuitArmorItem has no tankSize field (that lives on Ad Astra's SpaceSuitItem), so we define
    // our own capacity here. Value is in millibuckets; 1000 matches the netherite space suit.
    @Unique
    private static final long roll_mod$TANK_SIZE = 1000L;

    @Override
    public CommonStorage<FluidResource> getFluids(ItemStack itemStack, ItemContext context) {
        // Only the chestplate carries the oxygen tank. NanoSuitArmorItem is the same class for every
        // slot, so without this guard a helmet/leggings/boots would each expose their own fillable
        // tank. `this` is the item; it knows its own slot. Non-chest pieces report no fluid storage;
        // callers (FluidUtils / FluidApi lookup) all null-check, so this is safe.
        if (((ArmorItem) (Object) this).getEquipmentSlot() != EquipmentSlot.CHEST) {
            return null;
        }
        return new SimpleFluidStorage(
                context,
                ModDataManagers.FLUID_CONTENTS.componentType(),
                1,
                FluidAmounts.toPlatformAmount(roll_mod$TANK_SIZE)
        ).filter(0, f -> f.is(ModFluidTags.OXYGEN));
    }

    // armorTick is EI's per-tick hook for worn armor (NanoSuitArmorItem overrides it); inject at TAIL
    // so the suit's own logic runs first. This replaces the copied inventoryTick override, which could
    // not work: the target does not override inventoryTick, and a mixin cannot call super on it.
    @Inject(method = "armorTick", at = @At("TAIL"))
    private void roll_mod$oxygenTick(LivingEntity entity, EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (slot != EquipmentSlot.CHEST) {
            return;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        entity.setTicksFrozen(0);
        if (entity.tickCount % 12 == 0 && roll_mod$hasOxygen(stack)) {
            if (!OxygenApi.API.hasOxygen(entity)) {
                roll_mod$consumeOxygen(stack, 1L);
            }

            if (entity.isEyeInFluid(FluidTags.WATER)) {
                roll_mod$consumeOxygen(stack, 1L);
                entity.setAirSupply(Math.min(entity.getMaxAirSupply(), entity.getAirSupply() + 40));
            }
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
