package com.roll_54.roll_mod.compat.mi;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.mi.machines.ParallelMultiblocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swedz.tesseract.neoforge.capabilities.CapabilitiesListeners;
import net.swedz.tesseract.neoforge.compat.mi.TesseractMI;
import net.swedz.tesseract.neoforge.registry.holder.BlockHolder;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The registries Tesseract puts this mod's MI machines into, handed out by {@link RMMIHookRegistry}.
 *
 * <p>Tesseract's {@code HackedMachineRegistrationHelper.registerMachine} registers a machine's block,
 * item and block entity through {@code hook.registry()}, so a hook registry that returns
 * {@code null} cannot register machines at all. These are this mod's own, so machines land under
 * {@code roll_mod:}. Wiring mirrors Extended Industrialization's: holders are collected as they
 * register, their registration listeners fire on common setup, and Tesseract's capability listeners
 * fire on {@link RegisterCapabilitiesEvent}.
 */
public final class RMMIRegistries {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RollMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RollMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RollMod.MODID);

    private static final Set<BlockHolder<?>> BLOCK_HOLDERS = new LinkedHashSet<>();
    private static final Set<ItemHolder<?>> ITEM_HOLDERS = new LinkedHashSet<>();

    private RMMIRegistries() {}

    public static void register(IEventBus modBus) {
        TesseractMI.init(RollMod.MODID);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(RMMIRegistries::onCommonSetup);
        modBus.addListener(RMMIRegistries::onRegisterCapabilities);
    }

    static void include(BlockHolder<?> holder) {
        BLOCK_HOLDERS.add(holder);
    }

    static void include(ItemHolder<?> holder) {
        ITEM_HOLDERS.add(holder);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ITEM_HOLDERS.forEach(ItemHolder::triggerRegistrationListener);
            BLOCK_HOLDERS.forEach(BlockHolder::triggerRegistrationListener);
            ParallelMultiblocks.registerWorkstations();
            // After: it may join the category the line above creates.
            com.roll_54.roll_mod.compat.mi.machines.CoilPyrolyseOven.registerWorkstations();
            com.roll_54.roll_mod.compat.mi.machines.OreWashers.registerWorkstations();
            com.roll_54.roll_mod.compat.mi.machines.HighlyAdvancedDistillery.registerWorkstations();
        });
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        CapabilitiesListeners.triggerAll(RollMod.MODID, event);
    }
}
