package com.roll_54.roll_mod.minestar.debug;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionData;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionListing;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionManager;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskGroups;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskStatus;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTasksState;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.minestar.hub.PlayerTier;
import com.roll_54.roll_mod.minestar.hub.home.HomeData;
import com.roll_54.roll_mod.minestar.hub.home.HomeShare;
import com.roll_54.roll_mod.minestar.hub.home.HomeViewers;
import com.roll_54.roll_mod.minestar.hub.home.PlayerHome;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import com.roll_54.roll_mod.minestar.hub.warp.WarpData;
import com.roll_54.roll_mod.minestar.hub.warp.WarpViewers;
import com.roll_54.roll_mod.minestar.kits.Kit;
import com.roll_54.roll_mod.minestar.kits.KitCooldowns;
import com.roll_54.roll_mod.minestar.kits.KitStore;
import com.roll_54.roll_mod.minestar.kits.KitViewers;
import com.roll_54.roll_mod.minestar.letters.Letter;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import com.roll_54.roll_mod.minestar.letters.LetterStore;
import com.roll_54.roll_mod.minestar.letters.LetterViewers;
import com.roll_54.roll_mod.minestar.moderation.BanStore;
import com.roll_54.roll_mod.minestar.moderation.ModerationStatus;
import com.roll_54.roll_mod.minestar.moderation.ModerationViewers;
import com.roll_54.roll_mod.minestar.moderation.WarnStore;
import com.roll_54.roll_mod.minestar.tpa.TpaMode;
import com.roll_54.roll_mod.minestar.tpa.TpaRequest;
import com.roll_54.roll_mod.minestar.tpa.TpaService;
import com.roll_54.roll_mod.minestar.tpa.TpaSettings;
import com.roll_54.roll_mod.minestar.tpa.TpaViewers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Test content for every part of the hub, written straight into each store — past the player-facing
 * limits and costs, and past the service layer, which would announce, kick, log to
 * {@code moderation.log}, send punishment letters or auto-ban at three warns.
 *
 * <p>Other players in the data are fake: stable UUIDs no real account has, named
 * {@code DebugPlayerN}. Everything created is tagged ({@code [Test] } names, {@code Debug} as author,
 * seller or moderator) and recorded in a {@link DebugLedger}, so {@link #clear} undoes exactly this
 * and never a real entry.
 */
final class DebugFill {

    static final String TAG = "[Test] ";
    static final String BY = "Debug";

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    /** What the target's balance is set to, so buying and bidding can be tried. */
    private static final long TEST_BALANCE = 1_000_000L;

    private static final Item[] AUCTION_ITEMS = {
            Items.DIAMOND, Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD, Items.NETHERITE_SCRAP,
            Items.OAK_LOG, Items.COBBLESTONE, Items.ENDER_PEARL, Items.BLAZE_ROD, Items.GHAST_TEAR,
            Items.DIAMOND_SWORD, Items.IRON_PICKAXE, Items.BOW, Items.GOLDEN_APPLE, Items.EXPERIENCE_BOTTLE,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.SLIME_BALL, Items.NAME_TAG};

    private DebugFill() {}

    /** A player that does not exist: the same UUID for the same number, every time. */
    static UUID fake(int n) {
        return UUID.nameUUIDFromBytes(("roll_mod_debug:" + n).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static String fakeName(int n) {
        return "DebugPlayer" + n;
    }

    /* ============================================= fill ============================================= */

    /** Fills everything for {@code p} and returns one line per system, for the command's reply. */
    static List<String> fill(ServerPlayer p) {
        MinecraftServer server = p.server;
        DebugLedger ledger = DebugLedger.load(p.getUUID());
        List<String> report = new ArrayList<>();

        // Saved whatever happens: a system that throws half way must not leave the ones before it
        // created but unrecorded, where clear could never find them.
        try {
            report.add("homes: " + homes(p, ledger));
            report.add("warps: " + warps(p, ledger));
            report.add("letters: " + letters(p, ledger));
            report.add("auction: " + auction(p, ledger));
            report.add("moderation: " + moderation(p, ledger));
            report.add("tpa: " + tpa(p, ledger));
            report.add("kits: " + kits(p, ledger));
            report.add("tier: " + tier(p, ledger));
            report.add("daily tasks: " + daily(p, ledger));
        } finally {
            ledger.save();
            refresh(server, p);
        }

        // Async: the old balance is only known once the database answers, so it records itself.
        currency(p);
        report.add("currency: set to " + TEST_BALANCE + " (old balance saved)");
        return report;
    }

    private static String homes(ServerPlayer p, DebugLedger ledger) {
        HomeData data = HomeData.get(p.server);
        ResourceLocation dim = p.level().dimension().location();
        long stamp = System.currentTimeMillis() % 1000;
        for (int i = 1; i <= 10; i++) {
            double angle = i * Math.PI / 5;
            PlayerHome home = PlayerHome.imported(p, TAG + "Home " + stamp + "-" + i, dim,
                    p.getX() + Math.cos(angle) * 8 * i, p.getY(), p.getZ() + Math.sin(angle) * 8 * i,
                    p.getYRot(), 0);
            // Two of them have guests: one still invited, one accepted — the owner's roster.
            if (i <= 2) {
                home = home.withShares(List.of(
                        HomeShare.pending(fake(1), fakeName(1)),
                        HomeShare.pending(fake(2), fakeName(2)).accepted()));
            }
            data.add(home);
            ledger.add("homes", home.id());
        }

        // Homes of other people: one inviting the target (the invite badge), one they are a guest in.
        long now = System.currentTimeMillis();
        PlayerHome invite = new PlayerHome(UUID.randomUUID(), fake(3), fakeName(3), TAG + "Invite", dim,
                p.getX(), p.getY(), p.getZ(), p.getYRot(), 0, now,
                List.of(HomeShare.pending(p.getUUID(), p.getGameProfile().getName())));
        PlayerHome guest = new PlayerHome(UUID.randomUUID(), fake(4), fakeName(4), TAG + "Guest", dim,
                p.getX(), p.getY(), p.getZ(), p.getYRot(), 0, now,
                List.of(HomeShare.pending(p.getUUID(), p.getGameProfile().getName()).accepted()));
        data.add(invite);
        data.add(guest);
        ledger.add("homes", invite.id());
        ledger.add("homes", guest.id());
        return "10 own (2 shared), 1 invite, 1 guest";
    }

    private static String warps(ServerPlayer p, DebugLedger ledger) {
        WarpData data = WarpData.get(p.server);
        ResourceLocation dim = p.level().dimension().location();
        WarpApproval[] approvals = WarpApproval.values();
        long today = DailyTaskManager.currentPeriodDay();
        long now = System.currentTimeMillis();
        // Exactly the longest description the create form allows.
        String longDescription = "A long description to see how the warp list wraps and clips text. "
                .repeat(3).substring(0, Warp.MAX_DESCRIPTION);

        for (int i = 1; i <= 20; i++) {
            WarpApproval approval = approvals[i % approvals.length];
            String name = TAG + "Warp " + i;
            String description = switch (i % 3) {
                case 0 -> "";
                case 1 -> "Test warp number " + i;
                default -> longDescription;
            };
            long price = i % 4 == 0 ? 0 : i * 25L;
            Set<UUID> visitors = new LinkedHashSet<>();
            for (int v = 1; v <= i % 6; v++) visitors.add(fake(10 + v));
            // Three reported warps, for the moderation queue and its badge.
            Set<UUID> reports = i % 7 == 0 ? Set.of(fake(20), fake(21)) : Set.of();

            Warp warp;
            if (i <= 5) {
                warp = Warp.at(p, name, description, price);
                warp = new Warp(warp.id(), warp.owner(), warp.ownerName(), warp.name(), warp.description(),
                        warp.dimension(), warp.x() + i * 6, warp.y(), warp.z() - i * 6, warp.yaw(), warp.pitch(),
                        warp.price(), warp.created(), approval, BY, reports, Set.copyOf(visitors), today,
                        visitors.size() * 3L);
            } else {
                int owner = 30 + i % 5;
                warp = new Warp(UUID.randomUUID(), fake(owner), fakeName(owner), name, description, dim,
                        p.getX() - i * 6, p.getY(), p.getZ() + i * 6, p.getYRot(), 0,
                        price, now - i * HOUR, approval, BY, reports, Set.copyOf(visitors), today,
                        visitors.size() * 3L + i);
            }
            data.add(warp);
            ledger.add("warps", warp.id());
        }
        return "20 (all approval states, 2 reported, 5 owned)";
    }

    private static String letters(ServerPlayer p, DebugLedger ledger) {
        MinecraftServer server = p.server;
        long now = System.currentTimeMillis();
        UUID target = p.getUUID();
        String longBody = String.join("\n\n", java.util.Collections.nCopies(12,
                "This is a long test letter body, repeated so the reader has to scroll to reach the end."));

        List<Letter> letters = List.of(
                personal(TAG + "Welcome", "A short test letter.", now - 2 * DAY, List.of(), Map.of(), target),
                personal(TAG + "Long letter", longBody, now - DAY, List.of(), Map.of(), target),
                personal(TAG + "A gift", "This one has rewards to accept.", now - 6 * HOUR,
                        List.of(LetterReward.item(new ItemStack(Items.DIAMOND, 3)),
                                LetterReward.item(new ItemStack(Items.GOLDEN_APPLE, 2))),
                        Map.of(), target),
                personal(TAG + "More gifts", "Several rewards, to fill the chip row.", now - HOUR,
                        List.of(LetterReward.item(new ItemStack(Items.IRON_INGOT, 16)),
                                LetterReward.item(new ItemStack(Items.BREAD, 8)),
                                LetterReward.item(new ItemStack(Items.TORCH, 32))),
                        Map.of(), target),
                // Already accepted: shows the "taken" state, and can never pay out again.
                personal(TAG + "Already taken", "Rewards on this one were already accepted.", now - 3 * DAY,
                        List.of(LetterReward.item(new ItemStack(Items.STICK, 1))),
                        Map.of(target, now - 2 * DAY), target),
                // Broadcasts only staff see in the moderation list: one expired, one scheduled far
                // ahead. No live broadcast, which would land in every player's inbox.
                new Letter(UUID.randomUUID(), TAG + "Expired broadcast", "An expired broadcast.", BY,
                        now - 10 * DAY, now - 10 * DAY, now - DAY, List.of(), "", Map.of(), null),
                new Letter(UUID.randomUUID(), TAG + "Scheduled broadcast", "Goes out in a year.", BY,
                        now, now + 365 * DAY, 0, List.of(), "", Map.of(), null));

        for (Letter letter : letters) {
            LetterStore.put(server, letter);
            ledger.add("letters", letter.id());
        }
        return "5 personal (3 with rewards, 1 accepted), 2 staff broadcasts";
    }

    private static Letter personal(String title, String body, long sentAt, List<LetterReward> rewards,
                                   Map<UUID, Long> accepted, UUID recipient) {
        return new Letter(UUID.randomUUID(), title, body, BY, sentAt, sentAt, 0, rewards, "",
                accepted, recipient);
    }

    private static String auction(ServerPlayer p, DebugLedger ledger) {
        AuctionData data = AuctionData.get(p.server);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            Item item = AUCTION_ITEMS[i % AUCTION_ITEMS.length];
            int count = Math.min(item.getDefaultMaxStackSize(), 1 + (i * 7) % 64);
            ItemStack stack = new ItemStack(item, count);
            boolean own = i >= 16;
            UUID seller = own ? p.getUUID() : fake(40 + i % 6);
            String sellerName = own ? p.getGameProfile().getName() : fakeName(40 + i % 6);
            boolean bidding = i % 5 >= 3;
            long expires = now + HOUR + (i * 7 * DAY) / 20;
            long price = 50L + i * 37L;

            AuctionListing listing = bidding
                    ? new AuctionListing(UUID.randomUUID(), seller, sellerName, stack, true, 0, price,
                            i % 2 == 0 ? price * 4 : 0, now - i * HOUR, expires)
                    : new AuctionListing(UUID.randomUUID(), seller, sellerName, stack, false, price, 0, 0,
                            now - i * HOUR, expires);
            // A couple of bidding listings already have a bid on them.
            if (bidding && i % 2 == 1) {
                listing.currentBid = price + 25;
                listing.currentBidderId = fake(50);
                listing.currentBidderName = fakeName(50);
            }
            data.addListing(listing);
            ledger.add("listings", listing.id);
        }

        // The collection: things "won" or returned, waiting to be claimed. Deliberately cheap.
        List<ItemStack> claims = List.of(new ItemStack(Items.DIRT, 16), new ItemStack(Items.STICK, 8),
                new ItemStack(Items.COBBLESTONE, 32), new ItemStack(Items.WHEAT_SEEDS, 5));
        for (ItemStack claim : claims) {
            data.addClaim(p.getUUID(), claim.copy());
            ledger.add("claims", BuiltInRegistries.ITEM.getKey(claim.getItem()) + "|" + claim.getCount());
        }
        return "20 listings (4 yours, 8 bidding, 2 with bids), 4 to collect";
    }

    private static String moderation(ServerPlayer p, DebugLedger ledger) {
        long now = System.currentTimeMillis();
        UUID target = p.getUUID();
        String name = p.getGameProfile().getName();

        WarnStore.add(target, name, new WarnStore.Warn(now - DAY, BY, "1.2", TAG + "first test warn"));
        WarnStore.add(target, name, new WarnStore.Warn(now, BY, "2.5", TAG + "second test warn"));

        // A real mute, as asked: the target cannot chat until clear or the hour runs out.
        MuteStore.mute(target, now + HOUR, BY, TAG + "test mute");

        // Banned players are the only way someone offline gets a row in the moderation list.
        for (int i = 1; i <= 5; i++) {
            UUID fake = fake(60 + i);
            long until = i <= 2 ? 0L : now + i * DAY;
            BanStore.ban(fake, until, BY, i % 2 == 0 ? "1.2" : "2.5", TAG + "test ban " + i, fakeName(60 + i));
            ledger.add("bans", fake);
            if (i <= 2) {
                WarnStore.add(fake, fakeName(60 + i), new WarnStore.Warn(now, BY, "1.2", TAG + "warn"));
            }
            if (i == 3) {
                MuteStore.mute(fake, now + DAY, BY, TAG + "test mute");
            }
        }
        return "2 warns + 1h mute on you, 5 banned fake players";
    }

    private static String tpa(ServerPlayer p, DebugLedger ledger) {
        long expires = System.currentTimeMillis() + 30 * MINUTE;
        UUID target = p.getUUID();
        String name = p.getGameProfile().getName();
        List<TpaRequest> requests = List.of(
                new TpaRequest(UUID.randomUUID(), fake(70), fakeName(70), target, name, TpaRequest.Kind.TO, expires),
                new TpaRequest(UUID.randomUUID(), fake(71), fakeName(71), target, name, TpaRequest.Kind.HERE, expires),
                new TpaRequest(UUID.randomUUID(), target, name, fake(72), fakeName(72), TpaRequest.Kind.TO, expires));
        for (TpaRequest request : requests) {
            TpaService.debugPut(request);
            ledger.add("tpa", request.id());
        }

        ledger.prior("tpaMode", new JsonPrimitive(TpaSettings.mode(target).name()));
        TpaSettings.set(p, TpaMode.TEAM_AND_ALLIES);
        return "2 incoming, 1 outgoing (in memory, 30 min), mode TEAM_AND_ALLIES";
    }

    private static String kits(ServerPlayer p, DebugLedger ledger) {
        MinecraftServer server = p.server;
        long now = System.currentTimeMillis();
        List<Kit> kits = List.of(
                new Kit("debug_kit_1", 20L * 60, "", List.of(new ItemStack(Items.BREAD, 4))),
                new Kit("debug_kit_2", 20L * 60 * 60, "",
                        List.of(new ItemStack(Items.TORCH, 16), new ItemStack(Items.STICK, 4))),
                new Kit("debug_kit_3", 20L * 60 * 60 * 24, "",
                        List.of(new ItemStack(Items.APPLE, 2), new ItemStack(Items.DIRT, 8))));
        for (int i = 0; i < kits.size(); i++) {
            Kit kit = kits.get(i);
            KitStore.put(server, kit);
            ledger.add("kits", kit.name());
            // The last two start on cooldown.
            if (i >= 1) KitCooldowns.claimed(p.getUUID(), kit.name(), now);
        }
        return "3 (2 on cooldown; visible to ops)";
    }

    private static String tier(ServerPlayer p, DebugLedger ledger) {
        ledger.prior("tier", new JsonPrimitive(PlayerTier.of(p)));
        PlayerTier.set(p, 5);
        return "5";
    }

    /**
     * Every task of the target's group completed, quietly: the state is edited directly rather than
     * through {@code addRawProgress}, which announces each completion to the whole party. The group's
     * slots are snapshotted first so clear can put them back.
     */
    private static String daily(ServerPlayer p, DebugLedger ledger) {
        DailyTasksState state = DailyTaskManager.state(p.server);
        UUID groupId = DailyTaskGroups.idOf(p);
        // view() also brings the group onto today's tasks, so it goes before the group is read.
        List<DailyTaskManager.TaskView> views = new ArrayList<>();
        for (int i = 0; i < DailyTasksState.MAX_TASK_COUNT; i++) views.add(DailyTaskManager.view(p, i));
        DailyTasksState.GroupState gs = state.group(groupId);

        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("group", groupId.toString());
        snapshot.addProperty("periodDay", gs.periodDay);
        JsonArray progress = new JsonArray();
        JsonArray completed = new JsonArray();
        for (int i = 0; i < DailyTasksState.MAX_TASK_COUNT; i++) {
            progress.add(gs.progress[i]);
            completed.add(gs.completed[i]);
        }
        snapshot.add("progress", progress);
        snapshot.add("completed", completed);
        ledger.prior("daily", snapshot);

        int done = 0;
        for (int i = 0; i < views.size(); i++) {
            DailyTaskManager.TaskView view = views.get(i);
            if (view == null) continue;
            gs.progress[i] = view.required();
            gs.completed[i] = true;
            done++;
        }
        gs.invalidateHooks();
        state.setDirty();
        return done + " tasks completed, bonus claimable (whole party)";
    }

    private static void currency(ServerPlayer p) {
        MinecraftServer server = p.server;
        UUID target = p.getUUID();
        CurrencyService.get(server, target, CurrencyType.MAIN).thenAccept(old -> {
            DebugLedger ledger = DebugLedger.load(target);
            ledger.prior("balance", new JsonPrimitive(old));
            ledger.save();
            CurrencyService.set(server, target, CurrencyType.MAIN, TEST_BALANCE);
        }).exceptionally(e -> {
            RollMod.LOGGER.error("[Debug] could not read {}'s balance; left it alone", target, e);
            return null;
        });
    }

    /* ============================================= clear ============================================ */

    /** Undoes everything the ledger holds for {@code p}. Returns one line per system. */
    static List<String> clear(ServerPlayer p) {
        MinecraftServer server = p.server;
        UUID target = p.getUUID();
        DebugLedger ledger = DebugLedger.load(target);
        List<String> report = new ArrayList<>();

        // By ledger, then a sweep for anything tagged that the ledger never recorded — a fill that
        // failed before saving it. The tag is unique to this command, so the sweep cannot reach a
        // real entry.
        HomeData homes = HomeData.get(server);
        long homeCount = ledger.uuids("homes").stream().filter(homes::remove).count();
        for (PlayerHome home : List.copyOf(homes.all())) {
            if (home.name().startsWith(TAG) && homes.remove(home.id())) homeCount++;
        }
        report.add("homes: " + homeCount);

        WarpData warps = WarpData.get(server);
        long warpCount = ledger.uuids("warps").stream().filter(warps::remove).count();
        for (Warp warp : List.copyOf(warps.all())) {
            if (warp.name().startsWith(TAG) && warps.remove(warp.id())) warpCount++;
        }
        report.add("warps: " + warpCount);

        long letterCount = ledger.uuids("letters").stream()
                .filter(id -> LetterStore.remove(server, id)).count();
        for (Letter letter : List.copyOf(LetterStore.all(server))) {
            if (BY.equals(letter.author()) && letter.title().startsWith(TAG)
                    && LetterStore.remove(server, letter.id())) letterCount++;
        }
        report.add("letters: " + letterCount);

        AuctionData auction = AuctionData.get(server);
        int listings = 0;
        for (UUID id : ledger.uuids("listings")) {
            AuctionListing listing = auction.findById(id);
            if (listing != null) {
                auction.removeListing(listing);
                listings++;
            }
        }
        report.add("auction: " + listings + " listings, " + removeClaims(auction, target, ledger) + " claims");

        int warns = WarnStore.removeWhere(target, w -> BY.equals(w.by()));
        MuteStore.Mute mute = MuteStore.mute(target);
        boolean unmuted = mute != null && BY.equals(mute.by()) && MuteStore.unmute(target);
        int bans = 0;
        for (UUID fake : ledger.uuids("bans")) {
            if (BanStore.unban(fake)) bans++;
            WarnStore.clear(fake);
            MuteStore.unmute(fake);
        }
        report.add("moderation: " + warns + " warns" + (unmuted ? ", unmuted" : "") + ", " + bans + " bans");

        report.add("tpa: " + ledger.uuids("tpa").stream().filter(TpaService::debugRemove).count()
                + " requests" + restoreTpaMode(p, ledger));

        int kits = 0;
        for (String kit : new LinkedHashSet<>(ledger.strings("kits"))) {
            if (KitStore.remove(server, kit)) kits++;
            KitCooldowns.forget(kit);
        }
        report.add("kits: " + kits);

        JsonElement tier = ledger.prior("tier");
        if (tier != null) {
            PlayerTier.set(p, tier.getAsInt());
            report.add("tier: restored to " + tier.getAsInt());
        }

        report.add("daily tasks: " + restoreDaily(server, ledger));

        JsonElement balance = ledger.prior("balance");
        if (balance != null) {
            CurrencyService.set(server, target, CurrencyType.MAIN, balance.getAsLong());
            report.add("currency: restored to " + balance.getAsLong());
        }

        ledger.delete();
        refresh(server, p);
        report.add("not undone: anything already claimed from test letters, the collection, kits or "
                + "daily rewards, and money spent on test listings");
        return report;
    }

    /** Takes back the collection entries fill added, if they are still unclaimed. */
    private static int removeClaims(AuctionData auction, UUID target, DebugLedger ledger) {
        List<ItemStack> claims = new ArrayList<>(auction.getClaims(target));
        int removed = 0;
        for (String entry : ledger.strings("claims")) {
            String[] parts = entry.split("\\|");
            if (parts.length != 2) continue;
            ResourceLocation id = ResourceLocation.tryParse(parts[0]);
            int count;
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            for (int i = 0; i < claims.size(); i++) {
                ItemStack stack = claims.get(i);
                if (stack.getCount() == count && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) {
                    claims.remove(i);
                    removed++;
                    break;
                }
            }
        }
        if (removed > 0) auction.setClaims(target, claims);
        return removed;
    }

    private static String restoreTpaMode(ServerPlayer p, DebugLedger ledger) {
        JsonElement mode = ledger.prior("tpaMode");
        if (mode == null) return "";
        try {
            TpaMode restored = TpaMode.valueOf(mode.getAsString());
            TpaSettings.set(p, restored);
            return ", mode restored to " + restored;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Puts the group's slots back as they were — unless the group has rolled to a new day since,
     * when the snapshot belongs to tasks that no longer exist and restoring it would corrupt today's.
     */
    private static String restoreDaily(MinecraftServer server, DebugLedger ledger) {
        JsonElement raw = ledger.prior("daily");
        if (raw == null || !raw.isJsonObject()) return "nothing recorded";
        JsonObject snapshot = raw.getAsJsonObject();
        DailyTasksState state = DailyTaskManager.state(server);
        DailyTasksState.GroupState gs = state.group(UUID.fromString(snapshot.get("group").getAsString()));
        if (gs.periodDay != snapshot.get("periodDay").getAsLong()) {
            return "new day since fill; left as is";
        }
        JsonArray progress = snapshot.getAsJsonArray("progress");
        JsonArray completed = snapshot.getAsJsonArray("completed");
        for (int i = 0; i < DailyTasksState.MAX_TASK_COUNT && i < progress.size(); i++) {
            gs.progress[i] = progress.get(i).getAsInt();
            gs.completed[i] = completed.get(i).getAsBoolean();
        }
        gs.invalidateHooks();
        state.setDirty();
        return "restored";
    }

    /* ============================================ refresh =========================================== */

    /** Pushes the changes to everyone with the hub open, and the target's badges. */
    private static void refresh(MinecraftServer server, ServerPlayer p) {
        HomeViewers.resync(server);
        WarpViewers.resync(server);
        LetterViewers.resync(server);
        AuctionManager.resyncAll(server);
        ModerationViewers.resync(server);
        TpaViewers.resync(server);
        KitViewers.resync(server);
        DailyTaskStatus.markDirty();
        ModerationStatus.sendTo(p);
    }
}
