package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.fluid.FluidNameStyle;
import com.roll_54.roll_mod.fluid.RollBucketItem;
import com.roll_54.roll_mod.fluid.RollFluidType;
import com.roll_54.roll_mod.fluid.RollLiquidBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * roll_mod's fluids, registered the way Modern Industrialization registers its own ({@code MIFluids} /
 * {@code FluidDefinition}): each fluid is one line naming it and giving it <b>one colour</b>, and that
 * colour drives everything else — the name of the fluid, its block and its bucket ({@link FluidNameStyle},
 * with MI's minimum luminance), and, for fluids without a texture of their own, the tint.
 *
 * <p>Unlike MI's fluids these stay placeable: each is the full NeoForge set — a {@link FluidType}, a
 * source and a flowing {@link Fluid}, the {@link LiquidBlock} that sits in the world (nuclear waste
 * radiates through it) and a {@link BucketItem}. Textures are attached client-side in
 * {@code RollModClient} from {@link RollFluid#stillTextureId()} / {@link RollFluid#flowingTextureId()},
 * tinted with {@link RollFluid#tintColor()}.
 */
public final class FluidRegistry {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RollMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, RollMod.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RollMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RollMod.MODID);

    private static final List<RollFluid> ALL = new ArrayList<>();

    /** Drawn as is. */
    private static final int NO_TINT = 0xFFFFFFFF;
    /** Vanilla water is greyscale, so tinting it gives a plain fluid in any colour (MI's water-like look). */
    private static final String WATER_STILL = "minecraft:block/water_still";
    private static final String WATER_FLOW = "minecraft:block/water_flow";

    // Textured fluids: the colour is their texture's average and only colours the name.
    // Mutagen ships no flowing texture, so the still one is used for both.
    public static final RollFluid MUTAGEN = textured("mutagen", 0x881E57, MapColor.COLOR_MAGENTA,
            "block/fluid/mutagen_still", "block/fluid/mutagen_still");
    public static final RollFluid NUCLEAR_WASTE = textured("nuclear_waste", 0x358525, MapColor.COLOR_LIGHT_GREEN,
            "block/fluid/nuclear_waste_still", "block/fluid/nuclear_waste_flow");
    /** Named in a &9 → &b gradient (blue to aqua) rather than one colour. */
    public static final RollFluid BLOCK_BLENCIUM = register("block_blencium", 0x008DFF, MapColor.COLOR_BLUE,
            "block/fluid/block_blencium_still", "block/fluid/block_blencium_flow", false,
            FluidNameStyle.gradient(0x5555FF, 0x55FFFF));

    // Liquid herbicides for the Crop Managers, one per item tier: 1000 mB equals one item of that tier
    // (CropBreedingSettings). Each in its tier's lore colour.
    public static final RollFluid HERBICIDE_TIER_1 = tinted("herbicide_tier_1", 0x44FF8E, MapColor.COLOR_LIGHT_GREEN);
    public static final RollFluid HERBICIDE_TIER_2 = tinted("herbicide_tier_2", 0xFF4B41, MapColor.COLOR_RED);
    public static final RollFluid HERBICIDE_TIER_3 = tinted("herbicide_tier_3", 0x632CFF, MapColor.COLOR_PURPLE);

    public record RollFluid(
            String name,
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
            DeferredBlock<LiquidBlock> block,
            DeferredItem<BucketItem> bucket,
            String stillTexture,
            String flowingTexture,
            /** The fluid's colour, RGB. */
            int color,
            /** Whether {@link #color} tints the textures (fluids without a texture of their own). */
            boolean tinted,
            FluidNameStyle nameStyle
    ) {
        /** A bare path is in roll_mod's namespace; {@code ns:path} points anywhere else. */
        public ResourceLocation stillTextureId() {
            return textureId(stillTexture);
        }

        public ResourceLocation flowingTextureId() {
            return textureId(flowingTexture);
        }

        public boolean isTinted() {
            return tinted;
        }

        /** ARGB tint for the fluid renderer and the dynamic bucket. */
        public int tintColor() {
            return tinted ? 0xFF000000 | color : NO_TINT;
        }

        private static ResourceLocation textureId(String texture) {
            return texture.contains(":") ? ResourceLocation.parse(texture) : RollMod.id(texture);
        }
    }

    /** A fluid with its own textures, named in {@code color}. */
    private static RollFluid textured(String name, int color, MapColor mapColor, String stillTexture,
                                      String flowingTexture) {
        return register(name, color, mapColor, stillTexture, flowingTexture, false, FluidNameStyle.solid(color));
    }

    /** A fluid drawn as vanilla water tinted with {@code color}, and named in it. */
    private static RollFluid tinted(String name, int color, MapColor mapColor) {
        return register(name, color, mapColor, WATER_STILL, WATER_FLOW, true, FluidNameStyle.solid(color));
    }

    private static RollFluid register(String name, int color, MapColor mapColor, String stillTexture,
                                      String flowingTexture, boolean tinted, FluidNameStyle nameStyle) {
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name, () -> new RollFluidType(
                FluidType.Properties.create()
                        .canSwim(true)
                        .canDrown(true)
                        .supportsBoating(true)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY),
                nameStyle));

        // The fluid properties need the source, flowing fluid, block and bucket before they exist,
        // so they point at holders by id; the real entries are registered under the same ids below.
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source =
                DeferredHolder.create(Registries.FLUID, RollMod.id(name));
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing =
                DeferredHolder.create(Registries.FLUID, RollMod.id("flowing_" + name));
        DeferredBlock<LiquidBlock> block = DeferredBlock.createBlock(RollMod.id(name));
        DeferredItem<BucketItem> bucket = DeferredItem.createItem(RollMod.id(name + "_bucket"));

        BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(type, source, flowing)
                .bucket(bucket)
                .block(block);

        FLUIDS.register(name, () -> new BaseFlowingFluid.Source(props));
        FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(props));
        BLOCKS.register(name, () -> new RollLiquidBlock(source.get(),
                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(mapColor), nameStyle));
        ITEMS.register(name + "_bucket", () -> new RollBucketItem(source.get(),
                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1), nameStyle));

        RollFluid fluid = new RollFluid(name, type, source, flowing, block, bucket, stillTexture, flowingTexture,
                color, tinted, nameStyle);
        ALL.add(fluid);
        return fluid;
    }

    public static List<RollFluid> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    private FluidRegistry() {}
}
