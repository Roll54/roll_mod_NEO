package com.roll_54.roll_mod.minestar.radio;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.roll_54.roll_mod.minestar.radio.ClientRadioCache.Station;
import com.roll_54.roll_mod.network.packet.RadioPackets;
import com.roll_54.roll_mod.registry.ItemRegistry;
import earth.terrarium.adastra.common.utils.radio.StationInfo;
import earth.terrarium.adastra.common.utils.radio.StationLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who has a radio implant, and what everyone should hear.
 *
 * <p>Checked once a second rather than on install: Create Cybernetics has no install event to hook,
 * and a second's delay before the tab appears is not something anyone notices.
 *
 * <p>Two things go out, each only when it changes: to each owner, their own state ({@link
 * RadioPackets.Sync}, which also drives the tab); and to everyone, the map of who is broadcasting
 * what ({@link RadioPackets.Broadcasts}). Every client plays the broadcasts of the players it can
 * see, so nothing about range or line of sight is the server's business.
 */
public final class RadioService {

    private static final int INTERVAL_TICKS = 20;
    private static final String CYBERNETICS = "createcybernetics";

    private static final Map<UUID, RadioPackets.Sync> SENT_OWN = new HashMap<>();
    private static final Set<UUID> INFORMED = new HashSet<>();
    private static Map<Integer, String> broadcasts = Map.of();
    private static int ticks;
    private static boolean dirty;

    private RadioService() {}

    /** Re-check on the next tick rather than within a second: a setting was just changed. */
    public static void markDirty() {
        dirty = true;
    }

    /** A player left: whatever they were last sent no longer is on their side. */
    public static void forget(UUID player) {
        SENT_OWN.remove(player);
        INFORMED.remove(player);
        dirty = true;
    }

    public static boolean hasImplant(ServerPlayer player) {
        if (!ModList.get().isLoaded(CYBERNETICS)) return false;
        return player.getData(ModAttachments.CYBERWARE)
                .isInstalled(ItemRegistry.BRAINUPGRADE_RADIO.get(), CyberwareSlot.BRAIN);
    }

    /** Ad Astra's stations, as the tab lists them. */
    public static List<Station> stations() {
        List<Station> out = new ArrayList<>();
        for (StationInfo info : StationLoader.stations()) out.add(new Station(info.url(), info.title(), info.name()));
        return out;
    }

    public static void tick(MinecraftServer server) {
        if (!dirty && ++ticks < INTERVAL_TICKS) return;
        ticks = 0;
        dirty = false;

        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        Set<UUID> present = new HashSet<>();
        Map<Integer, String> now = new HashMap<>();
        List<Station> stations = null;

        for (ServerPlayer player : online) {
            present.add(player.getUUID());
            boolean installed = hasImplant(player);
            RadioSettings.Setting setting = RadioSettings.get(player.getUUID());
            String station = StationLoader.hasStation(setting.station()) ? setting.station() : "";

            if (installed && setting.broadcast() && !station.isEmpty()) now.put(player.getId(), station);

            // Stations only travel with the first sync and when the implant comes or goes; the list is
            // fixed for the life of the server.
            RadioPackets.Sync last = SENT_OWN.get(player.getUUID());
            boolean needStations = last == null || last.installed() != installed;
            if (needStations && installed && stations == null) stations = stations();
            RadioPackets.Sync sync = new RadioPackets.Sync(installed, station, setting.hear(), setting.broadcast(),
                    !needStations ? null : installed ? stations : List.of());
            if (last == null || !last.sameState(sync)) {
                SENT_OWN.put(player.getUUID(), sync);
                PacketDistributor.sendToPlayer(player, sync);
            }
        }
        SENT_OWN.keySet().retainAll(present);
        INFORMED.retainAll(present);

        boolean changed = !now.equals(broadcasts);
        broadcasts = Map.copyOf(now);
        RadioPackets.Broadcasts packet = new RadioPackets.Broadcasts(broadcasts);
        for (ServerPlayer player : online) {
            // Everyone on a change; otherwise only whoever has not had the map yet (just joined).
            boolean fresh = INFORMED.add(player.getUUID());
            if (changed || fresh) PacketDistributor.sendToPlayer(player, packet);
        }
    }
}
