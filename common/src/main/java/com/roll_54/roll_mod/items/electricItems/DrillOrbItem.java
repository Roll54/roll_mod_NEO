package com.roll_54.roll_mod.items.electricItems;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a drill puts what the miner's inventory has no room for: one orb entity holding up to
 * {@link #MAX_ITEMS} items, instead of a heap of item entities that every player's movement has
 * to track. Overflow tops up orbs already lying next to the miner before a new one is dropped.
 *
 * <p>Walking over an orb moves into the inventory whatever fits, and the rest stays in the orb;
 * the orb itself is never picked up. One that ends up in an inventory anyway (a magnet, a
 * hopper) empties on right-click. Orbs ignore fire, lava and explosions, and last
 * {@link #LIFESPAN_TICKS} from the last time a drill filled them.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public class DrillOrbItem extends Item {

    public static final int MAX_ITEMS = 10_000;
    /** 20 minutes; it is a single entity, so it can afford to wait for its owner far longer than loose items. */
    public static final int LIFESPAN_TICKS = 20 * 60 * 20;
    /** How close to the miner an orb has to be for overflow to go into it rather than a new one. */
    private static final double MERGE_RADIUS = 4.0;
    /** A player standing on an orb with a full inventory touches it every tick; trying every tick is wasted work. */
    private static final int UNPACK_INTERVAL = 10;
    private static final int TOOLTIP_LINES = 6;

    public DrillOrbItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant()
                .component(ComponentsRegistry.DRILL_ORB_CONTENTS.get(), DrillOrbContents.EMPTY));
    }

    public static DrillOrbContents contents(ItemStack orb) {
        return orb.getOrDefault(ComponentsRegistry.DRILL_ORB_CONTENTS.get(), DrillOrbContents.EMPTY);
    }

    /* ------------------------------------------ filling ------------------------------------------ */

    /** Puts {@code overflow} into orbs at the miner's feet: the nearby ones first, new ones for the rest. */
    public static void storeNear(ServerPlayer player, List<ItemStack> overflow) {
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack stack : overflow) {
            if (!stack.isEmpty() && !(stack.getItem() instanceof DrillOrbItem)) rest.add(stack.copy());
        }
        if (rest.isEmpty()) return;

        ServerLevel level = player.serverLevel();
        List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(MERGE_RADIUS),
                e -> e.isAlive() && e.getItem().getItem() instanceof DrillOrbItem);
        for (ItemEntity entity : nearby) {
            ItemStack orb = entity.getItem().copy();
            if (fill(orb, rest)) {
                entity.setItem(orb);
                entity.lifespan = entity.getAge() + LIFESPAN_TICKS;
            }
            if (rest.isEmpty()) return;
        }

        while (!rest.isEmpty()) {
            ItemStack orb = new ItemStack(ItemRegistry.DRILL_ORB.get());
            fill(orb, rest);
            ItemEntity entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), orb, 0, 0, 0);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    /** Moves from {@code rest} into the orb as much as it has room for; {@code true} if anything moved. */
    private static boolean fill(ItemStack orb, List<ItemStack> rest) {
        DrillOrbContents contents = contents(orb);
        int space = MAX_ITEMS - contents.totalCount();
        if (space <= 0) return false;

        List<ItemStack> held = contents.toStacks();
        var it = rest.iterator();
        while (it.hasNext() && space > 0) {
            ItemStack stack = it.next();
            int moved = Math.min(space, stack.getCount());
            held.add(stack.split(moved));
            space -= moved;
            if (stack.isEmpty()) it.remove();
        }
        orb.set(ComponentsRegistry.DRILL_ORB_CONTENTS.get(), DrillOrbContents.of(held));
        return true;
    }

    /* ------------------------------------------ emptying ----------------------------------------- */

    /**
     * Offers everything in the orb to the player and returns the orb with what was left, or
     * {@link ItemStack#EMPTY} once nothing is.
     */
    private static ItemStack unpackInto(ServerPlayer player, ItemStack orb) {
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : contents(orb).toStacks()) {
            ItemStack rest = DrillDropCollector.offerToPlayer(player, stack);
            if (!rest.isEmpty()) left.add(rest);
        }
        if (left.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = orb.copy();
        result.set(ComponentsRegistry.DRILL_ORB_CONTENTS.get(), DrillOrbContents.of(left));
        return result;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity entity = event.getItemEntity();
        ItemStack orb = entity.getItem();
        if (!(orb.getItem() instanceof DrillOrbItem)) return;

        // The orb is a container on the ground, never an inventory item of its own.
        event.setCanPickup(TriState.FALSE);
        if (!(event.getPlayer() instanceof ServerPlayer player) || entity.hasPickUpDelay()) return;
        if (entity.tickCount % UNPACK_INTERVAL != 0) return;

        int before = contents(orb).totalCount();
        ItemStack after = unpackInto(player, orb);
        if (after.isEmpty()) {
            player.take(entity, 1);
            entity.discard();
        } else if (contents(after).totalCount() != before) {
            entity.setItem(after);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack orb = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            // Off the hand first, so the orb's own slot is free for its contents.
            player.setItemInHand(hand, ItemStack.EMPTY);
            ItemStack after = unpackInto(serverPlayer, orb);
            if (!after.isEmpty()) {
                if (player.getItemInHand(hand).isEmpty()) {
                    player.setItemInHand(hand, after);
                } else {
                    storeNear(serverPlayer, contents(after).toStacks());
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /* ------------------------------------------- misc -------------------------------------------- */

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return LIFESPAN_TICKS;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DrillOrbContents contents = contents(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.drill_orb.count",
                        String.format("%,d", contents.totalCount()), String.format("%,d", MAX_ITEMS))
                .withStyle(ChatFormatting.GRAY));

        List<DrillOrbContents.Entry> entries = new ArrayList<>(contents.entries());
        entries.sort((a, b) -> Integer.compare(b.count(), a.count()));
        for (int i = 0; i < Math.min(TOOLTIP_LINES, entries.size()); i++) {
            DrillOrbContents.Entry e = entries.get(i);
            tooltip.add(Component.literal(String.format("%,d", e.count()) + "x ")
                    .append(e.prototype().getHoverName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (entries.size() > TOOLTIP_LINES) {
            tooltip.add(Component.translatable("tooltip.roll_mod.drill_orb.more", entries.size() - TOOLTIP_LINES)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.roll_mod.drill_orb.desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}
