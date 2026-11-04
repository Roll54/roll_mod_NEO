package com.roll_54.roll_mod.registry;

import aztech.modern_industrialization.api.energy.EnergyApi;
import com.roll_54.roll_mod.blocks.entity.RollSolarPanelBlockEntity;
import com.roll_54.roll_mod.fluid.RollBucketItem;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.items.modularsaber.ModularSaberItem;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import java.util.List;

@EventBusSubscriber
public class CapabilityRegistry {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // NeoForge only gives BucketItem itself a fluid handler (an exact class check), so roll_mod's
        // own buckets — mutagen included — need one too, or MI tanks and other fluid-handler blocks
        // can't be filled from them by right-click.
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof RollBucketItem) {
                event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidBucketWrapper(stack), item);
            }
        }
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.TEST_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.REDSTONE_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ENERGIUM_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LAPOTRON_BATTERY_T1.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LAPOTRON_BATTERY_T2.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LAPOTRON_BATTERY_T3.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.ULTRA_BATTERY.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.HV_STORM_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LV_STORM_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.LV_ORE_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.HV_ORE_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.EV_ORE_SCANNER.get());
        ISimpleEnergyItem.registerStorage(event, ItemRegistry.MULTI_PROTECTING_GRAVI_CHESTPLATE.get());
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