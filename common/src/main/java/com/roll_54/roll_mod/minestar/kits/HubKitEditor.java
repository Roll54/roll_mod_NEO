package com.roll_54.roll_mod.minestar.kits;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * A kit's items edited inside the hub, server side: the moderation tab's kit editor window holds real slots (armour,
 * the 36 inventory slots, offhand) over the hub's own player inventory, bound here to one handler per open hub.
 * Items move as in the old chest editor: what goes in leaves the moderator's inventory and becomes the kit when the
 * window closes (or the hub does); what is taken out is theirs.
 *
 * <p>{@link #handler} is called while the hub's tree is built, on both sides, so both build the same slots. On the
 * server it registers the player's session; {@link #load} fills it with a kit, {@link #save} writes it back and
 * empties it. The slots only take items while a kit is loaded (server) or the window is open (client), so nothing
 * can be shift-clicked into an empty editor. Curio items, which the window does not show, are kept as they were.
 */
public final class HubKitEditor {

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    /** Client: whether the editor window is open, so the client's slots predict what the server allows. */
    public static volatile boolean clientOpen;

    private HubKitEditor() {}

    private static final class Session {
        final ItemStackHandler items = new ItemStackHandler(KitSlots.MAIN_SLOTS);
        String kit;
        final List<ItemStack> keptItems = new ArrayList<>();
        final List<String> keptSlots = new ArrayList<>();
    }

    /** The handler the hub's editor slots bind to; the server's is the player's session, the client's a stand-in. */
    public static IItemHandler handler(Player player) {
        if (!(player instanceof ServerPlayer server)) return new ItemStackHandler(KitSlots.MAIN_SLOTS);
        Session session = new Session();
        Session old = SESSIONS.put(server.getUUID(), session);
        if (old != null) flush(server, old);
        return session.items;
    }

    /** A slot of the editor; {@code max} is 1 for armour. */
    public static Slot slot(Player player, IItemHandler handler, int index, int max) {
        BooleanSupplier editable = player instanceof ServerPlayer server
                ? () -> {
                    Session session = SESSIONS.get(server.getUUID());
                    return session != null && session.items == handler && session.kit != null;
                }
                : () -> clientOpen;
        return new EditSlot(handler, index, max, editable);
    }

    /** Whether the player has a hub open to edit in; without one the old kit window is used. */
    static boolean available(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** Puts {@code kit} in the player's hub editor, saving whatever was in it first. */
    static void load(ServerPlayer player, Kit kit) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        flush(player, session);
        for (int i = 0; i < kit.items().size(); i++) {
            ItemStack stack = kit.items().get(i);
            String key = i < kit.slots().size() ? kit.slots().get(i) : "";
            int index = KitSlots.main(key);
            if (index >= 0 && session.items.getStackInSlot(index).isEmpty()) {
                session.items.setStackInSlot(index, stack.copy());
            } else {
                session.keptItems.add(stack.copy());
                session.keptSlots.add(key);
            }
        }
        session.kit = kit.name();
    }

    /** The window closed: what it holds becomes the kit. */
    public static void save(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) flush(player, session);
    }

    /** Any menu closing: the hub's takes its editor with it, saving first. */
    public static void onClosed(ServerPlayer player, AbstractContainerMenu menu) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        for (Slot slot : menu.slots) {
            if (slot instanceof SlotItemHandler handled && handled.getItemHandler() == session.items) {
                SESSIONS.remove(player.getUUID(), session);
                flush(player, session);
                return;
            }
        }
    }

    public static void forget(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) flush(player, session);
    }

    private static void flush(ServerPlayer player, Session session) {
        if (session.kit == null) return;
        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < session.items.getSlots(); i++) {
            ItemStack stack = session.items.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            items.add(stack.copy());
            slots.add(KitSlots.main(i));
            session.items.setStackInSlot(i, ItemStack.EMPTY);
        }
        items.addAll(session.keptItems);
        slots.addAll(session.keptSlots);
        String kit = session.kit;
        session.kit = null;
        session.keptItems.clear();
        session.keptSlots.clear();
        KitService.saveItems(player, kit, items, slots);
    }

    private static final class EditSlot extends SlotItemHandler {

        private final int max;
        private final BooleanSupplier editable;

        EditSlot(IItemHandler handler, int index, int max, BooleanSupplier editable) {
            super(handler, index, 0, 0);
            this.max = max;
            this.editable = editable;
        }

        @Override
        public boolean mayPickup(Player player) {
            return editable.getAsBoolean() && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return editable.getAsBoolean() && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return Math.min(max, super.getMaxStackSize());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(max, Math.min(stack.getMaxStackSize(), getItemHandler().getSlotLimit(getSlotIndex())));
        }
    }
}
