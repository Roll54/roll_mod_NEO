package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.guicomponents.ShapeSelection;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformers;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.ElectricMultipliedCraftingMultiblockBlockEntity;
import net.swedz.tesseract.neoforge.compat.mi.machine.builder.SpecialMachineBuilder;

import java.util.List;
import java.util.stream.IntStream;

import static aztech.modern_industrialization.machines.multiblocks.HatchTypes.*;

/**
 * MI's distillation tower, made bigger and run in parallel: a 5×5 tower of 1 to {@link #MAX_LAYERS}
 * layers, each 3 blocks tall, that runs {@code distillation_tower} recipes ×{@link #PARALLEL} at full EU
 * per parallel.
 *
 * <p>As on MI's tower, the player picks the height in the machine's screen ({@link ShapeSelection}), only
 * that shape is matched, and a tower of N layers can hand out N fluid outputs: MI sorts output hatches by
 * height, so the k-th output of a recipe goes to the hatch on the k-th layer.
 *
 * <p>From the bottom: the cap with the controller in its front middle, where the energy and fluid input
 * hatches go; then per layer two open levels (netherite frame corners, osmirium casing and osmiridium pipes
 * in the middle) and a ring of tungstensteel heat proof casing that takes that layer's fluid output hatch.
 * The top layer's ring is a full cap. Built from the user's {@code highly_advanced_distillery.nbt}, whose
 * layers were 2 blocks tall.
 */
public final class HighlyAdvancedDistillery {

    public static final String ID = "highly_advanced_distillery";
    public static final String ENGLISH_NAME = "Highly Advanced Distillery";
    public static final String CASING = ParallelMultiblocks.CASING;
    public static final ResourceLocation OVERLAYS =
            ResourceLocation.parse("modern_industrialization:block/machines/distillation_tower");
    public static final int MAX_LAYERS = 9;
    private static final int PARALLEL = 32;

    /*
     * One level each, front row (the controller's) first, read left to right as seen from the front. All
     * four are symmetric, so the mirroring MI applies does not matter. A netherite frame, B heat proof
     * casing (the only block taking hatches), C osmirium casing, D osmiridium pipe casing.
     */
    private static final String[] BOTTOM = { "AB#BA", "BBBBB", "BBBBB", "BBBBB", "ABBBA" };
    private static final String[] OPEN = { "A   A", " CDC ", " DCD ", " CDC ", "A   A" };
    private static final String[] RING = { "ABBBA", "BCDCB", "BDCDB", "BCDCB", "ABBBA" };
    private static final String[] TOP = { "ABBBA", "BBBBB", "BBBBB", "BBBBB", "ABBBA" };

    private static final HatchFlags INPUTS = new HatchFlags.Builder().with(ENERGY_INPUT, FLUID_INPUT).build();
    private static final HatchFlags OUTPUT = new HatchFlags.Builder().with(FLUID_OUTPUT).build();

    private static ShapeTemplate[] shapes;
    private static boolean registered;

    private HighlyAdvancedDistillery() {}

    /** Call from the multiblock hook, after {@link ParallelMultiblocks#CASING} has been registered. */
    public static void register(MultiblockMachinesMIHookContext hook) {
        shapes = new ShapeTemplate[MAX_LAYERS];
        for (int i = 0; i < MAX_LAYERS; i++) {
            shapes[i] = shape(i + 1);
        }
        SpecialMachineBuilder builder = hook.builder(ID, ENGLISH_NAME, BlockEntity::new);
        builder.registerMachine();
        // One viewer page per height, as MI shows its tower.
        for (int i = 0; i < shapes.length; i++) {
            builder.registerMultiblockShape(shapes[i], String.valueOf(i + 1));
        }
        registered = true;
    }

    private static ShapeTemplate shape(int layers) {
        ShapeTemplate.Builder builder = new ShapeTemplate.Builder(MachineCasings.get(CASING));
        level(builder, 0, BOTTOM, INPUTS);
        for (int layer = 1; layer <= layers; layer++) {
            int base = 3 * (layer - 1);
            level(builder, base + 1, OPEN, null);
            level(builder, base + 2, OPEN, null);
            level(builder, base + 3, layer == layers ? TOP : RING, OUTPUT);
        }
        return builder.build();
    }

    /** One 5×5 level; the controller sits at x 0, z 0 and the tower runs back along +z. */
    private static void level(ShapeTemplate.Builder builder, int y, String[] rows, HatchFlags casingFlags) {
        for (int z = 0; z < rows.length; z++) {
            String row = rows[z];
            for (int column = 0; column < row.length(); column++) {
                char key = row.charAt(column);
                if (key == ' ' || key == '#') continue;
                builder.add(2 - column, y, z, member(key), key == 'B' ? casingFlags : null);
            }
        }
    }

    private static SimpleMember member(char key) {
        String block = switch (key) {
            case 'A' -> "roll_mod:netherite_frame";
            case 'B' -> "modern_industrialization:tungsten_steel_heat_proof_casing";
            case 'C' -> "modern_industrialization:osmirium_casing";
            case 'D' -> "modern_industrialization:osmiridium_machine_casing_pipe";
            default -> throw new IllegalArgumentException("Unknown key " + key);
        };
        return SimpleMember.forBlockId(ResourceLocation.parse(block));
    }

    /** A workstation in every recipe-viewer category of the distillation tower recipe type. */
    public static void registerWorkstations() {
        if (!registered) return;
        ResourceLocation machine = RollMod.id(ID);
        ReiMachineRecipes.categories.values().stream()
                .filter(params -> params.recipeType == MIMachineRecipeTypes.DISTILLATION_TOWER)
                .map(params -> params.category)
                .toList()
                .forEach(category -> ReiMachineRecipes.registerWorkstation(category, machine));
    }

    /**
     * {@link ElectricMultipliedCraftingMultiblockBlockEntity} with MI's tower behaviour: the picked height
     * decides the shape and how many fluid outputs a recipe may use.
     */
    private static final class BlockEntity extends ElectricMultipliedCraftingMultiblockBlockEntity {

        BlockEntity(BEP bep) {
            super(bep, RollMod.id(ID), shapes, MachineTier.MULTIBLOCK, MIMachineRecipeTypes.DISTILLATION_TOWER, PARALLEL,
                    EuCostTransformers.percentage(() -> 1f));
            List<Component> names = IntStream.rangeClosed(1, MAX_LAYERS)
                    .<Component>mapToObj(n -> Component.translatable("gui.roll_mod.highly_advanced_distillery.layers", n))
                    .toList();
            registerGuiComponent(new ShapeSelection(new ShapeSelection.Behavior() {
                @Override
                public void handleClick(int clickedLine, int delta) {
                    activeShape.incrementShape(BlockEntity.this, delta);
                }

                @Override
                public int getCurrentIndex(int line) {
                    return activeShape.getActiveShapeIndex();
                }
            }, new ShapeSelection.LineInfo(names, false)));
        }

        @Override
        public int getMaxFluidOutputs() {
            return activeShape.getActiveShapeIndex() + 1;
        }
    }
}
