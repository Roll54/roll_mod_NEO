package com.roll_54.roll_mod.util;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import com.roll_54.roll_mod.recipe.CauldronWashingRecipe;
import com.roll_54.roll_mod.registry.RecipeRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import java.util.List;
import java.util.Optional;

/**
 * Cauldron washing. The washes themselves are {@code roll_mod:cauldron_washing} recipes
 * ({@link CauldronWashingRecipe}): datagen writes roll_mod's, datapacks and KubeJS can add, change or remove any.
 * Right-clicking a water cauldron with an item one of them takes washes the whole stack.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class RollCauldronInteractions {

    /** @deprecated each recipe carries its own {@code byproduct_chance}. */
    @Deprecated
    public static final float DUST_DROP_CHANCE = 0.5f; // 50% for a dust, split evenly: 25% stone, 25% clay

    private RollCauldronInteractions() {
    }

    /** A right-click on a water cauldron: wash the held stack if a recipe takes it. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON)) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Optional<RecipeHolder<CauldronWashingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeRegistry.CAULDRON_WASHING_TYPE.get(), new SingleRecipeInput(stack), level);
        if (recipe.isEmpty()) return;
        if (!level.isClientSide) wash(state, level, pos, event.getEntity(), event.getHand(), stack, recipe.get().value());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    private static void wash(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                             ItemStack inputStack, CauldronWashingRecipe recipe) {
        int count = inputStack.getCount();
        ItemStack result = recipe.result().copyWithCount(Math.min(recipe.result().getCount() * count,
                recipe.result().getMaxStackSize()));
        player.setItemInHand(hand, result);
        if (recipe.usesWater()) LayeredCauldronBlock.lowerFillLevel(state, level, pos);

        List<Item> byproducts = recipe.byproducts();
        if (!byproducts.isEmpty()) {
            RandomSource random = level.getRandom();
            for (int i = 0; i < count; i++) {
                if (random.nextFloat() >= recipe.byproductChance()) continue;
                Item dust = byproducts.get(random.nextInt(byproducts.size()));
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        new ItemStack(dust)));
            }
        }
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    /**
     * @deprecated washes are {@code roll_mod:cauldron_washing} recipes now ({@link #onRightClickBlock}); this
     * hard-coded registration is no longer called. Kept, not deleted.
     */
    @Deprecated
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(RollCauldronInteractions::register);
    }

    /**
     * The byproducts roll_mod's own washes drop (stone and clay dust); datagen writes them into each recipe.
     * @return Array of dust items (stone and clay)
     */
    public static Item[] getDustItems() {
        return new Item[]{
                ItemRegistry.STONE_DUST.get(),
                ItemRegistry.CLAY_DUST.get()
        };
    }

    /** @deprecated see {@link #onCommonSetup}. */
    @Deprecated
    private static void register() {
        for (OreDefinition ore : OreDefinitions.ORE_DEFINITIONS) {
            String id = ore.id();

            // roll_mod:impure_yellow_limonite_dust -> roll_mod:yellow_limonite_dust
            registerWashing(
                    "impure_" + id + "_dust",
                    id + "_dust"
            );

            // roll_mod:crushed_yellow_limonite_ore -> roll_mod:purified_yellow_limonite_ore
            registerWashing(
                    "crushed_" + id + "_ore",
                    "purified_" + id + "_ore"
            );
        }
    }

    private static void registerWashing(String inputPath, String outputPath) {
        ResourceLocation inputId = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, inputPath);
        ResourceLocation outputId = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, outputPath);

        var input = BuiltInRegistries.ITEM.getOptional(inputId);
        var output = BuiltInRegistries.ITEM.getOptional(outputId);

        if (input.isEmpty() || output.isEmpty()) {
            RollMod.LOGGER.warn(
                    "Skipping cauldron washing recipe: {} -> {}. Missing input or output item.",
                    inputId,
                    outputId
            );
            return;
        }

        CauldronInteraction.WATER.map().put(input.get(), createWashingInteraction(output.get()));
    }

    private static CauldronInteraction createWashingInteraction(Item outputItem) {
        return (state, level, pos, player, hand, stack) ->
                washStack(state, level, pos, player, hand, stack, outputItem);
    }

    private static ItemInteractionResult washStack(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack inputStack,
            Item outputItem
    ) {
        if (!level.isClientSide) {
            int count = inputStack.getCount();

            ItemStack result = new ItemStack(outputItem, count);
            player.setItemInHand(hand, result);

            // Зменшує воду в котлі на 1 рівень, як ванільна промивка/очистка.
            // Якщо хочеш нескінченну воду — прибери цей рядок.
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);

            // 50% chance to drop a dust for EACH item in the stack: 25% stone, 25% clay
            RandomSource random = level.getRandom();
            Item[] dustItems = getDustItems();

            for (int i = 0; i < count; i++) {
                if (random.nextFloat() < DUST_DROP_CHANCE) {
                    Item dustItem = dustItems[random.nextInt(dustItems.length)];
                    ItemStack dustStack = new ItemStack(dustItem, 1);
                    ItemEntity dustEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, dustStack);
                    level.addFreshEntity(dustEntity);
                }
            }

            level.playSound(
                    null,
                    pos,
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS,
                    0.5F,
                    1.0F
            );
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}

