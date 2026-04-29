package com.roll_54.roll_mod.client.gecko;

import com.roll_54.roll_mod.items.armor.geckolib.MultiProtectingGraviChestItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class MultiProtectingGraviChestPlateRenderer extends GeoArmorRenderer<MultiProtectingGraviChestItem> {

    public MultiProtectingGraviChestPlateRenderer() {
        super(new MultiProtectingGraviChestPlateModel());
        this.addRenderLayer(new AutoGlowingGeoLayer(this));
    }
}
