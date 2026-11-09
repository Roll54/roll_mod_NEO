package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.AECapabilities;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.swedz.tesseract.neoforge.capabilities.CapabilitiesListeners;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;

import java.util.List;

/**
 * MI hatches that talk to an AE2 network themselves, registered under {@code roll_mod:} through Tesseract.
 *
 * <ul>
 *   <li>{@code me_item_input_hatch} / {@code me_fluid_input_hatch}: 16 slots, each kept stocked from ME
 *       with a configured item or fluid, like an ME interface. See {@link MeInputHatchBlockEntity}.
 *   <li>{@code me_item_output_hatch}: 3 buffer slots of unlimited size, pushed into ME every
 *       {@link #FLUSH_INTERVAL} ticks. See {@link MeItemOutputHatchBlockEntity}.
 *   <li>{@code me_fluid_output_hatch}: 3 tanks of unlimited size, likewise. See
 *       {@link MeFluidOutputHatchBlockEntity}.
 * </ul>
 *
 * <p>Every one is an AE2 grid node needing a channel. Its block is {@link MeHatchBlock}, which opens an
 * LDLib2 screen instead of MI's. Models are written by
 * {@link com.roll_54.roll_mod.data.datagen.RollMachineModelProvider} with the {@link #CASING_NAME} casing.
 */
public final class MeHatches {

    /** Config / stock slots of an input hatch. */
    public static final int INPUT_SLOTS = 16;
    /** Buffer slots (item) or tanks (fluid) of an output hatch. */
    public static final int OUTPUT_SLOTS = 3;
    /**
     * Slots MI puts an item output hatch's output into. MI caps every item slot at one stack per insert,
     * so this is what one recipe completion can hand over; each tick they are emptied into the buffer.
     */
    public static final int ITEM_RECEIVING_SLOTS = 27;
    /** Ticks between an input hatch's restocks, the fastest an AE2 interface ticks. */
    public static final int INPUT_INTERVAL = 5;
    /** Ticks between an output hatch's pushes into ME. */
    public static final int FLUSH_INTERVAL = 20;
    /**
     * Items an item input slot holds (and may be configured to), whatever the item's stack size. Above a stack
     * on purpose: one slot can feed a big parallel batch. MI's recipe check and consumption read the slot's
     * long amount, and the hatch fills it directly, so MI's one-stack insert limit does not apply.
     */
    public static final long ITEM_INPUT_CAPACITY = 1024;
    /** mB a fluid input slot holds (and may be configured to): as much as a fluid handler can report. */
    public static final long FLUID_INPUT_CAPACITY = Integer.MAX_VALUE;
    /** mB an output tank holds: unlimited. */
    public static final long FLUID_OUTPUT_CAPACITY = Long.MAX_VALUE;

    /** The casing, {@code roll_mod:me_hatch}; registered in {@code MachineRegistry.casings}. */
    public static final String CASING_NAME = "me_hatch";
    public static final String CASING = RollMod.MODID + ":" + CASING_NAME;
    public static final ResourceLocation CASING_TEXTURE = RollMod.id("block/me_hatch/casing");

    /**
     * @param prefix      id prefix; Tesseract adds {@code _input_hatch} / {@code _output_hatch}
     * @param englishName English name before "Input Hatch" / "Output Hatch"
     * @param fluid       fluid hatches, else item hatches
     */
    public record Spec(String prefix, String englishName, boolean fluid) {
        public List<String> ids() {
            return List.of(prefix + "_input_hatch", prefix + "_output_hatch");
        }

        /** Folder of the overlay textures: {@code overlay_front}, {@code overlay_side}, {@code output_<kind>}. */
        public ResourceLocation overlays() {
            return RollMod.id("block/me_hatch/" + (fluid ? "fluid" : "item"));
        }

        public String outputOverlay() {
            return fluid ? "output_fluid" : "output_item";
        }
    }

    public static final Spec ITEM = new Spec("me_item", "ME Item", false);
    public static final Spec FLUID = new Spec("me_fluid", "ME Fluid", true);
    public static final List<Spec> ALL = List.of(ITEM, FLUID);

    private MeHatches() {}

    public static void register(HatchMIHookContext hook) {
        for (Spec spec : ALL) {
            hook.builder(spec.prefix(), spec.englishName())
                    .special((bep, input, id) -> create(spec, bep, input, id), true)
                    .creator(MeHatchBlock::new)
                    // MI's own capability, as on its hatches, so other mods see these as MI hatches.
                    .registrator(spec.fluid() ? MachineBlockEntity::registerFluidApi : MachineBlockEntity::registerItemApi)
                    .registrator(MeHatches::registerGridNodeHost)
                    .registerMachine();
        }
    }

    private static HatchBlockEntity create(Spec spec, BEP bep, boolean input, ResourceLocation id) {
        if (input) return new MeInputHatchBlockEntity(bep, id, spec.fluid());
        return spec.fluid() ? new MeFluidOutputHatchBlockEntity(bep, id) : new MeItemOutputHatchBlockEntity(bep, id);
    }

    /** Lets AE2 cables connect: the hatch is its own in-world grid node host. */
    @SuppressWarnings("unchecked")
    private static void registerGridNodeHost(BlockEntityType<?> type) {
        BlockEntityType<MeHatchBlockEntity> hatchType = (BlockEntityType<MeHatchBlockEntity>) type;
        CapabilitiesListeners.register(RollMod.MODID, event -> event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST, hatchType, (hatch, context) -> hatch));
    }

    /**
     * Lists the buffer an ME item output hatch item carries ({@link ComponentsRegistry#ME_HATCH_BUFFER}), so it
     * is not mistaken for an empty one. Called from the client's tooltip handler, which cannot see AE2's types.
     */
    public static void appendBufferTooltip(ItemStack stack, List<Component> tooltip) {
        List<GenericStack> buffer = stack.get(ComponentsRegistry.ME_HATCH_BUFFER.get());
        if (buffer == null || buffer.isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.roll_mod.me_hatch.stored").withStyle(ChatFormatting.GOLD));
        for (GenericStack stored : buffer) {
            tooltip.add(Component.literal("  ")
                    .append(stored.what().getDisplayName())
                    .append(" ×" + stored.what().formatAmount(stored.amount(), AmountFormat.FULL))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
