package com.roll_54.roll_mod.items.electricItems;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
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
import java.util.Set;

/**
 * Puts everything an area-mining drill breaks straight into the miner's inventory instead of
 * leaving a heap of item entities on the ground. The trick is MI's steam drill one: while the
 * drill works its area, the drops of every block it breaks are taken out of {@link
 * BlockDropsEvent} and merged into a single list, and once the area is done each merged stack is
 * offered to the player.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DrillDropCollector {

    /**
     * What one mining operation wants done with its drops and experience, and what it got back.
     * The modular drill's Burn module voids the items; the Trash Filter voids only the item
     * types in its own 18-slot list; Auto-Smelt runs each drop through the furnace
     * recipe; and captured experience lands in {@code xpCollected} instead of spilling into the
     * world — for the XP Reactor's buffer or the player, the caller decides.
     */
    public static final class DrillOp {
        public boolean burnDrops;
        /** Trash Filter: drops of these item types are voided. Empty = no filter. */
        public Set<Item> trashFilter = Set.of();
        public boolean smeltDrops;
        public boolean captureXp;
        public int xpCollected;
    }

    /**
     * What the Trash Filter used to throw away.
     *
     * @deprecated The Trash Filter now voids the items set in its own list (the drill's config
     * screen). Kept, with its tag file, for datapacks that still reference it; nothing reads it.
     */
    @Deprecated
    public static final TagKey<Item> DRILL_TRASH =
            TagKey.create(Registries.ITEM, RollMod.id("drill_trash"));

    /** Furnace-recipe lookup with the vanilla cache, shared by every smelting drill op. */
    private static final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> SMELTING =
            RecipeManager.createCheck(RecipeType.SMELTING);

    /** Non-null only while a drill is mining. Server thread only, like the block breaking itself. */
    @Nullable
    private static List<ItemStack> totalDrops = null;
    @Nullable
    private static DrillOp currentOp = null;

    private DrillDropCollector() {
    }

    /**
     * Breaks blocks with drop collection on, then hands the merged stacks to the player. A nested
     * call just runs the mining: the outermost call owns the collection and does the handing out.
     */
    public static void collect(ServerPlayer player, Runnable mining) {
        collect(player, new DrillOp(), mining);
    }

    /** As {@link #collect(ServerPlayer, Runnable)}, with Burn/XP behavior from {@code op}. */
    public static void collect(ServerPlayer player, DrillOp op, Runnable mining) {
        if (totalDrops != null) {
            mining.run();
            return;
        }

        totalDrops = new ArrayList<>();
        currentOp = op;
        List<ItemStack> drops;
        try {
            mining.run();
        } finally {
            drops = totalDrops;
            totalDrops = null;
            currentOp = null;
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

        DrillOp op = currentOp;
        if (op != null && op.captureXp) {
            op.xpCollected += event.getDroppedExperience();
            event.setDroppedExperience(0);
        }

        if (op != null && op.burnDrops) {
            // Burn: the drops go nowhere, not even into the merged list.
            event.getDrops().clear();
            return;
        }

        outer:
        for (ItemEntity entity : event.getDrops()) {
            ItemStack dropped = entity.getItem();
            if (dropped.isEmpty()) {
                continue;
            }
            if (op != null && op.trashFilter.contains(dropped.getItem())) {
                continue;
            }
            if (op != null && op.smeltDrops) {
                dropped = smelted(event.getLevel(), dropped);
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

    /** The drop's furnace result at the same count, or the drop itself when nothing smelts it. */
    private static ItemStack smelted(ServerLevel level, ItemStack dropped) {
        return SMELTING.getRecipeFor(new SingleRecipeInput(dropped), level)
                .map(holder -> {
                    ItemStack result = holder.value().getResultItem(level.registryAccess());
                    return result.copyWithCount(result.getCount() * dropped.getCount());
                })
                .orElse(dropped);
    }

    /**
     * Offers the stack to the player as an item entity first, so that Sophisticated Backpacks and
     * mods like it get their chance to swallow it, and only then puts it in the inventory.
     * Merged stacks can hold more than a max stack; giveItemToPlayer spreads them over the slots
     * and drops at the player's feet whatever does not fit.
     */
    public static void giveToPlayer(ServerPlayer player, ItemStack stack) {
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
