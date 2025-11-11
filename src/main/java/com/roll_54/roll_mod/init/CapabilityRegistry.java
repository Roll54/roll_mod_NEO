package com.roll_54.roll_mod.init;

import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static com.roll_54.roll_mod.init.ItemRegistry.TEST_BATTERY;

@EventBusSubscriber
public class CapabilityRegistry {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ISimpleEnergyItem.registerStorage(event, TEST_BATTERY.get());
    }
}