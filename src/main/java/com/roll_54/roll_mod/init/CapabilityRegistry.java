package com.roll_54.roll_mod.init;

import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber
public class CapabilityRegistry {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.TEST_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.REDSTONE_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ENERGIUM_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LAPOTRON_BATTERY_T1.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LAPOTRON_BATTERY_T2.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LAPOTRON_BATTERY_T3.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ULTRA_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.HV_STORM_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LV_STORM_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LUNAR_PHASE_CLOCK.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ADVANCED_LV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.MV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ADVANCED_MV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.HV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ADVANCED_HV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.EV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ADVANCED_EV_MINING_DRILL.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ENERGY_SWORD.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.METEORITE_METAL_NANO_SABER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.MV_ELECTRIC_SABER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.HV_ELECTRIC_SABER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.EV_ELECTRIC_SABER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.IV_ELECTRIC_SABER.get());



    }
}