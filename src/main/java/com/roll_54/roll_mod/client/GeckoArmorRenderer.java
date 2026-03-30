package com.roll_54.roll_mod.client;


import com.roll_54.roll_mod.items.armor.geckolib.ExampleArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import static com.roll_54.roll_mod.RollMod.MODID;

public class GeckoArmorRenderer extends GeoArmorRenderer<ExampleArmorItem> {
    public GeckoArmorRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(MODID, "armor/gecko_armor")));

        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}