package com.roll_54.roll_mod.client.gecko;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.armor.geckolib.ClownHatArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ClownHatModel extends GeoModel<ClownHatArmorItem> {
    @Override
    public ResourceLocation getModelResource(ClownHatArmorItem object) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "geo/clown_hat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ClownHatArmorItem object) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/armor/clown_hat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ClownHatArmorItem animatable) {
        return null;
    }
}
