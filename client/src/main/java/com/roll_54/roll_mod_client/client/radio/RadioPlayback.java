package com.roll_54.roll_mod_client.client.radio;

import com.roll_54.roll_mod.minestar.radio.ClientRadioCache;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Plays what {@link ClientRadioCache} says should be heard: this player's own station when their
 * implant's "hear it" is on, and the stations of broadcasting players nearby.
 *
 * <p><b>One sound per station url.</b> Ad Astra shares one download per url while it connects, so
 * two sounds on the same url would read one stream between them. A station already in the
 * listener's head is therefore not also played from a broadcaster, and several broadcasters on one
 * station are heard from the nearest.
 *
 * <p>A stream that ends or fails to connect is retried, but no sooner than {@link #RETRY_MILLIS}
 * after it was started, so a dead url does not reconnect every tick.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class RadioPlayback {

    private static final long RETRY_MILLIS = 10_000L;
    /** A little past the audible range, so a broadcaster walking in is already playing at the edge. */
    private static final double START_RANGE = ImplantRadioSound.RANGE + 4;

    private static ImplantRadioSound own;
    private static String ownUrl;
    private static long ownStarted;

    private static final Map<String, ImplantRadioSound> NEARBY = new HashMap<>();
    private static final Map<String, Long> NEARBY_STARTED = new HashMap<>();

    private RadioPlayback() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopAll();
            return;
        }
        SoundManager sounds = mc.getSoundManager();
        long now = System.currentTimeMillis();

        // Own station.
        String wanted = ClientRadioCache.INSTALLED && ClientRadioCache.HEAR && !ClientRadioCache.STATION.isEmpty()
                ? ClientRadioCache.STATION : null;
        if (wanted == null || !wanted.equals(ownUrl)) {
            stopOwn(sounds);
            if (wanted != null) startOwn(sounds, wanted, now);
        } else if (!sounds.isActive(own) && now - ownStarted > RETRY_MILLIS) {
            stopOwn(sounds);
            startOwn(sounds, wanted, now);
        }

        // Nearby broadcasters: the nearest one per station, skipping ourselves and our own station.
        Map<String, Entity> nearest = new HashMap<>();
        for (Map.Entry<Integer, String> e : ClientRadioCache.BROADCASTS.entrySet()) {
            String url = e.getValue();
            if (url.equals(wanted)) continue;
            Entity entity = mc.level.getEntity(e.getKey());
            if (entity == null || entity == mc.player || entity.isRemoved()) continue;
            double distance = entity.distanceTo(mc.player);
            if (distance > START_RANGE) continue;
            Entity best = nearest.get(url);
            if (best == null || distance < best.distanceTo(mc.player)) nearest.put(url, entity);
        }

        Iterator<Map.Entry<String, ImplantRadioSound>> it = NEARBY.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ImplantRadioSound> e = it.next();
            Entity target = nearest.get(e.getKey());
            ImplantRadioSound sound = e.getValue();
            boolean dead = !sounds.isActive(sound)
                    && now - NEARBY_STARTED.getOrDefault(e.getKey(), 0L) > RETRY_MILLIS;
            if (target == null || dead) {
                sound.stop();
                sounds.stop(sound);
                it.remove();
                NEARBY_STARTED.remove(e.getKey());
            } else {
                sound.follow(target);
            }
        }
        for (Map.Entry<String, Entity> e : nearest.entrySet()) {
            if (NEARBY.containsKey(e.getKey())) continue;
            ImplantRadioSound sound = ImplantRadioSound.following(e.getKey(), e.getValue());
            NEARBY.put(e.getKey(), sound);
            NEARBY_STARTED.put(e.getKey(), now);
            sounds.play(sound);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stopAll();
        ClientRadioCache.clear();
    }

    private static void startOwn(SoundManager sounds, String url, long now) {
        own = ImplantRadioSound.inHead(url);
        ownUrl = url;
        ownStarted = now;
        sounds.play(own);
    }

    private static void stopOwn(SoundManager sounds) {
        if (own != null) {
            own.stop();
            sounds.stop(own);
        }
        own = null;
        ownUrl = null;
    }

    private static void stopAll() {
        if (own == null && NEARBY.isEmpty()) return;
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        stopOwn(sounds);
        for (ImplantRadioSound sound : NEARBY.values()) {
            sound.stop();
            sounds.stop(sound);
        }
        NEARBY.clear();
        NEARBY_STARTED.clear();
    }
}
