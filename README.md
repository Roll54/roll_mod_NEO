# Roll Mod

A server-suite and content mod for the **ModernTech (Minestar)** Minecraft server.
It bundles a database-backed economy, a daily-task system, a player hub with moderation tools,
custom tech and farming content, and a large set of compatibility patches and anti-dupe fixes
into a single project.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Version | 2.12.0 |
| Author | roll_54 |
| License | All Rights Reserved |

## Module layout

The project is split into three Gradle modules, each producing its own mod jar:

| Module | Mod id | Purpose |
|---|---|---|
| `common` | `roll_mod` | All shared logic, blocks, items, systems, data. Required on both sides. |
| `client` | `roll_mod_client` | Client-only: renderers, screens, keybinds, GeckoLib models, JEI categories, HUD. |
| `server` | `roll_mod_server` | Server-only: chat, PvP rules, shop, autogive, netherstorm/regen commands, crash guard, spawner patches. |

`client` and `server` both depend on `common` (`implementation project(':common')`).

## Features

### Economy (`economy/`)
- **Currency** — multi-currency balances stored in MySQL (HikariCP + JDBI), with offers,
  playtime rewards, `/rollmod money` commands, and FTB Quests reward/task types.
- **Plots** — buying and reselling land claims on top of Open Parties & Claims,
  with escalating resale prices.
- **Vending blocks** — player shops (`VendorBlock` / `DisplayBlock`) with trade GUIs,
  admin tools, and Jade / JEI / REI / EMI / Carry On integration.
- **Auction house** — `/auction` with listings, bids, buyouts, and an unclaimed-items collection.
- Limits and permissions throughout are driven by **LuckPerms** meta
  (see [`docs/luckperms-keys.txt`](docs/luckperms-keys.txt)).

### Minestar server suite (`minestar/`)
Replaces FTB Essentials-style functionality:
- **Daily tasks** — per-player or per-FTB-party daily task boards (23 task types),
  reward pools, paid/free rerolls, toasts, and an in-game UI.
- **Hub** — a tabbed player hub UI: homes, warps, moderation, letters, ranks, quest progress.
- **Moderation** — bans, warnings, TPS monitor, permission tools.
- **Letters** — staff-to-player mail with money, item, or command rewards.
- Plus kits, TPA, RTP, spawn/back teleports, fly/heal perks, an operator store,
  and command logging. Data is stored as JSON under `<gamedir>/minestar/`
  (see [`docs/minestar-files.md`](docs/minestar-files.md)).

### Content
- **Hydroponics** — hydroponic garden beds that merge into node groups with shared
  energy/water buffers, using acidity and fertilizer data maps
  (see [`docs/hydroponic-data-maps.md`](docs/hydroponic-data-maps.md)).
- **Radiation** — radioactive items build up radiation over time; hazmat gear protects,
  milk cures, and six poisoning tiers apply effects.
- **Nether storms** — scheduled storms in the Nether that spawn mob packs, with warning,
  drops, and protective-armor handling.
- **Tech** — energy drill/sword, batteries, prospector pick, storm scanner, rocket & dimension
  cartridges, solar panel, growth chamber, research workbench, telescope, pedestal, and an
  MI-compatible any-tier energy store (1 FE = 1 EU).
- **Blocks** — regenerating resource blocks, crop/weed managers, plushies, ore samples, and more.

### Compatibility & fixes
- **Integrations** (`compat/`): Modern Industrialization, MBD2/LDLib2 machines
  (see [`docs/mbd2-recipes.md`](docs/mbd2-recipes.md)), FTB Teams/Quests, AgriCraft,
  Farmer's Delight, Create Cybernetics, Functional Storage, Ad Astra, KubeJS, CraftTweaker.
- **Mixins** (`mixin/`): ~19 anti-dupe patches (shulkers, hoppers, pistons, rails, tridents,
  lecterns, beehives, and more), anvil level cap, command-block logging, phantom spawner
  control, and machine/armor tweaks.

## Building

```bash
./gradlew build
```

Development runs live under `client/run*` and `server/run*` via ModDevGradle run configs.

### Datagen

```bash
./gradlew :client:runData
```

Generated resources are written to `common/src/generated/resources`.

## Dependencies

Key dependencies are declared in `gradle.properties` and the module `build.gradle` files:
Modern Industrialization (optional), Extended Industrialization, GrandPower (jarJar),
Multiblocked2 + LDLib2, GeckoLib (client), FTB Teams/Quests, fzzy_config, Cloth Config,
GuideME, KubeJS, MixinExtras, LuckPerms API, and HikariCP/JDBI for the economy database.
Some compile-time jars are vendored locally in `common/jars/` and `server/jars/`.

## Documentation

| Document | Contents |
|---|---|
| [`docs/luckperms-keys.txt`](docs/luckperms-keys.txt) | Every LuckPerms permission and meta key the mod reads |
| [`LUCKPERMS_VALUE_PERMISSIONS.md`](LUCKPERMS_VALUE_PERMISSIONS.md) | Tutorial on number-valued LuckPerms meta |
| [`docs/minestar-files.md`](docs/minestar-files.md) | JSON data files under `<gamedir>/minestar/` and `/rollmod reload` |
| [`docs/hydroponic-data-maps.md`](docs/hydroponic-data-maps.md) | Acidity/fertilizer data maps for hydroponics |
| [`docs/mbd2-recipes.md`](docs/mbd2-recipes.md) | Creating MBD2 recipe types and machines with EU |
| [`ORE_GENERATION.md`](ORE_GENERATION.md) | GTMOGS ore-vein JSON format reference |
| [`docs/CODE_REVIEW.md`](docs/CODE_REVIEW.md) | Code review findings (logic & performance) |
