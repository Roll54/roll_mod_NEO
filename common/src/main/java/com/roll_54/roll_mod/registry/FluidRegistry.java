package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
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
 * roll_mod's placeable fluids. Each one is the full NeoForge set: a {@link FluidType} (properties,
 * name), a source and a flowing {@link Fluid}, the {@link LiquidBlock} that sits in the world, and a
 * {@link BucketItem}. Textures are attached client-side in {@code RollModClient} from
 * {@link RollFluid#stillTexture()} / {@link RollFluid#flowingTexture()}.
 */
public final class FluidRegistry {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RollMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, RollMod.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RollMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RollMod.MODID);

    private static final List<RollFluid> ALL = new ArrayList<>();

    // Mutagen ships no flowing texture, so the still one is used for both.
    public static final RollFluid MUTAGEN = register("mutagen", MapColor.COLOR_MAGENTA,
            "block/fluid/mutagen_still", "block/fluid/mutagen_still");
    public static final RollFluid NUCLEAR_WASTE = register("nuclear_waste", MapColor.COLOR_LIGHT_GREEN,
            "block/fluid/nuclear_waste_still", "block/fluid/nuclear_waste_flow");
    public static final RollFluid BLOCK_BLENCIUM = register("block_blencium", MapColor.COLOR_BLUE,
            "block/fluid/block_blencium_still", "block/fluid/block_blencium_flow");

    public record RollFluid(
            String name,
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
            DeferredBlock<LiquidBlock> block,
            DeferredItem<BucketItem> bucket,
            String stillTexture,
            String flowingTexture
    ) {}

    private static RollFluid register(String name, MapColor mapColor, String stillTexture, String flowingTexture) {
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name, () -> new FluidType(
                FluidType.Properties.create()
                        .canSwim(true)
                        .canDrown(true)
                        .supportsBoating(true)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

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
        BLOCKS.register(name, () -> new LiquidBlock(source.get(),
                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(mapColor)));
        ITEMS.register(name + "_bucket", () -> new BucketItem(source.get(),
                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

        RollFluid fluid = new RollFluid(name, type, source, flowing, block, bucket, stillTexture, flowingTexture);
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
