package com.roll_54.roll_mod.items.electricItems;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts everything an area-mining drill breaks straight into the miner's inventory instead of
 * leaving a heap of item entities on the ground. The trick is MI's steam drill one: while the
 * drill works its area, the drops of every block it breaks are taken out of {@link
 * BlockDropsEvent} and merged into a single list, and once the area is done each merged stack is
 * offered to the player.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DrillDropCollector {

    /** Non-null only while a drill is mining. Server thread only, like the block breaking itself. */
    @Nullable
    private static List<ItemStack> totalDrops = null;

    private DrillDropCollector() {
    }

    /**
     * Breaks blocks with drop collection on, then hands the merged stacks to the player. A nested
     * call just runs the mining: the outermost call owns the collection and does the handing out.
     */
    public static void collect(ServerPlayer player, Runnable mining) {
        if (totalDrops != null) {
            mining.run();
            return;
        }

        totalDrops = new ArrayList<>();
        List<ItemStack> drops;
        try {
            mining.run();
        } finally {
            drops = totalDrops;
            totalDrops = null;
        }

        for (ItemStack stack : drops) {
            giveToPlayer(player, stack);
        }
    }

    /**
     * Runs last so that loot modifiers and other mods have already had their say about the drops.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        List<ItemStack> collected = totalDrops;
        if (collected == null) {
            return;
        }

        outer:
        for (ItemEntity entity : event.getDrops()) {
            ItemStack dropped = entity.getItem();
            if (dropped.isEmpty()) {
                continue;
            }
            for (ItemStack merged : collected) {
                if (ItemStack.isSameItemSameComponents(merged, dropped)) {
                    merged.grow(dropped.getCount());
                    continue outer;
                }
            }
            collected.add(dropped);
        }
        // Emptied, not canceled: the block still drops its experience the usual way.
        event.getDrops().clear();
    }

    /**
     * Offers the stack to the player as an item entity first, so that Sophisticated Backpacks and
     * mods like it get their chance to swallow it, and only then puts it in the inventory.
     * Merged stacks can hold more than a max stack; giveItemToPlayer spreads them over the slots
     * and drops at the player's feet whatever does not fit.
     */
    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        var itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        // Picked up right away, and backpacks ignore entities that are still on pickup delay.
        itemEntity.setNoPickUpDelay();
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, itemEntity));

        // canPickup is ignored on purpose: a mod that took the stack is expected to have emptied
        // or removed the entity, which is what is checked here.
        if (!itemEntity.isRemoved()) {
            ItemHandlerHelper.giveItemToPlayer(player, itemEntity.getItem());
        }
    }
}
