package com.roll_54.roll_mod.minestar.friends;

import com.roll_54.roll_mod.network.packet.SkinPackets;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client's copy of the skins the server looked up for it — see {@link SkinPackets}. Every
 * {@code PlayerHeadElement} that was not handed a skin reads from here, so a head is never left on
 * the default face just because its player is offline.
 *
 * <p>Asking is batched: the heads drawn in one frame queue their ids, and one request goes out on the
 * next client task. Answers are kept as long as the server keeps them ({@link SkinLookup}); an id
 * asked about but never answered — its lookup was already in flight for someone else — is asked again
 * after {@link #RETRY_MILLIS}. Client only.
 */
public final class ClientSkinCache {

    private static final long HIT_TTL_MILLIS = 60 * 60_000L;
    private static final long MISS_TTL_MILLIS = 5 * 60_000L;
    private static final long RETRY_MILLIS = 10_000L;

    /** A skin, or {@code value} empty for "this player has none". */
    public record Entry(String value, String signature, long at) {
        boolean fresh(long now) {
            return now - at < (value.isEmpty() ? MISS_TTL_MILLIS : HIT_TTL_MILLIS);
        }
    }

    private static final Map<UUID, Entry> SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ASKED = new ConcurrentHashMap<>();
    private static final Set<UUID> QUEUE = new LinkedHashSet<>();

    private ClientSkinCache() {}

    /**
     * The skin known for {@code id}, or null while there is none yet. Asks the server whenever there
     * is no fresh answer and no recent question, so a stale entry is still returned while it refreshes.
     */
    @Nullable
    public static Entry get(UUID id) {
        long now = System.currentTimeMillis();
        Entry entry = SKINS.get(id);
        if (entry == null || !entry.fresh(now)) {
            Long asked = ASKED.get(id);
            if (asked == null || now - asked >= RETRY_MILLIS) {
                ASKED.put(id, now);
                queue(id);
            }
        }
        return entry;
    }

    private static void queue(UUID id) {
        boolean schedule;
        synchronized (QUEUE) {
            schedule = QUEUE.isEmpty();
            QUEUE.add(id);
        }
        if (schedule) net.minecraft.client.Minecraft.getInstance().tell(ClientSkinCache::flush);
    }

    private static void flush() {
        List<UUID> ids;
        synchronized (QUEUE) {
            ids = new ArrayList<>(QUEUE);
            QUEUE.clear();
        }
        if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) return;
        for (int i = 0; i < ids.size(); i += SkinPackets.MAX_IDS) {
            PacketDistributor.sendToServer(new SkinPackets.Request(
                    List.copyOf(ids.subList(i, Math.min(ids.size(), i + SkinPackets.MAX_IDS)))));
        }
    }

    public static void accept(List<SkinPackets.Skin> skins) {
        long now = System.currentTimeMillis();
        for (SkinPackets.Skin skin : skins) {
            SKINS.put(skin.id(), new Entry(skin.value(), skin.signature(), now));
            ASKED.remove(skin.id());
        }
    }
}
