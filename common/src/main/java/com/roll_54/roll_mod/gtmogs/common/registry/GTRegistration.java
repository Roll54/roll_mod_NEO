package com.roll_54.roll_mod.gtmogs.common.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.registrate.GTRegistrate;

public class GTRegistration {

    public static final GTRegistrate REGISTRATE = GTRegistrate.create(GTMOGS.MOD_ID);
    static {
        GTRegistration.REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    }

    private GTRegistration() {/**/}
}
