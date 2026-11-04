package com.roll_54.roll_mod.gtmogs.integration.xei.widgets;

import com.roll_54.roll_mod.compat.gtmogs.OreVeinLookups;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;

import com.roll_54.roll_mod.gtmogs.api.gui.widget.SlotWidget;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.transfer.item.CustomItemStackHandler;
import com.roll_54.roll_mod.gtmogs.api.worldgen.DimensionMarker;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator.VeinEntry;
import com.roll_54.roll_mod.gtmogs.integration.xei.handlers.item.CycleItemEntryHandler;
import lombok.Getter;

import java.util.*;

@Getter
public class GTOreVeinWidget extends WidgetGroup {

    private final String translationKey;
    private final int weight;
    private final String range;
    private final Set<ResourceKey<Level>> dimensionFilter;
    public final static int width = 120;

    // roll_mod: one slot per layer of the vein, wrapping into rows of six instead of one row that runs off the
    // 120 px panel, and everything under them moves down by the rows added (setupBaseGui runs before setupText).
    private static final int SLOT = 18;
    private static final int PER_ROW = 6;
    /** How far the text under the ores moves down: one slot height per extra row. */
    private int shift;

    public GTOreVeinWidget(Holder<OreVeinDefinition> ore) {
        super(0, 0, width, 160);
        this.translationKey = getOreName(ore);
        this.weight = ore.value().weight();
        this.dimensionFilter = ore.value().dimensionFilter();
        this.range = range(ore.value().heightRange());
        setClientSideWidget();
        setupBaseGui(ore.value());
        setupText(ore.value());
    }

    @SuppressWarnings("all")
    private static String range(HeightRangePlacement placement) {
        HeightProvider height = placement.height;
        int minHeight = 0, maxHeight = 0;
        if (height instanceof UniformHeight uniform) {
            minHeight = uniform.minInclusive.resolveY(null);
            maxHeight = uniform.maxInclusive.resolveY(null);
        }
        return String.format("%d - %d", minHeight, maxHeight);
    }

    /**
     * roll_mod: a slot per layer of the vein (magnetite, magnetite, gold, gold for a classic vein), each cycling
     * through that layer's sample, its ore in every rock of the vein's worlds and its raw ore
     * ({@link OreVeinLookups#slotStacks}), rather than a slot per block.
     */
    private void setupBaseGui(OreVeinDefinition oreDefinition) {
        List<VeinEntry> entries = oreDefinition.veinGenerator().getAllEntries();
        List<List<ItemStack>> stacks = slotStacks(oreDefinition);
        CycleItemEntryHandler handler = CycleItemEntryHandler.createFromStacks(stacks);
        int n = stacks.size();
        shift = (rows(n) - 1) * SLOT;
        for (int i = 0; i < n; i++) {
            SlotWidget oreSlot = new SlotWidget(handler, i, rowX(i, n), 18 + i / PER_ROW * SLOT, false, false);
            int chance = entries.get(i).chance();
            oreSlot.setOnAddedTooltips((stack, tooltips) -> tooltips.add(
                    Component.translatable("gtmogs.jei.ore_vein_diagram.chance", chance)));
            oreSlot.setIngredientIO(IngredientIO.OUTPUT);
            addWidget(oreSlot);
        }
    }

    private static List<List<ItemStack>> slotStacks(OreVeinDefinition vein) {
        return vein.veinGenerator().getAllEntries().stream()
                .map(entry -> OreVeinLookups.slotStacks(entry.blocks(), vein.dimensionFilter()))
                .toList();
    }

    private static int rows(int count) {
        return (count + PER_ROW - 1) / PER_ROW;
    }

    /** x of slot {@code index} of {@code count}, in rows of six, each row centred. */
    private static int rowX(int index, int count) {
        int row = index / PER_ROW;
        int inRow = Math.min(PER_ROW, count - row * PER_ROW);
        return (width - SLOT * inRow) / 2 + SLOT * (index % PER_ROW);
    }

    private void setupText(OreVeinDefinition vein) {
        addWidget(new ImageWidget(5, 0, width - 10, 16,
                new TextTexture(translationKey).setType(TextTexture.TextType.LEFT_ROLL)
                        .setWidth(width - 10)));
        addWidget(new LabelWidget(5, 40 + shift,
                LocalizationUtils.format("gtmogs.jei.ore_vein_diagram.spawn_range")));
        addWidget(new LabelWidget(5, 50 + shift, range));

        addWidget(new LabelWidget(5, 60 + shift,
                LocalizationUtils.format("gtmogs.jei.ore_vein_diagram.weight", weight)));
        addWidget(new LabelWidget(5, 70 + shift,
                LocalizationUtils.format("gtmogs.jei.ore_vein_diagram.dimensions")));
        setupDimensionMarker(vein, 80 + shift);
    }

    private void setupDimensionMarker(OreVeinDefinition vein, int yPosition) {
        if (this.dimensionFilter != null) {
            int interval = 2;
            int rowSlots = (width - 10 + interval) / (16 + interval);

            // roll_mod: every world the vein rolls in, a marker apiece in a row (the overworld veins also roll in
            // the mining world), each saying the vein's height range there, which dimension_overrides may change.
            List<ResourceKey<Level>> worlds = dimensionFilter.stream()
                    .sorted(Comparator.comparingInt(world -> marker(world).getTier()))
                    .toList();
            var handler = new CustomItemStackHandler(worlds.size());
            int bottom = yPosition;
            for (int i = 0; i < worlds.size(); i++) {
                ResourceKey<Level> world = worlds.get(i);
                int row = Math.floorDiv(i, rowSlots);
                handler.setStackInSlot(i, marker(world).getIcon());
                String worldRange = range(vein.heightRange(world));
                SlotWidget dimSlot = new SlotWidget(handler, i,
                        5 + (16 + interval) * (i - row * rowSlots),
                        yPosition + 18 * row,
                        false, false).setIngredientIO(IngredientIO.CATALYST);
                dimSlot.setOnAddedTooltips((slot, tooltips) -> tooltips.add(Component.literal("Y " + worldRange)));
                addWidget(dimSlot.setBackgroundTexture(IGuiTexture.EMPTY));
                bottom = yPosition + 18 * (row + 1);
            }
            if (bottom + 2 > getSizeHeight()) setSizeHeight(bottom + 2);
        } else {
            addWidget(new LabelWidget(5, yPosition, "Any"));
        }
    }

    private static DimensionMarker marker(ResourceKey<Level> world) {
        return GTRegistries.DIMENSION_MARKERS.getOptional(world.location())
                .orElse(new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER,
                        world.location().toString()));
    }

    /**
     * Everything the page's slots cycle through — ores in every rock, raw ores, samples — once each, so looking any
     * of them up in a recipe viewer finds the vein.
     */
    public static List<ItemStack> getContainedOresAndBlocks(OreVeinDefinition oreDefinition) {
        Map<Item, ItemStack> unique = new LinkedHashMap<>();
        for (List<ItemStack> slot : slotStacks(oreDefinition)) {
            for (ItemStack stack : slot) unique.putIfAbsent(stack.getItem(), stack);
        }
        return List.copyOf(unique.values());
    }

    public static String getOreName(Holder<OreVeinDefinition> ore) {
        return ore.getKey().location().toLanguageKey("ore_vein");
    }
}
