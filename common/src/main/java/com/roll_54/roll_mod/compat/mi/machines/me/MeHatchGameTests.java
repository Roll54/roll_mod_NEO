package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The ME hatches against a real network: a creative energy cell and an ME chest holding one cell, the hatch
 * beside the chest. No multiblock is built; the tests play the multiblock's part by taking from and putting
 * into the hatch's MI slots, as MI's crafter does. Runs on {@code :server:runGameTestServer}, or
 * {@code /test runall} in a dev world.
 */
@GameTestHolder(RollMod.MODID)
@PrefixGameTestTemplate(false)
public final class MeHatchGameTests {

    private static final String TEMPLATE = "gametest_empty_5x3x3";

    private static final BlockPos CELL = new BlockPos(0, 1, 1);
    private static final BlockPos CHEST = new BlockPos(1, 1, 1);
    private static final BlockPos HATCH = new BlockPos(1, 1, 2);

    private MeHatchGameTests() {}

    /* ---------------------------------------------------- input ----------------------------------------------------- */

    /** A config slot is stocked from ME, refilled after the multiblock takes some, and returned when cleared. */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void itemInputStocksRefillsAndReturns(GameTestHelper helper) {
        buildNetwork(helper, false, AEItemKey.of(Items.IRON_INGOT), 100);
        MeInputHatchBlockEntity hatch = place(helper, "me_item_input_hatch");
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        hatch.getConfig().setStack(0, new GenericStack(iron, 32));

        int[] stage = {0};
        helper.succeedWhen(() -> {
            if (stage[0] == 0) {
                assertStock(helper, hatch, 0, iron, 32);
                helper.assertValueEqual(networkCount(helper, iron), 68L, "network after stocking");
                // The multiblock uses 10.
                hatch.getInventory().getItemStacks().get(0).decrement(10);
                stage[0] = 1;
                helper.fail("refilling");
            }
            if (stage[0] == 1) {
                assertStock(helper, hatch, 0, iron, 32);
                helper.assertValueEqual(networkCount(helper, iron), 58L, "network after refill");
                hatch.getConfig().setStack(0, null);
                stage[0] = 2;
                helper.fail("returning");
            }
            helper.assertValueEqual(hatch.getInventory().getItemStacks().get(0).getAmount(), 0L, "stock after clear");
            helper.assertValueEqual(networkCount(helper, iron), 90L, "network after clear");
        });
    }

    /** An item slot stocks past a stack, up to {@link MeHatches#ITEM_INPUT_CAPACITY}; more is clamped to it. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void itemInputStocksPastAStack(GameTestHelper helper) {
        AEItemKey cobble = AEItemKey.of(Items.COBBLESTONE);
        buildNetwork(helper, false, cobble, 3_000);
        MeInputHatchBlockEntity hatch = place(helper, "me_item_input_hatch");
        hatch.getConfig().setStack(0, new GenericStack(cobble, MeHatches.ITEM_INPUT_CAPACITY));
        hatch.getConfig().setStack(1, new GenericStack(cobble, 5_000));
        helper.assertValueEqual(hatch.getConfig().getStack(1).amount(), MeHatches.ITEM_INPUT_CAPACITY, "clamped config");

        helper.succeedWhen(() -> {
            assertStock(helper, hatch, 0, cobble, MeHatches.ITEM_INPUT_CAPACITY);
            assertStock(helper, hatch, 1, cobble, MeHatches.ITEM_INPUT_CAPACITY);
            helper.assertValueEqual(networkCount(helper, cobble), 3_000L - 2 * MeHatches.ITEM_INPUT_CAPACITY, "network");
        });
    }

    /** Broken with no ME to take the stock, a slot above a stack drops as normal stacks, not one oversized one. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void itemInputDropsStockInStacks(GameTestHelper helper) {
        MeInputHatchBlockEntity hatch = place(helper, "me_item_input_hatch");
        ConfigurableItemStack slot = hatch.getInventory().getItemStacks().get(0);
        slot.setKey(ItemVariant.of(Items.COBBLESTONE));
        slot.setAmount(1_000);
        helper.destroyBlock(HATCH);

        helper.succeedWhen(() -> {
            BlockPos at = helper.absolutePos(HATCH);
            List<ItemStack> cobble = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2))
                    .stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.COBBLESTONE)).toList();
            helper.assertValueEqual(cobble.stream().mapToInt(ItemStack::getCount).sum(), 1_000, "cobblestone dropped");
            helper.assertTrue(cobble.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()),
                    "every drop within a stack");
        });
    }

    /** The fluid hatch does the same in mB. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fluidInputStocks(GameTestHelper helper) {
        AEFluidKey water = AEFluidKey.of(Fluids.WATER);
        buildNetwork(helper, true, water, 10_000);
        MeInputHatchBlockEntity hatch = place(helper, "me_fluid_input_hatch");
        hatch.getConfig().setStack(3, new GenericStack(water, 4_000));

        helper.succeedWhen(() -> {
            assertStock(helper, hatch, 3, water, 4_000);
            helper.assertValueEqual(networkCount(helper, water), 6_000L, "network");
        });
    }

    /** Stock that is not what its slot is set to goes back to ME. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void itemInputReturnsUnconfiguredStock(GameTestHelper helper) {
        AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
        buildNetwork(helper, false, gold, 1);
        MeInputHatchBlockEntity hatch = place(helper, "me_item_input_hatch");
        ConfigurableItemStack slot = hatch.getInventory().getItemStacks().get(5);
        slot.setKey(ItemVariant.of(Items.GOLD_INGOT));
        slot.setAmount(20);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(hatch.getInventory().getItemStacks().get(5).getAmount(), 0L, "stock");
            helper.assertValueEqual(networkCount(helper, gold), 21L, "network");
        });
    }

    /* ---------------------------------------------------- output ---------------------------------------------------- */

    /** Output put into a receiving slot reaches ME within a flush. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void itemOutputFlushesToMe(GameTestHelper helper) {
        AEItemKey cobble = AEItemKey.of(Items.COBBLESTONE);
        buildNetwork(helper, false, null, 0);
        MeItemOutputHatchBlockEntity hatch = place(helper, "me_item_output_hatch");
        output(hatch, 0, Items.COBBLESTONE, 64);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(networkCount(helper, cobble), 64L, "network");
            helper.assertTrue(hatch.getBuffer(0) == null, "buffer empty");
            helper.assertValueEqual(hatch.getInventory().getItemStacks().get(0).getAmount(), 0L, "receiving slot");
        });
    }

    /**
     * With no network the buffer keeps taking output, in any amount; a fourth type waits in its receiving
     * slot. Once a network is there, everything flushes.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void itemOutputBuffersOfflineThenFlushes(GameTestHelper helper) {
        MeItemOutputHatchBlockEntity hatch = place(helper, "me_item_output_hatch");
        Item[] types = {Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL};
        int[] rounds = {0};
        int[] stage = {0};

        helper.succeedWhen(() -> {
            if (stage[0] == 0) {
                // Ten completions of 64 cobble, one per tick, each emptied into the buffer the next tick.
                if (rounds[0] < 10) {
                    if (hatch.getInventory().getItemStacks().get(0).getAmount() == 0) {
                        output(hatch, 0, Items.COBBLESTONE, 64);
                        rounds[0]++;
                    }
                    helper.fail("outputting");
                }
                assertBuffer(helper, hatch, AEItemKey.of(Items.COBBLESTONE), 640);
                for (int i = 1; i < types.length; i++) output(hatch, i, types[i], 10);
                stage[0] = 1;
                helper.fail("four types");
            }
            if (stage[0] == 1) {
                // Cobble plus two of the three new types fill the buffer; the last waits.
                helper.assertValueEqual(hatch.getInventory().getItemStacks().get(3).getAmount(), 10L, "4th type waits");
                for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) helper.assertTrue(hatch.getBuffer(i) != null, "buffer " + i);
                buildNetwork(helper, false, null, 0);
                stage[0] = 2;
                helper.fail("flushing");
            }
            helper.assertValueEqual(networkCount(helper, AEItemKey.of(Items.COBBLESTONE)), 640L, "cobble in ME");
            helper.assertValueEqual(networkCount(helper, AEItemKey.of(Items.GRAVEL)), 10L, "4th type in ME");
            for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) helper.assertTrue(hatch.getBuffer(i) == null, "buffer " + i);
        });
    }

    /** Broken with a buffer ME cannot take, the hatch item carries it; placed again, it is back. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void itemOutputBufferTravelsWithTheItem(GameTestHelper helper) {
        MeItemOutputHatchBlockEntity hatch = place(helper, "me_item_output_hatch");
        output(hatch, 0, Items.DIAMOND, 50);

        boolean[] broken = {false};
        helper.succeedWhen(() -> {
            if (!broken[0]) {
                assertBuffer(helper, hatch, AEItemKey.of(Items.DIAMOND), 50);
                helper.destroyBlock(HATCH);
                broken[0] = true;
                helper.fail("dropping");
            }
            BlockPos at = helper.absolutePos(HATCH);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
            ItemStack dropped = drops.stream().map(ItemEntity::getItem)
                    .filter(stack -> stack.has(ComponentsRegistry.ME_HATCH_BUFFER.get()))
                    .findFirst().orElse(ItemStack.EMPTY);
            helper.assertFalse(dropped.isEmpty(), "hatch item with its buffer");
            helper.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(Items.DIAMOND)), "no loose diamonds");

            MeItemOutputHatchBlockEntity placed = place(helper, "me_item_output_hatch");
            placed.onPlaced(null, dropped);
            assertBuffer(helper, placed, AEItemKey.of(Items.DIAMOND), 50);
        });
    }

    /** Fluid output tanks reach ME within a flush. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fluidOutputFlushesToMe(GameTestHelper helper) {
        AEFluidKey water = AEFluidKey.of(Fluids.WATER);
        buildNetwork(helper, true, null, 0);
        MeFluidOutputHatchBlockEntity hatch = place(helper, "me_fluid_output_hatch");
        ConfigurableFluidStack tank = hatch.getInventory().getFluidStacks().get(1);
        tank.setKey(FluidVariant.of(Fluids.WATER));
        tank.setAmount(5_000);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(networkCount(helper, water), 5_000L, "network");
            helper.assertTrue(hatch.getTank(1) == null, "tank empty");
        });
    }

    /* ---------------------------------------------------- setup ----------------------------------------------------- */

    /** Energy cell and an ME chest with a 1k item or fluid cell, holding {@code amount} of {@code key}. */
    private static void buildNetwork(GameTestHelper helper, boolean fluidCell, AEKey key, long amount) {
        helper.setBlock(CELL, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(CHEST, AEBlocks.ME_CHEST.block());
        ItemStack cell = fluidCell ? AEItems.FLUID_CELL_1K.stack() : AEItems.ITEM_CELL_1K.stack();
        if (key != null && amount > 0) {
            StorageCells.getCellInventory(cell, null).insert(key, amount, Actionable.MODULATE, IActionSource.empty());
        }
        MEChestBlockEntity chest = helper.getBlockEntity(CHEST);
        chest.setCell(cell);
    }

    @SuppressWarnings("unchecked")
    private static <T extends MeHatchBlockEntity> T place(GameTestHelper helper, String id) {
        Block block = BuiltInRegistries.BLOCK.get(RollMod.id(id));
        helper.setBlock(HATCH, block);
        return (T) helper.getBlockEntity(HATCH);
    }

    /** Plays the crafter: puts {@code amount} of {@code item} into receiving slot {@code slot}. */
    private static void output(MeItemOutputHatchBlockEntity hatch, int slot, Item item, long amount) {
        ConfigurableItemStack stack = hatch.getInventory().getItemStacks().get(slot);
        stack.setKey(ItemVariant.of(item));
        stack.setAmount(amount);
    }

    private static void assertStock(GameTestHelper helper, MeInputHatchBlockEntity hatch, int slot, AEKey key, long amount) {
        GenericStack stock = hatch.getStock(slot);
        helper.assertTrue(stock != null && stock.what().equals(key), "stock key");
        helper.assertValueEqual(stock.amount(), amount, "stock amount");
    }

    private static void assertBuffer(GameTestHelper helper, MeItemOutputHatchBlockEntity hatch, AEKey key, long amount) {
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            GenericStack buffered = hatch.getBuffer(i);
            if (buffered != null && buffered.what().equals(key)) {
                helper.assertValueEqual(buffered.amount(), amount, "buffered " + key);
                return;
            }
        }
        helper.fail("nothing buffered of " + key);
    }

    private static long networkCount(GameTestHelper helper, AEKey key) {
        MEChestBlockEntity chest = helper.getBlockEntity(CHEST);
        var inv = chest.getCellInventory(0);
        return inv == null ? 0 : inv.getAvailableStacks().get(key);
    }
}
