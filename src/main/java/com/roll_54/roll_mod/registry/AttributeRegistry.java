package com.roll_54.roll_mod.registry;


import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.attribute.SulfurArmorAttribute;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.registries.DeferredRegister;


public class AttributeRegistry {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, RollMod.MODID);

    public static final Holder<Attribute> SULFUR_ARMOR = ATTRIBUTES.register("sulfur_armor", () -> new SulfurArmorAttribute("sulfur_armor", 0).setSyncable(true));
    // public static final Holder<Attribute> INFINITE_DAMAGE = ATTRIBUTES.register("infinite_damage", () -> new InfiniteDamageAttribute().setSyncable(true));

}