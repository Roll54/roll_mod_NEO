package com.roll_54.roll_mod.client.gecko;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.armor.geckolib.MultiProtectingGraviChestItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MultiProtectingGraviChestPlateModel extends GeoModel<MultiProtectingGraviChestItem> {
    @Override
    public ResourceLocation getModelResource(MultiProtectingGraviChestItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "geo/item/armor/multi_protecting_gravi_chest_plate.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MultiProtectingGraviChestItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/item/armor/multi_protecting_gravi_chest_plate.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MultiProtectingGraviChestItem animatable) {
        return null;
              //  ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "animations/item/armor/multi_protecting_gravi_chest_plate.animation.json");
    }
}
