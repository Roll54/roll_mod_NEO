package com.roll_54.roll_mod.client.gecko;

import com.roll_54.roll_mod.items.armor.geckolib.HazmatHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HazmatHelmetRenderer extends GeoArmorRenderer<HazmatHelmetItem> {
    public HazmatHelmetRenderer() {
        super(new HazmatHelmetModel());
    }
}
