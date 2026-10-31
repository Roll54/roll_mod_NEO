package com.roll_54.roll_mod.economy.vendingblock.gametest;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.Position;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockRegistry;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The vending block's ME interface, end to end. The network is a creative energy cell, an ME chest
 * holding the network's items, a one-block crafting CPU and a pattern provider with a molecular
 * assembler; the vendor sits beside the chest only, so the provider never pushes into it. Runs on
 * {@code :server:runGameTestServer}, or {@code /test runall} in a dev world.
 */
@GameTestHolder(RollMod.MODID)
@PrefixGameTestTemplate(false)
public final class VendorInterfaceGameTests {

  private static final String TEMPLATE = "gametest_empty_5x3x3";

  private static final BlockPos CELL = new BlockPos(0, 1, 1);
  private static final BlockPos CHEST = new BlockPos(1, 1, 1);
  private static final BlockPos CPU = new BlockPos(2, 1, 1);
  private static final BlockPos PROVIDER = new BlockPos(3, 1, 1);
  private static final BlockPos ASSEMBLER = new BlockPos(4, 1, 1);
  private static final BlockPos VENDOR = new BlockPos(1, 1, 2);

  private VendorInterfaceGameTests() {}

  /** A config slot stocks its item from the network, like an interface. */
  @GameTest(template = TEMPLATE, timeoutTicks = 200)
  public static void stocksConfiguredItems(GameTestHelper helper) {
    buildNetwork(helper, new ItemStack(Items.DIAMOND, 64));
    VendorBlockEntity vendor = placeVendor(helper);
    configure(vendor, 0, Items.DIAMOND, 32);

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(vendor.stockOf(new ItemStack(Items.DIAMOND)), 32, "stock");
          helper.assertValueEqual(networkCount(helper, Items.DIAMOND), 32L, "network");
        });
  }

  /** A purchase is paid out of the interface storage, which then restocks. */
  @GameTest(template = TEMPLATE, timeoutTicks = 200)
  public static void restocksAfterSale(GameTestHelper helper) {
    buildNetwork(helper, new ItemStack(Items.DIAMOND, 64));
    VendorBlockEntity vendor = placeVendor(helper);
    vendor.addPosition(new Position(new ItemStack(Items.DIAMOND), 4, 10));
    configure(vendor, 0, Items.DIAMOND, 16);

    boolean[] sold = {false};
    helper.succeedWhen(
        () -> {
          if (!sold[0]) {
            // Wait for the first stocking, then sell one purchase.
            helper.assertValueEqual(vendor.stockOf(new ItemStack(Items.DIAMOND)), 16, "stocked");
            helper.assertValueEqual(vendor.takeStock(new ItemStack(Items.DIAMOND), 4), 4, "sold");
            sold[0] = true;
          }
          helper.assertValueEqual(vendor.stockOf(new ItemStack(Items.DIAMOND)), 16, "restocked");
          helper.assertValueEqual(networkCount(helper, Items.DIAMOND), 44L, "network");
        });
  }

  /** Goods bought from players are not configured, so the interface pushes them into the network. */
  @GameTest(template = TEMPLATE, timeoutTicks = 200)
  public static void pushesBoughtGoodsToNetwork(GameTestHelper helper) {
    buildNetwork(helper, ItemStack.EMPTY);
    VendorBlockEntity vendor = placeVendor(helper);
    vendor.setBuyMode(true);
    vendor.addPosition(new Position(new ItemStack(Items.COBBLESTONE), 1, 1));

    helper.runAfterDelay(
        20,
        () -> {
          helper.assertValueEqual(
              vendor.storeGoods(new ItemStack(Items.COBBLESTONE), 10, false), 10, "stored");
          helper.succeedWhen(
              () -> {
                helper.assertValueEqual(vendor.stockOf(new ItemStack(Items.COBBLESTONE)), 0, "shop");
                helper.assertValueEqual(networkCount(helper, Items.COBBLESTONE), 10L, "network");
              });
        });
  }

  /** With a crafting card, what the network lacks is crafted into the interface storage. */
  @GameTest(template = TEMPLATE, timeoutTicks = 600)
  public static void craftsMissingStockWithCard(GameTestHelper helper) {
    buildNetwork(helper, new ItemStack(Items.OAK_LOG, 16));
    addPlanksPattern(helper);
    VendorBlockEntity vendor = placeVendor(helper);
    vendor.getUpgrades().addItems(AEItems.CRAFTING_CARD.stack());
    configure(vendor, 0, Items.OAK_PLANKS, 32);

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(vendor.stockOf(new ItemStack(Items.OAK_PLANKS)), 32, "planks");
          helper.assertValueEqual(networkCount(helper, Items.OAK_LOG), 8L, "logs");
        });
  }

  /** Without the card nothing is crafted. */
  @GameTest(template = TEMPLATE, timeoutTicks = 300)
  public static void noCraftingWithoutCard(GameTestHelper helper) {
    buildNetwork(helper, new ItemStack(Items.OAK_LOG, 16));
    addPlanksPattern(helper);
    VendorBlockEntity vendor = placeVendor(helper);
    configure(vendor, 0, Items.OAK_PLANKS, 32);

    helper.runAfterDelay(
        200,
        () -> {
          helper.assertValueEqual(vendor.stockOf(new ItemStack(Items.OAK_PLANKS)), 0, "planks");
          helper.assertValueEqual(networkCount(helper, Items.OAK_LOG), 16L, "logs");
          helper.succeed();
        });
  }

  /** Breaking the block drops the interface storage and the card. */
  @GameTest(template = TEMPLATE, timeoutTicks = 100)
  public static void dropsStorageAndCard(GameTestHelper helper) {
    VendorBlockEntity vendor = placeVendor(helper);
    vendor.getUpgrades().addItems(AEItems.CRAFTING_CARD.stack());
    vendor.storeGoods(new ItemStack(Items.GOLD_INGOT), 7, false);
    helper.runAfterDelay(
        5,
        () -> {
          helper.destroyBlock(VENDOR);
          helper.succeedWhen(
              () -> {
                helper.assertItemEntityPresent(AEItems.CRAFTING_CARD.asItem(), VENDOR, 2.0);
                helper.assertItemEntityCountIs(Items.GOLD_INGOT, VENDOR, 2.0, 7);
              });
        });
  }

  // --- setup ---------------------------------------------------------------------------------

  private static void buildNetwork(GameTestHelper helper, ItemStack contents) {
    helper.setBlock(CELL, AEBlocks.CREATIVE_ENERGY_CELL.block());
    helper.setBlock(CHEST, AEBlocks.ME_CHEST.block());
    helper.setBlock(CPU, AEBlocks.CRAFTING_STORAGE_1K.block());
    helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
    helper.setBlock(ASSEMBLER, AEBlocks.MOLECULAR_ASSEMBLER.block());

    ItemStack cell = AEItems.ITEM_CELL_1K.stack();
    if (!contents.isEmpty()) {
      StorageCells.getCellInventory(cell, null)
          .insert(
              AEItemKey.of(contents),
              contents.getCount(),
              Actionable.MODULATE,
              IActionSource.empty());
    }
    MEChestBlockEntity chest = helper.getBlockEntity(CHEST);
    chest.setCell(cell);
  }

  @SuppressWarnings("unchecked")
  private static void addPlanksPattern(GameTestHelper helper) {
    var recipe =
        (RecipeHolder<CraftingRecipe>)
            helper
                .getLevel()
                .getRecipeManager()
                .byKey(ResourceLocation.withDefaultNamespace("oak_planks"))
                .orElseThrow();
    ItemStack[] in = new ItemStack[9];
    Arrays.fill(in, ItemStack.EMPTY);
    in[0] = new ItemStack(Items.OAK_LOG);
    ItemStack pattern =
        PatternDetailsHelper.encodeCraftingPattern(
            recipe, in, new ItemStack(Items.OAK_PLANKS, 4), false, false);
    PatternProviderBlockEntity provider = helper.getBlockEntity(PROVIDER);
    provider.getLogic().getPatternInv().addItems(pattern);
  }

  private static VendorBlockEntity placeVendor(GameTestHelper helper) {
    helper.setBlock(VENDOR, BlockRegistry.VENDOR.get());
    return helper.getBlockEntity(VENDOR);
  }

  private static void configure(VendorBlockEntity vendor, int slot, Item item, long amount) {
    vendor
        .getInterfaceLogic()
        .getConfig()
        .setStack(slot, new GenericStack(AEItemKey.of(item), amount));
  }

  private static long networkCount(GameTestHelper helper, Item item) {
    MEChestBlockEntity chest = helper.getBlockEntity(CHEST);
    var inv = chest.getCellInventory(0);
    return inv == null ? 0 : inv.getAvailableStacks().get(AEItemKey.of(item));
  }
}
