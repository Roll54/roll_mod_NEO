package com.roll_54.roll_mod.registry;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.SliceableFoodBlock;
import com.roll_54.roll_mod.items.ComponentApplicatorItem;
import com.roll_54.roll_mod.items.HerbicideItem;
import com.roll_54.roll_mod.items.InjectionItem;
import com.roll_54.roll_mod.items.NetheriteSteamDrillItem;
import com.roll_54.roll_mod.items.armor.geckolib.SkinAnchorItem;
import com.roll_54.roll_mod.items.electricItems.EnergyDrillItem;
import com.roll_54.roll_mod.items.electricItems.EnergySwordItem;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleItem;
import com.roll_54.roll_mod.util.TooltipManager;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import umpaz.brewinandchewin.common.block.PizzaBlock;
import vectorwing.farmersdelight.common.block.PieBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ItemGroups {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RollMod.MODID);

    // Every roll_mod item is sorted by rule into one tab (see category()), so a newly registered item
    // always shows up somewhere without editing this file. Anything no rule claims falls into MAIN.

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.main"))
                    .icon(ItemGroups::createCustomHeadIcon)
                    .displayItems((params, out) -> fill(out, Category.MAIN))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_ARMOR = TABS.register(
            "tools_armor",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.tools_armor"))
                    .icon(() -> new ItemStack(ItemRegistry.METEORITE_MODULE.get()))
                    .displayItems((params, out) -> fill(out, Category.TOOLS_ARMOR))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD = TABS.register(
            "food",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.food"))
                    .icon(() -> new ItemStack(ItemRegistry.FLUORITE_PINEAPPLE.get()))
                    .displayItems((params, out) -> {
                        // Edible crops live in ROLL_CROPS too; food is the one tab allowed to repeat them.
                        for (Item item : modItems()) {
                            Category c = categories().get(item);
                            if (c == Category.FOOD || (c == Category.CROPS && isFood(item))) out.accept(item);
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CRAFTING = TABS.register(
            "crafting",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.crafting"))
                    .icon(() -> new ItemStack(ItemRegistry.METEORITE_METAL_INGOT.get()))
                    .displayItems((params, out) -> fill(out, Category.CRAFTING))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ADDONS = TABS.register(
            "addons",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.addons"))
                    .icon(() -> new ItemStack(ItemRegistry.BASECYBERWARE_LEFTARM_METEORITE_METAL.get()))
                    .displayItems((params, out) -> fill(out, Category.ADDONS))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROLL_CROPS = TABS.register(
            "roll_crops",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.roll_crops_tab"))
                    .icon(() -> new ItemStack(ItemRegistry.LATEX_DANDELION_FLOWER.get()))
                    .displayItems((params, out) -> fill(out, Category.CROPS))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERATED_ORES_TAB = TABS.register(
            "generated_ores",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.generated_ores"))
                    .icon(() -> new ItemStack(ItemRegistry.METEORITE_METAL_BLOCKOGRIZ.get()))
                    .displayItems((params, out) -> fill(out, Category.ORES))
                    .build()
    );

    /** Everything, deprecated items included — the only tab that still lists those. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEV_TAB = TABS.register(
            "dev",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roll_mod.dev"))
                    .icon(() -> new ItemStack(ItemRegistry.SULFUR_BERRY.get()))
                    .displayItems((params, out) -> {
                        for (Item item : modItems()) {
                            out.accept(item);
                        }
                    })
                    .build()
    );

    private enum Category { DEPRECATED, VENDING, ORES, ADDONS, CROPS, TOOLS_ARMOR, FOOD, CRAFTING, MAIN }

    /** Packages of other mods whose item types make an item an add-on item (Ad Astra gas tanks, cyberware). */
    private static final List<String> ADDON_PACKAGES = List.of(
            "earth.terrarium.adastra.", "com.perigrine3.createcybernetics.", "com.agricraft.");

    private static Map<Item, Category> categories;

    /** Every item in the roll_mod namespace with its tab, in registration order. Registries are frozen by now. */
    private static synchronized Map<Item, Category> categories() {
        if (categories == null) {
            Set<Item> ores = items(GeneratedOreRegistry.ITEMS.getEntries());
            Set<Item> vending = items(com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry.ITEMS.getEntries());
            Set<Item> crops = items(ItemRegistry.CROPS.getEntries());
            Map<Item, Category> map = new LinkedHashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (RollMod.MODID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                    map.put(item, category(item, ores, vending, crops));
                }
            }
            categories = map;
        }
        return categories;
    }

    private static Collection<Item> modItems() {
        return categories().keySet();
    }

    private static void fill(CreativeModeTab.Output out, Category category) {
        for (Item item : modItems()) {
            if (categories().get(item) != category) continue;
            out.accept(item);
            // Energy items also come fully charged, right after the empty one.
            if (item instanceof ISimpleEnergyItem energy) {
                ItemStack full = new ItemStack(item);
                full.set(energy.getEnergyComponent(), energy.getEnergyCapacity(full));
                out.accept(full);
            }
        }
    }

    private static Category category(Item item, Set<Item> ores, Set<Item> vending, Set<Item> crops) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (isDeprecated(item)) return Category.DEPRECATED;
        if (ores.contains(item)) return Category.ORES;
        if (vending.contains(item)) return Category.VENDING; // listed by the economy tab
        if (isAddon(item)) return Category.ADDONS;
        if (crops.contains(item)) return Category.CROPS;
        if (item instanceof TieredItem || item instanceof ArmorItem || item instanceof ISimpleEnergyItem
                || item instanceof DrillModuleItem || item instanceof InjectionItem
                || item instanceof ComponentApplicatorItem || item instanceof SkinAnchorItem
                || item instanceof NetheriteSteamDrillItem) {
            return Category.TOOLS_ARMOR;
        }
        if (isFood(item)) return Category.FOOD;
        boolean plain = item.getClass() == Item.class || item.getClass() == TooltipManager.TooltipItem.class;
        if (plain && !path.startsWith("research_") && !item.components().has(DataComponents.JUKEBOX_PLAYABLE)
                && !path.equals("minestar_logo") && !path.equals("error_item")) {
            return Category.CRAFTING;
        }
        return Category.MAIN;
    }

    private static boolean isFood(Item item) {
        return item.components().has(DataComponents.FOOD)
                || (item instanceof BlockItem block
                        && (block.getBlock() instanceof SliceableFoodBlock || block.getBlock() instanceof PieBlock
                        || block.getBlock() instanceof PizzaBlock));
    }

    /** Kept registered so existing stacks load, but replaced: only the ALL ITEMS tab shows these. */
    private static boolean isDeprecated(Item item) {
        return item instanceof EnergyDrillItem || item instanceof EnergySwordItem
                || item == BlockRegistry.CROP_MANAGER.get().asItem();
    }

    private static boolean isAddon(Item item) {
        if (item instanceof HerbicideItem) return true; // AgriCraft crop herbicides
        for (Class<?> c = item.getClass(); c != null && c != Item.class; c = c.getSuperclass()) {
            if (fromAddon(c)) return true;
            for (Class<?> i : c.getInterfaces()) {
                if (fromAddon(i)) return true;
            }
        }
        return false;
    }

    private static boolean fromAddon(Class<?> c) {
        for (String p : ADDON_PACKAGES) {
            if (c.getName().startsWith(p)) return true;
        }
        return false;
    }

    private static Set<Item> items(Collection<? extends DeferredHolder<Item, ? extends Item>> entries) {
        Set<Item> set = new HashSet<>();
        for (DeferredHolder<Item, ? extends Item> h : entries) set.add(h.get());
        return set;
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

