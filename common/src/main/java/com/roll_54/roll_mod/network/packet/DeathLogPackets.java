package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.deathlog.ClientDeathLogCache;
import com.roll_54.roll_mod.minestar.deathlog.DeathLogService;
import com.roll_54.roll_mod.minestar.deathlog.DeathLogStore;
import com.roll_54.roll_mod.minestar.deathlog.DeathRecord;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The moderation tab's death log. Pulled, not pushed: the client asks for the player list, one
 * player's deaths, or one death's stacks, and the server answers only those who may read it.
 * Every answer is small except {@link Detail}, which is one death and nothing more.
 */
public final class DeathLogPackets {

    private static final int MAX_VICTIMS = 2000;
    private static final int MAX_DEATHS = 500;
    private static final int MAX_STACKS = 256;
    private static final int MAX_RESTORES = 32;
    /** Per player and per kind of request: a click is fine, a flood is not. */
    private static final long MIN_INTERVAL_MILLIS = 100L;
    private static final Map<String, Long> LAST_REQUEST = new ConcurrentHashMap<>();
    /** The unused ids of an INDEX request. */
    private static final UUID NONE = new UUID(0L, 0L);

    private DeathLogPackets() {}

    public enum Kind { INDEX, LIST, DETAIL }

    private static <E extends Enum<E>> E of(E[] values, int ordinal) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        return values[Math.floorMod(ordinal, values.length)];
    }

    /** One death as the list shows it: no stacks. */
    /** {@code reason} is the death message as a component, rendered in the viewer's language. */
    public record Summary(UUID id, long diedAt, String dimension, int x, int y, int z, Component reason,
                          String damageType, String killer, boolean kept, int items,
                          List<DeathRecord.Restore> restores) {

        static Summary of(DeathRecord r, HolderLookup.Provider registries) {
            List<DeathRecord.Restore> restores = r.restores();
            if (restores.size() > MAX_RESTORES) restores = restores.subList(restores.size() - MAX_RESTORES, restores.size());
            return new Summary(r.id(), r.diedAt(), r.dimension(), r.x(), r.y(), r.z(), reason(r, registries),
                    r.damageType(), r.killer(), r.keptInventory(), r.itemCount(), List.copyOf(restores));
        }

        /** The stored component, or the plain text when there is none or it no longer parses. */
        private static Component reason(DeathRecord r, HolderLookup.Provider registries) {
            if (!r.causeJson().isEmpty()) {
                try {
                    Component parsed = Component.Serializer.fromJson(r.causeJson(), registries);
                    if (parsed != null) return parsed;
                } catch (RuntimeException ignored) {
                    // Falls through to the text, which every record has.
                }
            }
            return Component.literal(r.cause());
        }

        void encode(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeLong(diedAt);
            buf.writeUtf(clamp(dimension, 128), 128);
            buf.writeVarInt(x);
            buf.writeVarInt(y);
            buf.writeVarInt(z);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, reason);
            buf.writeUtf(clamp(damageType, 128), 128);
            buf.writeUtf(clamp(killer, 64), 64);
            buf.writeBoolean(kept);
            buf.writeVarInt(items);
            buf.writeVarInt(restores.size());
            for (DeathRecord.Restore r : restores) {
                buf.writeUtf(clamp(r.by(), 64), 64);
                buf.writeUtf(clamp(r.to(), 64), 64);
                buf.writeLong(r.at());
                buf.writeUtf(clamp(r.mode(), 16), 16);
            }
        }

        static Summary decode(RegistryFriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            long diedAt = buf.readLong();
            String dimension = buf.readUtf(128);
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            int z = buf.readVarInt();
            Component reason = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
            String damageType = buf.readUtf(128);
            String killer = buf.readUtf(64);
            boolean kept = buf.readBoolean();
            int items = buf.readVarInt();
            int count = Math.min(MAX_RESTORES, buf.readVarInt());
            List<DeathRecord.Restore> restores = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                restores.add(new DeathRecord.Restore(buf.readUtf(64), buf.readUtf(64), buf.readLong(), buf.readUtf(16)));
            }
            return new Summary(id, diedAt, dimension, x, y, z, reason, damageType, killer, kept, items,
                    List.copyOf(restores));
        }
    }

    /* ------------------------------------------ client → server ------------------------------------------ */

    /** Client → server: send me the player list, one player's deaths, or one death's stacks. */
    public record Request(Kind kind, UUID victim, UUID id) implements CustomPacketPayload {

        public static final Type<Request> TYPE = new Type<>(RollMod.id("deathlog_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.kind().ordinal());
                    buf.writeUUID(p.victim());
                    buf.writeUUID(p.id());
                },
                buf -> new Request(of(Kind.values(), buf.readVarInt()), buf.readUUID(), buf.readUUID()));

        public static Request index() {
            return new Request(Kind.INDEX, NONE, NONE);
        }

        public static Request list(UUID victim) {
            return new Request(Kind.LIST, victim, victim);
        }

        public static Request detail(UUID victim, UUID id) {
            return new Request(Kind.DETAIL, victim, id);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Request payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                if (!ModerationPermissions.canUseDeathlog(player)) return;
                long now = System.currentTimeMillis();
                Long last = LAST_REQUEST.put(player.getUUID() + "/" + payload.kind(), now);
                if (last != null && now - last < MIN_INTERVAL_MILLIS) return;

                switch (payload.kind()) {
                    case INDEX -> PacketDistributor.sendToPlayer(player, new Index(DeathLogStore.victims()));
                    case LIST -> sendList(player, payload.victim());
                    case DETAIL -> {
                        DeathRecord record = DeathLogStore.find(player.server, payload.victim(), payload.id());
                        if (record != null) PacketDistributor.sendToPlayer(player, Detail.of(record));
                    }
                }
            });
        }
    }

    /** Client → server: hand a death over — to me, or back to the player who died. */
    public record Act(UUID victim, UUID id, DeathRecord.Mode mode) implements CustomPacketPayload {

        public static final Type<Act> TYPE = new Type<>(RollMod.id("deathlog_act"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.victim());
                    buf.writeUUID(p.id());
                    buf.writeVarInt(p.mode().ordinal());
                },
                buf -> new Act(buf.readUUID(), buf.readUUID(), of(DeathRecord.Mode.values(), buf.readVarInt())));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Act payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                if (!ModerationPermissions.canUseDeathlog(player)) return;
                DeathRecord record = DeathLogStore.find(player.server, payload.victim(), payload.id());
                if (record == null) {
                    player.sendSystemMessage(Component.translatable("chat.roll_mod.deathlog.missing"));
                    return;
                }
                if (payload.mode() == DeathRecord.Mode.RETRIEVE) {
                    DeathLogService.retrieve(player, record);
                    player.sendSystemMessage(Component.translatable("chat.roll_mod.deathlog.retrieved",
                            record.victimName()));
                } else {
                    DeathLogService.returnToPlayer(player, record);
                    player.sendSystemMessage(Component.translatable("chat.roll_mod.deathlog.returned",
                            record.victimName()));
                }
                // The restore is noted on the record; show it.
                sendList(player, payload.victim());
            });
        }
    }

    private static void sendList(ServerPlayer player, UUID victim) {
        List<DeathRecord> deaths = DeathLogStore.deaths(player.server, victim);
        List<Summary> summaries = new ArrayList<>(deaths.size());
        for (DeathRecord r : deaths) summaries.add(Summary.of(r, player.registryAccess()));
        PacketDistributor.sendToPlayer(player, new Deaths(victim, summaries));
    }

    /* ------------------------------------------ server → client ------------------------------------------ */

    /** Server → client: every player with a death on record. */
    public record Index(List<DeathLogStore.Victim> victims) implements CustomPacketPayload {

        public static final Type<Index> TYPE = new Type<>(RollMod.id("deathlog_index"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Index> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    int count = Math.min(MAX_VICTIMS, p.victims().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) {
                        DeathLogStore.Victim v = p.victims().get(i);
                        buf.writeUUID(v.id());
                        buf.writeUtf(clamp(v.name(), 64), 64);
                        buf.writeVarInt(v.deaths());
                        buf.writeLong(v.lastDeath());
                    }
                },
                buf -> {
                    int count = Math.min(MAX_VICTIMS, buf.readVarInt());
                    List<DeathLogStore.Victim> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        list.add(new DeathLogStore.Victim(buf.readUUID(), buf.readUtf(64), buf.readVarInt(),
                                buf.readLong()));
                    }
                    return new Index(List.copyOf(list));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Index payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientDeathLogCache.VICTIMS = payload.victims();
                ClientDeathLogCache.VERSION++;
            });
        }
    }

    /** Server → client: one player's deaths, newest first. */
    public record Deaths(UUID victim, List<Summary> deaths) implements CustomPacketPayload {

        public static final Type<Deaths> TYPE = new Type<>(RollMod.id("deathlog_deaths"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Deaths> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.victim());
                    int count = Math.min(MAX_DEATHS, p.deaths().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) p.deaths().get(i).encode(buf);
                },
                buf -> {
                    UUID victim = buf.readUUID();
                    int count = Math.min(MAX_DEATHS, buf.readVarInt());
                    List<Summary> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) list.add(Summary.decode(buf));
                    return new Deaths(victim, List.copyOf(list));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Deaths payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientDeathLogCache.DEATHS_OF = payload.victim();
                ClientDeathLogCache.DEATHS = payload.deaths();
                ClientDeathLogCache.VERSION++;
            });
        }
    }

    /** Server → client: one death's stacks. Curios and cyberware as plain stacks; slots do not matter to the eye. */
    public record Detail(UUID id, Map<Integer, ItemStack> inventory, List<ItemStack> curios,
                         List<ItemStack> cyberware) implements CustomPacketPayload {

        public static final Type<Detail> TYPE = new Type<>(RollMod.id("deathlog_detail"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Detail> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.id());
                    int count = Math.min(MAX_STACKS, p.inventory().size());
                    buf.writeVarInt(count);
                    int written = 0;
                    for (Map.Entry<Integer, ItemStack> e : p.inventory().entrySet()) {
                        if (written++ >= count) break;
                        buf.writeVarInt(e.getKey());
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.getValue());
                    }
                    writeStacks(buf, p.curios());
                    writeStacks(buf, p.cyberware());
                },
                buf -> {
                    UUID id = buf.readUUID();
                    int count = Math.min(MAX_STACKS, buf.readVarInt());
                    Map<Integer, ItemStack> inventory = new TreeMap<>();
                    for (int i = 0; i < count; i++) {
                        int slot = buf.readVarInt();
                        inventory.put(slot, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    return new Detail(id, inventory, readStacks(buf), readStacks(buf));
                });

        static Detail of(DeathRecord r) {
            List<ItemStack> curios = new ArrayList<>();
            r.curios().forEach(c -> curios.add(c.stack()));
            return new Detail(r.id(), r.inventory(), curios, r.cyberwareItems());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Detail payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientDeathLogCache.DETAIL_ID = payload.id();
                ClientDeathLogCache.INVENTORY = payload.inventory();
                ClientDeathLogCache.CURIOS = payload.curios();
                ClientDeathLogCache.CYBERWARE = payload.cyberware();
                ClientDeathLogCache.VERSION++;
            });
        }
    }

    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        int count = Math.min(MAX_STACKS, stacks.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stacks.get(i));
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX_STACKS, buf.readVarInt());
        List<ItemStack> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        return List.copyOf(out);
    }

    /** {@code writeUtf} throws past its limit, so an over-long entry is cut, not fatal. */
    private static String clamp(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
