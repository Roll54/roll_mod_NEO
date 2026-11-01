package com.roll_54.roll_mod.minestar.kits;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.roll_54.roll_mod.compat.curios.CuriosCompat;
import com.roll_54.roll_mod.compat.curios.CuriosCompat.CurioRow;
import com.roll_54.roll_mod.network.packet.KitWindowPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The kit window, server side: a moderator's editor ({@link #openEditor}) or a player's read-only
 * preview ({@link #openPreview}) of one kit, laid out like the player's own inventory screen — armour,
 * offhand, the 36 inventory slots, curio rows ({@link KitWindowUI}). Each item shows in the slot the
 * kit's {@code slots} put it in ({@link KitSlots}); that is only for showing it, a claim still adds
 * everything to the inventory.
 *
 * <p>The editor's slots are a copy of the kit, not the kit itself: closing the window saves what is
 * in it ({@link KitService#saveItems}). The structure is the moderation inventory window's
 * ({@code InvSeeSessions}): a session per open window, stored before the open so the holder can read
 * it while LdLib2 builds the menu, and the layout sent ahead for the client.
 */
public final class KitWindows {

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private KitWindows() {}

    /** One open kit window: what it shows and the handlers its slots are bound to. */
    static final class Session {
        final KitWindowLayout layout;
        final ItemStackHandler main = new ItemStackHandler(KitSlots.MAIN_SLOTS);
        final List<ItemStackHandler> curios = new ArrayList<>();

        private Session(KitWindowLayout layout, Kit kit) {
            this.layout = layout;
            layout.curios().forEach(row -> curios.add(new ItemStackHandler(row.size())));
            for (int i = 0; i < kit.items().size(); i++) {
                ItemStack stack = kit.items().get(i).copy();
                String key = kit.slots().get(i);
                int index = KitSlots.main(key);
                if (index >= 0) {
                    main.setStackInSlot(index, stack);
                    continue;
                }
                KitSlots.Curio curio = KitSlots.curio(key);
                if (curio == null) continue;
                for (int r = 0; r < layout.curios().size(); r++) {
                    if (layout.curios().get(r).id().equals(curio.type()) && curio.index() < curios.get(r).getSlots()) {
                        curios.get(r).setStackInSlot(curio.index(), stack);
                    }
                }
            }
        }

        /** What the window holds now, as a kit's items and their slots. */
        void collect(List<ItemStack> items, List<String> slots) {
            for (int i = 0; i < main.getSlots(); i++) {
                ItemStack stack = main.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                items.add(stack.copy());
                slots.add(KitSlots.main(i));
            }
            for (int r = 0; r < curios.size(); r++) {
                ItemStackHandler row = curios.get(r);
                for (int i = 0; i < row.getSlots(); i++) {
                    ItemStack stack = row.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    items.add(stack.copy());
                    slots.add(KitSlots.curio(layout.curios().get(r).id(), i));
                }
            }
        }
    }

    /** The holder LdLib2 builds the window from, on either side. Registered in {@code RollMod}. */
    public static PlayerUIMenuType.PlayerUIHolder holder(Player player) {
        Session session = player instanceof ServerPlayer ? SESSIONS.get(player.getUUID()) : null;
        return new Holder(session);
    }

    /**
     * What the window was built for. On the server it carries the session; on the client it has none
     * and builds from the layout the server sent ahead.
     */
    public record Holder(@Nullable Session session) implements PlayerUIMenuType.PlayerUIHolder {

        @Override
        public ModularUI createUI(Player player) {
            KitWindowLayout layout = session != null ? session.layout
                    : player.level().isClientSide() ? ClientKitWindowCache.LAYOUT : KitWindowLayout.EMPTY;
            return KitWindowUI.createUI(player, layout, session);
        }

        @Override
        public boolean isStillValid(Player player) {
            if (!(player instanceof ServerPlayer sp)) return true;
            return session != null && SESSIONS.get(sp.getUUID()) == session;
        }
    }

    /** The editor, for a moderator the caller has checked ({@link KitService#mayRename}). */
    static void openEditor(ServerPlayer moderator, Kit kit) {
        open(moderator, kit, true);
    }

    /** The read-only preview, for a player the caller has checked may see the kit. */
    static void openPreview(ServerPlayer player, Kit kit) {
        open(player, kit, false);
    }

    private static void open(ServerPlayer player, Kit kit, boolean editable) {
        KitWindowLayout layout = new KitWindowLayout(kit.name(), kit.title(), editable, curioRows(player, kit)).clamped();
        Session session = new Session(layout, kit);
        // Stored before the open: the holder reads it back while LdLib2 builds the menu. A window
        // already open closes as part of this open (saving, if it was an editor) and stops matching.
        SESSIONS.put(player.getUUID(), session);
        PacketDistributor.sendToPlayer(player, new KitWindowPackets.Layout(layout));
        PlayerUIMenuType.openUI(player, KitWindowUI.UI_ID);
    }

    /**
     * The curio rows to show: the viewer's own slot types (cosmetic rows left out — a kit's curio is
     * worn, not shown off), then any type the kit uses that the viewer lacks, each wide enough for
     * the kit's items in it.
     */
    private static List<CurioRow> curioRows(ServerPlayer player, Kit kit) {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        for (CurioRow row : CuriosCompat.rows(player)) {
            if (!row.cosmetic()) sizes.merge(row.id(), row.size(), Math::max);
        }
        for (String key : kit.slots()) {
            KitSlots.Curio curio = KitSlots.curio(key);
            if (curio != null) sizes.merge(curio.type(), curio.index() + 1, Math::max);
        }
        List<CurioRow> rows = new ArrayList<>();
        sizes.forEach((id, size) -> rows.add(new CurioRow(id, size, false)));
        return rows;
    }

    /** Called when any menu closes on the server: an editor saves, and the window is forgotten. */
    public static void onClosed(ServerPlayer player, AbstractContainerMenu menu) {
        if (!(menu instanceof ModularUIContainerMenu modular) || !(modular.uiHolder instanceof Holder holder)
                || holder.session() == null) {
            return;
        }
        Session session = holder.session();
        SESSIONS.remove(player.getUUID(), session);
        if (!session.layout.editable()) return;
        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>();
        session.collect(items, slots);
        KitService.saveItems(player, session.layout.kit(), items, slots);
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }
}
