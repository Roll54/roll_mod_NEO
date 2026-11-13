package com.roll_54.roll_mod.minestar.kits;

import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Who may take which kit, and when they may take it again.
 *
 * <p>Every rule lives here rather than in the command or the packet handler, so the hub can ask the
 * same questions before it offers a button — the shape {@code WarpService} set.
 */
public final class KitService {

    private KitService() {}

    /* --------------------------------------------- access ------------------------------------------- */

    /**
     * Whether this player may claim this kit.
     *
     * <p>Without LuckPerms nobody holds any node, which would hide every kit — useless in a
     * single-player world or a dev run — so an operator still gets them all.
     */
    public static boolean canUse(ServerPlayer player, Kit kit) {
        if (!LuckPermsCompat.LOADED) return player.hasPermissions(2);
        return player.hasPermissions(2) || LuckPermsCompat.canUseKit(player, kit.name());
    }

    /** The kits this player may see, in file order. */
    public static List<Kit> visibleTo(ServerPlayer player) {
        List<Kit> visible = new ArrayList<>();
        for (Kit kit : KitStore.all(player.server)) {
            if (canUse(player, kit)) visible.add(kit);
        }
        return visible;
    }

    /* -------------------------------------------- cooldown ------------------------------------------ */

    /**
     * The wait on this kit, in ticks — the kit file's value, for everybody.
     *
     * <p>No rank shortens it, no permission waives it, and an operator is not exempt either. The
     * cooldown is the only thing standing between a kit and an infinite supply of what is in it, so
     * a way around it would be a way around the kit. A rank that should wait less wants a kit of
     * its own, with its own node and its own cooldown.
     */
    public static long cooldownTicks(Kit kit) {
        return kit.cooldownTicks();
    }

    /**
     * Milliseconds before this player may claim this kit again; {@code 0} when it is ready, {@code -1} for a
     * one-time kit they already claimed (as on Eden).
     */
    public static long remainingMillis(ServerPlayer player, Kit kit) {
        long ticks = cooldownTicks(kit);
        if (kit.oneTime()) return KitCooldowns.lastClaimed(player.getUUID(), kit.name()) > 0 ? -1L : 0L;
        if (ticks <= 0) return 0L;

        long last = KitCooldowns.lastClaimed(player.getUUID(), kit.name());
        if (last <= 0) return 0L;

        return Math.max(0L, ticks * 50L - (System.currentTimeMillis() - last));
    }

    /* --------------------------------------------- claim -------------------------------------------- */

    /**
     * Hands the kit over, if the player may have it.
     *
     * <p>What will not fit is dropped at their feet rather than refused: a player who claims a kit
     * with a full inventory means to have it, and losing half of it to a silent refusal is worse
     * than a pile of items on the floor.
     *
     * @return whether anything was given.
     */
    public static boolean claim(ServerPlayer player, String name) {
        Kit kit = KitStore.byName(player.server, name);
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", name);
            return false;
        }
        if (!canUse(player, kit)) {
            refuse(player, "msg.roll_mod.kit.denied", kit.name());
            return false;
        }

        long remaining = remainingMillis(player, kit);
        if (remaining < 0) {
            refuse(player, "msg.roll_mod.kit.claimedOnce", kit.title());
            return false;
        }
        if (remaining > 0) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.cooldown",
                    kit.name(), Durations.format(remaining))
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        for (ItemStack stack : kit.items()) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                player.drop(copy, false);
            }
        }

        KitCooldowns.claimed(player.getUUID(), kit.name(), System.currentTimeMillis());
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.claimed", kit.name()));
        KitViewers.resync(player.server);
        return true;
    }

    /* --------------------------------------------- create ------------------------------------------- */

    /**
     * Captures the creator's inventory as a kit.
     *
     * <p>Main inventory and hotbar only, as FTB Essentials did — armour and the offhand are what the
     * operator is wearing while they build the kit, not part of it.
     */
    public static boolean create(ServerPlayer player, String rawName, long cooldownTicks) {
        String name = Kit.clampName(rawName);
        if (!Kit.validName(name)) {
            refuse(player, "msg.roll_mod.kit.badName", rawName);
            return false;
        }

        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>();
        List<ItemStack> inventory = player.getInventory().items;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) continue;
            items.add(inventory.get(i).copy());
            // Where it sat, so the kit window shows the creator's own arrangement.
            slots.add(KitSlots.main(i));
        }
        if (items.isEmpty()) {
            refuse(player, "msg.roll_mod.kit.empty", name);
            return false;
        }

        // A recreated kit keeps the display name a moderator gave it: that is about the kit's place
        // in the hub, not about what is in it.
        Kit previous = KitStore.byName(player.server, name);
        KitStore.put(player.server, new Kit(name, Math.max(0L, cooldownTicks), "", List.copyOf(items),
                previous == null ? "" : previous.displayName(), slots));
        // A recreated kit is a different kit; carrying the old waits over would be a trap.
        KitCooldowns.forget(name);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.created",
                name, items.size(), cooldownTicks));
        KitViewers.resync(player.server);
        syncNames(player.server);
        return true;
    }

    public static boolean delete(ServerPlayer player, String rawName) {
        String name = Kit.clampName(rawName);
        if (!KitStore.remove(player.server, name)) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        KitCooldowns.forget(name);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.deleted", name));
        KitViewers.resync(player.server);
        syncNames(player.server);
        return true;
    }

    /** Who may give kits their display names: whoever may create and delete them. */
    public static boolean mayRename(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /**
     * Sets the name players see a kit as, from the moderation tab. The id — file name, permission
     * node, what {@code /rollmod kit} takes — stays as it is; blank goes back to showing the id.
     */
    public static boolean rename(ServerPlayer player, String rawName, String displayName) {
        if (!mayRename(player)) return false;
        Kit kit = KitStore.byName(player.server, rawName);
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        Kit renamed = kit.withDisplayName(displayName);
        KitStore.put(player.server, renamed);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.renamed", kit.name(),
                com.roll_54.roll_mod.util.LegacyText.display(renamed.title())));
        KitViewers.resync(player.server);
        syncNames(player.server);
        return true;
    }

    /* ------------------------------------------ moderation tab --------------------------------------- */

    /** Opens the kit's editor window ({@link KitWindows}); closing it saves the items. */
    public static boolean openEditor(ServerPlayer player, String rawName) {
        if (!mayRename(player)) return false;
        Kit kit = KitStore.byName(player.server, rawName);
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        edit(player, kit);
        return true;
    }

    /** In the hub's own editor window when the hub is open (it always is, from the moderation tab), else the kit window. */
    private static void edit(ServerPlayer player, Kit kit) {
        if (HubKitEditor.available(player)) HubKitEditor.load(player, kit);
        else KitWindows.openEditor(player, kit);
    }

    /** Opens a kit's contents, read-only, for a player who may see that kit ({@link #visibleTo}). */
    public static boolean openPreview(ServerPlayer player, String rawName) {
        Kit kit = KitStore.byName(player.server, rawName);
        // A locked kit can be previewed too, as on Eden: its card is shown, so is what it holds.
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        // The Kits tab shows it in a window inside the hub: what the kit holds goes to the client, and no screen
        // is opened (opening one closed and reopened the hub). Main inventory, armour and offhand only.
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < kit.items().size(); i++) {
            int slot = KitSlots.main(kit.slots().get(i));
            if (slot < 0 || kit.items().get(i).isEmpty()) continue;
            slots.add(slot);
            items.add(kit.items().get(i).copy());
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new com.roll_54.roll_mod.network.packet.KitWindowPackets.Contents(kit.name(), kit.title(),
                        kit.description(), kit.cooldownTicks(), slots, items));
        return true;
    }

    /**
     * What the editor held when it closed becomes the kit's items, each with its slot. Everything
     * else — cooldown, permission, display name — and the waits players already have stay as they
     * were: the same kit, restocked.
     */
    static void saveItems(ServerPlayer player, String name, List<ItemStack> items, List<String> slots) {
        if (!mayRename(player)) return;
        Kit kit = KitStore.byName(player.server, name);
        if (kit == null) {
            // Deleted while the editor was open: what was put in is not lost, it becomes the kit again.
            kit = new Kit(name, 0L, "", List.of(), "");
        }
        KitStore.put(player.server, new Kit(kit.name(), kit.cooldownTicks(), kit.permission(), List.copyOf(items),
                kit.displayName(), slots, kit.description()));
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.itemsSaved", kit.name(), items.size()));
        KitViewers.resync(player.server);
        syncNames(player.server);
    }

    /** A new, empty kit from the moderation tab; its items go in through the editor. */
    public static boolean createEmpty(ServerPlayer player, String rawName, long cooldownTicks) {
        if (!mayRename(player)) return false;
        String name = Kit.clampName(rawName);
        if (!Kit.validName(name)) {
            refuse(player, "msg.roll_mod.kit.badName", rawName);
            return false;
        }
        if (KitStore.byName(player.server, name) != null) {
            refuse(player, "msg.roll_mod.kit.exists", name);
            return false;
        }
        KitStore.put(player.server, new Kit(name, Math.max(0L, cooldownTicks), "", List.of(), ""));
        KitCooldowns.forget(name);
        KitViewers.resync(player.server);
        syncNames(player.server);
        edit(player, KitStore.byName(player.server, name));
        return true;
    }

    /** Changes how long players wait between claims. Waits already running are measured against the new value. */
    public static boolean setCooldown(ServerPlayer player, String rawName, long cooldownTicks) {
        if (!mayRename(player)) return false;
        Kit kit = KitStore.byName(player.server, rawName);
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        // Below zero is a one-time kit (Kit.ONCE), as on Eden.
        long ticks = cooldownTicks < 0 ? Kit.ONCE : cooldownTicks;
        KitStore.put(player.server, kit.withCooldown(ticks));
        player.sendSystemMessage(ticks < 0
                ? Component.translatable("msg.roll_mod.kit.cooldownOnce", kit.name())
                : Component.translatable("msg.roll_mod.kit.cooldownSet", kit.name(), Durations.format(ticks * 50L)));
        KitViewers.resync(player.server);
        syncNames(player.server);
        return true;
    }

    /** A description players see on the kit's card ({@code &} colour codes allowed); blank clears it. */
    public static boolean setDescription(ServerPlayer player, String rawName, String description) {
        if (!mayRename(player)) return false;
        Kit kit = KitStore.byName(player.server, rawName);
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        KitStore.put(player.server, kit.withDescription(description));
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.descriptionSet", kit.name()));
        KitViewers.resync(player.server);
        syncNames(player.server);
        return true;
    }

    /** {@link #delete} from the moderation tab, which — unlike the command — has to check the player itself. */
    public static boolean deleteFromTab(ServerPlayer player, String rawName) {
        return mayRename(player) && delete(player, rawName);
    }

    /** The full kit list with display names, to everyone online who may rename kits. */
    public static void syncNames(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) syncNamesTo(player);
    }

    public static void syncNamesTo(ServerPlayer player) {
        if (!mayRename(player)) return;
        List<ClientKitNamesCache.Entry> entries = new ArrayList<>();
        for (Kit kit : KitStore.all(player.server)) {
            entries.add(new ClientKitNamesCache.Entry(kit.name(),
                    kit.displayName() == null ? "" : kit.displayName(), kit.cooldownTicks(), kit.items().size(),
                    kit.description()));
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new com.roll_54.roll_mod.network.packet.KitNamePackets.Sync(entries));
    }

    private static void refuse(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }
}
