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

@EventBusSubscriber(modid = RollMod.MODID)
public final class RollCauldronInteractions {

    private static final float DUST_DROP_CHANCE = 0.2f; // 20% chance

    private RollCauldronInteractions() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(RollCauldronInteractions::register);
    }

    /**
     * Gets the dust items array lazily to ensure items are loaded.
     * @return Array of dust items (stone and clay)
     */
    private static Item[] getDustItems() {
        return new Item[]{
                ItemRegistry.STONE_DUST.get(),
                ItemRegistry.CLAY_DUST.get()
        };
    }

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

            // 20% chance to drop stone or clay dust for EACH item in the stack
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

