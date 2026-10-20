package com.roll_54.roll_mod_client.client.gecko.block;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ModuleInstallationTableModel extends GeoModel<ModuleInstallationTableBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ModuleInstallationTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "geo/block/module_installation_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ModuleInstallationTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/block/module_installation_table.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ModuleInstallationTableBlockEntity animatable) {
        return null;
    }
}
