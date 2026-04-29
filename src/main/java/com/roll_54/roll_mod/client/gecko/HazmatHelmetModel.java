package com.roll_54.roll_mod.client.gecko;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.armor.geckolib.HazmatHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HazmatHelmetModel extends GeoModel<HazmatHelmetItem> {


    @Override
    public ResourceLocation getModelResource(HazmatHelmetItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "geo/item/armor/hazmat_helmet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HazmatHelmetItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/item/armor/hazmat_helmet.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HazmatHelmetItem animatable) {
        return null;
    }
}
