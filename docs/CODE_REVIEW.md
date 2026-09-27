# Code Review — Roll Mod (2026-09-25)

Full-codebase review for logic mistakes and performance issues (all of `common/`, `client/`,
`server/`). Findings were produced by a seven-area parallel review and the critical ones were
re-verified against the source before fixing. Paths below are relative to
`common/src/main/java/com/roll_54/roll_mod/` unless prefixed with `client/` or `server/`.

**Status legend:** ✅ FIXED in this review · ⬜ open (reported only)

**Totals:** 15 critical (all fixed) · ~34 logic · ~17 performance · ~20 minor.

---

## 1. Critical — all fixed ✅

### Unauthenticated vending-block packets (item/money theft)
The admin/settings UIs are gated client-side (Vendor Key / ownership), but the packets did no
server-side checks — any modified client could send them against any vendor.

1. ✅ `economy/vendingblock/network/OwnerChangePacket.java` — any player could rename any
   vendor's owner to themselves and then extract storage, receive payments, and break the block.
   Now requires the Vendor Key.
2. ✅ `.../network/InfiniteInventoryPacket.java` — any player could set any vendor infinite:
   sell mode conjures unstocked items, buy mode prints money via `addBalance`. Now Vendor Key only.
3. ✅ `.../network/DiscardsPaymentPacket.java` — any player could void an owner's revenue (sell
   mode) or destroy sold items (buy mode). Now Vendor Key only.
4. ✅ `.../network/BuyModePacket.java` — buy mode unlocks hopper extraction of position items
   (`VendorBlockEntity.extractItemHandler`), so a stranger flipping it could drain a shop's sale
   stock with a hopper. Now owner-or-key. (The buyer-UI toggle still renders for everyone; a
   follow-up could hide it for non-owners.)
5. ✅ `.../network/FilterSlotUpdatePacket.java` — no owner check, no slot bounds, arbitrary
   client `ItemStack`: slot 0 with count > 1 dropped the excess as real item entities (an item
   printer), slot ≥ 2 threw. Now owner-or-key, slot 1 only, count clamped to 1, and the
   full-block/blacklist validation re-run server-side.

### Vendor transaction races (dupes / free money)
6. ✅ `economy/vendingblock/blockentity/transaction/VendorBlockTransaction.java` (purchase) —
   stock was checked before the async payment and never re-checked; `deductFromStorage`'s return
   was discarded and the full amount handed out. Two buyers racing the last stock each received
   items. Now: the continuation re-checks disconnect/block-removed/stock and refunds via a
   checked reversal; `giveProduct` hands out only what actually left storage and drops (instead
   of voiding) anything that no longer fits the buyer's inventory.
7. ✅ Same file (sell) — the owner was paid before `takeAndStore`, which removes "up to" the
   amount and ignored shortfalls: spam-selling one batch of items yielded several payments. Now
   the continuation re-checks the seller still holds the items (and is connected, and the block
   still stands) and reverses the payment otherwise. Reversal failures are logged with player,
   amount and mode.

### Other criticals
8. ✅ `network/packet/PacketLaunchRocket.java:78` — the landing X/Z came from the
   client-supplied packet `pos`, letting a player teleport anywhere (and force chunk generation
   there). Now uses `menu.blockEntity.getBlockPos()`.
9. ✅ `items/electricItems/EnergyBatteryItem.java:160` + `registry/ItemRegistry.java` — the
   MI-component charging path wrote the ENERGY component onto the whole target stack (every item
   in a stack of N gained the energy, battery paid once), and `nano_battery` was stackable.
   Now: charge only stacks of count 1, and `nano_battery` gets `stacksTo(1)`.
10. ✅ `minestar/letters/LetterService.java` — COMMAND letter rewards run as the *reader* at
    permission level 4, but attaching them only needed the letters permission (level 2 / LP
    node): a level-2 moderator could send themselves `op @s`. Now attaching a COMMAND reward
    requires level 4, checked in both `create` and `update` (new message key
    `msg.roll_mod.letters.commandNeedsOp` in both lang files).
11. ✅ `mixin/adstra/NanoArmorMixin.java:93` — unconditional `(ServerPlayer)` cast in a hook
    that fires for every `LivingEntity` wearing the suit: an armor stand or mob wearing an
    oxygen-filled nano chestplate crashed the server tick every 12 ticks. Now guarded with
    `instanceof`.
12. ✅ `compat/MBD2/energy/MIEnergyTrait.java:104` — auto-IO cached the capability at the
    machine's *own* port position, so EU auto input/output never touched the neighbor (and
    `IO.BOTH` shuffled energy into itself every tick). Now caches at `pos.relative(side)` with
    `side.getOpposite()`, matching MBD2's own trait.
13. ✅ Brigadier gate merge — `server/.../commands/NetherstormCommand.java` gated the shared
    `rollmod` root, and `server/.../commands/AutoGiveCommands.java` +
    `minestar/moderation/ModerationCommands.java` gated the shared `admin` literal. Brigadier
    keeps only the first-registered node's `requires`, and other classes register those literals
    ungated, so depending on registration order either any player could run
    `warn`/`ban`/`autogive set`/`netherstorm start`, or every `/rollmod` command demanded
    level 2. All three now gate their unique first child (the pattern `ItemSkinCommand` and
    `DailyTasksCommand` already used). *(Verified safe: `CurrencyCommand`'s gated `admin` sits
    under the unique `money` literal, so it was never affected.)*
14. ✅ `server/.../minestar/ShopReceiveCommand.java:36` — the Minestar shop callback runs on
    that library's own executor and was running `/give`-style commands off the server thread
    (inventory/world corruption risk). Delivery is now wrapped in `player.server.execute(...)`.
15. ✅ `economy/api/ftb/task/CurrencyTask.java:53` — quest progress was credited immediately
    while the withdraw was fire-and-forget: a failed withdraw (stale balance, DB error → `false`)
    still completed the quest, and quests can pay currency rewards. Progress is now credited only
    on a successful withdraw.

---

## 2. Logic issues (open) ⬜

Context for the money items: `CurrencyRepository.onDatabase` converts every DB exception into a
`false` result, so `.exceptionally(...)` handlers on repository futures are dead code — failures
surface as an ignored `false`.

### Economy / money
- ⬜ `economy/vendingblock/auction/AuctionManager.java:404` (`tickExpiry`) — the expired listing
  is removed, the winner's item goes to claims, and the seller payout is a fire-and-forget
  `addBalance` whose `false` result nothing reads. A DB failure permanently destroys the
  seller's money. Suggested: a persisted "pending payouts" ledger retried on later ticks, checked
  before the listing is removed.
- ⬜ `AuctionManager.java:227, 238, 245, 296, 306, 315` — every escrow refund and the buyout
  payout are unchecked `addBalance` calls; any DB hiccup silently deletes that player's money.
  Same ledger fix.
- ⬜ `AuctionManager.java:165` (`buyFixed`) — the losing buyer of a race is reversed via
  `pay(sellerId, buyer, price)` whose result is ignored; if the seller already spent the double
  payment the loser paid for nothing. Reserve the listing before initiating payment, or check the
  reversal and add a claim/pending refund.
- ⬜ `AuctionManager.java:435-441` (`giveOrClaim`) — writes into a possibly already-saved,
  disconnected player's inventory (buyer captured before the async hop); the delivered portion
  is lost. Check `hasDisconnected()` and route to claims.
- ⬜ `economy/plot/PlotService.java:501-508` — resale payout ignores the `addBalance` result:
  on `false` the buyer has the plot, the listing is gone, and the seller is silently unpaid.
- ⬜ `PlotService.java:495-497` — `refund()` drops the deposit result; a failed refund destroys
  the buyer's money with no log (call sites at 228/233/273/278).
- ⬜ `PlotService.java:232-235, 277-280` — `claimAll`/`transfer` return `false` mid-loop
  *without rolling back* already-claimed chunks (the anchor chunk goes first), so a partial
  failure leaves the buyer owning the plot *and* refunded — and able to `sellToServer` it.
  `sellToServer` (366-369) already rolls back; the buy paths should too.
- ⬜ `PlotService.java:216, 259` — the escalating price is computed before the async withdraw
  and never re-validated; rapid multi-buys all pay the lowest price. Recompute `priceFor` in the
  server-thread continuation.
- ⬜ `PlotService.java:219-242, 263-297` — server shutdown between the withdraw (DB executor)
  and the queued server-thread continuation loses the buyer's money with no record. Journal the
  withdraw (as the offer system does) or at least log it before the continuation.
- ⬜ `PlotService.java:242, 297` — the `.exceptionally(fail)` handler is attached after the
  success continuation, so an exception *after* the grant refunds the price while the buyer keeps
  the plot. Narrow the scope or flag post-grant.
- ⬜ `economy/api/CurrencyOfferService.java:240-255` — `SWEEPING` is reset inside
  `whenCompleteAsync(server)`; a shutdown mid-sweep leaves it `true` for the JVM's life, so offer
  expiry silently stops in later singleplayer worlds. Reset in a plain `whenComplete`.
- ⬜ `economy/vendingblock/blockentity/transaction/VendorBlockTransaction.java` — a vendor
  broken mid-payment still had `onRemove` drop all storage before the continuation ran
  (partially mitigated by the new `vendor.isRemoved()` check, which now refunds instead).

### Daily tasks
- ⬜ `minestar/dailytasks/DailyTaskManager.java:413-419` (`rerollSlot`) — `.exceptionally` also
  fires when the *withdraw itself* failed (money likely never taken) yet unconditionally deposits
  the cost back → mints Starcoins on a DB error. Distinguish withdraw-stage failure (`handle` on
  that stage) from post-withdraw failure.
- ⬜ `DailyTaskManager` `PENDING_REROLLS` — never cleared on server stop; a future that never
  completes leaves the UUID stuck for the JVM's life (singleplayer: that player can never
  paid-reroll in the next world). Clear statics in a `ServerStoppingEvent` handler and add
  `orTimeout` to the withdraw.
- ⬜ `minestar/dailytasks/DailyTaskEvents.java:263-291` (`resolvePendingClips`) — a throwing
  entry propagates out of `ServerTickEvent.Post` and crashes the tick; `PENDING_CLIPS.clear()`
  only runs if the loop completes. Per-entry try/catch + clear in `finally`.
- ⬜ `tasks/PlaytimeTask.java:11`, `tasks/WalkDistanceTask.java:11` vs
  `DailyTaskManager.requiredAmount` (554-561) — the javadocs promise solo 2× scaling but
  `requiredAmount` returns `baseAmount()` for `members <= 1`: solo requirements are half the
  documented values (or the docs are stale). Reconcile.

### Minestar suite
- ⬜ `minestar/letters/LetterService.java:157-161` (`accept`) — never checks
  `letter.isFor(reader)`: a player who learns a personal letter's UUID can accept and be paid its
  rewards. One-line gate before `markAccepted`.
- ⬜ `minestar/hub/warp/WarpService.java:296-312` — the warp fee is charged *after* arrival with
  the transfer result discarded; spending the money during the 3-second warm-up teleports for
  free while the "paid" message is still shown. Move the message/owner credit into
  `thenAccept(ok -> ...)`.
- ⬜ `minestar/data/MinestarFiles.java:105-111` — `write()` truncates the destination in place;
  a crash mid-write leaves a truncated `bans.json`/`warns.json`/`letters.json` which the loader
  then "treats as empty" — every ban silently vanishes. Write to a temp file +
  `Files.move(..., ATOMIC_MOVE)`.
- ⬜ `minestar/data/MuteStore.java:99` — one hand-edited non-object row aborts the whole
  `mutes.json` load with a `ClassCastException` (Ban/WarnStore guard this; MuteStore doesn't).

### Content
- ⬜ `blocks/entity/GrowthChamberBlockEntity.java:66-71` — `ContainerData.set` switch has
  fall-through (no breaks): syncing progress overwrites `maxProgress`, corrupting the GUI arrow.
  Use arrow cases.
- ⬜ `blocks/entity/WeedManagerBlockEntity.java:169-170` — load restores energy through
  `receiveEnergy`, clamped to 1000 FE: up to 99% of the saved buffer is lost on every chunk
  reload. Use `AnyTierEnergyStore.setEnergy` (as CropManager does).
- ⬜ `blocks/TelescopeBlock.java:62-64` — `onRemove` is overridden empty and never calls super:
  breaking a telescope leaves a stale block entity and silently destroys its 64-item inventory.
- ⬜ `blocks/regenblock/AbstractRegenBlock.java:82-98` — `dropResources` is called
  unconditionally: creative players get drops and `hasCorrectToolForDrops` is never consulted —
  a tool-gate-free infinite ore faucet (the block regenerates by design).
- ⬜ `items/electricItems/EnergyDrillItem.java:164-223` — the primary broken block is never
  charged energy, so a drill at 0 EU mines forever at full speed; area drops also go to creative
  players and ignore tool correctness.
- ⬜ `items/electricItems/refactored/ComponentEnergyDrill.java:27,43,53,63` — upgrade getters
  read from `new ItemStack(this)` (a component-less fresh stack), so installed upgrades can never
  take effect (latent — no external callers yet).
- ⬜ `items/ComponentApplicatorItem.java:79-86` — `copyNonBlacklisted` iterates the merged
  prototype+patch map, stamping the applicator's own defaults (`max_stack_size` 64,
  `attribute_modifiers`, rarity) onto targets — weapons lose their damage modifiers and
  unstackable tools become stackable. Iterate `getComponentsPatch()` instead.
- ⬜ `blocks/entity/CropManagerBlockEntity.java:74-139`, `WeedManagerBlockEntity.java:64-121` —
  harvest results / energy spend are never `setChanged()`, so machines roll back on an unlucky
  chunk unload; `insertItemIntoSlots` can also void part of a multi-stack harvest after a
  one-free-slot `hasSpace` pass.
- ⬜ `netherstorm/StormHandler.java:41-56` — the static `StormState` is never reset on server
  stop: a second singleplayer world in the same session keeps the previous world's storm timers
  and marks the unloaded world's SavedData dirty. Null it in `ServerStoppedEvent`.
- ⬜ `netherstorm/StormHandler.java:246-303` — storm spawns are unbounded and persistent:
  3–10 `setPersistenceRequired()` wither skeletons per player per 15 min accumulate for the whole
  storm and remain after it; creative/spectator players are deliberately not excluded and each
  mob guarantees netherite scrap. Cap live storm mobs, drop persistence, skip creative.

### Mixins / compat
- ⬜ `mixin/antidupe/HopperBlockEntityMixin.java:22` — returning `true` for a 32767-delay item
  makes the hopper take the success path: one display item resting on a hopper permanently blocks
  all pickup and churns `setChanged`. Return `false` instead.
- ⬜ `mixin/EIArmorMaterialsMixin.java` — not listed in any mixins JSON (the only orphan;
  verified against all 6 configs): the nano defense adjustment silently never applies. Add it to
  `roll_mod.mixins.json` or delete the file.
- ⬜ `mixin/antidupe/ShulkerBoxBlockMixin.java:26-29` — clears the shulker's contents on *any*
  `getDrops` call, which is a query mods call speculatively — each such call voids a placed
  shulker's inventory. Clear from an actual removal path instead.
- ⬜ `mixin/adstra/NanoArmorMixin.java:92-95` — the 3 mB "storm" oxygen drain has no
  storm-active or dimension gate: tanks bleed ~5 mB/s everywhere, all the time (≈3 min per
  1000 mB in the Overworld). Gate on `StormHandler` state + Nether if unintended.
- ⬜ `mixin/antidupe/ConnectionMixin.java:35` — reads the inventory on the Netty thread with an
  unvalidated slot: a negative slot throws in the pipeline, and even valid reads race the server
  thread. Bounds-check (hotbar or 40) or move to the main-thread handler.
- ⬜ `mixin/adstra/StationLoaderMixin.java:53` — redirects Ad Astra's radio-station fetch to a
  bundled JSON that does not exist, so the station list is always empty. Ship the file or drop
  the mixin entry.
- ⬜ `mixin/antidupe/TridentChargeMixin.java:30-33` — cancels every container-click packet at
  Netty HEAD while an item is in use, with no resync: legit clicks racing item use produce ghost
  items. Cancel in the main-thread handler or force `sendAllDataToRemote`.

### Client / server modules
- ⬜ `server/.../minestar/PvpManager.java:19-55` — PvP-off and its cooldown live at the root of
  `getPersistentData()`, which is not copied on death: a player who disabled PvP is attackable
  again after their first death. Copy in `PlayerEvent.Clone` (as `PerkEvents` does).
- ⬜ `client/.../util/StarcoinPriceTooltip.java:28-56` — fetches a `data/` file through the
  client *assets* resource manager, so the price map is always empty and the feature is dead
  (`reset()` is also never called). Use a synced data map.
- ⬜ `client/.../RollModClient.java:96-104` — `data != null && lense || module`: the null guard
  doesn't cover the second operand → NPE inside AgriCraft's overlay predicate. Parenthesize.
- ⬜ `client/.../client/KeyInputHandler.java:48-56` — a static `boolean[3]` is the packet's
  authoritative toggle value while the real state lives in the item component: after relog or
  swapping chestplates the first keypress "does nothing". Derive from the worn stack.
- ⬜ `server/.../util/SpawnerChestHelper.java:76-96` — the fit check runs against a permissive
  scratch handler, then the commit discards leftovers: filtered/limited destination inventories
  silently destroy drops, contradicting the class's all-or-nothing javadoc. Simulate against the
  real handler and re-drop leftovers.
- ⬜ `server/.../minestar/AutoGiveHandler.java:36-51` — `tickCounter % TIME_TO_GIVE` divides by
  zero if the config is 0 (unvalidated int), and one misconfigured item `return`s out of the whole
  player loop instead of `continue`. (`RandomMessageHandler.java:23` needs the same validation.)
- ⬜ `server/.../mixin/crash/MinecraftServerCrashGuardMixin.java:31-36` — catches `Throwable`,
  swallowing `OutOfMemoryError` and vanilla's deliberate `ReportedException` crash path: the
  server keeps ticking half-mutated state and no crash report is written. Rethrow `Error` and
  `ReportedException`.
- ⬜ `registry/BlockEntites.java:36-38` — `TELESCOPE_BE` is built with the
  `RocketControllerBlockEntity::new` factory: any type-factory path (structure templates,
  `loadStatic`) produces the wrong BE, which fails `isValid` and loses its NBT.

---

## 3. Performance issues (open) ⬜

### The hub UI polling model (biggest server cost)
LDLib2 evaluates every synced binding's supplier **every server tick**, and `HubUI` builds all
tab subtrees eagerly, so every player with the hub open on *any* tab pays for all tabs:

- ⬜ `minestar/dailytasks/gui/DailyTasksUI.java:361-377, 222-231, 195-196` — 69 supplier
  evaluations per tick per viewer: ~48 `DailyTaskManager.view()` calls (each an FTB party lookup
  + SavedData fetch + reward re-derivation with a fresh `Random`; the same view recomputed 6× per
  row), 8 `rerollCost` (a LuckPerms meta query each), 8 `rerollsLeft`, 4 bonus, plus a
  `ZonedDateTime` per tick. ≈1,360 party lookups + 160 LuckPerms queries **per second per
  viewer**. Fix: one memoized snapshot per player per tick that all bindings read; cache the
  free-reroll allowance per period; throttle the header.
- ⬜ `minestar/hub/gui/HomeTab.java:115-122` + `minestar/hub/QuestProgress.java` — four bindings
  each walk the *entire FTB Quests book* per tick per viewer, plus a LuckPerms group walk for the
  rank binding. One cached `Counts` snapshot, refreshed ≤1/s.
- ⬜ `minestar/hub/HubCommand.java:68-80` — opening the hub registers the player in 9 viewer
  sets, removed only on logout, never on UI close: every warp/kit/home/letter/plot/auction change
  then pushes full snapshots (with DB reads and OPAC lookups) to everyone who *ever* opened the
  hub this session. Remove viewers on menu close. Related full-resync costs:
  `economy/vendingblock/auction/AuctionManager.java:60-70` (whole auction house re-serialized to
  every viewer on every change) and `economy/plot/PlotViewers.java:34-41`.

### Per-event / per-tick hot paths
- ✅ FIXED `minestar/dailytasks/DailyTaskManager.java` (`progress`) — every block break, kill,
  craft, fish, eat and the 1 Hz stat sampler paid a party lookup + state fetch + 8-slot loop even
  when the group had no task for that hook. Now each `GroupState` keeps a lazily rebuilt
  `EnumSet<DailyTaskHook>` of its *uncompleted* tasks' hooks (invalidated on roll, reroll,
  slot replacement and completion), and `progress()` early-returns on a miss via
  `DailyTaskGroups.idOf` / `TeamsFacade.partyId` — the same FTB lookup as before but without the
  per-event member-set copy. The miss path is now two map lookups with zero allocation; a
  finished task's hook also drops out of the set, so a completed MINE task stops taxing block
  breaks for the rest of the day.
- ⬜ `radiation/RadiationHandler.java:124-156` — re-adds up to ~9 `MobEffectInstance`s per tick
  per irradiated player (one clientbound packet each) and calls `removeEffect` per tick for clean
  players. Apply on a 20-tick interval with duration > interval.
- ⬜ `netherstorm/StormHandler.java:124-142` — three `addEffect` per tick for unprotected
  players, two `removeEffect` per tick for protected ones (which also strips vanilla POISON from
  unrelated sources), and `state.dirty()` every tick.
- ⬜ `blocks/entity/CropManagerBlockEntity.java:63-68` — the guard is `tickCounter >= 1` (the
  comment says 10): 121 `AgriApi.getCrop` lookups every tick per machine. Change to `>= 10`.
- ⬜ `blocks/entity/GrowthChamberBlockEntity.java:118-130` and
  `ResearchWorkbenchBlockEntity.java:95-152` — full `RecipeManager.getRecipeFor` scan every tick
  per machine (twice on the craft tick). Cache the matched recipe until the input changes.
- ⬜ `items/electricItems/EnergyBatteryItem.java:103-132` — each active battery probes armor +
  offhand + 36 slots with `getCapability` every tick. Throttle.
- ⬜ `util/SafeSpot.java:40-53` via `PacketLaunchRocket` — a 41×41-column scan that force-loads
  (and in a fresh dimension, generates) chunks synchronously in the packet handler:
  multi-second freezes on launches to unexplored coordinates. Budget per tick as `RtpService`
  does.
- ⬜ `minestar/rtp/RtpService.java:234` — `candidate()` performs synchronous chunk *generation*
  per promising candidate; worst-case config allows ~80 sync generations in one tick. Cap actual
  generations to 1 per search per tick.
- ⬜ `util/PlayerLookup.java:50-63` — `cached()` claims it never blocks, but
  `GameProfileCache.get(String)` makes a synchronous Mojang HTTP call on a miss, stalling the
  server thread on every unknown-name resolve.

### Synchronous file IO on the server thread
- ⬜ `minestar/data/PlayerPositions.java:113-150` — every mod teleport *and every player death*
  rewrites the whole `teleports.json` synchronously; same pattern in `KitCooldowns` and
  `TpaSettings`. Dirty-flag + interval flush.
- ⬜ `minestar/letters/LetterStore.java:93-104, 181-210` — every letter accept rewrites all of
  `letters.json` (re-encoding every ItemStack): a 100-player broadcast = 100 sequential full
  writes. Batch/defer.

### Client-side
- ⬜ `economy/vendingblock/gui/auction/AuctionUI.java:284` — the browse grid re-filters and
  re-sorts the whole listing list every client tick. Recompute on a cache-generation change.
- ⬜ `compat/LDLib/MyLDLibPlugin.java:64-66` — reflection field lookup per furnace-menu open;
  cache the `Field`.

---

## 4. Minor (open) ⬜

- ⬜ `minestar/dailytasks/DailyTaskEvents.java:156-173` — furnace/pot SMELT/COOK credit is
  reassigned on *any* right-click (even sneak-placing against it, once per hand): one sneak-click
  hijacks an AFK player's smelter credit. Claim on place + real container-open.
- ⬜ `DailyTaskEvents.java:174,184` — `getData(STATION_OWNER)` *attaches* the serialized default
  to every furnace that ever smelts; use `getExistingData` on read paths (chunk NBT bloat).
- ⬜ `minestar/dailytasks/DailyTaskManager.java:272` — `rerollFor` uses quota `forPlayer` while
  the roll uses `forGroup`: an admin reroll can shrink a party's board until the next 06:00.
- ⬜ `minestar/dailytasks/DailyTasksCommand.java:73-75` — the info line prints the multiplier as
  `memberCount + 1`, wrong for solo players and custom `teamMultiplier()`s (display only).
- ⬜ `minestar/dailytasks/DailyTaskStatus.java:28-34` — the dirty flag is global: one group's
  completion refreshes badges (mute/warn/letter/LuckPerms/claimable lookups) for every online
  player, rate-limited to 1/s. Mark per group.
- ⬜ `economy/vendingblock/gui/auction/AuctionUI.java:359` — the prefilled minimum bid hardcodes
  increment 1; with a configured `AH_MIN_BID_INCREMENT > 1` it is always rejected. Sync the
  increment.
- ⬜ `economy/vendingblock/network/AuctionActionPacket.java:38`,
  `network/packet/WarpActionPacket.java:80`, `network/packet/HomeActionPacket.java:92` — enum
  ordinals decoded without `Math.floorMod`/bounds (the sibling packets do this deliberately):
  crafted values throw during decode.
- ⬜ `mixin/antidupe/ScreenHandlerMixin.java:22-30` — the SWAP de-alias patches only ordinal 2;
  the identical pattern in the both-non-empty branch is left unpatched.
- ⬜ `mixin/antidupe/TripwireHookBlockMixin.java:40-42` — the reimplementation drops vanilla's
  `getOptionalValue` guards (a vanilla crash fix), regressing to an `IllegalArgumentException`.
- ⬜ `blocks/entity/GrowthChamberBlockEntity.java:136-138,176-181` — output rebuilt as
  `new ItemStack(...)` (drops recipe components); insert check assumes max 64.
- ⬜ `recipe/ItemResearchRecipe.java:121` — `duration` uses `Codec.INT`: a datapack value ≤ 0
  consumes a catalyst every tick. Use `ExtraCodecs.POSITIVE_INT`.
- ⬜ `hydroponics/HydroponicReagents.java` (FERTILIZER branch) — rounding credits up to
  `perBucket/1000 − 1` free units per partial fill (the ACIDITY branch is correct).
- ⬜ `blocks/SulfurBerryBlock.java:85` — `System.out.println` debug on every break (and it
  explodes for creative players).
- ⬜ `netherstorm/StormDropsHandler.java:52-54` — dead null guard (`BuiltInRegistries.ITEM.get`
  returns AIR, never null), and the sulfur drop applies to *every* non-player Nether death (bats,
  farm animals) — very farmable. Restrict to hostiles.
- ⬜ `items/electricItems/EnergySwordItem.java:128-149,176-180` — with `0 < stored <
  energyPerHit` the buffed damage still applies for one free hit. Align the attribute check with
  `stored >= energyPerHit`.
- ⬜ `cosmetics/storage/CosmeticsDatabase.java:29-40` — the javadoc's "differences from
  DatabaseManager" bullets describe behavior the economy class no longer has (both databases now
  reset correctly across world reloads). Update it so nobody "re-fixes" the economy manager.
- ⬜ `client/.../mixin/MoneyShovingMixin.java:42-43` — the currency panel centers on the screen
  instead of using `leftPos`: it detaches from the inventory when the recipe book is open.
- ⬜ `server/.../crash/CrashReporter.java:28,41` — `LAST_REPORT` grows without bound when
  exception messages contain varying data (coordinates, UUIDs). Cap or evict.
- ⬜ `client/.../client/HubButtonHandler.onLoggingOut` — writes directly to public static cache
  fields with inline fully-qualified names (style; the tooltip rebuild itself is properly
  guarded).

---

## 5. Verified clean (worth knowing)

- SQL-level money atomicity: `CurrencyRepository.withdraw`/`transfer` and
  `CurrencyOfferRepository.hold`/`claimOne` use conditional UPDATEs / single transactions — no
  check-then-act at the DB level.
- `DatabaseManager` and `CosmeticsDatabase` both reset correctly across singleplayer world
  reloads; Jdbi closes its resources; cosmetics SQL runs on a serialized per-player chain off the
  server thread.
- Auction double-bids are impossible (server-thread continuations re-check `minNextBid`);
  negative amounts/prices are rejected everywhere that matters.
- Daily tasks: day-turnover math is DST-correct, seeding is restart-stable, double-claim guards
  are sound, all 23 `matches()` implementations and their tags check out.
- Letters double-claim is prevented (recorded before paying); moderation packets re-check
  permissions per action; TPA/RTP/warp cooldown and double-fee lockouts are sound.
- All mixin JSON entries map to existing classes (except `EIArmorMaterialsMixin`, see above);
  the piston/rail/beehive/lectern/book/horse anti-dupe mixins are faithful ports.
- `ItemRegistry` has no duplicate ids (287 registrations scanned); attachment/component codecs
  round-trip cleanly.
