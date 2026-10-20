package com.roll_54.roll_mod.blocks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import com.roll_54.roll_mod.network.packet.HueriStatuePackets;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code <player:UUID>} and {@code <player:UUID:rank>} in the Hueri statue's text: the player's
 * name, and with {@code :rank} their Minestar group before it, in the group's colour.
 *
 * <p>The text is a client asset but names and Minestar groups are only known to the server, so
 * opening a statue is a round trip: the client sends the UUIDs its text names
 * ({@link HueriStatuePackets.Open}), the server looks each one up — Minestar's profile through the
 * friends tab's cache, else the server's own profile cache for the name — and answers with
 * {@link HueriStatuePackets.Players} just before it opens the screen. The packets arrive in order,
 * so the screen is built with every name already there.
 *
 * <p>A lookup that has not answered within {@link #TIMEOUT_SECONDS} is skipped rather than held
 * against the open; that player shows as {@link #UNKNOWN}.
 */
public final class HueriStatuePlayers {

    /** {@code <player:UUID>} or {@code <player:UUID:rank>}. */
    public static final Pattern TAG = Pattern.compile(
            "<player:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(:rank)?>");

    /** How many players one open may ask about. */
    public static final int MAX = 64;
    private static final long TIMEOUT_SECONDS = 3L;
    public static final String UNKNOWN = "???";

    /** Players with an open waiting on lookups; a second click meanwhile is ignored. */
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private HueriStatuePlayers() {}

    /**
     * One looked-up player. {@code group} is empty without a Minestar group; {@code color} is
     * 0xRRGGBB, {@code -1} for none.
     */
    public record Entry(UUID id, String name, String group, int color) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(name, 64);
            buf.writeUtf(group, 64);
            buf.writeInt(color);
        }

        public static Entry decode(FriendlyByteBuf buf) {
            return new Entry(buf.readUUID(), buf.readUtf(64), buf.readUtf(64), buf.readInt());
        }
    }

    /** Every player tag's UUID in {@code text}, in order, at most {@link #MAX}. */
    public static List<UUID> ids(String text) {
        List<UUID> out = new ArrayList<>();
        Matcher m = TAG.matcher(text);
        while (m.find() && out.size() < MAX) {
            UUID id = UUID.fromString(m.group(1));
            if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    /** What one tag becomes, from {@code entry} (null when the server had no answer for it). */
    public static Component display(@Nullable Entry entry, boolean rank) {
        if (entry == null) return Component.literal(UNKNOWN);
        MutableComponent out = Component.empty();
        if (rank && !entry.group().isEmpty()) {
            MutableComponent group = Component.literal(entry.group());
            if (entry.color() >= 0) group = group.withColor(entry.color());
            out.append(group).append(" ");
        }
        return out.append(Component.literal(entry.name()));
    }

    /**
     * Server side of a click on a statue: looks up {@code ids}, sends them, marks the statue read
     * and opens the screen. Ignored unless {@code pos} is a statue's lower half within reach.
     */
    public static void open(ServerPlayer player, BlockPos pos, List<UUID> ids) {
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof HueriStatueBlock)
                || state.getValue(HueriStatueBlock.HALF) != DoubleBlockHalf.LOWER
                || !player.canInteractWithBlock(pos, 4.0D)) return;
        if (!PENDING.add(player.getUUID())) return;

        MinecraftServer server = player.server;
        List<UUID> asked = ids.size() <= MAX ? ids : ids.subList(0, MAX);
        List<CompletableFuture<Entry>> lookups = new ArrayList<>();
        for (UUID id : asked) lookups.add(lookup(server, id));

        UUID viewer = player.getUUID();
        CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new))
                .completeOnTimeout(null, TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((ignored, error) -> server.execute(() -> {
                    PENDING.remove(viewer);
                    ServerPlayer online = server.getPlayerList().getPlayer(viewer);
                    if (online == null) return;
                    List<Entry> entries = new ArrayList<>();
                    for (CompletableFuture<Entry> lookup : lookups) {
                        Entry entry = lookup.getNow(null);
                        if (entry != null) entries.add(entry);
                    }
                    PacketDistributor.sendToPlayer(online, new HueriStatuePackets.Players(entries));
                    finishOpen(online, pos);
                }));
    }

    /** The open itself, once the names are on their way: read, screen, sound. */
    private static void finishOpen(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockState(pos).getBlock() instanceof HueriStatueBlock)) return;
        if (!player.getData(RMMAttachment.HUERI_STATUE_SEEN)) {
            player.setData(RMMAttachment.HUERI_STATUE_SEEN, true);
            PacketDistributor.sendToPlayer(player, new com.roll_54.roll_mod.network.packet.HueriStatueSeenPacket(true));
        }
        BlockUIMenuType.openUI(player, pos);
        player.level().playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * One player: Minestar's profile when it answers, its name and group; failing that the server's
     * profile cache for the name alone; {@code null} when neither knows them. Never completes
     * exceptionally.
     */
    private static CompletableFuture<Entry> lookup(MinecraftServer server, UUID id) {
        String known = knownName(server, id);
        return FriendsService.detail(id)
                .thenApply(detail -> entry(id, detail, known))
                .exceptionally(error -> {
                    // Minestar is optional here — the dev client and single player have none.
                    RollMod.LOGGER.debug("[{}] no Minestar profile for {} on the Hueri statue", RollMod.MODID, id, error);
                    return known == null ? null : new Entry(id, known, "", -1);
                });
    }

    private static Entry entry(UUID id, FriendDetail detail, @Nullable String known) {
        String name = detail.name().isEmpty() ? (known == null ? UNKNOWN : known) : detail.name();
        return new Entry(id, name, detail.groupName(), detail.groupColor());
    }

    @Nullable
    private static String knownName(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();
        var cache = server.getProfileCache();
        return cache == null ? null : cache.get(id).map(profile -> profile.getName()).orElse(null);
    }

    static void forget(UUID player) {
        PENDING.remove(player);
    }
}
