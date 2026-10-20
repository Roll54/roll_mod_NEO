package com.roll_54.roll_mod.minestar.moderation.invsee;

import com.roll_54.roll_mod.compat.createcybernetics.CyberwareSlotHandler;
import com.roll_54.roll_mod.compat.curios.CuriosCompat;
import com.roll_54.roll_mod.compat.curios.CuriosCompat.CurioRow;
import com.roll_54.roll_mod.minestar.moderation.ModerationLog;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * One moderator's open inventory window, server side: whose items it shows, and the handlers its
 * slots are bound to. Built by {@link InvSeeSessions#open}. Each window gets a session of its own,
 * but every session on the same online player edits the same live objects, and every session on
 * the same offline player shares one {@link OfflineInventory}. So two moderators never overwrite
 * each other.
 */
public final class InvSeeSession {

    final UUID moderator;
    final String moderatorName;
    final InvSeeLayout layout;
    /** The live player, for an online session; {@code null} offline. */
    @Nullable final ServerPlayer target;
    /** The saved inventory, for an offline session; {@code null} online. */
    @Nullable final OfflineInventory offline;

    final IItemHandlerModifiable inventory;
    final List<IItemHandlerModifiable> curios;
    /** The implant slots, when the page is implants; {@code null} otherwise. */
    @Nullable final IItemHandlerModifiable cyberware;
    final IntSupplier humanity;

    private InvSeeSession(ServerPlayer moderator, InvSeeLayout layout, @Nullable ServerPlayer target,
                          @Nullable OfflineInventory offline) {
        this.moderator = moderator.getUUID();
        this.moderatorName = moderator.getGameProfile().getName();
        this.layout = layout;
        this.target = target;
        this.offline = offline;

        BooleanSupplier writable = () -> layout.editable() && stillThere()
                && moderatorMayEdit(moderator);
        InvSeeEdit log = this::log;

        if (offline != null) {
            this.inventory = new EditGuard(new PlayerInventoryHandler(offline.inventory), writable,
                    InvSeeSession::slotName, log, () -> {
                        if (!offline.save()) moderator.sendSystemMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "gui.roll_mod.invsee.save_failed", layout.name()));
                    });
        } else {
            this.inventory = new EditGuard(new PlayerInventoryHandler(target.getInventory()), writable,
                    InvSeeSession::slotName, log, () -> {});
        }

        List<IItemHandlerModifiable> rows = new ArrayList<>();
        for (CurioRow row : layout.curios()) {
            IItemHandlerModifiable live = target == null ? null : CuriosCompat.handler(target, row);
            // A row the player lost between the layout and here still needs its slots to exist.
            IItemHandlerModifiable base = live != null ? live : new ItemStackHandler(row.size()) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return false;
                }
            };
            String prefix = "curio " + row.id() + (row.cosmetic() ? " (cosmetic)" : "") + " #";
            rows.add(new EditGuard(base, live != null ? writable : () -> false, i -> prefix + i, log,
                    () -> {}));
        }
        this.curios = List.copyOf(rows);

        if (layout.page() == InvSeeLayout.Page.IMPLANTS && layout.cyberware() && target != null) {
            CyberwareSlotHandler handler = new CyberwareSlotHandler(target, writable,
                    (where, before, after) -> log("implant " + where, before, after));
            this.cyberware = handler;
            this.humanity = handler::humanity;
        } else {
            this.cyberware = null;
            this.humanity = () -> 0;
        }
    }

    static InvSeeSession online(ServerPlayer moderator, InvSeeLayout layout, ServerPlayer target) {
        return new InvSeeSession(moderator, layout, target, null);
    }

    static InvSeeSession offline(ServerPlayer moderator, InvSeeLayout layout, OfflineInventory inventory) {
        return new InvSeeSession(moderator, layout, null, inventory);
    }

    public InvSeeLayout layout() {
        return layout;
    }

    /**
     * Whether the player is still where this session found them. An online target has to be the
     * same live entity: a respawn or a relog is a new one, and edits to the old one would be lost.
     * An offline target has to still be offline, or the file would be written under a live player
     * who is about to save over it.
     */
    boolean stillThere() {
        if (target != null) {
            return !target.hasDisconnected()
                    && target.server.getPlayerList().getPlayer(target.getUUID()) == target;
        }
        return offline != null && !offline.ownerOnline();
    }

    /** Whether the window should stay open for this viewer. */
    boolean valid(ServerPlayer viewer) {
        return viewer.getUUID().equals(moderator) && ModerationPermissions.canInvSee(viewer) && stillThere();
    }

    private static boolean moderatorMayEdit(ServerPlayer moderator) {
        return !moderator.hasDisconnected() && ModerationPermissions.canInvEdit(moderator);
    }

    private void log(String where, ItemStack before, ItemStack after) {
        ModerationLog.append(moderatorName, layout.offline() ? "edited the saved inventory of" : "edited the inventory of",
                layout.name(), where + ": " + describe(before) + " -> " + describe(after));
    }

    static String slotName(int slot) {
        EquipmentSlot armour = PlayerInventoryHandler.armour(slot);
        if (armour != null) return "armour " + armour.getName();
        if (slot == 40) return "offhand";
        if (slot < 9) return "hotbar #" + slot;
        return "inventory #" + slot;
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        String text = stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!stack.getComponentsPatch().isEmpty()) {
            // Enchantments, names and the like, so the log can tell two diamond swords apart; cut
            // short so a shulker box full of books does not write a page per line.
            String patch = stack.getComponentsPatch().toString();
            text += " " + (patch.length() > 300 ? patch.substring(0, 300) + "…" : patch);
        }
        return text;
    }
}
