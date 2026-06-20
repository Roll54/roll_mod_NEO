package com.roll_54.roll_mod_client.client.gecko;

import com.roll_54.roll_mod.items.armor.geckolib.ClownHatArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ClownHatRenderer extends GeoArmorRenderer<ClownHatArmorItem> {
    public ClownHatRenderer() {
        super(new ClownHatModel());
    }
}
