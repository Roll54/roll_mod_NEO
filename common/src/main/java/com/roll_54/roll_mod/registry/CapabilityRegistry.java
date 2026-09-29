package com.roll_54.roll_mod.registry;

import aztech.modern_industrialization.api.energy.EnergyApi;
import com.roll_54.roll_mod.blocks.entity.CropManagerBlockEntity;
import com.roll_54.roll_mod.blocks.entity.RollSolarPanelBlockEntity;
import com.roll_54.roll_mod.blocks.entity.WeedManagerBlockEntity;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.items.modularsaber.ModularSaberItem;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import java.util.List;

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
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.MULTI_PROTECTING_GRAVI_CHESTPLATE.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.IV_ELECTRIC_PICKAXE.get());
        // Modular drills get a hand-built storage instead of registerStorage: with an XP Reactor
        // installed the drill's max input is 0, so every external charger's receive() is refused
        // and mined experience stays the only way in.
        for (var drill : List.of(ItemRegistry.LV_MODULAR_DRILL, ItemRegistry.MV_MODULAR_DRILL,
                ItemRegistry.HV_MODULAR_DRILL, ItemRegistry.EV_MODULAR_DRILL, ItemRegistry.IV_MODULAR_DRILL)) {
            ModularDrillItem item = drill.get();
            event.registerItem(ILongEnergyStorage.ITEM,
                    (stack, ctx) -> ISimpleEnergyItem.createStorage(stack,
                            item.getEnergyComponent(),
                            item.getEnergyCapacity(stack),
                            item.getEnergyMaxInput(stack),
                            0L),
                    item);
        }
        // Modular sabers, for the same reason: Battery changes the capacity and the XP Reactor locks
        // the input, so the storage must be asked per stack.
        for (var saber : List.of(ItemRegistry.LV_MODULAR_SABER, ItemRegistry.MV_MODULAR_SABER,
                ItemRegistry.HV_MODULAR_SABER, ItemRegistry.EV_MODULAR_SABER, ItemRegistry.IV_MODULAR_SABER)) {
            ModularSaberItem item = saber.get();
            event.registerItem(ILongEnergyStorage.ITEM,
                    (stack, ctx) -> ISimpleEnergyItem.createStorage(stack,
                            item.getEnergyComponent(),
                            item.getEnergyCapacity(stack),
                            item.getEnergyMaxInput(stack),
                            0L),
                    item);
        }
        // Crop Manager: the same AnyTierEnergyStore answers both energy capabilities, so an MI
        // cable of any tier and an FE cable feed the one buffer at 1 FE == 1 EU. Every face is a
        // port and no face is an output — it only ever consumes.
        event.registerBlockEntity(
                EnergyApi.SIDED,
                BlockEntites.CROP_MANAGER_BE.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                BlockEntites.CROP_MANAGER_BE.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntites.CROP_MANAGER_BE.get(),
                (blockEntity, side) -> new SidedInvWrapper((CropManagerBlockEntity) blockEntity, side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                BlockEntites.WEED_MANAGER_BE.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntites.WEED_MANAGER_BE.get(),
                (blockEntity, side) -> new SidedInvWrapper((WeedManagerBlockEntity) blockEntity, side)
        );
        // Hydroponic Garden Bed: everything is backed by the shared node, so every face is a
        // valid port and no face is an output. MIEnergyStorage extends NeoForge's IEnergyStorage,
        // so the same object serves an MI cable and an FE cable at 1 FE == 1 EU.
        event.registerBlockEntity(
                EnergyApi.SIDED,
                BlockEntites.HYDROPONIC_GARDEN_BED_BE.get(),
                (blockEntity, side) -> blockEntity.energyPort()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                BlockEntites.HYDROPONIC_GARDEN_BED_BE.get(),
                (blockEntity, side) -> blockEntity.energyPort()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntites.HYDROPONIC_GARDEN_BED_BE.get(),
                (blockEntity, side) -> blockEntity.fluidPort()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntites.HYDROPONIC_GARDEN_BED_BE.get(),
                (blockEntity, side) -> blockEntity.inputs()
        );

        // Native MI energy source: cables connecting to the panel's bottom face pull from it.
        event.registerBlockEntity(
                EnergyApi.SIDED,
                BlockEntites.SOLAR_PANEL_BE.get(),
                (blockEntity, side) -> side == Direction.DOWN ? blockEntity.getEnergyOutput() : null
        );
    }
}