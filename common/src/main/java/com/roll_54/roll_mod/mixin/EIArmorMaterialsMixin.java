package com.roll_54.roll_mod.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.swedz.extended_industrialization.EIArmorMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

@Mixin(EIArmorMaterials.class)
public abstract class EIArmorMaterialsMixin {

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
    private static Function<ResourceLocation, ArmorMaterial> roll_mod$wrapCreator(
            Function<ResourceLocation, ArmorMaterial> creator
    ) {
        return id -> roll_mod$adjustDefense(id, creator.apply(id));
    }

    @Unique
    private static ArmorMaterial roll_mod$adjustDefense(ResourceLocation id, ArmorMaterial base) {
        // only touch the two non-quantum nano materials; leave nano_quantum (0 defense) alone
        String path = id.getPath();
        if (!path.equals("nano") && !path.equals("nano_gravichestplate")) {
            return base;
        }

        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 10);
        }

        return new ArmorMaterial(
                defense,
                base.enchantmentValue(),
                base.equipSound(),
                base.repairIngredient(),
                base.layers(),
                base.toughness(),
                base.knockbackResistance()
        );
    }
}