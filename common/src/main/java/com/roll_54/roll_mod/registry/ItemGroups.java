package com.roll_54.roll_mod.registry;

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

                        // Bronze-GEAR
                        add(out, ItemRegistry.BISMUTH_BRONZE_HELMET);
                        add(out, ItemRegistry.BISMUTH_BRONZE_CHESTPLATE);
                        add(out, ItemRegistry.BISMUTH_BRONZE_LEGGINGS);
                        add(out, ItemRegistry.BISMUTH_BRONZE_BOOTS);
                        add(out, ItemRegistry.BLACK_BRONZE_HELMET);
                        add(out, ItemRegistry.BLACK_BRONZE_CHESTPLATE);
                        add(out, ItemRegistry.BLACK_BRONZE_LEGGINGS);
                        add(out, ItemRegistry.BLACK_BRONZE_BOOTS);
                        add(out, ItemRegistry.STEEL_HELMET);
                        add(out, ItemRegistry.STEEL_CHESTPLATE);
                        add(out, ItemRegistry.STEEL_LEGGINGS);
                        add(out, ItemRegistry.STEEL_BOOTS);
                        add(out, ItemRegistry.BLACK_STEEL_HELMET);
                        add(out, ItemRegistry.BLACK_STEEL_CHESTPLATE);
                        add(out, ItemRegistry.BLACK_STEEL_LEGGINGS);
                        add(out, ItemRegistry.BLACK_STEEL_BOOTS);
                        add(out, ItemRegistry.BLACK_BRONZE_SWORD);
                        add(out, ItemRegistry.BLACK_BRONZE_PICKAXE);
                        add(out, ItemRegistry.BLACK_BRONZE_AXE);
                        add(out, ItemRegistry.BLACK_BRONZE_SHOVEL);
                        add(out, ItemRegistry.BLACK_BRONZE_HOE);
                        add(out, ItemRegistry.BISMUTH_BRONZE_SWORD);
                        add(out, ItemRegistry.BISMUTH_BRONZE_PICKAXE);
                        add(out, ItemRegistry.BISMUTH_BRONZE_AXE);
                        add(out, ItemRegistry.BISMUTH_BRONZE_SHOVEL);
                        add(out, ItemRegistry.BISMUTH_BRONZE_HOE);
                        add(out, ItemRegistry.BLACK_STEEL_SWORD);
                        add(out, ItemRegistry.BLACK_STEEL_PICKAXE);
                        add(out, ItemRegistry.BLACK_STEEL_AXE);
                        add(out, ItemRegistry.BLACK_STEEL_SHOVEL);
                        add(out, ItemRegistry.BLACK_STEEL_HOE);
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

                        // The old fixed sabers are deprecated and deliberately absent here — only
                        // DEV_TAB still lists them. These are their modular replacements.
                        for (var saber : java.util.List.of(
                                ItemRegistry.LV_MODULAR_SABER, ItemRegistry.MV_MODULAR_SABER,
                                ItemRegistry.HV_MODULAR_SABER, ItemRegistry.EV_MODULAR_SABER,
                                ItemRegistry.IV_MODULAR_SABER)) {
                            add(out, saber);
                            ItemStack full = new ItemStack(saber.get());
                            full.set(MIComponents.ENERGY.get(), saber.get().voltage().baseCapacity);
                            out.accept(full);
                        }

                        // The old fixed drills are deprecated and deliberately absent here — only
                        // DEV_TAB still lists them. These are their modular replacements.
                        for (var drill : java.util.List.of(
                                ItemRegistry.LV_MODULAR_DRILL, ItemRegistry.MV_MODULAR_DRILL,
                                ItemRegistry.HV_MODULAR_DRILL, ItemRegistry.EV_MODULAR_DRILL,
                                ItemRegistry.IV_MODULAR_DRILL)) {
                            add(out, drill);
                            ItemStack full = new ItemStack(drill.get());
                            full.set(MIComponents.ENERGY.get(), drill.get().voltage().baseCapacity);
                            out.accept(full);
                        }

                        add(out, ItemRegistry.SPEED_MODULE_I);
                        add(out, ItemRegistry.SPEED_MODULE_II);
                        add(out, ItemRegistry.SPEED_MODULE_III);
                        add(out, ItemRegistry.SPEED_MODULE_IV);
                        add(out, ItemRegistry.SILK_TOUCH_MODULE);
                        add(out, ItemRegistry.FORTUNE_MODULE_I);
                        add(out, ItemRegistry.FORTUNE_MODULE_II);
                        add(out, ItemRegistry.FORTUNE_MODULE_III);
                        add(out, ItemRegistry.FORTUNE_MODULE_IV);
                        add(out, ItemRegistry.FORTUNE_MODULE_V);
                        add(out, ItemRegistry.XP_REACTOR_MODULE);
                        add(out, ItemRegistry.OVERCHARGE_MODULE);
                        add(out, ItemRegistry.REACH_MODULE_I);
                        add(out, ItemRegistry.REACH_MODULE_II);
                        add(out, ItemRegistry.REACH_MODULE_III);
                        add(out, ItemRegistry.BURN_MODULE);
                        add(out, ItemRegistry.BATTERY_MODULE_I);
                        add(out, ItemRegistry.BATTERY_MODULE_II);
                        add(out, ItemRegistry.BATTERY_MODULE_III);
                        add(out, ItemRegistry.BATTERY_MODULE_IV);
                        add(out, ItemRegistry.AOE_MODULE_I);
                        add(out, ItemRegistry.AOE_MODULE_II);
                        add(out, ItemRegistry.AOE_MODULE_III);
                        add(out, ItemRegistry.AOE_MODULE_IV);
                        add(out, ItemRegistry.AOE_MODULE_V);
                        add(out, ItemRegistry.SPEED_MODULE_V);
                        add(out, ItemRegistry.BATTERY_MODULE_V);
                        add(out, ItemRegistry.AUTO_SMELT_MODULE);
                        add(out, ItemRegistry.TRASH_FILTER_MODULE);
                        add(out, ItemRegistry.ECO_MODULE_I);
                        add(out, ItemRegistry.ECO_MODULE_II);
                        add(out, ItemRegistry.LAVA_SOLIDIFIER_MODULE);
                        add(out, ItemRegistry.SPEAK_MODULE);

                        // Saber modules; Battery, Eco, Speed, Fortune/Looting and the XP Reactor above
                        // fit sabers too. The old Looting and Attack Speed modules are deprecated and left out.
                        for (var module : java.util.List.of(
                                ItemRegistry.DAMAGE_MODULE_I, ItemRegistry.DAMAGE_MODULE_II, ItemRegistry.DAMAGE_MODULE_III,
                                ItemRegistry.DAMAGE_MODULE_IV, ItemRegistry.DAMAGE_MODULE_V,
                                ItemRegistry.SWEEP_MODULE_I, ItemRegistry.SWEEP_MODULE_II, ItemRegistry.SWEEP_MODULE_III,
                                ItemRegistry.XP_MULTIPLIER_MODULE_I, ItemRegistry.XP_MULTIPLIER_MODULE_II,
                                ItemRegistry.XP_MULTIPLIER_MODULE_III,
                                ItemRegistry.XP_MULTIPLIER_MODULE_IV, ItemRegistry.XP_MULTIPLIER_MODULE_V,
                                ItemRegistry.DROP_COLLECTOR_MODULE,
                                ItemRegistry.VAMPIRISM_MODULE_I, ItemRegistry.VAMPIRISM_MODULE_II,
                                ItemRegistry.VAMPIRISM_MODULE_III,
                                ItemRegistry.BEHEADING_MODULE_I, ItemRegistry.BEHEADING_MODULE_II,
                                ItemRegistry.BEHEADING_MODULE_III,
                                ItemRegistry.IRRADIATION_MODULE_I, ItemRegistry.IRRADIATION_MODULE_II,
                                ItemRegistry.IRRADIATION_MODULE_III,
                                ItemRegistry.METEORITE_MODULE)) {
                            add(out, module);
                        }

                        out.accept(BlockRegistry.MODULE_INSTALLATION_TABLE.get().asItem());

                        add(out, ItemRegistry.NETHERITE_STEAM_MINING_DRILL);

                        add(out, ItemRegistry.SKIN_APPLICATOR);

                        // Research Workbench
                        out.accept(BlockRegistry.RESEARCH_WORKBENCH.get().asItem());

                        // Hydroponic Garden Bed
                        out.accept(BlockRegistry.HYDROPONIC_GARDEN_BED.get().asItem());
                        add(out, ItemRegistry.BLUEPRINT_FIRE_RESISTANCE);

                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROLL_CROPS = TABS.register(
            "roll_crops",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.roll_crops_tab"))
                    .icon(() -> new ItemStack(ItemRegistry.LATEX_DANDELION_FLOWER.get()))
                    .displayItems((params, out) -> {
                        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistry.CROPS.getEntries()) {
                            out.accept(entry.get());
                        }
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

