package com.roll_54.roll_mod.registry;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;


public class DamageTypes {

    public static final ResourceKey<DamageType> SULFUR_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "sulur_damage"));

    public static DamageSource sulfurDamageType(Entity causer) {
        return new DamageSource(
                causer.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SULFUR_DAMAGE_TYPE),
                causer);
    }

    public static final ResourceKey<DamageType> RADIATION_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "radiation"));

    public static DamageSource radiationDamageType(Entity causer) {
        return new DamageSource(
                causer.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RADIATION_DAMAGE_TYPE),
                causer);
    }

    // Literally broken glass (ItemRegistry.BROKEN_GLASS_LITERALLY): 3 hearts per bowl. Tagged
    // #minecraft:bypasses_armor, so armor does not soften it — it is in your stomach, not on you.
    public static final ResourceKey<DamageType> BROKEN_GLASS_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "broken_glass"));

    public static DamageSource brokenGlassDamageType(Entity causer) {
        return new DamageSource(
                causer.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BROKEN_GLASS_DAMAGE_TYPE));
    }
}
