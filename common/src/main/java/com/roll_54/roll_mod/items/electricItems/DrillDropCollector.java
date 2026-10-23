package com.roll_54.roll_mod.items.electricItems;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Puts everything an area-mining drill breaks straight into the miner's inventory instead of
 * leaving a heap of item entities on the ground. The trick is MI's steam drill one: while the
 * drill works its area, the drops of every block it breaks are taken out of {@link
 * BlockDropsEvent} and merged into a single list, and once the area is done each merged stack is
 * offered to the player. What does not fit goes into a {@link DrillOrbItem} at the player's feet.
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

        // What the inventory has no room for goes into drill orbs, not onto the ground as a heap.
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack stack : drops) {
            ItemStack rest = offerToPlayer(player, stack);
            if (!rest.isEmpty()) overflow.add(rest);
        }
        if (!overflow.isEmpty()) {
            DrillOrbItem.storeNear(player, overflow);
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

        for (ItemEntity entity : event.getDrops()) {
            absorb(collected, op, event.getLevel(), entity.getItem());
        }
        // Emptied, not canceled: the block still drops its experience the usual way.
        event.getDrops().clear();
    }

    /**
     * Catches items that other mods spawn straight into the world while the drill works, bypassing
     * {@link BlockDropsEvent}. Create Cybernetics' orange data shard is one: it doubles ore drops by
     * calling {@code Block.popResource} from its {@code BlockEvent.BreakEvent} handler (and swaps
     * ancient debris for netherite scrap the same way). Those extras get the same Burn, Trash Filter
     * and Auto-Smelt treatment as the regular drops and go to the player or a drill orb with them.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        List<ItemStack> collected = totalDrops;
        if (collected == null || event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof ItemEntity entity) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        DrillOp op = currentOp;
        if (op == null || !op.burnDrops) {
            absorb(collected, op, level, entity.getItem());
        }
        event.setCanceled(true);
    }

    /** Filters, smelts and merges one dropped stack into the operation's collected list. */
    private static void absorb(List<ItemStack> collected, @Nullable DrillOp op, ServerLevel level, ItemStack dropped) {
        if (dropped.isEmpty()) {
            return;
        }
        if (op != null && op.trashFilter.contains(dropped.getItem())) {
            return;
        }
        if (op != null && op.smeltDrops) {
            dropped = smelted(level, dropped);
        }
        for (ItemStack merged : collected) {
            if (ItemStack.isSameItemSameComponents(merged, dropped)) {
                merged.grow(dropped.getCount());
                return;
            }
        }
        collected.add(dropped);
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
        // Item entities must stay saveable: the stack codec rejects counts above 99, and the
        // remainder that does not fit the inventory is dropped as a single entity.
        int chunk = Math.max(1, Math.min(stack.getMaxStackSize(), 99));
        while (!stack.isEmpty()) {
            giveChunkToPlayer(player, stack.split(chunk));
        }
    }

    private static void giveChunkToPlayer(ServerPlayer player, ItemStack stack) {
        ItemStack rest = offeredToMods(player, stack);
        if (!rest.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, rest);
        }
    }

    /**
     * As {@link #giveToPlayer}, but whatever fits neither a backpack nor the inventory is handed
     * back instead of dropped. The stack may be of any size; it is consumed.
     */
    public static ItemStack offerToPlayer(ServerPlayer player, ItemStack stack) {
        int chunk = Math.max(1, Math.min(stack.getMaxStackSize(), 99));
        while (!stack.isEmpty()) {
            int size = Math.min(chunk, stack.getCount());
            ItemStack rest = offerChunkToPlayer(player, stack.split(size));
            if (!rest.isEmpty()) {
                rest.grow(stack.getCount());
                // Nothing of this chunk was taken, so nothing of the same item would be either.
                if (rest.getCount() == size + stack.getCount()) return rest;
                stack = rest;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack offerChunkToPlayer(ServerPlayer player, ItemStack stack) {
        ItemStack rest = offeredToMods(player, stack);
        if (rest.isEmpty()) return ItemStack.EMPTY;

        // What giveItemToPlayer does, minus the dropping of what does not fit.
        int before = rest.getCount();
        rest = ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), rest, false);
        if (rest.getCount() != before) {
            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
        return rest;
    }

    /** Lets backpack mods swallow the stack first; returns what they left, possibly empty. */
    private static ItemStack offeredToMods(ServerPlayer player, ItemStack stack) {
        var itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        // Picked up right away, and backpacks ignore entities that are still on pickup delay.
        itemEntity.setNoPickUpDelay();
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, itemEntity));

        // canPickup is ignored on purpose: a mod that took the stack is expected to have emptied
        // or removed the entity, which is what is checked here.
        return itemEntity.isRemoved() ? ItemStack.EMPTY : itemEntity.getItem();
    }
}
