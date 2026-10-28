# Server crash protection

The dedicated-server jar (`roll_mod_server`) keeps the server alive when something throws during a tick,
instead of letting vanilla write a crash report and shut down. There are three layers, from narrowest to
widest: **chunk freezing**, the **per-level tick guard**, and the **whole-tick guard**. On top of those,
`roll_mod` itself guards Modern Industrialization fluid pipes against broken neighbours.

All of it lives in `server/src/main/java/com/roll_54/roll_mod_server/crash/` and
`.../mixin/crash/`, except the MI pipe guard, which is in `common` (`compat/mi/PipeTransferGuard`,
`mixin/mi/FluidPipe*GuardMixin`).

## Chunk freezing

A frozen chunk stops ticking completely:

| Stopped | How |
|---------|-----|
| Entity ticks | `Level.guardEntityTick` is cancelled for entities whose chunk is frozen |
| Block entity ticks | `ServerLevel.shouldTickBlocksAt` returns `false` |
| Random ticks, snow/ice/precipitation, sculk and vibrations | Same `shouldTickBlocksAt` gate |
| Scheduled block and fluid ticks | `ServerLevel.isPositionTickingWithEntitiesLoaded` (the `LevelTicks` predicate) returns `false` — the ticks **stay queued** and run after the chunk thaws, so redstone and flowing water don't get stuck |
| Natural mob spawning | `ServerLevel.isNaturalSpawningAllowed(ChunkPos)` returns `false` |

Two exceptions keep a frozen chunk clearable: **players always tick**, and **mobs that are already dying
finish dying**, so killing them still removes them. Items are still picked up, because pickup runs on the
player's tick.

### Why a chunk freezes

| Reason | Trigger | Thaws |
|--------|---------|-------|
| `crash` | An entity, block entity (its tick or its `onLoad`), scheduled tick or random tick inside the chunk threw | Only by `/rollmod admin chunkfreeze unfreeze` or a restart — whatever threw will most likely throw again |
| `manual` | Staff ran `/rollmod admin chunkfreeze pause [<player>]` | Only by `/rollmod admin chunkfreeze unfreeze` or a restart |
| `overload` | At least `chunkGuard.entityLimit` (200) non-player entities in the chunk, counted every `scanIntervalTicks` | By itself, once the count drops below `chunkGuard.resumeBelow` (150) |

Block entity `onLoad` needs its own guard. NeoForge calls it for freshly loaded block entities at the top
of `Level.tickBlockEntities`, as `freshBlockEntities.forEach(...)`. Without the guard, one throwing `onLoad`
aborts that loop before the list is cleared, so it is retried every tick, and the block entity tick loop after
it never runs, which means **no block entity in the whole dimension ticks**. This happened with Sophisticated
Backpacks 3.26.7 (`Linked backpack endpoint is not registered in its group` from
`BackpackBlockEntity.onLoad`, a placed backpack whose linked-storage group lost it). Now each `onLoad` runs
on its own, and the one that throws freezes only its chunk.

Reasons rank `overload` < `manual` < `crash`, and a stronger reason replaces a weaker one. Only `overload`
thaws by itself, so pausing an overloaded chunk keeps it frozen after its entity count drops, and so does a
crash in it. Pausing a chunk that is already paused or crashed fails with "already frozen".

### Who is told

| Reason | Chat | Files |
|--------|------|-------|
| `overload` | **Moderators** (everyone `ModerationPermissions.canModerate`), the same channel as the low-TPS alert: *Chunk [x, z @ dim] froze: 220 entities (limit 200) — mostly Zombie ×180. It resumes once they are cleared.* (`msg.roll_mod.moderation.chunk.overload`) | `minestar/chunk_freeze.log` |
| `manual` | Whoever ran the command gets the confirmation; other ops get the usual admin-command echo | `minestar/chunk_freeze.log` (`paused by <name>`) |
| `crash` | **Ops**: `[roll_mod] Froze chunk [x, z @ dim] (crash): <what threw>`, plus the `CrashReporter` line with the trace on hover | `minestar/chunk_freeze.log`, the full trace in the server log |

The chunk coordinates in either message can be clicked; that pre-fills
`/execute in <dimension> run tp @s <x> ~ <z>`. Each chunk is announced once when it freezes, not on every
scan. Resuming is not announced in chat, but it is written to `chunk_freeze.log`, as is an unfreeze by
command:

```
[2026-10-18 14:01:11] froze chunk 0,0 (blocks 8,8) in minecraft:overworld: overload: 220 entities (limit 200)
[2026-10-18 14:01:14] resumed chunk 0,0 (blocks 8,8) in minecraft:overworld: 10 entities
```

Frozen chunks are kept in memory only and are cleared when the server stops.

### Command

Op level 2.

| Command | What it does |
|---------|--------------|
| `/rollmod admin chunkfreeze list` | Every frozen chunk: dimension, chunk coords (clickable), reason, minutes frozen, and what triggered it |
| `/rollmod admin chunkfreeze pause` | Pause the chunk you are standing in |
| `/rollmod admin chunkfreeze pause <player>` | Pause the chunk that online player is standing in. The player keeps ticking (players always do); everything else there stops |
| `/rollmod admin chunkfreeze unfreeze` | Thaw the chunk you are standing in |
| `/rollmod admin chunkfreeze unfreeze <dimension> <chunkX> <chunkZ>` | Thaw a chunk by **chunk** coordinates (block coordinate ÷ 16) |
| `/rollmod admin chunkfreeze unfreezeall` | Thaw everything |

An overload-frozen chunk that is still over the limit refreezes on the next scan — clear the entities
first.

### Config (`chunkGuard` in `roll_mod.json5`)

| Key | Default | Meaning |
|-----|---------|---------|
| `freezeOnCrash` | `true` | Freeze a chunk when something inside it throws. `false` lets the exception through to the tick guards below (old behaviour) |
| `entityLimit` | `200` | Non-player entities in one chunk that freeze it. `0` disables overload freezing |
| `resumeBelow` | `150` | An overload-frozen chunk resumes once its count is below this (capped at `entityLimit`) |
| `scanIntervalTicks` | `20` | How often entity counts are rescanned (20 ticks = 1 second) |

These are plain `int`/`boolean` fields (with `@ValidatedInt.Restrict` ranges) rather than `Validated*`
objects, because `:server` reads them and cannot see fzzy_config's types.

### What it does not cover

Things that tick per level rather than per chunk — MI pipe networks, AE2 grids, anything driven by
`LevelTickEvent` — can't be attributed to a chunk, so they are never frozen. Their exceptions are caught by
the per-level guard instead.

## Tick guards

| Guard | Wraps | When it catches, what is lost |
|-------|-------|-------------------------------|
| `MinecraftServerLevelTickGuardMixin` | In `MinecraftServer.tickChildren`, separately: the level-tick pre-event, `ServerLevel.tick`, the level-tick post-event | Only the rest of that one step for that one level. Other levels, network connections, the player list and chunk sending still tick |
| `InventoryTickGuardMixin` | Each `ItemStack.inventoryTick` call in `Inventory.tick` | Only that one item's tick for that player. The player, the rest of their inventory and the server keep ticking |
| `MinecraftServerCrashGuardMixin` | The whole `tickServer` call in `runServer` | The rest of the tick. Outer fallback for anything thrown outside the calls above |

Both hand the exception to `CrashReporter`. Neither covers worker threads (worldgen, network) or
startup/shutdown.

### Inventory tick guard

Players always tick, even in frozen chunks, so an item in a player's inventory whose `inventoryTick`
throws used to fail the whole player tick: vanilla turns that into "Ticking player" and kicks the player
with *Internal server error* — on every login, so they could never stay on to drop the item. Now only that
item's tick is skipped and the exception goes to `CrashReporter`.

This was added for Sophisticated Backpacks 3.26.7 / Core 1.5.5: a backpack in the inventory whose
linked-storage group no longer exists throws `IllegalStateException: Unknown linked storage group <uuid>`
from `BackpackItem.inventoryTick`. The backpack itself stays broken (its own per-tick features, like
upgrades that run from the inventory, don't run), so the player should move its contents out or staff
should replace it. Only the main inventory, armor and offhand are covered; Curios slots tick through
Curios' own code and are not.

## `CrashReporter`

Every caught exception is grouped by a signature (exception class, message, top stack frame).

- **First time:** full stack trace in the console, and a red chat line to online ops with the trace on hover.
- **Same signature within 10 seconds:** nothing is logged or sent; it is only counted.
- **After the cooldown:** one line — `Server exception repeating (suppressed): <exception> — N more in the last 10s` — and the chat line again.

NeoForge's event bus logs its own trace before rethrowing an exception from an event listener; that one is
outside `CrashReporter` and is not rate-limited.

## MI fluid pipe guard

`FluidPipeDrainGuardMixin` and `FluidPipeFillGuardMixin` wrap the `IFluidHandler.drain`/`fill` calls MI's
`FluidNetwork` makes on neighbouring blocks. If the neighbour throws, the transfer counts as zero and the
pipe network keeps ticking. `PipeTransferGuard` logs the full trace once per handler class and error, then
one line a minute with how many were suppressed (`[MI pipes] drain on … still throwing`).

This was added for Sophisticated Backpacks 3.26.7 / Core 1.5.5: an MI pipe draining a backpack's tank
upgrade throws `IllegalStateException: Cannot encode empty ItemStack` when the backpack belongs to linked
storage with an empty carrier. Before the guard this aborted the level tick on every tick.

The backpack probably removes the fluid before it throws, so a pipe still attached to such a backpack can
void a little fluid each tick it tries. When `[MI pipes]` warnings show up, find the pipe and disconnect it.
