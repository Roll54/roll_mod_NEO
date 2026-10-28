package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.guicomponents.ShapeSelection;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.MachineRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformers;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.ElectricMultipliedCraftingMultiblockBlockEntity;
import net.swedz.tesseract.neoforge.compat.mi.machine.builder.SpecialMachineBuilder;

import java.util.List;

import static aztech.modern_industrialization.machines.multiblocks.HatchTypes.*;

/**
 * The pack's KubeJS pyrolyse oven, rebuilt the way MI builds its Electric Blast Furnace: the same
 * 3×3×4 heatproof shell, with the coil rings made of any one coil from {@link #TIERS}. The coil
 * decides how many recipes run at once, from ×4 on cupronickel to ×64 on tungsten. Above it sits
 * {@link ParallelMultiblocks#PYROLYSE_OVEN}, the tungstensteel one at ×128.
 *
 * <p>As on the EBF, there is one shape per coil and the player picks the one they built in the
 * machine's screen (MI's {@link ShapeSelection}). Only the picked shape is matched, so a mixed or
 * different coil does not form. Unlike the EBF the coil sets no EU/t ceiling: every pyrolyse
 * recipe the KubeJS oven runs, this one runs on any coil. Each parallel costs a recipe's full EU.
 */
public final class CoilPyrolyseOven {

    public static final String ID = "advanced_pyrolyse_oven";
    public static final String ENGLISH_NAME = "Advanced Pyrolyse Oven";
    /** The KubeJS oven's casing, under MI's name for it. */
    public static final String CASING = "heatproof_machine_casing";
    /** The KubeJS oven's front overlay. */
    public static final ResourceLocation OVERLAYS = ResourceLocation.parse("modern_industrialization:block/machines/pyrolyse_oven");

    /** One coil the rings may be built from, and the parallels it gives. */
    public record Tier(ResourceLocation coil, int parallel) {
        Component displayName() {
            return Component.translatable("gui.roll_mod.coil_pyrolyse_oven.tier",
                    BuiltInRegistries.BLOCK.get(coil).getName(), parallel);
        }
    }

    /** Cheapest first, the order MI lists EBF tiers in. */
    public static final List<Tier> TIERS = List.of(
            new Tier(ResourceLocation.parse("modern_industrialization:cupronickel_coil"), 4),
            new Tier(ResourceLocation.parse("modern_industrialization:kanthal_coil"), 16),
            new Tier(ResourceLocation.parse("modern_industrialization:tungsten_coil"), 64));

    private static final HatchFlags HATCHES = new HatchFlags.Builder()
            .with(ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT).build();

    /**
     * The KubeJS layers, read as {@link ParallelMultiblocks} describes: back to front, each bottom
     * first. {@code H} heatproof casing (takes hatches), {@code C} the tier's coil.
     */
    private static final String[][] LAYERS = {
            { "HHH", "HHH", "HHH" },
            { "CCC", "C C", "CCC" },
            { "CCC", "C C", "CCC" },
            { "HHH", "H#H", "HHH" },
    };

    private static ShapeTemplate[] shapes;
    private static boolean registered;

    private CoilPyrolyseOven() {}

    /** Call from the multiblock hook, next to {@link ParallelMultiblocks#register}. */
    public static void register(MultiblockMachinesMIHookContext hook) {
        MachineRecipeType recipeType = MachineRegistry.RecipeTypes.PYROLYSE_OVEN;
        if (recipeType == null) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its recipe type is missing.", ID);
            return;
        }
        shapes = new ShapeTemplate[TIERS.size()];
        for (int i = 0; i < TIERS.size(); i++) {
            shapes[i] = new ShapeTemplate.LayeredBuilder(MachineCasings.HEATPROOF, LAYERS)
                    .key('H', SimpleMember.forBlockId(ResourceLocation.parse("modern_industrialization:heatproof_machine_casing")), HATCHES)
                    .key('C', SimpleMember.forBlockId(TIERS.get(i).coil()), HatchFlags.NO_HATCH)
                    .build();
        }

        SpecialMachineBuilder builder = hook.builder(ID, ENGLISH_NAME, bep -> new BlockEntity(bep, recipeType));
        builder.registerMachine();
        // One viewer page per coil, as the EBF shows its own.
        for (int i = 0; i < shapes.length; i++) {
            builder.registerMultiblockShape(shapes[i], TIERS.get(i).coil().getPath());
        }
        registered = true;
    }

    /**
     * A workstation in every recipe-viewer category of the pyrolyse recipe type: the KubeJS oven's
     * when the pack has it, otherwise the one {@link ParallelMultiblocks#registerWorkstations} makes
     * for the tungstensteel oven. Call after that.
     */
    public static void registerWorkstations() {
        if (!registered) return;
        MachineRecipeType type = MachineRegistry.RecipeTypes.PYROLYSE_OVEN;
        ResourceLocation machine = RollMod.id(ID);
        ReiMachineRecipes.categories.values().stream()
                .filter(params -> params.recipeType == type)
                .map(params -> params.category)
                .toList()
                .forEach(category -> ReiMachineRecipes.registerWorkstation(category, machine));
    }

    /**
     * {@link ElectricMultipliedCraftingMultiblockBlockEntity} whose parallels follow the picked coil.
     * The constructor's limit is the top tier's; {@link #getMaxMultiplier} is what the crafter reads.
     */
    private static final class BlockEntity extends ElectricMultipliedCraftingMultiblockBlockEntity {

        BlockEntity(BEP bep, MachineRecipeType recipeType) {
            super(bep, RollMod.id(ID), shapes, MachineTier.MULTIBLOCK, recipeType,
                    TIERS.getLast().parallel(), EuCostTransformers.percentage(() -> 1f));
            List<Component> names = TIERS.stream().map(Tier::displayName).toList();
            registerGuiComponent(new ShapeSelection(new ShapeSelection.Behavior() {
                @Override
                public void handleClick(int clickedLine, int delta) {
                    activeShape.incrementShape(BlockEntity.this, delta);
                }

                @Override
                public int getCurrentIndex(int line) {
                    return activeShape.getActiveShapeIndex();
                }
            }, new ShapeSelection.LineInfo(names, true)));
        }

        @Override
        public int getMaxMultiplier() {
            int index = activeShape.getActiveShapeIndex();
            return TIERS.get(Math.max(0, Math.min(index, TIERS.size() - 1))).parallel();
        }

        @Override
        public ShapeTemplate getBigShape() {
            return shapes[shapes.length - 1];
        }
    }
}
