package com.roll_54.roll_mod.compat.MBD2.greenhouse;
/*
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMultiblockSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseItemOnEvent;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;


 * The EV Greenhouse: a 3×3×3 MBD2 multiblock that grows an AgriCraft seed (see {@link GrowerTrait}).
 * Crops leave and EU, water and fertilizer arrive through ordinary MI hatches built into it.
 *
 * <p>The structure is the editor project {@code mbd2:new_multiblock}, controller in the middle of the
 * front face, read back to front with each layer bottom first:
 * <pre>
 *   back           middle         front
 *   H F H          F S F          H C H      bottom
 *   F G F          G . G          F G F      middle      H highly advanced hull, or an MI hatch
 *   H F H          F L F          H F H      top         F netherite frame   G glass   S grass
 *                                                        L lapotronic laser  . air (the crop)
 * </pre>
 * At least one MI item output, fluid input and energy input hatch, any tier. Once formed, the client
 * stops drawing the structure's blocks ({@link HiddenStructureBlocks}) and the controller draws the geo
 * model ({@link GreenhouseModelRenderer}) over the whole 3×3×3; the blocks themselves never change.
 *
 * <p>Built in Java like {@code RollMBD2Machines}, and for the same reason registered at
 * {@code FMLConstructModEvent}: nothing may resolve a registry entry until the pattern factory runs.

public class GreenhouseMachines {

    public static final ResourceLocation CONTROLLER_ID = RollMod.id("ev_greenhouse");

    /** Trait names of the controller.
    public static final String SEED_TRAIT = "seed";
    public static final String GROWER_TRAIT = "grower";

    private static final ResourceLocation HULL = ResourceLocation.parse("modern_industrialization:highly_advanced_machine_hull");
    private static final ResourceLocation FRAME = RollMod.id("netherite_frame");
    private static final ResourceLocation LASER = RollMod.id("lapotronic_laser_block");

    public GreenhouseMachines() {
        // Upgrades go in by right-click, which MBD2 reports on the game bus.
        NeoForge.EVENT_BUS.addListener(GreenhouseMachines::onUseItem);
    }

    @SubscribeEvent
    public void onRegisterMachines(MBDRegistryEvent.Machine event) {
        event.register(controller());
        RollMod.LOGGER.info("[{}] registered MBD2 multiblock '{}'", RollMod.MODID, CONTROLLER_ID);
    }

    // ── Controller ──────────────────────────────────────────────────────────

    private static MultiblockMachineDefinition controller() {
        MultiblockMachineDefinition.Builder builder = MultiblockMachineDefinition.builder();
        builder.multiblockSettings(() -> ConfigMultiblockSettings.builder()
                .showUIOnlyFormed(true)
                // The model hides the controller among the other blocks; clicking any of them opens it.
                .showUIWhenClickStructure(true)
                .build());
        builder.id(CONTROLLER_ID)
                .rootState(controllerState())
                .blockProperties(ConfigBlockProperties.builder()
                        .rotationState(RotationState.NON_Y_AXIS)
                        .destroyTime(5.0f)
                        .explosionResistance(12.0f)
                        .build())
                .machineSettings(GreenhouseMachines::controllerSettings)
                // No MBD2 recipes: the grower trait does the work from serverTick().
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().enable(false).build());
        MultiblockMachineDefinition definition = builder.build();
        definition.blockPatternFactory(controller -> pattern(definition));
        // The preview a sneak + right-click on the unformed controller shows (and JEI/EMI's multiblock
        // page). MBD2 builds one from the pattern only for editor projects; a Java definition must set it.
        definition.shapeInfoFactory(Util.memoize(self -> new MultiblockShapeInfo[] {
                new MultiblockShapeInfo(pattern(definition).getPreview(new int[] { 1, 1, 1 }))
        }));
        return definition;
    }

    /**
     * Unformed: a plain hull. Formed: the geo model, with a box big enough that it is not culled
     * while the controller itself is off screen.

    private static MachineState controllerState() {
        @SuppressWarnings("unchecked")
        MachineState.Builder<MachineState> builder = (MachineState.Builder<MachineState>) MachineState.baseBuilder();
        return builder
                .modelRenderer(RollMod.id("block/ev_greenhouse_controller"))
                .renderingBox(new AABB(-2, 0, -2, 3, 3, 3))
                .shape(Shapes.block())
                .child("formed", formed -> formed
                        .renderer(GreenhouseModelRenderer::new)
                        .child("working", working -> working.child("waiting"))
                        .child("suspend"))
                .build();
    }

    private static ConfigMachineSettings controllerSettings() {
        ItemSlotCapabilityTraitDefinition seed = new ItemSlotCapabilityTraitDefinition();
        seed.setName(SEED_TRAIT);
        seed.setSlotSize(1);
        // BOTH: the player takes the seed back out; pipes may only put one in.
        seed.setGuiIO(IO.BOTH);
        seed.getCapabilityIO().setAllIO(IO.IN);

        GrowerTraitDefinition grower = new GrowerTraitDefinition();
        grower.setName(GROWER_TRAIT);

        return ConfigMachineSettings.builder()
                .hasUI(true)
                .uiTemplate(GreenhouseUI.template(seed, grower))
                .traitDefinitions(List.of(seed, grower))
                .build();
    }

    // ── Structure ───────────────────────────────────────────────────────────

    private static BlockPattern pattern(MBDMachineDefinition controller) {
        TraceabilityPredicate hullOrHatch = Predicates.blocks(block(HULL))
                .or(Predicates.blocks(hatches("_item_output_hatch")).setMinGlobalLimited(1))
                .or(Predicates.blocks(hatches("_fluid_input_hatch")).setMinGlobalLimited(1))
                .or(Predicates.blocks(hatches("_energy_input_hatch")).setMinGlobalLimited(1));
        return FactoryBlockPattern.start()
                .aisle("HFH", "FGF", "HFH")
                .aisle("FSF", "G_G", "FLF")
                .aisle("HCH", "FGF", "HFH")
                // Its own block, not any(): the structure preview draws the candidates.
                .where('C', Predicates.controller(Predicates.blocks(controller.block())))
                .where('H', hullOrHatch)
                .where('F', Predicates.blocks(block(FRAME)))
                .where('G', Predicates.blocks(Blocks.GLASS))
                .where('S', Predicates.blocks(Blocks.GRASS_BLOCK))
                .where('L', Predicates.blocks(block(LASER)))
                .where('_', Predicates.air())
                .build();
    }

    private static Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.get(id);
    }


     * Every MI hatch of one kind, whichever mod registers it ({@code modern_industrialization:lv_item_output_hatch},
     * this mod's fluid hatches, ...): MI names them {@code <tier>_<kind>}. Cheapest tier first, so the
     * structure preview shows that one.

    private static Block[] hatches(String suffix) {
        return BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> entry.getKey().location().getPath().endsWith(suffix))
                .sorted(java.util.Comparator.comparing(entry -> entry.getKey().location().toString()))
                .map(java.util.Map.Entry::getValue)
                .toArray(Block[]::new);
    }

    // ── Upgrades ────────────────────────────────────────────────────────────

    /**
     * MI upgrade items go into the controller by right-click, as into an MI machine; sneaking with an
     * empty hand takes them back.

    private static void onUseItem(MachineUseItemOnEvent event) {
        if (!CONTROLLER_ID.equals(event.getMachine().getDefinition().id())) return;
        GrowerTrait grower = event.getMachine().getTraitByName(GrowerTrait.class, GROWER_TRAIT);
        if (grower == null) return;
        Player player = event.getPlayer();
        ItemStack held = player.getItemInHand(event.getHand());
        boolean client = player.level().isClientSide();
        if (held.isEmpty() && player.isShiftKeyDown() && !grower.getUpgrades().isEmpty()) {
            if (!client) player.getInventory().placeItemBackInInventory(grower.takeUpgrades());
            event.setItemInteractionResult(ItemInteractionResult.sidedSuccess(client));
        } else if (client ? UpgradeComponent.getExtraEu(held.getItem()) > 0 : grower.insertUpgrades(held, player.isCreative())) {
            // The client only knows it is an upgrade; answering for it keeps the screen from opening.
            event.setItemInteractionResult(ItemInteractionResult.sidedSuccess(client));
        }
    }
}
*/