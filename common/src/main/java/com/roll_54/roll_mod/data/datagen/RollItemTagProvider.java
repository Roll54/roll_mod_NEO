package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.items.BlockogrizItem;
import com.roll_54.roll_mod.items.HerbicideItem;
import com.roll_54.roll_mod.items.electricItems.EnergyDrillItem;
import com.roll_54.roll_mod.items.electricItems.EnergySwordItem;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

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
        addHerbicideTag();

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
            if (holder.get() instanceof HerbicideItem) {
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
            if (item instanceof EnergySwordItem || item instanceof SwordItem) {
                swords.add(item);
            } else if (item instanceof EnergyDrillItem
                    || item instanceof ComponentEnergyDrill
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
}
