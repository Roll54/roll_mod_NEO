package com.roll_54.roll_mod.minestar.moderation.invsee;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.mojang.authlib.GameProfile;
import com.roll_54.roll_mod.compat.curios.CuriosCompat;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.network.packet.InvSeePackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server side of the moderator's inventory window: who has one open, on whom, and what opening
 * one checks.
 *
 * <p>Online and offline are two separate requests on purpose. The moderation tab offers them as
 * different buttons, and this refuses a request whose idea of the player is out of date. An
 * "offline" open on a player who has since logged in, or an "online" one on a player who has left,
 * is turned down with a message rather than quietly switched to the other mode. Editing a live
 * player and rewriting a saved file are different things, and the moderator should always know
 * which one they are doing.
 */
public final class InvSeeSessions {

    /** The open window per moderator. */
    private static final Map<UUID, InvSeeSession> SESSIONS = new ConcurrentHashMap<>();

    private InvSeeSessions() {}

    /** The holder LdLib2 builds the window from, on either side. Registered in {@code RollMod}. */
    public static PlayerUIMenuType.PlayerUIHolder holder(Player player) {
        InvSeeSession session = player instanceof ServerPlayer ? SESSIONS.get(player.getUUID()) : null;
        return new Holder(session);
    }

    /**
     * What the window was built for. On the server it carries the session, and the window stays
     * open only while that session does. On the client it has none and builds from the layout the
     * server sent ahead ({@link ClientInvSeeCache}).
     */
    public record Holder(@Nullable InvSeeSession session) implements PlayerUIMenuType.PlayerUIHolder {

        @Override
        public ModularUI createUI(Player player) {
            InvSeeLayout layout = session != null ? session.layout()
                    : player.level().isClientSide() ? ClientInvSeeCache.LAYOUT : InvSeeLayout.EMPTY;
            return InvSeeUI.createUI(player, layout, session);
        }

        @Override
        public boolean isStillValid(Player player) {
            if (!(player instanceof ServerPlayer sp)) return true;
            return session != null && SESSIONS.get(sp.getUUID()) == session && session.valid(sp);
        }
    }

    /**
     * Opens the window, or tells the moderator why not.
     *
     * @param id      the player, or {@code null} to look them up by {@code name}
     * @param offline the moderator asked for the offline file, not the live player
     */
    public static void open(ServerPlayer moderator, @Nullable UUID id, String name,
                            InvSeeLayout.Page page, boolean offline) {
        if (!ModerationPermissions.canInvSee(moderator)) return;
        MinecraftServer server = moderator.server;

        ServerPlayer online = id != null ? server.getPlayerList().getPlayer(id)
                : server.getPlayerList().getPlayerByName(name);
        if (online != null && online.getUUID().equals(moderator.getUUID())) {
            deny(moderator, "gui.roll_mod.invsee.self");
            return;
        }
        boolean editable = ModerationPermissions.canInvEdit(moderator);

        if (!offline) {
            if (online == null) {
                deny(moderator, "gui.roll_mod.invsee.not_online", display(id, name));
                return;
            }
            boolean cyberware = ModList.get().isLoaded("createcybernetics");
            if (page == InvSeeLayout.Page.IMPLANTS && !cyberware) {
                deny(moderator, "gui.roll_mod.invsee.no_cyberware");
                return;
            }
            String targetName = online.getGameProfile().getName();
            List<CuriosCompat.CurioRow> curios = page == InvSeeLayout.Page.INVENTORY
                    ? CuriosCompat.rows(online) : List.of();
            InvSeeLayout layout = new InvSeeLayout(online.getUUID(), targetName, false, editable, page,
                    curios, cyberware).clamped();
            start(moderator, InvSeeSession.online(moderator, layout, online));
            return;
        }

        if (online != null) {
            deny(moderator, "gui.roll_mod.invsee.is_online", online.getGameProfile().getName());
            return;
        }
        GameProfile profile = profile(server, id, name);
        if (profile == null) {
            deny(moderator, "gui.roll_mod.invsee.unknown", display(id, name));
            return;
        }
        if (profile.getId().equals(moderator.getUUID())) {
            deny(moderator, "gui.roll_mod.invsee.self");
            return;
        }
        // Another moderator's window on the same file: share its copy, so neither write undoes the other.
        OfflineInventory inventory = shared(profile.getId());
        if (inventory == null) inventory = OfflineInventory.load(server, profile);
        if (inventory == null) {
            deny(moderator, "gui.roll_mod.invsee.no_data", profile.getName());
            return;
        }
        InvSeeLayout layout = new InvSeeLayout(profile.getId(), profile.getName(), true, editable,
                InvSeeLayout.Page.INVENTORY, List.of(), false).clamped();
        start(moderator, InvSeeSession.offline(moderator, layout, inventory));
    }

    private static void start(ServerPlayer moderator, InvSeeSession session) {
        // Stored before the open: the holder reads it back while LdLib2 builds the menu. A window
        // already open on someone else closes as part of this open and simply stops matching.
        SESSIONS.put(moderator.getUUID(), session);
        PacketDistributor.sendToPlayer(moderator, new InvSeePackets.Layout(session.layout()));
        PlayerUIMenuType.openUI(moderator, InvSeeUI.UI_ID);
    }

    /** Called when any menu closes on the server; forgets the session if it was this window. */
    public static void onClosed(ServerPlayer player, AbstractContainerMenu menu) {
        if (menu instanceof ModularUIContainerMenu modular && modular.uiHolder instanceof Holder holder
                && holder.session() != null) {
            SESSIONS.remove(player.getUUID(), holder.session());
        }
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }

    @Nullable
    private static OfflineInventory shared(UUID target) {
        for (InvSeeSession session : SESSIONS.values()) {
            if (session.offline != null && session.layout.target().equals(target)) return session.offline;
        }
        return null;
    }

    @Nullable
    private static GameProfile profile(MinecraftServer server, @Nullable UUID id, String name) {
        if (server.getProfileCache() == null) {
            return id == null ? null : new GameProfile(id, name.isBlank() ? id.toString() : name);
        }
        Optional<GameProfile> cached = id != null ? server.getProfileCache().get(id)
                : server.getProfileCache().get(name);
        if (cached.isPresent()) return cached.get();
        // Banned players are listed by UUID with the name the ban recorded, which is enough to open
        // their file even when the profile cache has forgotten them.
        return id == null ? null : new GameProfile(id, name.isBlank() ? id.toString() : name);
    }

    private static String display(@Nullable UUID id, String name) {
        return !name.isBlank() ? name : id == null ? "?" : id.toString();
    }

    private static void deny(ServerPlayer moderator, String key, Object... args) {
        moderator.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }
}
