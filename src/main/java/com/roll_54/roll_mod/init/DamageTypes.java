package com.roll_54.roll_mod.init;


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
}
