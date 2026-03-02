package com.roll_54.roll_mod.init;

import aztech.modern_industrialization.MIComponents;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public final class ItemGroups {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RollMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.main"))
                    .icon(ItemGroups::createCustomHeadIcon)
                    .displayItems((params, out) -> {


                        // Hazmat_armor
                        add(out, ItemRegistry.HAZMAT_HELMET);
                        add(out, ItemRegistry.HAZMAT_CHESTPLATE);
                        add(out, ItemRegistry.HAZMAT_LEGGINGS);
                        add(out, ItemRegistry.HAZMAT_BOOTS);

                        // Meteorite-GEAR
                        add(out, ItemRegistry.METEORITE_METAL_INGOT);
                        add(out, ItemRegistry.METEORITE_HELMET);
                        add(out, ItemRegistry.METEORITE_CHESTPLATE);
                        add(out, ItemRegistry.METEORITE_LEGGINGS);
                        add(out, ItemRegistry.METEORITE_BOOTS);
                        add(out, ItemRegistry.METEORITE_SWORD);
                        add(out, ItemRegistry.METEORITE_PICKAXE);
                        add(out, ItemRegistry.METEORITE_AXE);
                        add(out, ItemRegistry.METEORITE_SHOVEL);
                        add(out, ItemRegistry.METEORITE_HOE);
                        add(out, ItemRegistry.METEORITE_METAL_PROSPECTOR_PICKAXE);
                        // Scanners
                        add(out, ItemRegistry.LV_STORM_SCANNER);

                        ItemStack lvScanner = new ItemStack(ItemRegistry.LV_STORM_SCANNER.get());
                        lvScanner.set(MIComponents.ENERGY.get(), 1_000_000L);
                        out.accept(lvScanner);

                        add(out, ItemRegistry.HV_STORM_SCANNER);

                        ItemStack hvScanner = new ItemStack(ItemRegistry.HV_STORM_SCANNER.get());
                        hvScanner.set(MIComponents.ENERGY.get(), 10_000_000L);
                        out.accept(hvScanner);

                        //lunar clock
                        add(out, ItemRegistry.LUNAR_PHASE_CLOCK);

                        ItemStack lunarClock = new ItemStack(ItemRegistry.LUNAR_PHASE_CLOCK.get());
                        lunarClock.set(MIComponents.ENERGY.get(), 5_000_000L);
                        out.accept(lunarClock);

                        //batteries
                        add(out, ItemRegistry.REDSTONE_BATTERY);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.REDSTONE_BATTERY.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.ENERGIUM_BATTERY);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.ENERGIUM_BATTERY.get());
                            full.set(MIComponents.ENERGY.get(), 10_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.LAPOTRON_BATTERY_T1);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.LAPOTRON_BATTERY_T1.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.LAPOTRON_BATTERY_T2);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.LAPOTRON_BATTERY_T2.get());
                            full.set(MIComponents.ENERGY.get(), 50_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.LAPOTRON_BATTERY_T3);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.LAPOTRON_BATTERY_T3.get());
                            full.set(MIComponents.ENERGY.get(), 500_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.ULTRA_BATTERY);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.ULTRA_BATTERY.get());
                            full.set(MIComponents.ENERGY.get(), 10_000_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.PROSPECTOR_PICK_ITEM);

                        add(out, ItemRegistry.METEORITE_METAL_NANO_SABER);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.METEORITE_METAL_NANO_SABER.get());
                            full.set(MIComponents.ENERGY.get(), 54_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.MV_ELECTRIC_SABER);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.MV_ELECTRIC_SABER.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.HV_ELECTRIC_SABER);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.HV_ELECTRIC_SABER.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.EV_ELECTRIC_SABER);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.EV_ELECTRIC_SABER.get());
                            full.set(MIComponents.ENERGY.get(), 10_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.IV_ELECTRIC_SABER);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.IV_ELECTRIC_SABER.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.LV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.LV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.ADVANCED_LV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.ADVANCED_LV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 2_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.MV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.MV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 10_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.ADVANCED_MV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.ADVANCED_MV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 20_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.HV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.HV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 1_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.ADVANCED_HV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.ADVANCED_HV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 2_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.EV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.EV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 50_000_000_000L);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.ADVANCED_EV_MINING_DRILL);
                        {
                            ItemStack full = new ItemStack(ItemRegistry.ADVANCED_EV_MINING_DRILL.get());
                            full.set(MIComponents.ENERGY.get(), 100_000_000_000L);
                            out.accept(full);
                        }
                        add(out, ItemRegistry.SKIN_APPLICATOR);

                        // Research Workbench
                        out.accept(BlockRegistry.RESEARCH_WORKBENCH.get().asItem());
                        add(out, ItemRegistry.BLUEPRINT_FIRE_RESISTANCE);

                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEV_TAB = TABS.register(
            "dev",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.dev"))
                    .icon(() -> new ItemStack(ItemRegistry.SULFUR_BERRY.get()))
                    .displayItems((params, out) -> {

                        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistry.ITEMS.getEntries()) {
                            out.accept(entry.get());
                        }
                        for (DeferredHolder<Block, ? extends Block> entry : BlockRegistry.BLOCKS.getEntries()) {

                            Block block = entry.get();
                            Item item = block.asItem();
                            if (item != Items.AIR) {
                                out.accept(item);
                            }
                        }
                    })
                    .build()
    );


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERATED_ORES_TAB = TABS.register(
            "generated_ores",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.generated_ores"))
                    .icon(() -> new ItemStack(ItemRegistry.METEORITE_PICKAXE.get()))
                    .displayItems((params, out) -> {
                        // All items from GeneratedOreRegistry
                        for (DeferredHolder<Item, ? extends Item> entry : GeneratedOreRegistry.ITEMS.getEntries()) {
                            out.accept(entry.get());
                        }

                        // All blocks from GeneratedOreRegistry
                        for (DeferredHolder<Block, ? extends Block> entry : GeneratedOreRegistry.BLOCKS.getEntries()) {
                            Block block = entry.get();
                            Item item = block.asItem();
                            if (item != Items.AIR) {
                                out.accept(item);
                            }
                        }
                    })
                    .build()
    );

    private static <T extends Item> void add(CreativeModeTab.Output out, DeferredHolder<Item, T> h) {
        out.accept(h.get());
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    // === Іконка вкладки: кастомна голова з текстурою (порт із Fabric) ===
    private static ItemStack createCustomHeadIcon() {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);

        // === set custom texture via Data Components ===
        String valueBase64 = "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDY2YTk2ZjliZDNhOWU3YzliZDE1MjJmZDNkYzdkNTU3MmYyMjJkY2Y1N2UwYTkzMWE2OGU4YWIzNTYyZTBmZCJ9fX0=";

        GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead");
        profile.getProperties().put("textures", new Property("textures", valueBase64));

        head.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        return head;
    }
}

