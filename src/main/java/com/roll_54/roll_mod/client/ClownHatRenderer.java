package com.roll_54.roll_mod.client;

import com.roll_54.roll_mod.items.armor.geckolib.ClownHatArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import com.roll_54.roll_mod.client.ClownHatModel;

public class ClownHatRenderer extends GeoArmorRenderer<ClownHatArmorItem> {
    public ClownHatRenderer() {
        super(new ClownHatModel());
    }
}
