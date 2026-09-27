package com.roll_54.roll_mod.compat.argicraft;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.CropSticksItem;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.SpecialPlantable;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A hoe never plants. Tilling returns PASS on farmland, so vanilla offers the same click to the
 * off-hand — and whatever sits there (crop sticks, AgriCraft seeds, vanilla seeds that AgriCraft's
 * {@code VanillaSeedConversion} turns into sticks, saplings) gets planted on the freshly tilled
 * field. This refuses that off-hand click outright while a hoe is in the main hand.
 *
 * <p>Runs on both sides, so the client does not predict a placement either, and at HIGHEST so it
 * lands before AgriCraft's own {@code RightClickBlock} listener, which skips canceled events.
 * Planting from the main hand, and off-hand items that plant nothing, are untouched.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HoeOffhandPlantGuard {
    private HoeOffhandPlantGuard() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.OFF_HAND) return;
        if (!event.getEntity().getMainHandItem().canPerformAction(ItemAbilities.HOE_TILL)) return;
        if (!isPlanting(event.getItemStack())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static boolean isPlanting(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof CropSticksItem || item instanceof AgriSeedItem || item instanceof SeedBagItem) return true;
        if (item instanceof SpecialPlantable) return true;
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof BushBlock) return true;
        // Anything AgriCraft would convert into a planted seed on a click.
        return AgriApi.getGenomeAdapter(stack).isPresent();
    }
}
