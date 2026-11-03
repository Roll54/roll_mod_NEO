package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.items.BlockogrizItem;
import com.roll_54.roll_mod.items.HerbicideItem;
import com.roll_54.roll_mod.items.modularsaber.ModularSaberItem;
import com.roll_54.roll_mod.items.electricItems.refactored.ComponentEnergyDrill;
import com.roll_54.roll_mod.registry.ItemRegistry;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.component.DataComponents;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.roll_54.roll_mod.RollMod.MODID;
import static com.roll_54.roll_mod.registry.TagRegistry.*;


public class RollItemTagProvider extends ItemTagsProvider {

    public RollItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(
                output,
                lookupProvider,
                // parent item tags (none)
                CompletableFuture.completedFuture(TagLookup.empty()),
                MODID,
                existingFileHelper
        );
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        addSkinSlotTags();
        addTierArmorTags();
        addHerbicideTag();
        addVanillaLikeTags();

        var raw = tag(RAW_ORE);
        var crushed = tag(CRUSHED_ORE);
        var crushedRefined = tag(CRUSHED_REFINED_ORE);
        var crushedPurified = tag(CRUSHED_PURIFIED_ORE);
        var dust = tag(DUST_ORE);
        var dustPure = tag(DUST_PURE_ORE);
        var dustImpure = tag(DUST_IMPURE_ORE);
        var cDusts = tag(C_DUSTSTAG);

        for (OreDefinition definition : OreDefinitions.ORE_DEFINITIONS) {

            String material = definition.id();

            ResourceLocation rawItem = ResourceLocation.fromNamespaceAndPath(MODID, "raw_" + material);
            ResourceLocation crushedItem = ResourceLocation.fromNamespaceAndPath(MODID, "crushed_" + material + "_ore");
            ResourceLocation refinedItem = ResourceLocation.fromNamespaceAndPath(MODID, "refined_" + material + "_ore");
            ResourceLocation purifiedItem = ResourceLocation.fromNamespaceAndPath(MODID, "purified_" + material + "_ore");
            ResourceLocation dustItem = ResourceLocation.fromNamespaceAndPath(MODID, material + "_dust");
            ResourceLocation dustPureItem = ResourceLocation.fromNamespaceAndPath(MODID, "pure_" + material + "_dust");
            ResourceLocation dustImpureItem = ResourceLocation.fromNamespaceAndPath(MODID, "impure_" + material + "_dust");


            raw.addOptional(rawItem);
            crushed.addOptional(crushedItem);
            crushedRefined.addOptional(refinedItem);
            crushedPurified.addOptional(purifiedItem);
            dust.addOptional(dustItem);
            dustPure.addOptional(dustPureItem);
            dustImpure.addOptional(dustImpureItem);
            cDusts.addOptional(dustItem);

            TagKey<Item> materialTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", material)
            );

            var materialAppender = tag(materialTag);

            materialAppender.addOptional(rawItem);
            materialAppender.addOptional(crushedItem);
            materialAppender.addOptional(refinedItem);
            materialAppender.addOptional(purifiedItem);
            materialAppender.addOptional(dustItem);
            materialAppender.addOptional(dustPureItem);
            materialAppender.addOptional(dustImpureItem);

            TagKey<Item> craftMaterial = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", "ores/" + material)
            );

            var craftMaterialAppender = tag(craftMaterial);

            craftMaterialAppender.addOptional(rawItem);

            for (OreTextureTemplates.BlockSubLayer layer : OreTextureTemplates.BlockSubLayer.values()) {

                ResourceLocation oreBlockItem = ResourceLocation.fromNamespaceAndPath(
                        MODID,
                        layer.id() + "_" + material
                );

                craftMaterialAppender.addOptional(oreBlockItem);
            }

            TagKey<Item> dustMaterial = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", "dusts/" + material)
            );

            var dustMaterialAppender = tag(dustMaterial);

            dustMaterialAppender.addOptional(dustItem);

        }


    }

    /**
     * Every {@link HerbicideItem} the mod registers. Consumed by the Crop Manager Mk2's herbicide
     * slot filter, which is built before the item registry exists and so can only name a tag.
     */
    private void addHerbicideTag() {
        var herbicides = tag(HERBICIDES);
        for (var holder : ItemRegistry.ITEMS.getEntries()) {
            // The plain roll_mod:herbicide is kept out on purpose: only the tiered herbicides feed the machines.
            if (holder.get() instanceof HerbicideItem && holder.get() != ItemRegistry.HERBICIDE.get()) {
                herbicides.add(holder.get());
            }
        }
    }

    /**
     * Which items a cosmetic skin may be applied to, by slot.
     *
     * <p>{@code SkinCategory#of} now also probes {@code instanceof} and {@code ItemAbility} at
     * runtime, so this no longer has to enumerate everything skinnable — anything from another mod
     * classifies itself. What it still has to cover is the mod's own gear that neither probe can
     * see: {@code EnergySwordItem} and {@code ComponentEnergyDrill} extend plain {@link Item} and
     * answer no ability. Emitting those here keeps a misclassification visible in
     * {@code src/generated/resources} instead of showing up only as a skin that refuses to apply.
     *
     * <p>The plain vanilla subclasses are emitted too, redundantly with the runtime probe. They cost
     * nothing and make the file readable as the full picture of the mod's own items.
     */
    private void addSkinSlotTags() {
        var swords = tag(SKIN_SLOT_SWORD);
        var axes = tag(SKIN_SLOT_AXE);
        var pickaxes = tag(SKIN_SLOT_PICKAXE);
        var helmets = tag(SKIN_SLOT_HELMET);

        // Declared so datapacks have a file to override, and so /reload has something to reload.
        tag(SKIN_BLACKLIST);

        for (var holder : ItemRegistry.ITEMS.getEntries()) {
            Item item = holder.get();

            // Pickaxe is tested before axe: a paxel answers to both, and the mod treats a multi-tool
            // as a pickaxe. SkinCategory repeats this ordering for untagged third-party tools.
            if (item instanceof ModularSaberItem || item instanceof SwordItem) {
                swords.add(item);
            } else if (item instanceof ComponentEnergyDrill
                    || item instanceof BlockogrizItem
                    || item instanceof PickaxeItem) {
                pickaxes.add(item);
            } else if (item instanceof AxeItem) {
                axes.add(item);
            } else if (item instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.HELMET) {
                helmets.add(item);
            }
        }
    }

    /**
     * The tier armour sets — bismuth bronze, black bronze, steel, black steel — in the vanilla slot
     * tags, which is what makes them armour to the rest of the game: vanilla's enchantable tags
     * ({@code enchantable/armor}, {@code head_armor}, {@code durability}, {@code equippable},
     * {@code vanishing}, …) all include {@code #minecraft:head_armor} and its siblings, so without these
     * nothing could be put on them at a table or an anvil. Also trimmable, and {@code c:armors}.
     */
    private void addTierArmorTags() {
        List<List<DeferredHolder<Item, Item>>> sets = List.of(
                List.of(ItemRegistry.BISMUTH_BRONZE_HELMET, ItemRegistry.BISMUTH_BRONZE_CHESTPLATE,
                        ItemRegistry.BISMUTH_BRONZE_LEGGINGS, ItemRegistry.BISMUTH_BRONZE_BOOTS),
                List.of(ItemRegistry.BLACK_BRONZE_HELMET, ItemRegistry.BLACK_BRONZE_CHESTPLATE,
                        ItemRegistry.BLACK_BRONZE_LEGGINGS, ItemRegistry.BLACK_BRONZE_BOOTS),
                List.of(ItemRegistry.STEEL_HELMET, ItemRegistry.STEEL_CHESTPLATE,
                        ItemRegistry.STEEL_LEGGINGS, ItemRegistry.STEEL_BOOTS),
                List.of(ItemRegistry.BLACK_STEEL_HELMET, ItemRegistry.BLACK_STEEL_CHESTPLATE,
                        ItemRegistry.BLACK_STEEL_LEGGINGS, ItemRegistry.BLACK_STEEL_BOOTS));
        for (List<DeferredHolder<Item, Item>> set : sets) {
            tag(ItemTags.HEAD_ARMOR).add(set.get(0).get());
            tag(ItemTags.CHEST_ARMOR).add(set.get(1).get());
            tag(ItemTags.LEG_ARMOR).add(set.get(2).get());
            tag(ItemTags.FOOT_ARMOR).add(set.get(3).get());
            for (DeferredHolder<Item, Item> piece : set) {
                tag(ItemTags.TRIMMABLE_ARMOR).add(piece.get());
                tag(Tags.Items.ARMORS).add(piece.get());
            }
        }
    }

    /**
     * The tags vanilla and the {@code c:} convention give every item of a kind, for every roll_mod item
     * by what it is: weapons and tools in their tool tags, armour in its slot tag, food and drinks,
     * ingots and dusts with their material. Vanilla's enchanting tags ({@code enchantable/sword},
     * {@code enchantable/mining}, {@code enchantable/armor}, …) are built from these, so this is also
     * what lets the items be enchanted like their vanilla counterparts.
     */
    private void addVanillaLikeTags() {
        for (var holder : ItemRegistry.ITEMS.getEntries()) {
            Item item = holder.get();
            String id = holder.getId().getPath();

            // Weapons and tools.
            if (item instanceof SwordItem || item instanceof ModularSaberItem) {
                tag(ItemTags.SWORDS).add(item);
                tag(Tags.Items.MELEE_WEAPON_TOOLS).add(item);
                tag(Tags.Items.TOOLS).add(item);
            } else if (item instanceof BlockogrizItem) {
                // A paxel: a pickaxe, an axe and a shovel in one.
                tag(ItemTags.PICKAXES).add(item);
                tag(ItemTags.AXES).add(item);
                tag(ItemTags.SHOVELS).add(item);
                miningTool(item);
            } else if (item instanceof PickaxeItem || item instanceof ModularDrillItem || item instanceof ComponentEnergyDrill) {
                tag(ItemTags.PICKAXES).add(item);
                miningTool(item);
            } else if (item instanceof AxeItem) {
                tag(ItemTags.AXES).add(item);
                miningTool(item);
            } else if (item instanceof ShovelItem) {
                tag(ItemTags.SHOVELS).add(item);
                miningTool(item);
            } else if (item instanceof HoeItem) {
                tag(ItemTags.HOES).add(item);
                miningTool(item);
            }

            // Armour, by slot.
            if (item instanceof ArmorItem armor && armor.getType() != ArmorItem.Type.BODY) {
                tag(switch (armor.getType()) {
                    case HELMET -> ItemTags.HEAD_ARMOR;
                    case CHESTPLATE -> ItemTags.CHEST_ARMOR;
                    case LEGGINGS -> ItemTags.LEG_ARMOR;
                    default -> ItemTags.FOOT_ARMOR;
                }).add(item);
                tag(Tags.Items.ARMORS).add(item);
            }

            // Food and drinks.
            ItemStack stack = item.getDefaultInstance();
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (item.getUseAnimation(stack) == UseAnim.DRINK) {
                tag(Tags.Items.DRINKS).add(item);
            } else if (food != null) {
                tag(Tags.Items.FOODS).add(item);
                foodKind(item, id, food);
            }

            // Ingots and dusts, with their material: c:ingots/<material>, c:dusts/<material>.
            if (id.endsWith("_ingot") && !id.startsWith("hot_")) {
                String material = id.substring(0, id.length() - "_ingot".length());
                tag(Tags.Items.INGOTS).add(item);
                tag(commonTag("ingots/" + material)).add(item);
            } else if (id.endsWith("_dust") && !id.startsWith("impure_") && !id.startsWith("pure_")
                    && !id.startsWith("tiny_") && !id.startsWith("small_")) {
                String material = id.substring(0, id.length() - "_dust".length());
                tag(Tags.Items.DUSTS).add(item);
                tag(commonTag("dusts/" + material)).add(item);
            }
        }
    }

    private void miningTool(Item item) {
        tag(Tags.Items.MINING_TOOL_TOOLS).add(item);
        tag(Tags.Items.TOOLS).add(item);
    }

    /** The {@code c:foods/*} kind a food's id names, and food poisoning when it poisons or starves. */
    private void foodKind(Item item, String id, FoodProperties food) {
        if (id.endsWith("_seeds")) return;
        if (id.contains("berry") && !id.contains("pie") && !id.contains("coffee")) tag(Tags.Items.FOODS_BERRY).add(item);
        if (id.contains("pineapple") && !id.contains("pizza")) tag(Tags.Items.FOODS_FRUIT).add(item);
        if (id.contains("bell_pepper") || id.contains("potato")) tag(Tags.Items.FOODS_VEGETABLE).add(item);
        if (id.contains("pie") && !id.contains("pineapple")) tag(Tags.Items.FOODS_PIE).add(item);
        if (id.contains("candy") || id.contains("gum")) tag(Tags.Items.FOODS_CANDY).add(item);
        if (id.startsWith("golden_")) tag(Tags.Items.FOODS_GOLDEN).add(item);
        for (FoodProperties.PossibleEffect effect : food.effects()) {
            var type = effect.effect().getEffect();
            if (type.is(MobEffects.POISON) || type.is(MobEffects.HUNGER)) {
                tag(Tags.Items.FOODS_FOOD_POISONING).add(item);
                break;
            }
        }
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
