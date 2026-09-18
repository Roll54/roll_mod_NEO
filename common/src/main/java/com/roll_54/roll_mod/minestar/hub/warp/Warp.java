package com.roll_54.roll_mod.minestar.hub.warp;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One player-created teleport destination.
 *
 * <p>The position is stored as the creator stood when they made it — warps are always placed at the
 * creator's feet, never typed in, so a warp always points somewhere its owner could actually reach.
 *
 * @param price         what a visitor pays the owner to arrive; {@code 0} is free
 * @param approval      where moderation stands on it — see {@link WarpApproval}
 * @param moderator     who last set {@link #approval}, so a blocked owner knows who to appeal to
 * @param reports       who has reported it; a set, so each player counts once
 * @param visitors      who has arrived during {@link #visitorsDay}; a set, so re-warping in a loop
 *                      cannot inflate the popularity ranking
 * @param visitorsDay   the period {@link #visitors} was collected in — see {@link #visitorsToday()}
 * @param visitsTotal   completed arrivals all-time, counting repeat trips but never the owner's
 */
public record Warp(UUID id, UUID owner, String ownerName, String name, String description,
                   ResourceLocation dimension, double x, double y, double z,
                   float yaw, float pitch, long price, long created,
                   WarpApproval approval, String moderator, Set<UUID> reports,
                   Set<UUID> visitors, long visitorsDay, long visitsTotal) {

    /** The longest name and description the create form will accept, and the server enforces. */
    public static final int MAX_NAME = 24;
    public static final int MAX_DESCRIPTION = 120;

    /** A warp at the player's current position, awaiting review. */
    public static Warp at(ServerPlayer player, String name, String description, long price) {
        return new Warp(UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(),
                trim(name, MAX_NAME), trim(description, MAX_DESCRIPTION),
                player.level().dimension().location(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                Math.max(0, price), System.currentTimeMillis(),
                WarpApproval.NOT_YET, "", Set.of(),
                Set.of(), DailyTaskManager.currentPeriodDay(), 0);
    }

    public Warp withApproval(WarpApproval approval, String moderator) {
        return new Warp(id, owner, ownerName, name, description, dimension, x, y, z, yaw, pitch,
                price, created, approval, moderator, reports, visitors, visitorsDay, visitsTotal);
    }

    public Warp withReports(Set<UUID> reports) {
        return new Warp(id, owner, ownerName, name, description, dimension, x, y, z, yaw, pitch,
                price, created, approval, moderator, Set.copyOf(reports),
                visitors, visitorsDay, visitsTotal);
    }

    /* ------------------------------------------- popularity ------------------------------------------ */

    /**
     * How many different players have arrived here today.
     *
     * <p>Reads {@code 0} once {@link #visitorsDay} has passed, rather than needing something to run
     * at the 06:00 boundary to clear it: a server that was offline over the rollover still answers
     * correctly the first time it is asked. The period is the daily-tasks one, so "today" means the
     * same thing in the warps tab as it does in the daily-tasks tab.
     */
    public int visitorsToday() {
        return visitorsDay == DailyTaskManager.currentPeriodDay() ? visitors.size() : 0;
    }

    /**
     * This warp with one arrival recorded, or {@code this} when the arrival does not count — which
     * is only ever the owner's own trip, so a warp cannot be promoted up the list by its owner
     * walking in and out of it.
     *
     * <p>The two figures deliberately count differently. {@link #visitsTotal} rises on <em>every</em>
     * arrival, because "how much has this been used" is a plain tally. The visitor set takes each
     * player once a day, because that is what the list is ordered by and a raw tally there could be
     * farmed by re-warping in a loop.
     */
    public Warp withVisit(UUID visitor) {
        if (owner.equals(visitor)) return this;

        long today = DailyTaskManager.currentPeriodDay();
        // A new day starts the set over; the all-time total never resets.
        Set<UUID> updated = new LinkedHashSet<>(visitorsDay == today ? visitors : Set.of());
        updated.add(visitor);

        return new Warp(id, owner, ownerName, name, description, dimension, x, y, z, yaw, pitch,
                price, created, approval, moderator, reports,
                Set.copyOf(updated), today, visitsTotal + 1);
    }

    /** Only the owner may travel to a warp moderation has blocked. */
    public boolean canTeleport(UUID player) {
        return approval != WarpApproval.DISAPPROVED || owner.equals(player);
    }

    /**
     * What {@code viewer} pays to arrive: owners travel to their own warps for nothing.
     *
     * <p>Lives on the record rather than in {@code WarpService} because the detail panel needs the
     * same rule to decide whether to render a price or "free", and the record is the half of the
     * warp the client already holds. Asking the service would drag {@code CurrencyService} and
     * {@code ServerPlayer} onto the client's classpath for a one-line comparison.
     */
    public long priceFor(UUID viewer) {
        return owner.equals(viewer) ? 0 : price;
    }

    /** Official warps are pinned to the top of the list and never name their creator. */
    public boolean isAdmin() {
        return approval == WarpApproval.ADMIN;
    }

    private static String trim(String text, int max) {
        String clean = text == null ? "" : text.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("owner", owner);
        tag.putString("ownerName", ownerName);
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putString("dimension", dimension.toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putLong("price", price);
        tag.putLong("created", created);
        tag.putString("approval", approval.id());
        tag.putString("moderator", moderator);
        tag.put("reports", ids(reports));
        tag.put("visitors", ids(visitors));
        tag.putLong("visitorsDay", visitorsDay);
        tag.putLong("visitsTotal", visitsTotal);
        return tag;
    }

    /** Every field added after the first release defaults, so older saved warps still load. */
    public static Warp load(CompoundTag tag, HolderLookup.Provider registries) {
        return new Warp(
                tag.getUUID("id"), tag.getUUID("owner"), tag.getString("ownerName"),
                tag.getString("name"), tag.getString("description"),
                ResourceLocation.parse(tag.getString("dimension")),
                tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"),
                tag.getLong("price"), tag.getLong("created"),
                WarpApproval.byId(tag.getString("approval")), tag.getString("moderator"),
                uuids(tag, "reports"),
                uuids(tag, "visitors"), tag.getLong("visitorsDay"), tag.getLong("visitsTotal"));
    }

    private static ListTag ids(Set<UUID> set) {
        ListTag list = new ListTag();
        for (UUID id : set) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static Set<UUID> uuids(CompoundTag tag, String key) {
        Set<UUID> set = new LinkedHashSet<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                set.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // A malformed id is not worth losing the whole warp over.
            }
        }
        return Set.copyOf(set);
    }
}
