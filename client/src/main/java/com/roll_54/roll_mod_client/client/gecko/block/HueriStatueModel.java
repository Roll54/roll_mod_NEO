package com.roll_54.roll_mod_client.client.gecko.block;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HueriStatueModel extends GeoModel<HueriStatueBlockEntity> {
    @Override
    public ResourceLocation getModelResource(HueriStatueBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "geo/block/hueri_statue.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HueriStatueBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/block/hueri_statue.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HueriStatueBlockEntity animatable) {
        return null;
    }
}
