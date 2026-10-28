package com.roll_54.roll_mod.economy.service;

import com.roll_54.roll_mod.RollMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The customer's work slot of each open service screen, server side, so its item goes back to the
 * player however the screen ends.
 *
 * <p>Not a {@code UIEvents.REMOVED} listener: LdLib2 fires that only from its client screen mixins,
 * so on the server it never ran and the item was voided with the screen. Three ends instead: the
 * menu closing, logging out (vanilla never closes the menu then, and saves the player right after
 * {@code PlayerLoggedOutEvent}), and dying (before the inventory drops, so the item drops or is
 * kept with it).
 *
 * <p>One per player: opening another menu closes the current one first, so the old slot is handed
 * back before the new screen registers its own.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ServiceWorkSlots {

  private static final Map<UUID, ItemStackHandler> OPEN = new ConcurrentHashMap<>();

  private ServiceWorkSlots() {}

  /** Remembers the slot of the screen just built for {@code player}, returning any older one first. */
  public static void open(ServerPlayer player, ItemStackHandler work) {
    ItemStackHandler previous = OPEN.put(player.getUUID(), work);
    if (previous != null && previous != work) giveBack(player, previous);
  }

  private static void close(ServerPlayer player) {
    ItemStackHandler work = OPEN.remove(player.getUUID());
    if (work != null) giveBack(player, work);
  }

  private static void giveBack(ServerPlayer player, ItemStackHandler work) {
    ItemStack left = work.extractItem(0, Integer.MAX_VALUE, false);
    if (!left.isEmpty()) player.getInventory().placeItemBackInInventory(left);
  }

  @SubscribeEvent
  public static void onContainerClose(PlayerContainerEvent.Close event) {
    if (event.getEntity() instanceof ServerPlayer player) close(player);
  }

  @SubscribeEvent
  public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) close(player);
  }

  /** Lowest priority: only once the death is certain, so a cancelled death keeps the screen working. */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onDeath(LivingDeathEvent event) {
    if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) close(player);
  }
}
