# DimThreads on Minestar

The server ticks every dimension on its own thread at the same time, instead of one after another on the server
thread. With ~17 dimensions (overworld, nether, end, 11 Ad Astra planets and orbits, `ae2:spatial_storage`, three
roll_mod dimensions) a tick then takes about as long as the slowest dimension instead of the sum of all of them.

This is done by **DimThreads**, a fork of SrRapero720's mod that makes the parallel ticking safe for the mods on this
server. The fork lives in `../dimthreads` (branch `minestar/1.21.1-neo`, version 1.3.0); its jar is
`server/jars/dimthread-NEOFORGE-mc1.21.1-v1.3.0.jar`. `roll_mod_server` declares it as a required dependency, so the
dedicated server will not start without it. The common/client jar does not need it.

## Why the fork

From the dev run of 2026-11-07 (`client/run/logs/latest.log`, integrated server, upstream DimThreads 1.2.1):

- **932 chunks were dropped without being saved.** MI's chunk listener throws `Thread is not server thread!` when a
  chunk unloads on a DimThreads thread (`dimthread_server_mars`, `dimthread_server_overworld`). In 1.21.1 the unload
  event fires *before* the chunk is saved, so every such chunk lost its changes since the last autosave (a rollback, so
  dupes and losses), and its block entities and MI pipe nodes were never unloaded. Each one logged
  `ChunkMap: Failed to save chunk [x, z]`.
- **The server crashed** (`crash-2026-11-07_04.35.30-server.txt`): the same throw on a chunk *load* in the Nether
  (natural spawning looked up a structure and loaded a chunk) failed chunk generation.
- **Upstream gave no speedup at all.** Its per-dimension tick ran inside a `static synchronized` method (added by the
  Forge port; the original Fabric mod has no lock), so dimensions ticked one at a time, just on other threads.
- The "Modern Industrialization fix" in upstream's 1.20.1 branch never applied (it targets a package, not a class)
  and would only have removed the check, leaving MI's shared listener map unsynchronized.

The 103-second "stall" earlier in that log is the singleplayer pause screen, not DimThreads.

## How it ticks

- Every server tick, the server thread hands each dimension to a pool thread named `dimthread_server_<dimension>`
  (`dimthread_server_idle` between ticks) and waits until all are done; then the rest of the tick runs as usual
  (connections, players, autosave).
- **Groups.** Dimensions that must not race each other tick on the same thread, one after the other: every pair joined
  by an AE2 Quantum Network Bridge (recomputed every tick), plus the `dimension_groups` config.
- **Owner thread.** While a dimension ticks, its pool thread is its "server thread": vanilla's checks
  (`Level.getBlockEntity`, chunk loading) treat it as such. Between ticks the server thread owns every dimension again.
- **Hand-offs.** Work that would touch another dimension while it ticks on another thread is queued to the server
  thread, which runs it at the end of the same tick (when no dimension is ticking). That is what "at the end of the
  tick" means below.
- **Crash guard.** Each dimension's three tick steps (level-tick pre event, `ServerLevel.tick`, post event) are guarded
  separately. roll_mod_server's `DimThreadsGuard` reports through `CrashReporter` and the dimension carries on with
  its next step, the same as the vanilla-loop guard (see `server-crash-protection.md`).
- `/neoforge tps` shows real per-dimension times again; the server's MSPT is roughly the slowest group plus the
  server-thread work.
- Measured on the dev world (18 dimensions, 4 threads, no players, an MI multiblock and cable network in 7 of them):
  overall tick 1.75 ms with `dimthread_active false`, 0.81 ms with it on; the per-dimension times added up to about
  2.2 ms per tick.

## Settings

| Setting | Where | Default | Meaning |
|---|---|---|---|
| `dimthread_active` | gamerule | `true` | `false` ticks dimensions the vanilla way, on the server thread. The kill switch: no restart needed |
| `dimthread_thread_count` | gamerule | 3 (from the config) | Pool size, 1 to the CPU count. Raise it on the server (e.g. CPU cores − 1); idle dimensions finish in microseconds |
| `default_gamerule_threads` | `config/dimthread-common.toml` | 3 | Initial value of the gamerule for new worlds |
| `ignore_tick_crash` | same | `false` | Only used when no crash guard is registered. roll_mod_server always registers one |
| `dimension_groups` | same | `[]` | Extra groups that always share a thread, e.g. `"minecraft:overworld,ae2:spatial_storage"` |

## Modern Industrialization: what behaves differently

Compared with upstream DimThreads 1.2.1 (what crashed) and with MI on a server without DimThreads. MI 2.4.x only:
2.5 changed some of the patched classes, so DimThreads refuses to start with it until those are checked.

| Area | Upstream DimThreads 1.2.1 | Now | Difference from MI without DimThreads |
|---|---|---|---|
| Chunk load/unload with MI blocks | Throws: chunks dropped unsaved, crash on load | Works | None |
| Multiblock shape checks after a block changes during a tick (explosion, piston, fluid, falling block, another machine) | Silently ignored: the structure stayed formed with missing blocks, output went into destroyed hatches and was lost | Noticed as soon as the change happens; a change made from another dimension's thread is applied at the end of the tick | At most one tick later in that rare cross-dimension case |
| Multiblock listener index (one map for all dimensions) | Unsynchronized | Concurrent | None |
| Pipe registered while its chunk loads | Threw "Can only load pipe on server from the server thread" | Works; a chunk loaded by another dimension's thread registers its pipes at the end of the tick | At most one tick later in that rare case |
| Electric cables | All networks shared one static target list (upstream got away with it by not running in parallel) | Each network tick has its own list | None: no energy can leak into another dimension's machines |
| Item pipes with whitelisted machine slots | One static set for all networks | One set per thread | None |
| Recipe lookup cache (all MI and EI machine types) | Cleared and refilled in place every 20 s | Rebuilt aside and swapped in whole, still every 20 s | None; a machine never sees a half-built cache, and a recipe reload still takes up to 20 s to reach the cache |
| Player statistics, crafted-item stats, FTB Teams sharing | Unsynchronized maps, one static set | One lock | None; totals identical |
| FTB Quests progress from machine output | Applied on the machine's thread | Applied on the server thread | roll_mod already batches it once a second (`QuestSubmitBuffer`), so nothing visible |
| ME wire (MI pipe ↔ AE2) | AE2 grid nodes created from dimension threads | The ME network tick runs on the server thread at the end of the tick | ME wire connections settle at most one tick later |
| AE2 energy P2P through MI | Cross-dimension buffers written from two threads | Dimensions linked by the Quantum Network Bridge tick together | None |
| KubeJS custom process conditions | Could run on several threads at once | One at a time | None |
| Steam drill 3×3 drops | One static list | One list per mining thread | None; under parallel ticking a drill could otherwise pull drops from another dimension |
| `/neoforge tps` | Per-dimension times missing | Real | None |

What does **not** change: recipes, speeds, energy amounts, output, multiblock shapes, machine GUIs. MI's own logic is
untouched; only where shared state lives and which thread runs a piece of work.

## MI addons

- **Extended Industrialization, Tesla.** While DimThreads is on, a Tesla network only delivers inside the tower's own
  dimension: the interdimensional upgrade does nothing, receivers calibrated to a tower in another dimension stay
  unpowered, and handheld receivers only charge players standing in the tower's dimension.
  `/gamerule dimthread_active false` brings the interdimensional behaviour back. Network bookkeeping is now
  thread-safe (it could create two networks for one tower, or crash a tower's tick).
- **Extended Industrialization, electric tools.** Only one electric tool mines its area at a time server-wide (a few
  milliseconds each), and drops from other dimensions are never pulled into the tool.
- **Tesseract API.** The localized-listener index (EI farmer and chainer) is safe across dimensions; a Tesla "is the
  transmitter loaded" check made from another dimension is answered from thread-safe chunk state.
- **MI Tweaks.** Nothing of its own; it gets the MI fixes above.

## Other mods covered

- **AE2 / Advanced AE.**
  - Quantum-Network-Bridge-linked dimensions tick together. A bridge links or unlinks at the end of a tick and the
    grouping follows on the next.
  - Spatial IO transitions run at the end of the tick.
  - Wireless terminals linked to a grid in another dimension already show out of range in AE2: no change.
  - **Advanced AE pick-craft (quantum armor) only reaches a grid in your own dimension**, or one bridged to it.
  - Several internal fixes with no visible effect: per-thread queue timer, thread-safe queues, grid list snapshot,
    multiblock flag, registries, per-dimension crafting calculation lists (two dimensions autocrafting at once could
    corrupt AE2's shared list), grid tick queues.
  - **Consistency.** A grid is only ever touched by one thread at a time, so every insert, extract and crafting step
    runs to completion as in plain AE2. The server thread runs the grid-wide work (pathing, energy, storage, crafting
    CPUs) while no dimension ticks. During the dimension ticks a grid is touched only by the worker of its dimension,
    and a bridged grid's dimensions share one worker (transitively: A–B plus B–C bridges = one worker for all three).
    Bridges link only on the server thread and groups are rebuilt before every tick, so a grid never spans two workers.
  - **Subnetworks** (storage bus, interface or import/export bus to another grid) need touching blocks, so both grids
    are in one dimension and on one worker. If the main network reaches dimension B through a bridge, the storage bus
    still sees the drives in B, read on the shared worker. A subnetwork in another dimension without a bridge has no
    AE2 connection at all. Every bridge takes its dimensions out of parallel ticking.
- **Ad Astra.** A lander, rover or boat falling out of an orbit drops to the planet at the end of the tick (it crashed
  before). Sleeping on a planet skips the night at the end of the tick.
- **Multiblocked2 / LDLib2.**
  - Fixed a hard freeze (MBD2 waited for the server thread on every block change inside a formed structure; the
    watchdog would have killed the server).
  - MBD2 machine menus close when the player changes dimension.
  - Machine KubeJS events run one at a time.
- **FallingTree** (found by the stress test, fixed in roll_mod's soft FallingTree compat, not in DimThreads):
  its log/leaf/wart caches are concurrent maps. TNT going off in three dimensions on the same tick had thrown
  `ConcurrentModificationException` from its leaf-decay listener in all three.

## Vanilla and NeoForge level

- **An open menu closes when its player changes dimension.** Otherwise the next tick syncs it from the new dimension's
  thread while it reads a block in the old one.
- A player teleported from a dimension's tick (by any mod) moves at the end of the tick.
- Fake players, overworld saved data read from other dimensions (maps, MI stats, AE2 player ids, daily tasks) and
  scoreboard updates are locked.
- `% N` timers in non-overworld dimensions that read the overworld clock (roll_mod hydroponics input pass, chunk
  freezer scan, service block) can fire 0 or 2 times in one tick now and then.
- Packets sent from dimension threads are flushed right away (vanilla batches them on the server thread): slightly
  more network writes, no gameplay effect.

## roll_mod changes

- `DimThreadsGuard`: the per-level crash guard while DimThreads runs (above).
- Chunk freezer, `CrashReporter` and MI pipe guard: safe when called from several dimension threads; chat goes out
  from the server thread.
- `QuestSubmitBuffer`: locked (MI machines in all dimensions feed it).
- FallingTree compat: concurrent caches (above).
- Daily tasks: progress reported from a dimension's thread (furnaces, smokers, blast furnaces, cooking pots, breeding,
  kills) is applied on the server thread at the end of the tick. It used to race and could roll a day's progress back.
- GTMOGS stall watchdog: per-dimension heartbeat; dumps name the dimension that never finished its tick.
- MixinExtras 0.5.3 in dev, matching what NeoForge ships in production.

## Rules and limits

- **Mods not listed here were not checked.** If a mod misbehaves only with DimThreads, switch it off with the gamerule
  and look for state shared between dimensions.
- **Cross-dimension work during a tick is the dangerous part.** Code that reads or writes another dimension while it
  ticks should go to the server thread (`server.execute`), or the two dimensions should be grouped.
- **Never block a dimension thread on the server thread** (`server.submit(...).join()`). The server thread is waiting
  for the dimension threads, so that deadlocks. `executeBlocking` is handled; other waits are not.
- **Updating any patched mod** (MI, EI, Tesseract, AE2, Advanced AE, Ad Astra, Multiblocked2, LDLib2) can break a fix.
  DimThreads only accepts the versions it was checked against. Run the dev server with
  `./gradlew :server:runServer -PmixinAudit` after an update: every class DimThreads patches is loaded at startup and
  the log says `Mixin self-check: all 56 DimThreads mixin targets loaded with their mixins applied`, or lists the ones
  that failed. (Outside that flag many of them only load in play, e.g. when the first Tesla tower ticks.)

## Checking it in game

1. Walk into new Nether chunks and visit Mars. The log must show no `Thread is not server thread` and no
   `Failed to save chunk`.
2. Blow up a casing of an MI multiblock in a non-overworld dimension: the machine must stop. Replace the casing: it
   must form again.
3. Run MI cables in two dimensions at once. Run a Tesla tower in its own dimension (works) and across dimensions
   (stays idle while DimThreads is on).
4. ME wire, AE2 cables in two dimensions, a Quantum Network Bridge between two dimensions, a spatial IO cycle.
5. Drop out of an Ad Astra orbit in a lander. Break an MBD2 structure block with an explosion.
6. `/neoforge tps`, then `/gamerule dimthread_active false` and `true` again.
