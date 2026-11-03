package com.roll_54.roll_mod.gtmogs.integration.xei.widgets;

import com.roll_54.roll_mod.compat.gtmogs.CyclingMarkerHandler;
import com.roll_54.roll_mod.compat.gtmogs.OreVeinLookups;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

import com.roll_54.roll_mod.gtmogs.api.gui.widget.SlotWidget;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.transfer.item.CustomItemStackHandler;
import com.roll_54.roll_mod.gtmogs.api.worldgen.DimensionMarker;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

import java.util.*;

@Getter
public class GTOreVeinWidget extends WidgetGroup {

    private final String translationKey;
    private final int weight;
    private final String range;
    private final Set<ResourceKey<Level>> dimensionFilter;
    public final static int width = 120;

    // roll_mod: a vein's ores wrap into rows of six instead of one row that runs off the 120 px panel, and
    // everything under them moves down by the rows added (setupBaseGui runs before setupText).
    private static final int SLOT = 18;
    private static final int PER_ROW = 6;
    /** Under the dimension slot, which sits at y 80 (plus the shift) and is one slot tall. */
    private static final int INDICATOR_Y = 100;
    /** How far the text under the ores moves down: one slot height per extra row. */
    private int shift;

    public GTOreVeinWidget(Holder<OreVeinDefinition> ore) {
        super(0, 0, width, 160);
        this.translationKey = getOreName(ore);
        this.weight = ore.value().weight();
        this.dimensionFilter = ore.value().dimensionFilter();
        this.range = range(ore.value());
        setClientSideWidget();
        setupBaseGui(ore.value());
        setupText(ore.value());
    }

    @SuppressWarnings("all")
    private String range(OreVeinDefinition oreDefinition) {
        HeightProvider height = oreDefinition.heightRange().height;
        int minHeight = 0, maxHeight = 0;
        if (height instanceof UniformHeight uniform) {
            minHeight = uniform.minInclusive.resolveY(null);
            maxHeight = uniform.maxInclusive.resolveY(null);
        }
        return String.format("%d - %d", minHeight, maxHeight);
    }

    private void setupBaseGui(OreVeinDefinition oreDefinition) {
        NonNullList<ItemStack> containedOresAsItemStacks = NonNullList.create();
        IntList chances = oreDefinition.veinGenerator().getAllChances();
        containedOresAsItemStacks.addAll(getRawMaterialList(oreDefinition));
        int n = containedOresAsItemStacks.size();
        shift = (rows(n) - 1) * SLOT;
        for (int i = 0; i < n; i++) {
            SlotWidget oreSlot = new SlotWidget(new CustomItemStackHandler(containedOresAsItemStacks), i,
                    rowX(i, n), 18 + i / PER_ROW * SLOT, false, false);
            int finalIndex = i;
            oreSlot.setOnAddedTooltips((stack, tooltips) -> tooltips.add(
                    Component.translatable("gtmogs.jei.ore_vein_diagram.chance", chances.getInt(finalIndex))));
            oreSlot.setIngredientIO(IngredientIO.OUTPUT);
            addWidget(oreSlot);
        }
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
        setupDimensionMarker(80 + shift);
        setupIndicators(vein);
    }

    /**
     * roll_mod: the vein's surface samples, in a row under the dimension slot, wrapped like the ores. They are
     * catalysts, so EMI's "uses" of a sample lead to its veins. The panel grows if they run past its bottom.
     */
    private void setupIndicators(OreVeinDefinition vein) {
        List<ItemStack> samples = OreVeinLookups.indicators(vein);
        if (samples.isEmpty()) return;
        int labelY = INDICATOR_Y + shift;
        addWidget(new LabelWidget(5, labelY, LocalizationUtils.format("roll_mod.ore_vein.indicators")));
        CustomItemStackHandler handler = new CustomItemStackHandler(NonNullList.copyOf(samples));
        int count = samples.size();
        int top = labelY + 10;
        for (int i = 0; i < count; i++) {
            addWidget(new SlotWidget(handler, i, rowX(i, count), top + i / PER_ROW * SLOT, false, false)
                    .setIngredientIO(IngredientIO.CATALYST));
        }
        int bottom = top + rows(count) * SLOT + 2;
        if (bottom > getSizeHeight()) setSizeHeight(bottom);
    }

    private void setupDimensionMarker(int yPosition) {
        if (this.dimensionFilter != null) {
            int interval = 2;
            int rowSlots = (width - 10 + interval) / (16 + interval);

            DimensionMarker[] dimMarkers = dimensionFilter.stream()
                    .map(ResourceKey::location)
                    .map(loc -> GTRegistries.DIMENSION_MARKERS.getOptional(loc)
                            .orElse(new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER,
                                    loc.toString())))
                    .sorted(Comparator.comparingInt(DimensionMarker::getTier))
                    .toArray(DimensionMarker[]::new);
            // roll_mod: one slot that cycles through every dimension the vein rolls in (the overworld veins
            // also roll in the mining world), a second apiece, instead of a slot per dimension.
            var handler = new CyclingMarkerHandler(dimMarkers.length);
            for (int i = 0; i < dimMarkers.length; i++) {
                var dimMarker = dimMarkers[i];
                var icon = dimMarker.getIcon();
                int row = Math.floorDiv(i, rowSlots);
                SlotWidget dimSlot = new SlotWidget(handler, i,
                        5 + (16 + interval) * (i - row * rowSlots),
                        yPosition + 18 * row,
                        false, false).setIngredientIO(IngredientIO.CATALYST);
                handler.setStackInSlot(i, icon);
                if (i == 0) addWidget(dimSlot.setBackgroundTexture(IGuiTexture.EMPTY));
            }
        } else {
            addWidget(new LabelWidget(5, yPosition, "Any"));
        }
    }

    public static List<ItemStack> getContainedOresAndBlocks(OreVeinDefinition oreDefinition) {
        return getRawMaterialList(oreDefinition);
    }

    public static List<ItemStack> getRawMaterialList(OreVeinDefinition oreDefinition) {
        return oreDefinition.veinGenerator().getAllBlocks().stream()
                .map(block -> block.getBlock().asItem().getDefaultInstance()).toList();
    }

    public static String getOreName(Holder<OreVeinDefinition> ore) {
        return ore.getKey().location().toLanguageKey("ore_vein");
    }
}
