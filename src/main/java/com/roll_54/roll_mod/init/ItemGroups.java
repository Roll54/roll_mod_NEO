package com.roll_54.roll_mod.init;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.roll_54.roll_mod.PYDatagen.PYBlocks;
import com.roll_54.roll_mod.PYDatagen.PYItems;
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
    // 🔼 спочатку реєстр
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RollMod.MODID);

    // 🔽 а потім уже MAIN і DEV_TAB
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.main"))
                    .icon(ItemGroups::createCustomHeadIcon)
                    .displayItems((params, out) -> {
                        add(out, ItemRegistry.METEORITE_METAL_INGOT);

                        // Hazmat-броня
                        add(out, ItemRegistry.HAZMAT_HELMET);
                        add(out, ItemRegistry.HAZMAT_CHESTPLATE);
                        add(out, ItemRegistry.HAZMAT_LEGGINGS);
                        add(out, ItemRegistry.HAZMAT_BOOTS);

                        // Meteorite-броня
                        add(out, ItemRegistry.METEORITE_HELMET);
                        add(out, ItemRegistry.METEORITE_CHESTPLATE);
                        add(out, ItemRegistry.METEORITE_LEGGINGS);
                        add(out, ItemRegistry.METEORITE_BOOTS);
                        add(out, ItemRegistry.METEORITE_SWORD);
                        add(out, ItemRegistry.METEORITE_PICKAXE);
                        add(out, ItemRegistry.METEORITE_AXE);
                        add(out, ItemRegistry.METEORITE_SHOVEL);
                        add(out, ItemRegistry.METEORITE_HOE);

                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEV_TAB = TABS.register(
            "dev",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.dev"))
                    .icon(() -> new ItemStack(Items.DEBUG_STICK))
                    .displayItems((params, out) -> {

                        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistry.ITEMS.getEntries()) {
                            out.accept(entry.get());
                        }
                        for (DeferredHolder<Block, ? extends Block> entry : BlockRegistry.BLOCKS.getEntries()) {
                            if (PYBlocks.ORE_BLOCKS.getEntries().contains(entry)) continue; // ← пропускаємо руди

                            Block block = entry.get();
                            Item item = block.asItem();
                            if (item != Items.AIR) {
                                out.accept(item);
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PY_DATAGEN_TAB = TABS.register(
            "py_datagen",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.py_datagen"))
                    .icon(() -> new ItemStack(BlockRegistry.ROLL_PLUSH.get()))
                    .displayItems((params, out) -> {

                        // усі предмети із PYItems
                        for (DeferredHolder<Item, ? extends Item> entry : PYItems.ORE_ITEMS.getEntries()) {
                            out.accept(entry.get());
                        }

                        // усі блоки із PYBlocks
                        for (DeferredHolder<Block, ? extends Block> entry : PYBlocks.ORE_BLOCKS.getEntries()) {
                            Block block = entry.get();
                            Item item = block.asItem();
                            if (item != net.minecraft.world.item.Items.AIR) {
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