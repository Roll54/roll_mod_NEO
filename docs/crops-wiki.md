# Crops wiki (AgriCraft 4.0.24 + roll_mod)

Pack-maker reference for crops on this server: how AgriCraft actually behaves, every datapack format with its
defaults, and everything roll_mod adds or changes. The upstream wiki is incomplete and partly wrong for 4.x;
everything here was read from the **AgriCraft 4.0.24 (NeoForge 1.21.1) bytecode**
(`common/jars/AgriCraft-neoforge-1.21.1-4.0.24.jar`) and from this repo. 4.0.24 is the "AgriCraft ReReloaded" line
(github.com/Vivoxti/AgriCraft-ReReloaded); behaviour that changed since 4.0.8 is marked *(4.0.24)*. roll_mod requires
4.0.24 or newer.

> **Keep this file current.** Any change to crop mechanics in roll_mod (crop mixins, hydroponic bed, herbicide,
> rakes, weed handling, crop datapacks or data maps, crop machines) or an AgriCraft version bump must update the
> affected sections in the same change. Facts marked *[unverified]* were not confirmed in the bytecode or in game.

Conventions: **S** = the crop's expressed Strength, **max** = a stat's maximum (**20 in roll_mod**, formulas still
use 10 — §1, §11.7). `(int)` means
truncation toward zero. "Growth tick" = one vanilla random tick on the crop block (≈ once per 68 s per block at
`randomTickSpeed` 3).

---

## Contents
1. [Genetics](#1-genetics)
2. [Growth](#2-growth)
3. [Growth conditions](#3-growth-conditions)
4. [Cross-breeding, cloning and mutations](#4-cross-breeding-cloning-and-mutations)
5. [Harvesting, clipping and seed drops](#5-harvesting-clipping-and-seed-drops)
6. [Weeds](#6-weeds)
7. [Fertilizers](#7-fertilizers)
8. [Datapack formats](#8-datapack-formats)
9. [AgriCraft config](#9-agricraft-config)
10. [Information tools](#10-information-tools)
11. [roll_mod additions and changes](#11-roll_mod-additions-and-changes) (gene ceilings: §11.7)
12. [Content shipped by roll_mod](#12-content-shipped-by-roll_mod)
13. [Gotchas and upstream bugs](#13-gotchas-and-upstream-bugs)
14. [Irrigation and greenhouses](#14-irrigation-and-greenhouses)

---

## 1. Genetics

Every planted crop and every seed carries a **genome**: one gene pair (two alleles) for the **species** and one
pair for each of six **stats**. Only one allele of each pair is *expressed* — the **dominant** one
(`AgriGenePair.getTrait`). The other (recessive) allele only matters for breeding.

| Stat | Range | What the expressed value does |
|---|---|---|
| `gain` | 1–20 | Number of harvest rolls: `(gain + 3) / 3` (int) → 1, 1, 2, 2, 2, 3, 3, 3, 4, 4 for 1…10, up to 7 at 20 |
| `growth` | 1–20 | Adds `growth × growth_bonus × growth_multiplier` to the growth chance |
| `strength` | 1–20 | Widens soil and light tolerance; bypasses biome/dimension lists, block conditions and seasons at thresholds |
| `resistance` | 1–20 | Chance a growth tick is spent on weeds instead: 50/40/40/30/30/20/20/10/10/0 % for 1…10, 0 % above |
| `fertility` | 1–20 | Chance a mature neighbour is picked as a breeding parent (§4); higher fertility is preferred |
| `mutativity` | 1–20 | How strongly stats drift when bred (species mutation does **not** use it) |

**roll_mod:** the max is **20** for every stat whatever `<stat>_max` says (`AgriStatMaxMixin`), but the four formulas
that divide by the max — resistance (weed roll), fertility (parent roll), mutativity (drift) and the strength season
bypass — keep using **10**. So 1–10 behave exactly as before; above 10, linear effects (gain rolls, growth bonus,
soil/light tolerance) keep growing and the capped ones stay at their cap (no weeds, always picked, always +1,
seasons ignored). How high a bred stat may climb is limited by gene ceilings, §11.7. `<stat>_min` still applies.

### Dominance
- **Stats:** an allele is dominant when its value is **≥** the other (`GeneStat$IntAllele.isDominant`). So the
  expressed stat is always the **higher** allele. A 7/3 Strength crop behaves as Strength 7.
- **Species** (`GeneSpecies$StringAllele.isDominant`):
  1. identical → dominant;
  2. otherwise the **higher mutation complexity** wins;
  3. equal complexity → the **alphabetically earlier full id** wins (namespace included, so `agricraft:*`
     beats `minecraft:*`).

  Complexity = 0 for a plant that is not the child of any mutation, otherwise
  `min over its mutations of (complexity(parent1) + complexity(parent2) + 1)`, counting only mutations whose child
  and both parents exist as plants *(4.0.24)*. It is computed once and cached (not refreshed by `/reload`). A freshly mutated child therefore usually outranks its parents and is expressed.

Seeds converted from vanilla items (planting a vanilla seed, or putting it in the analyzer) get every stat at
its minimum (1/1).

---

## 2. Growth

`CropBlock.randomTick` → `plant.onRandomTick` (modifiers) → `CropBlockEntity.applyGrowthTick()`:

1. **Weed roll** (`shouldWeedsActivate`, §6). If it hits, the whole tick goes to weeds and the plant does not grow.
2. Else, on **cross crop sticks** → cross-breeding tick (§4).
3. Else, with a plant → evaluate all growth conditions (§3) and act on the **worst** response:

| Response | Effect on this tick |
|---|---|
| `FERTILE` | growth roll (below) |
| `INFERTILE` | nothing — the plant stalls, no regression |
| `LETHAL` | plant goes back one stage; at stage 0 it dies |
| `KILL_IT_WITH_FIRE` | plant dies immediately (smoke + sound) |

Priority: KILL > LETHAL > INFERTILE > FERTILE. A plant also counts as INFERTILE if it lacks headroom: plants taller
than 16 px need air in each block above (one per 16 px of `stages` height).

**Growth roll** (`calculateGrowthRate`): the plant advances one stage when

```
soil   = soil.growth_modifier (1.0 without soil) × greenhouse.growth_modifier (1.05, only inside a greenhouse §14)
rate   = soil × ( plant.growth_chance + growth × plant.growth_bonus × growth_multiplier )
rate > random[0,1)
```

- `growth_multiplier` (config, default 1.0) scales **only** the bonus part; the greenhouse factor *(4.0.24)*
  scales the whole rate.
- `rate ≥ 1` means guaranteed growth every non-weed tick.
- Example: wheat (0.75 / 0.025) on farmland (1.0): 0.775 at Growth 1, 1.0 at Growth 10.
- Wooden crop sticks in lava are destroyed on random tick.
- **Extra growth ticks:** a sprinkler above gives the crop extra full growth ticks — capped by roll_mod at about the
  vanilla random-tick rate, i.e. ≈ 2× growth (§14, §11.5).
- **Create harvester** *(4.0.24, with Create)*: harvests mature crops normally and resets their weed; an
  **immature crop loses one stage** (and so does its weed).

Stages: the plant is mature (`isFinal`) at index `stages.length - 1`.

---

## 3. Growth conditions

Checked in this order: humidity, acidity, nutrients, light, block, biome, dimension, season, fluid
(`AgriGrowthConditionRegistry`). All use the expressed **Strength S**.

### Soil (humidity / acidity / nutrients)
The soil is the block **directly below** the crop, matched against `soils/*.json` variants. No soil → INVALID →
INFERTILE.

```
k = (int)(tolerance_factor × S)
equal:            value-k ≤ soil ≤ value+k
equal_or_lower:   0       ≤ soil ≤ value+k
equal_or_higher:  value-k ≤ soil
```
comparing enum ordinals (§8.9). Example, wheat humidity `wet` (3) tolerance 0.15: k = 0 for S 1–6 (only WET),
k = 1 for S 7–10 (DAMP…WATERY).

### Light
```
[min_light - (int)(light_tolerance_factor × S) ,  max_light + (int)(light_tolerance_factor × S)]
```
against `getMaxLocalRawBrightness` (max of block light and sky light minus sky darkening → **depends on time of
day**). Outside → INFERTILE. Bundled plants use `max_light: 16`.

### Block conditions
Checks the block **two below the crop** (under the soil). Entries with `strength ≤ S` are skipped. The **first
non-skipped entry decides**: FERTILE if the block (and all listed states) match, else INFERTILE — a list is *not*
an OR. No entries (or all skipped) → FERTILE.

### Biomes / dimensions
If `S ≥ ignore_from_strength` (default **11**, i.e. never with max 10) → FERTILE. Otherwise blacklist: INFERTILE
when listed; whitelist: INFERTILE when not listed. **IDs only, no tags.** An explicit empty whitelist blocks growth
everywhere. Omitted field → no restriction.

### Seasons
Only with a season mod (Serene Seasons plugin). FERTILE if seasons are inactive, the list is empty, `S ≥ 10`
(strength max), the crop is inside a greenhouse with `ignores_seasons` *(4.0.24, §14)*, or the current season is
listed; else INFERTILE. Omitted → all four seasons.

### Mystical Agriculture farmland *(4.0.24, only with Mystical Agriculture)*
A 10th condition for `mysticalagriculture:*` plants (not inferium) when MA compat and MA's "requires effective
farmland" option are on: INFERTILE unless the block directly below is effective farmland for the crop's tier. It
reads the block itself, not the AgriCraft soil, so MA crops are infertile on a Hydroponic Garden Bed.

### Failure hints *(4.0.24)*
Tooltips now say which way a failed soil value must move ("too dry … needs more moisture") and show a riddle line for
every non-skipped block condition, even though only the first one decides.

### Fluid (at the crop's own position)
Not affected by Strength. With the default (`minecraft:empty`):
- no fluid → FERTILE; **waterlogged → LETHAL** (regresses every growth tick); lava → FERTILE (quirk below).

General rule, with `F` = resolved fluid list:
- `F` empty (e.g. empty tag): lava → KILL_IT_WITH_FIRE, none → FERTILE, other → LETHAL.
- fluid in `F` → FERTILE (LETHAL if listed `states` don't match).
- fluid not in `F` → **lava source FERTILE** (looks like an upstream bug), anything else LETHAL.

---

## 4. Cross-breeding, cloning and mutations

Only **cross crop sticks** (double sticks) receive new plants; single empty sticks never do.

**When:** each growth tick on cross sticks *after* the weed roll (cross sticks have no plant → 50 % of ticks go to
weeds when weeds are on). Upstream also runs it once per fertilizer potency step; **roll_mod removes that** — see
§11.5, fertilizer can't force spreading.

**Parents** (`selectAndSortCandidates`): the 4 horizontal neighbours that have a plant, are **mature**, and (if
`only_fertile_crops_spread`) are FERTILE. Sorted by Fertility (highest first); each must pass the fertility roll
*(4.0.24)*:
```
boosted = min(max, round(F × mutation_chance_multiplier))      (config, default 1.2)
picked if nextInt(max) < boosted   → F1…F10: 10, 20, 40, 50, 60, 70, 80, 100, 100, 100 %
```
At multiplier 1.0 this is the old `F/10`. The multiplier does **not** scale mutation JSON `chance`.

| Surviving parents | Result |
|---|---|
| 0 | nothing |
| 1 | **clone**: needs `cloneable` and `spread_chance > random`. Exact genome copy (or mutated per gene if `clone_mutations`) |
| ≥ 2 | **combine** the two with highest fertility — always succeeds, `spread_chance` not used |

The child replaces the cross sticks with single planted sticks. Whether it can grow there is not checked, and
existing weeds are kept. With `compat.mysticalagriculture_cloning` *(4.0.24, default off)* a lone
`mysticalagriculture:*` parent may clone even when not `cloneable`. A breeding provider registered by another mod
(`ExternalCrossBreedingSupport`, only the Starfield compat uses it) can take over the whole breeding tick.

### Species
1. Take a random allele (50/50 dominant/recessive) from each parent: A and B.
2. Find mutations whose parents are `{A, B}` in either order (skipping mutations whose child or parents don't
   exist as plants, *4.0.24*); pick one at random.
3. If `mutation.chance > random`, the pair becomes (child, random of A/B); else (A, B).

**Mutativity does not affect species mutation** — only the mutation JSON `chance` does.

**roll_mod:** the species roll only happens when **both parents have ≥ 1000 mutagen**; otherwise the pair stays
(A, B). A child produced by a mutation gets **default genes**: both species alleles the new plant, every stat at its
minimum (1/1). See §11.7.

### Stats (`DefaultStatMutator`)
For each stat: allele₁ from parent1 (random dom/rec) drifts using parent1's expressed Mutativity M₁; allele₂
likewise from parent2.
```
changes        with P = (max + M) / (2·max)        → M1 55 %, M5 75 %, M10 100 %
if it changes: +1 with P = floor((max + M) / 2) / max, else -1
result clamped to [stat_min, stat_max]
```
Net per allele: M1 → +1 27.5 % / ±0 45 % / −1 27.5 %; M5 → 52.5 / 25 / 22.5 %; M10 (and above) → always +1.
Mutativity is itself a stat and drifts the same way. The expressed value is the higher allele.

**roll_mod:** every allele is then capped at `max(gene ceiling, highest allele either parent has for that stat)` —
the ceiling is 5 by default, see §11.7.

---

## 5. Harvesting, clipping and seed drops

- **Right-click harvest** (mature only): `products` rolled `(gain + 3) / 3` times; each roll checks **every**
  product independently (`chance > random`, amount uniform in `[min, max]`). Plant resets to `harvest_stage`.
  No seed.
- **Breaking the crop:** sticks (2 for cross), products if mature, plus **one seed with the full genome** if a
  plant is present and (mature or not `only_mature_seed_drops`).
- **Clipper** *(changed in 4.0.24)*: a clipper click always goes to the clipper and never harvests. It clips only a
  **mature plant with non-empty `clip_products`** — drops them (chance + `[min, max]`, **Gain not applied**) and
  resets the plant to **stage 0**, not `harvest_stage`. Immature or no clip products → refused with a chat message.
  (4.0.8 harvested instead when the plant was mature.) All roll_mod plants and our vanilla overrides have clip
  products (§12).
- **Grass seeds** *(4.0.24)*: loot modifier `agricraft:grass` on short/tall grass (not with shears): clears the
  vanilla drops (when `allow_grass_drop_resets`), then with chance 0.2 drops a wheat/carrot/potato AgriCraft seed
  with stats 1 — about 7.3 / 6.7 / 6 % per break (the weight pick is off by one). Format in §8.10.
- Seed `grass_drop_chance`, `seed_drop_chance`, `seed_drop_bonus` are still read from JSON but **never used**.
- **Automation** *(4.0.24)*: Industrial Foregoing plant gatherer, PneumaticCraft drones and the Create harvester
  harvest mature crops through `AgriApi.harvest` (normal harvest).

---

## 6. Weeds

Weeds act only on weed ticks (`shouldWeedsActivate`):
- with a plant: `nextInt(max) ≥ (max + Resistance) / 2` → 50 % at Res 1 down to 0 % at Res 10;
- empty / cross sticks: 50 %;
- *(4.0.24)* then multiplied by the crop sticks' material: wooden ×1, **iron ×0.7, obsidian ×0.4** (applies to the
  weed tick, the lethal second roll and a neighbour's spread roll). On cross sticks this leaves 50 / 65 / 80 % of
  ticks for breeding.

On a weed tick:
- **No weed:** the first weed in the registry with `spawn_chance > random` appears at stage 0. Default weed:
  0.5 × 0.25 = **12.5 %** per random tick on empty sticks or a Resistance-1 plant.
- **Growing weed:** +1 stage with `soil.growth_modifier × weed.growth_chance`.
- **Mature weed:**
  - if `lethal` and `mature_weeds_kill_plants`, and a *second* weed roll passes → the plant drops a stage (dies at
    stage 0). Effective chance per tick ≈ P(weed tick)².
  - if `aggressive` and `weeds_spreading` → each horizontal neighbour crop without weeds that passes its own weed
    roll gets the weed at stage 0 (empty sticks included).

A weed's own `requirement` block is decoded but **never checked**. `disable_weeds` turns all of this off.

**Rakes** (`RakeItem.rakeCrop`): wooden = −1 weed stage (removed at 0), iron = removed; drops `rake_products` scaled
by stage (default weed has none). *(4.0.24)* clicking a weedy crop with a rake is handled by the crop block on the
server; a rake on a crop without weeds falls through to the normal click (harvests a mature crop). roll_mod adds
biomass — §11.2.

---

## 7. Fertilizers

Click a crop with a registered fertilizer item (`fertilizers/*.json`; the only bundled one is bone meal):
- Accepted on: a plant that is not mature and not in `neutral_on`; empty sticks only if weeds from fertilizer are
  allowed (`disable_fertilizer_weeds` false) and `trigger_weeds`. Otherwise the click is consumed but the item is
  not.
- **Cross crop sticks never accept fertilizer in roll_mod** (§11.5). Upstream accepts it there when
  `allow_fertilizer_mutation` and `trigger_mutation` are set, which lets bone meal force neighbours to spread;
  `trigger_mutation` and `allow_fertilizer_mutation` therefore have no effect here.
- The effect runs `potency` times:
  - plant in `negative_on` with `reduce_growth`: 50 % per run to lose a stage; at stage 0 removed if `kill_plant`;
  - otherwise a **fertilizer growth tick**: with `disable_fertilizer_weeds = true` (default) that is a
    **guaranteed +1 stage** if the plant is fertile (no chance roll, no weed roll). On cross sticks it does
    nothing in roll_mod (upstream: a breeding tick).
- Bundled bone meal: potency 1, mutation and weeds on, reduce/kill on, `neutral_on` = all AgriCraft resource crops
  and all Mystical Agriculture crops (bone meal does nothing to those).
- Dispensers use the vanilla bonemeal path (needs plant + fertile + not mature). Sneak-right-click with bone meal on
  a crop is blocked.

---

## 8. Datapack formats

All five are datapack registries (same codec for load and sync). Entry id = `<namespace>:<file name>`.

| Registry | Folder |
|---|---|
| plants | `data/<ns>/agricraft/plants/<id>.json` |
| weeds | `data/<ns>/agricraft/weeds/<id>.json` |
| soils | `data/<ns>/agricraft/soils/<id>.json` |
| mutations | `data/<ns>/agricraft/mutations/<id>.json` |
| fertilizers | `data/<ns>/agricraft/fertilizers/<id>.json` |

Plant tags (for fertilizer `neutral_on`/`negative_on`): `data/<ns>/tags/agricraft/plants/<tag>.json`
*[folder inferred from the 1.21 convention]*.

**TagOrElement** fields accept `"ns:id"` or `"#ns:tag"`. Unknown ids silently become the registry default
(`minecraft:air` / `minecraft:empty`); unknown tags resolve to an empty list.

"Required" = decode fails without it. Several required fields are **unused** at runtime (marked).

### 8.1 Plant
| Field | Type | Req | Default | Notes |
|---|---|---|---|---|
| `mods` | string list | yes | – | unused |
| `seeds` | AgriSeed list | yes | – | items that are this plant's seed; `[]` allowed |
| `stages` | int list | yes | – | one entry per stage = height in px (>16 needs headroom) |
| `harvest_stage` | int | yes | – | stage index after right-click harvest |
| `growth_chance` | double | yes | – | base growth chance (§2) |
| `growth_bonus` | double | yes | – | per Growth point (§2) |
| `cloneable` | bool | yes | – | allows single-parent cloning |
| `spread_chance` | double | yes | – | clone chance (§4) |
| `fireproof` | bool | no | `false` | seed item entities immune to fire |
| `products` | AgriProduct list | no | `[]` | harvest drops |
| `clip_products` | AgriProduct list | no | `[]` | clipper drops; empty = plant can't be clipped *(4.0.24)* |
| `requirement` | Requirement | yes | – | §8.3 |
| `modifiers` | list of `{id, value}` | no | `[]` | §8.2 |
| `particle_effects` | list | no | `[]` | §8.2 |
| `weeds_resistance` | bool | no | `false` | **roll_mod addition**, §11.1 |

**AgriSeed**
| Field | Type | Req | Default | Notes |
|---|---|---|---|---|
| `item` | item TagOrElement | yes | – | |
| `override_planting` | bool | yes | – | right-clicking this vanilla seed plants an AgriCraft crop (with `override_vanilla_farming`) |
| `nbt` | compound | no | `{}` | if set, every key must equal the stack's `custom_data` |
| `grass_drop_chance` | double | yes | – | **unused** |
| `seed_drop_chance` | double | yes | – | **unused** |
| `seed_drop_bonus` | double | yes | – | **unused** |

**AgriProduct** (also used for weed `rake_products`)
| Field | Type | Req | Notes |
|---|---|---|---|
| `item` | item TagOrElement | yes | a tag drops one random member per drop |
| `min`, `max` | int | yes | count uniform in `[min, max]` |
| `chance` | double | yes | drops if `chance > random` |
| `required` | bool | yes | **unused** |
| `nbt` | compound | no | **has no effect** (upstream bug, §13) |

### 8.2 Plant modifiers and particles
`modifiers`: `{ "id": "...", "value": "" }`. Built-in ids: `agricraft:brightness`, `burn`, `bushy`, `experience`,
`poison`, `redstone`, `thorns`, `wither` (no value); `agricraft:fungus` (value = a `FungusBlock` id),
`agricraft:tree` (value = a `BonemealableBlock` id), `agricraft:summon` (value = entity type id). Unknown ids or
invalid values are dropped silently.

`particle_effects` entry: `particle` (simple particle id), `delta_x`, `delta_y`, `delta_z` (double), `probability`
(3 attempts per call), `stages` (int list of stage indices) — all required.

### 8.3 Requirement
| Field | Type | Req | Default |
|---|---|---|---|
| `soil_humidity` | SoilCondition | yes | – |
| `soil_acidity` | SoilCondition | yes | – |
| `soil_nutrients` | SoilCondition | yes | – |
| `min_light` | int | yes | – |
| `max_light` | int | yes | – |
| `light_tolerance_factor` | double | yes | – |
| `biomes` | ListCondition | no | no restriction |
| `dimensions` | ListCondition | no | no restriction |
| `seasons` | string list | no | all four |
| `block_conditions` | BlockCondition list | no | `[]` |
| `fluid_condition` | FluidCondition | no | `{ "fluid": "minecraft:empty" }` |

- **SoilCondition:** `value` (enum §8.9), `type` (`equal` / `equal_or_lower` / `equal_or_higher`),
  `tolerance_factor` (double) — all required.
- **ListCondition:** `values` (id list, required), `blacklist` (bool, required), `ignore_from_strength`
  (int, optional, default **11**).
- **BlockCondition:** `block` (block TagOrElement, required), `states` (`"property=value"` list, optional, all must
  match), `strength` (int, required; skipped when `S ≥ strength`).
- **FluidCondition:** `fluid` (fluid TagOrElement, required), `states` (optional).
- **Seasons:** `spring`, `summer`, `autumn`/`fall`, `winter`, `any`/`all` (case-insensitive).

```json
"requirement": {
  "soil_humidity":  { "value": "wet",             "type": "equal",           "tolerance_factor": 0.15 },
  "soil_acidity":   { "value": "slightly_acidic", "type": "equal",           "tolerance_factor": 0.2 },
  "soil_nutrients": { "value": "high",            "type": "equal_or_higher", "tolerance_factor": 0.1 },
  "min_light": 10, "max_light": 16, "light_tolerance_factor": 0.5,
  "biomes":     { "values": ["minecraft:desert"], "blacklist": false, "ignore_from_strength": 8 },
  "dimensions": { "values": ["minecraft:the_nether"], "blacklist": true },
  "seasons": ["summer", "autumn"],
  "block_conditions": [ { "block": "#c:ores/iron", "strength": 11 } ],
  "fluid_condition": { "fluid": "minecraft:water" }
}
```

### 8.4 Soil
| Field | Type | Req | Notes |
|---|---|---|---|
| `mods` | string list | yes | unused |
| `variants` | list of `{ block, states? }` | yes | block TagOrElement + optional state list |
| `humidity` | enum string | yes | plain string, e.g. `"wet"` |
| `acidity` | enum string | yes | |
| `nutrients` | enum string | yes | |
| `growth_modifier` | double | yes | multiplies plant and weed growth |

```json
{ "mods": [], "variants": [ { "block": "minecraft:farmland" } ],
  "humidity": "wet", "acidity": "slightly_acidic", "nutrients": "high", "growth_modifier": 1.0 }
```

### 8.5 Mutation
Exactly four required fields, **no conditions**; parents match in either order. *(4.0.24)* a mutation whose child or
parents don't exist as plants is ignored.
```json
{ "child": "minecraft:carrot", "parent1": "minecraft:wheat", "parent2": "minecraft:potato", "chance": 0.75 }
```

### 8.6 Weed
`mods` (unused), `stages`, `spawn_chance`, `growth_chance`, `aggressive`, `lethal`, `requirement` (decoded,
**never checked**) — all required; `rake_products` optional (`[]`). Bundled: `agricraft:weed` (spawn 0.25,
growth 0.9, aggressive, lethal, no rake products).

### 8.7 Fertilizer
All required: `mods` (unused), `variants` (`{ item, nbt? }`), `trigger_mutation`, `trigger_weeds`, `potency`,
`reduce_growth`, `kill_plant`, `neutral_on`, `negative_on` (plant ids or `#plant tags`), `particles`
(`{ particle, delta_x, delta_y, delta_z, amount, when: ["positive"|"negative"|"neutral"] }`). Behaviour in §7.

### 8.8 Bundled AgriCraft content
- Vanilla/AgriCraft plants, soils, mutations and bone meal live in the jar's `data/` and always load (unchanged
  between 4.0.8 and 4.0.24).
- `datapacks/<modid>` (data) and `resourcepacks/<modid>` (crop/seed models) in the jar are added as built-in packs
  `builtin/agricraft_{datapacks,resourcepacks}_<modid>` at the **top**, above mod data, when that mod is loaded and its
  compat switch is on (only `mysticalagriculture` and `stardewcraft` have one). With `register_packs_by_default`
  they are *required* and can't be disabled per world. 4.0.24 packs:

  | Pack | Plants | Mutations | Soils | Fertilizers |
  |---|---|---|---|---|
  | biomesoplenty | 11 | 11 | – | – |
  | croptopia | 58 | – | – | – |
  | farmersdelight (**blocked by roll_mod**, §11.5) | 4 | 4 | 2 | – |
  | immersiveengineering | 1 | 1 | – | – |
  | mysticalagriculture | 136 | – | 5 | 1 (`mystical_fertilizer`, potency 5) |
  | pamhc2crops | 97 | 97 | – | – |

  Because they sit above mod data, a mod can't override their files by shipping the same ids.
- Tags *(4.0.24)*: now in the 1.21 folders (`tags/block`, `tags/item`) — in 4.0.8 they sat in legacy folders and
  were empty, so `#agricraft:ores/*` block conditions and `minecraft:maintains_farmland` (farmland under any
  AgriCraft crop no longer dries out) only work since 4.0.24. New: `agricraft:greenhouse_glass` (glass, tinted
  glass, ice, `#c:glass_blocks`, `#c:glass_panes`), `agricraft:grate` in `minecraft:climbable`.
- Bundled soil `growth_modifier`: farmland/clay/soul soil 1.0, podzol 1.1, mycelium/sand/soul sand 0.8,
  crimson/warped nylium 0.75, gravel 0.5.

### 8.9 Soil enum values (ordinal order)
Case-insensitive; synonyms accepted on read, written as the lowercase name.

| # | Humidity | Acidity | Nutrients |
|---|---|---|---|
| 0 | `arid` | `highly_acidic` | `none` (`zero`, `empty`) |
| 1 | `dry` | `acidic` | `very_low` (`scarce`, `poor`) |
| 2 | `damp` (`moist`) | `slightly_acidic` (`standard`, `default`) | `low` |
| 3 | `wet` (`standard`, `default`) | `neutral` | `medium` (`normal`, `average`) |
| 4 | `watery` | `slightly_alkaline` | `high` (`standard`, `default`) |
| 5 | `flooded` | `alkaline` | `very_high` (`rich`) |
| 6 | – | `highly_alkaline` | – |

Acidity also accepts pH numbers: 0–2 highly acidic, 3–5 acidic, 6 slightly acidic, 7 neutral, 8 slightly alkaline,
9–11 alkaline, 12–14 highly alkaline.

### 8.10 Grass loot modifier *(4.0.24)*
NeoForge global loot modifier type `agricraft:grass` (listed in `data/neoforge/loot_modifiers/global_loot_modifiers.json`):

| Field | Type | Notes |
|---|---|---|
| `conditions` | loot conditions | bundled: short/tall grass, tool not `#c:tools/shear` |
| `reset` | bool | clear the vanilla drops first (only if config `allow_grass_drop_resets`) |
| `chance` | double | chance to drop one seed when the loot is empty |
| `seeds` | list of `{ plant, min_stat, max_stat, weight }` | all stats of the seed get both alleles = random in `[min_stat, max_stat]` |

Bundled `agricraft:short_grass_drops` / `tall_grass_drops`: reset true, chance 0.2, wheat/carrot/potato weight 10,
stats 1.

---

## 9. AgriCraft config

`config/agricraft.jsonc` (ResourcefulConfig). Our run and server copies use the defaults.

| Option | Default | Effect |
|---|---|---|
| `register_packs_by_default` | true | enables bundled compat packs (§8.8) |
| `crop_sticks_prevent_trampling` | true | no trampling under sticks; sticks survive on any sturdy block |
| `plant_off_crop_sticks` | true | allows planting directly on soil |
| `crop_sticks_collide` | true | sticks have collision |
| `only_fertile_crops_spread` | false | breeding parents must be FERTILE |
| `allow_fertilizer_mutation` | true | fertilizer can run a breeding tick on cross sticks — **overridden by roll_mod (never)** |
| `clone_mutations` | false | cloning drifts genes instead of copying |
| `override_vanilla_farming` | true | vanilla seeds with `override_planting` plant AgriCraft crops |
| `converts_seed_only_in_analyzer` | false | disables in-world vanilla-seed conversion |
| `growth_multiplier` | 1.0 (0–3) | scales only the Growth-bonus part |
| `mutation_chance_multiplier` *(4.0.24)* | 1.2 (0–5) | scales the fertility parent roll (§4), not mutation chances |
| `allow_grass_drop_resets` *(4.0.24)* | true | grass loot modifier may clear vanilla grass drops (§5) |
| `only_mature_seed_drops` | false | seed drops only from mature crops |
| `disable_weeds` | false | turns weeds off |
| `mature_weeds_kill_plants` | true | lethal mature weeds regress plants |
| `weeds_spreading` | true | aggressive weeds spread |
| `weeds_destroy_crop_sticks` | false | **unused** |
| `disable_fertilizer_weeds` | true | fertilizer = guaranteed +1 stage, no weed roll, rejected on empty sticks |
| `raking_drops_items` | true | rakes drop `rake_products` |
| `seed_compost_value` | 0.3 | composter value of seeds (since 4.0.24 also written to the `compostables` data map on reload) |
| `seed_bag_capactity` | 64 | seed bag size (typo is upstream's) |
| `seed_bag_enchant_cost` | 10 | |
| `<stat>_min` / `<stat>_max` / `<stat>_hidden` | 1 / 10 / false | per stat; hidden stats are omitted from tooltips, magnifying glass and Jade, but not the seed analyzer. **`_max` is ignored by roll_mod (always 20, §1)** |
| compat `mysticalagriculture` | true | MA effective-farmland condition, tints, MA compat pack, MA seed conversion (functional since 4.0.24) |
| compat `mysticalagriculture_cloning` *(4.0.24)* | false | lone MA parents clone even if not cloneable |
| compat `pneumaticcraft`, `industrialforegoing` | true | drone / plant-gatherer harvest handlers (functional since 4.0.24) |
| compat `starfield` *(4.0.24)* | true | Starfield Pastoral (`stardewcraft`) integration |
| `irrigation.*`, `greenhouse.*` *(4.0.24)* | | see §14 |

roll_mod has **no crop options of its own** in `MyConfig` and no crop LuckPerms keys; its crop numbers are
constants in `CropBreedingSettings` (§11.7).

---

## 10. Information tools

- **Seed tooltip:** always shows expressed non-hidden stats; species id only with F3+H. There is no "analyzed"
  flag — nothing is locked behind the analyzer.
- **Seed analyzer:** dominant – recessive for species and all stats. Converts vanilla seeds (min stats).
  *(4.0.24)* server-side insertion, respects claims (`mayBuild`/`mayInteract`), 8-block reach.
- **Magnifying glass** (either hand, or a helmet crafted with one): species pair, stats, stage (sneak),
  fertility status with the list of failed conditions, weed and its stage, soil properties when looking at soil.
  roll_mod adds herbicide, mutagen, N/P/K, growth-boost, gene-ceiling and mutation lines (§11.7) and the Crop
  Analyzer cyberware (§11.6).
- **Jade / WTHIT:** growth %, and with shift: species, stats, fertility, failed conditions, weeds. **The One Probe**
  *(4.0.24)*: growth %, extended: species, stats, weed.
- **Journal:** per-plant pages computed at Strength 1; research now persists *(fixed in 4.0.24)*. New **Soils** pages
  list every registered soil with its humidity/acidity/nutrients and growth modifier (no research needed; the
  Hydroponic Garden Bed isn't listed).
- **Greenhouse Monitor** *(4.0.24)*: §14.
- Since 4.0.24 the magnifying-glass overlay re-wraps lines as plain text, so coloured lines (e.g. our Herbicide line)
  lose their styling.

---

## 11. roll_mod additions and changes

### 11.1 `weeds_resistance` plant field
`AgriPlantMixin` wraps `AgriPlant.CODEC`, adding an optional boolean **`weeds_resistance`** (default `false`) to
plant JSON. When `true`, weeds can never appear on or spread onto that crop (`setWeed`/`spreadWeeds` are cancelled
in `CropBlockEntityMixin`), and the Weed Manager / Crop Managers skip it.

### 11.2 Herbicide and rakes
- **Crop NBT `herbicideAmount`** (int ≥ 0, saved with the crop; `CropBlockEntityMixin`). While > 0, weeds cannot
  appear or spread, and each weed tick burns herbicide instead of growing weeds:
  `burn = random(1..5) × max(1, 11 − Resistance)` — higher Resistance makes herbicide last longer.
- **Herbicide items** (`HerbicideItem`, tag `roll_mod:herbicides`): right-click a crop to add the item's
  `roll_mod:herbicide` component value to `herbicideAmount`, remove any weed (dropping biomass) and consume one
  item.

  | Item | Amount |
  |---|---|
  | `roll_mod:herbicide` | 1 (no component) |
  | `roll_mod:herbicide_tier_1` | 200 |
  | `roll_mod:herbicide_tier_2` | 1000 |
  | `roll_mod:herbicide_tier_3` | 4000 |
- **Rakes** (`RakeItemMixin`): AgriCraft's `rakeCrop` does the raking (§6); roll_mod adds **`roll_mod:biomass`** when
  a weed is fully removed: `ceil(growth% × 6)`, clamped 1–6 (`HerbicideHelper`), server-side, on every rake path.
  The item path (`RakeItem.useOn`: sneaking, off hand, automation) rakes on the server only — upstream still rakes
  there on both sides and the weed comes back after the client's predicted removal.
- Magnifying-glass tooltip shows `Herbicide: N` (`message.roll_mod.magnifying.herbicide`) on every planted crop and
  cross sticks.
- **Liquid herbicide** (`roll_mod:herbicide_tier_1/2/3`, fluid tag `roll_mod:herbicides`): one fluid per item tier,
  **1000 mB = one item** of that tier (0.2 / 1 / 4 herbicide per mB). Vanilla water tinted with the tier's colour
  (0x44ff8e / 0xff4b41 / 0x632cff); buckets are NeoForge dynamic buckets. Used by the Crop Managers' herbicide tank.
  Like every roll_mod fluid (`FluidRegistry`, registered MI-style from one colour), the fluid, its block and its
  bucket are named in that colour.
- Herbicide also gates gene ceilings: ≥ 1000 on both parents when they breed (§11.7).

### 11.3 Machines
| Machine | Area | Work | Energy | Notes |
|---|---|---|---|---|
| Weed Manager `roll_mod:weed_manager` | 5×5, own Y | every 10 ticks | 100k FE buffer, 1000 FE/t in, 500 FE per removal | herbicide only, 1 biomass per removal; skips crops whose herbicide exceeds 2× the stack value |
| Crop Manager `roll_mod:crop_manager` | 11×11, own Y | every tick | 100k buffer, 500 per op | **deprecated**, hidden from tabs; de-weeds and harvests |
| MBD2 Crop Manager LV / MV (`crop_manager_mk2`) / HV | 3×3 / 5×5 / 7×7 behind the machine | every 20 ticks (`tick_interval`) | 50k / 100k / 200k buffer, 500 / 1000 / 2000 per t | trait `crop_harvester`; GUI toggles collect, herbicide, void biomass, show area (+ feed N/P/K on MV/HV, + apply mutagen on HV); 1 biomass per weed |

MBD2 trait fields and slots: see `docs/mbd2-recipes.md`. All managers respect `weeds_resistance`.

Crop Manager inputs per tier (each scan, in this order per crop: de-weed, feed, mutagen, harvest):

| Input | LV | MV | HV | Notes |
|---|---|---|---|---|
| Herbicide item slot | ✔ | ✔ | ✔ | `#roll_mod:herbicides` items, one per weeded crop |
| Herbicide tank (16 B) | ✔ | ✔ | ✔ | `#roll_mod:herbicides` fluids, used when the slot is empty: 1000 mB per weeded crop |
| N/P/K item slot + tank (16 B) | – | ✔ | ✔ | anything with a `roll_mod:npk` value; filled into the machine's own N/P/K buffer (1000 each), then each planted crop is topped up to 100 N/P/K |
| Mutagen tank (32 B) | – | – | ✔ | `#roll_mod:mutagens`; tops each planted crop up to 4000 mutagen, 1 mB = 1 |

Feeding a crop (N/P/K or mutagen) costs `energy_per_feed` (100) per crop topped up. Up to 16 N/P/K items are eaten
per scan; fluid N/P/K drains at most 1000 mB per scan, only as much as the buffer has room for.

### 11.4 Hydroponic Garden Bed
`roll_mod:hydroponic_garden_bed` is a soil whose humidity, acidity and nutrients are set in its GUI
(`AgriApiSoilMixin` answers AgriCraft's soil lookup from the bed's block entity).

| Setting | Range | AgriCraft property | Per-bed upkeep per tick |
|---|---|---|---|
| pH | 1–7 | acidity HIGHLY_ACIDIC … HIGHLY_ALKALINE | acidity buffer −500/−100/−10/0/+10/+100/+500 |
| Water | 1–6 | humidity ARID … FLOODED | 0/2/3/4/5/6 mB water |
| Fertility | 1–6 | nutrients NONE … VERY_HIGH | 0/2/3/4/5/6 mB fertilizer |
| — | — | — | 2500 EU/t energy |

- Fully supplied → the configured soil with **`growth_modifier` 6.0**. Disabled or short of anything → inert soil
  (arid / neutral / none, 1.0) — crops stall but never pop off.
- Beds join horizontally into a node of up to 25 sharing settings, buffers and 3 input slots; every loaded bed
  pays its own upkeep.
- Reagents come from data maps `roll_mod:acidity` and `roll_mod:fertilizer` — format and shipped values in
  **`docs/hydroponic-data-maps.md`**. JEI has a *Hydroponic Reagents* category.
- Fallback soil JSON: `data/roll_mod/agricraft/soils/hydroponic_garden_bed.json` (used only if the mixin can't
  answer).

### 11.4a EV Greenhouse
`roll_mod:ev_greenhouse` is an MBD2 multiblock (code in `compat/MBD2/greenhouse/`) that grows the AgriCraft seed in
its one slot **without a crop in the world**; the seed is never used up. 3×3×3, controller in the middle of the
front face, read back → front, each layer bottom first:

| | back | middle | front |
|---|---|---|---|
| bottom | `H F H` | `F S F` | `H C H` |
| middle | `F G F` | `G . G` | `F G F` |
| top | `H F H` | `F L F` | `H F H` |

`H` highly advanced machine hull **or an MI hatch** — at least one item output, one fluid input and one energy
input hatch, any tier — `F` netherite frame, `G` glass, `S` grass block, `L` lapotronic laser block, `.` air (the
crop), `C` controller. Formed, the client stops drawing the structure's blocks (they stay in the world, only hidden)
and the controller draws the geo model; the plant is drawn at its stage in the air cell.

- **Growth** (`GreenhouseGrowth`): as a crop on a fully supplied Hydroponic Garden Bed (soil ×6.0), but with the
  *expected* time instead of rolls — a stage takes `4096 / (3 × min(1, 6 × (growth_chance + growth × growth_bonus ×
  growthMultiplier)))` ticks. Weeds, light, seasons, soil and every other requirement are ignored.
- **Harvest**: AgriCraft's own — `(gain + 3) / 3` rolls of the plant's products — into the item output hatches, then the
  plant restarts at its after-harvest stage. If the harvest does not fit the item output hatches it waits.
- **Costs**: each stage starts by taking 1000 mB water and 1000 fertilizer (`roll_mod:fertilizer` data map units)
  from the fluid input hatches; growing costs 2048 EU × the stage's ticks, from the energy input hatches.
- **Overclock** (MI's): the machine may draw up to 8192 EU/t plus MI upgrades (right-click into the controller,
  sneak + empty hand takes them back). Above 2048 EU/t it grows proportionally faster, ramping like MI's
  `CrafterComponent` (+1 efficiency per harvest, −1 per idle or starved tick, cap `2048 + efficiency × cycleEU / 600`).
- Numbers live in `GreenhouseSettings`. JEI category *EV Greenhouse Yield*: per plant, the average items of each
  product per harvest for gain 1–2, 3–5, 6–8 and 9–10, and the base regrowth time at growth 1 and 10.

### 11.5 Bug fixes to AgriCraft
- `AgriSeedItemMixin`: a seed without a genome used on anything but crop sticks or an analyzer no longer gets eaten
  leaving empty sticks.
- `CropBlockEntityMixin`: null-safe `isFullyGrown()` (fixes an FTB Ultimine crash on bare sticks).
- `CropBlockEntityMixin`: **fertilizer can't trigger spreading.** `acceptsFertilizer` returns false on cross crop
  sticks (bone meal is not used up), and the fertilizer-only growth tick does nothing on cross sticks, which also
  covers machines that apply fertilizer directly. Natural spreading on random ticks is unchanged.
- `HoeOffhandPlantGuard`: with a hoe in the main hand, off-hand planting (seeds, sticks, seed bags, plantables) is
  cancelled, so tilling doesn't also plant.
- Client `CropRequirementCategoryRecipeMixin`: the JEI requirement page falls back to `[roll_mod:hydroponic_garden_bed]`
  when no soil matches (instead of crashing), since the bed's GUI can be set to any humidity/acidity/nutrients.
- `AgriCraftCompatPackMixin`: **AgriCraft's built-in Farmer's Delight pack is not added** (data and resources). It
  registers `farmersdelight:cabbage/tomato/rice` on the same seeds as our `roll_mod:cabbage/tomato/rice`; seed lookup
  takes the first plant, so FD's would win and break `iceberg_mint` (cabbage + sugar_cane) and everything bred from
  it. It also adds soils on the FD rich soil blocks and the mutations onion = allium + potato, rice = seagrass +
  wheat, tomato = wheat + beetroot, cabbage = onion + seagrass. TODO in §13.
- `SprinklerGrowthMixin`: **sprinklers grow crops at most ≈ 2×.** The per-column growth chance becomes
  `min(sprinkler_growth_chance, period × randomTickSpeed / 4096)`, `period = max(49, sprinkler_interval)` ticks — as
  many extra random ticks as vanilla gives (≈ 0.036 at defaults instead of 0.2, which was ≈ 6.6×). Applies to every
  block the sprinkler waters, cross crop sticks included; randomTickSpeed 0 turns the boost off.
- Removed in 4.0.24 upgrade: our load fix clearing stale weed data — AgriCraft does it itself now.

### 11.6 Other crop-related features
- **Mutagen Syringe** (`roll_mod:mutagen_syringe`): right-click a planted crop → +500 mutagen, gives back an empty
  `roll_mod:syringe`. Mutagen bleeds 100/s, so a syringe alone lasts 5 s (§11.7).
- **Crop Analyzer** cyberware (Create Cybernetics, eyes): `crop_analyzer_module` (humanity 5, 5 energy/t, needs
  cybereyes) and `crop_analyzer_lense` (humanity 10, 10 energy/t, needs eyeballs) show the magnifying-glass overlay
  without holding the glass.
- **Daily task** `clip_agri_crops`: base 12, team ×1.75. Counts clicks **with the AgriCraft Clipper** that clip a
  crop (its stage goes back); refused clips and plain harvesting don't count.
- **Creative tabs:** roll_mod crop items in `roll_crops`; herbicides and AgriCraft-based items in Addons.
- Vanilla-style crops outside AgriCraft: `roll_mod:latex_dandelion` (age 0–6) and `roll_mod:sulfur_berry_block`
  (age 0–3, bonemealable; berries have 20 % Sulfur Poisoning).

### 11.7 Gene ceilings, breeding gates and nutrients
Code: `compat/argicraft/CropBreeding` (rules), `CropBreedingSettings` (every number below), mixins
`AgriCrossBreedEngineMixin`, `DefaultPlantMutatorMixin`, `DefaultStatMutatorMixin`, `AgriStatMaxMixin`,
`AgriSeasonConditionMixin`, `AgriPlantHarvestMixin`, `CropBlockEntityMixin`.

**Per-crop pools** (saved with the crop, synced to clients):

| NBT | What | Filled by | Drains |
|---|---|---|---|
| `herbicideAmount` | herbicide (§11.2) | herbicide items, Crop Managers | weed ticks |
| `mutagenAmount` + `mutagenTick` | mutagen | Mutagen Syringe (+500), HV Crop Manager (to 4000) | **100/s**, worked out from game time (crops don't tick) |
| `npkN`, `npkP`, `npkK` | nitrogen, phosphorus, potassium | MV/HV Crop Managers (to 100 each) | 10 each per harvest |

A crop is **supplied** while each of N, P and K is ≥ 10.

**Gene ceiling.** When two parents are combined (§4), each child stat allele may only climb to the ceiling both
parents qualify for (the pair gets the lower of the two):

| Ceiling | Each parent needs |
|---|---|
| 5 | — (default) |
| 10 | herbicide ≥ 1000 |
| 15 | + supplied with N/P/K |
| 20 | + mutagen ≥ 1000, **and both parents the same species** (different species cap at 15) |

The cap is `max(ceiling, highest allele either parent already has for that stat)`, so breeding never lowers what
the parents carry — it only stops the +1 drift past the ceiling. Old crops above 5 keep their genes. Cloning
(one parent) is untouched.

**Species mutation** needs **both parents ≥ 1000 mutagen**; without it the mutation roll is skipped. A mutation
child starts with **default genes** (homozygous new species, every stat at its minimum). Breeding within a species
is how genes improve.

**Upkeep.** On every harvest (right-click, `AgriApi.harvest`, Crop Managers, Create harvester — anything that ends
in `AgriPlant.onHarvest`), a supplied crop pays 10 N, P and K. An unsupplied crop instead has **every stat allele
above 10 dropped to 10** — genes from the N/P/K and mutagen ceilings need N/P/K to stay. Block-break drops don't
count as a harvest.

**Growth.** A supplied crop's growth rate is **×2** (§2's `rate`, so it still saturates at 1.0). Not on a Hydroponic
Garden Bed: the bed's own ×6 soil doesn't stack with it.

**N/P/K data map** `roll_mod:npk` (items and fluids), `data/<ns>/data_maps/{item,fluid}/npk.json`:
```json
{ "values": { "minecraft:bone_meal": { "p": 10 } } }
```
Fields `n`, `p`, `k` (ints, optional, default 0). Items are per item, fluids per bucket (1000 mB), scaled linearly.
Shipped: bone meal = 10 P (`RollDataMapProvider`); real N and K sources are still to be chosen.

**Magnifying glass** (and Crop Analyzer) on a planted crop: herbicide, mutagen (with drain), N/P/K, supplied /
unsupplied (and whether genes will regress), growth boost state, the crop's ceiling as a parent with the next
requirement, and whether it may mutate. On cross sticks: the ceiling and mutation status of the two most fertile
mature neighbours (AgriCraft's pick without the fertility roll), or that a pair is missing.

**EV Greenhouse** grows from a seed, not a crop: no pools, no upkeep, stats used as they are. Its JEI page groups
gain up to 20.

---

## 12. Content shipped by roll_mod

All under `common/src/main/resources/data/`.

### Soils
| Id | Humidity / acidity / nutrients | Modifier | Blocks |
|---|---|---|---|
| `roll_mod:coarse_dirt` | watery / neutral / medium | 0.5 | `minecraft:coarse_dirt` |
| `roll_mod:hydroponic_garden_bed` | arid / neutral / none | 1.0 | fallback only (§11.4) |
| `agricraft:lush_dirt` | wet / slightly_acidic / high | 2.0 | `biomeswevegone:lush_dirt`, `lush_farmland` |
| `agricraft:rich_soil` | wet / slightly_acidic / high | 3.0 | `farmersdelight:rich_soil`, `rich_soil_farmland` (FD pack's own soils for these are blocked, §11.5) |

### Plants (`roll_mod`)
All: `growth_chance` 0.65, `growth_bonus` 0.025, 8 stages, cloneable, `override_planting` true. `spread_chance` 0.1
and `harvest_stage` 4 unless noted. Soil = humidity / acidity / nutrients.

| Plant | Soil | Light / seasons | Notes |
|---|---|---|---|
| black_bell_pepper | arid / acidic / very_high (≥) | 0–0 (light tol 0.1), all | → rusticdelight black 1–3 |
| blue_bell_pepper | dry / neutral / low (≤) | 0–0 (light tol 0.2), all | → rusticdelight blue 2–3 |
| cabbage | dry / slightly_alkaline / low | 0–10, spring | FD cabbage |
| coffee | damp / neutral / medium | 0–15, all | → coffee_beans |
| cotton | watery / neutral / medium (all tol 0) | 13–15, summer+autumn | growth 0.75 / bonus 0.005, 4 textures, seed `rusticdelight:cotton_seeds` → cotton boll 5–10 |
| fluorite_pineapple | flooded / slightly_acidic (≤) / medium | 10–16, spring | |
| green_bell_pepper | dry / slightly_alkaline / low | | → rusticdelight green pepper 1–4 |
| hops | wet / slightly_acidic / high (≥) | | harvest stage 2 |
| iceberg_mint | damp / slightly_acidic / very_high (≥) | | spread 0.6 |
| latex_dandelion | dry / slightly_alkaline / low | | harvest 2, spread 0.6, **weeds_resistance** |
| magnetite_spinach | damp / neutral / medium | all seasons | |
| nikelia | wet / neutral / low | 0–10, all | |
| onion | damp / neutral / medium (≤) | 0–10, all | growth 0.75, seed `farmersdelight:onion` → onion 2–5 + allium 1–2 |
| orange_bell_pepper | wet / slightly_acidic / high (≥) | 10–16 | → rusticdelight orange 2–3 |
| pink_bell_pepper | damp / slightly_acidic / high (≥) | 10–16 | → rusticdelight pink 2–3 |
| purple_bell_pepper | dry / slightly_acidic / medium | 0–0 (light tol 0.2), all | → rusticdelight purple 2–3 |
| red_bell_pepper | wet / slightly_acidic / high (≥) | | spread 0.6 |
| rice | dry / slightly_alkaline / low | 0–10, spring | seed `farmersdelight:rice` → FD rice panicle 1–4 |
| rutile_bell_pepper | arid / neutral / very_high (≥) | all seasons | |
| stonelia | watery (≥) / neutral / medium (≥) | | harvest 1, → stone_dust 1–2 @ 50 % |
| sulfur_berry | arid / neutral / very_high (≥) | 0–5 | harvest 3, 1–5 berries, **weeds_resistance** |
| sulfur_berry_coffee | dry / acidic / low | all seasons | |
| tomato | wet / slightly_acidic / high | | FD tomato |
| white_bell_pepper | damp / neutral / medium | 13–16 | → rusticdelight white 2–3 |
| wild_bell_pepper | damp / slightly_acidic / very_high (≥) | 5–16 | red/yellow/green 20 % each, **weeds_resistance** |
| wild_dark_bell_pepper | dry / slightly_acidic / very_high (≥) | 0–4 (light tol 0.2), all | black/purple/blue 20 % each, **weeds_resistance** |
| wild_pale_bell_pepper | damp / slightly_acidic / very_high (≥) | 5–16 | orange/white/pink 20 % each, **weeds_resistance** |
| yellow_bell_pepper | damp / neutral / low (≤) | | |

### Mutations (`roll_mod`, chance 0.5 unless noted)
| Child | Parents |
|---|---|
| cotton | hops + allium (0.25) |
| fluorite_pineapple | iceberg_mint + sweet_berries |
| green_bell_pepper | wild_bell_pepper + cactus |
| hops | wheat + iceberg_mint |
| iceberg_mint | cabbage + sugar_cane |
| latex_dandelion | dandelion + dandelion |
| magnetite_spinach | iceberg_mint + green_bell_pepper |
| nikelia | stonelia + sulfur_berry |
| onion | potato + allium (0.25) |
| red_bell_pepper | wild_bell_pepper + red_tulip |
| rutile_bell_pepper | yellow_bell_pepper + magnetite_spinach (0.01) |
| stonelia | nether_wart + melon |
| sulfur_berry_coffee | coffee + sulfur_berry |
| yellow_bell_pepper | wild_bell_pepper + orange_tulip |

Cabbage, coffee, rice, sulfur_berry, tomato and wild_bell_pepper have no mutation (planted from seed).
The 8 newer peppers (wild_pale, wild_dark, orange, white, pink, black, purple, blue) have **no mutation and no
grass drop yet**, so they are only obtainable via commands/creative. Their products need **Rustic Delight ≥ 1.7.1**
(older versions lack the orange/white/pink/black/purple/blue peppers and the plant JSONs fail to load). Their seed
items `roll_mod:<colour>_bell_pepper_seeds` have no textures yet, so their seed models aren't generated.

### Overrides of AgriCraft's own files
`data/minecraft/agricraft/**` copies the jar's files (identical in 4.0.8 and 4.0.24), except:
- **wheat, potato, pumpkin, dandelion, brown_mushroom** add `"weeds_resistance": true`;
- the 18 vanilla plants that had no `clip_products` — bamboo, beetroot, brown/red mushroom, cactus, carrot,
  crimson/warped fungus, kelp, melon, nether wart, potato, pumpkin, sea pickle, seagrass, sugar cane, sweet berries,
  wheat — get **`clip_products`: 1 of their own product (melon → melon slice) at 50 %**, so they stay clippable under
  4.0.24's rule (§5).

`data/agricraft/agricraft/soils/gravel.json` and `weeds/weed.json` are identical to AgriCraft's and override nothing.

---

## 13. Gotchas and upstream bugs

Datapack:
1. Required but **unused**: `mods` (everywhere), seed `grass_drop_chance`/`seed_drop_chance`/`seed_drop_bonus`,
   product `required`. Grass seed drops come only from the loot modifier (§8.10).
2. Product `nbt` has **no effect** (merged into a copy that is thrown away).
3. `biomes`/`dimensions` take ids only, no tags; an empty whitelist blocks growth everywhere; default
   `ignore_from_strength` 11 means "never bypassed".
4. `block_conditions`: only the first non-skipped entry decides, and it checks the block **under the soil**.
5. Without `fluid_condition`, **waterlogged crops regress**; lava counts as fertile.
6. Mutations have no conditions; species mutation ignores Mutativity.
7. Weed `requirement` is never checked.
8. Unknown item/block/fluid ids silently become air/empty instead of erroring.

Upstream bugs and quirks seen in the bytecode *[not confirmed in game]*:
- `AgriSeed.CODEC` binds `seed_drop_bonus`/`seed_drop_chance` in swapped order (harmless — both unused).
- Config `weeds_destroy_crop_sticks` does nothing.
- `AgriSeedItem.useOn` still consumes a genome-less seed outside its genome check (roll_mod works around it, §11.5).
- `sprinkler_water_consumption` really costs `max(1, value / 20)` mB per tick while sweeping (10 → 20 mB/s, not 10).
- `rain_fill_rate` is per vanilla precipitation event, not per tick, so rain fills tanks slowly.
- Grass seed weight pick is off by one (wheat 11/30, carrot 10/30, potato 9/30).
- A greenhouse's interior is fixed when created; "too big" also covers spaces that are not enclosed.
- Fixed in 4.0.24: journal research persisting; the trowel deleting plants (genome now written back); stale client
  crop data after load.

roll_mod gene rules (§11.7):
- Mutagen bleeds even when nobody is looking; a crop holds it for `amount / 100` seconds after the last top-up.
- The gene ceiling is checked against the parents *at the moment of breeding*: herbicide burnt down by weed ticks,
  N/P/K spent on a harvest or mutagen bled away before then all count.
- `<stat>_max` in `agricraft.jsonc` has no effect any more.

Open TODOs (roll_mod):
- Pick nitrogen and potassium sources for `roll_mod:npk` (only bone meal → P ships).
- **Adopt the Farmer's Delight plants** instead of blocking AgriCraft's FD pack: drop `roll_mod:cabbage/tomato/rice`,
  move their mutations (iceberg_mint = cabbage + sugar_cane, …) to the `farmersdelight:*` ids, settle the rich soil
  values, then unblock the pack in `AgriCraftCompatPackMixin`.

---

## 14. Irrigation and greenhouses

New in 4.0.24. Settings are config-only (`irrigation.*`, `greenhouse.*` in `agricraft.jsonc`), not datapacks.

### Irrigation
- **Tank** (`irrigation_tank`): water only, `tank_capacity` 8000 mB per block. Buckets fill/empty 1000 mB; pipes
  through the fluid capability; rain adds `rain_fill_rate` (5) mB per precipitation event; drains into a tank below;
  side-by-side tanks share water. Above ~43 % full it counts as a water source block (likely hydrates farmland
  *[inferred]*).
- **Channel** (`irrigation_channel`, `irrigation_channel_hollow`): `channel_capacity` 500 mB, horizontal connections
  to channels and tanks; water flows each tick toward lower surfaces (a tank must be > ~29 % full to feed a channel).
  **Channel valve** item: right-click toggles open/closed on a normal channel; a hollow channel closes on redstone.
- **Sprinkler**: hangs directly under a channel and pulls water while the valve is open. Sweeps a **7×7** area one
  column per tick (49 ticks a sweep, new sweep every `sprinkler_interval` 40 ticks — effectively continuous),
  scanning 1–5 blocks down per column:
  - farmland → moisture 7, stop;
  - a bonemealable block (AgriCraft crops, vanilla crops, saplings, grass) → a full random tick with
    `sprinkler_growth_chance`, keep scanning down. **roll_mod caps this at ≈ 2× growth** (§11.5);
  - anything else stops the column.

  Sprinklers don't change AgriCraft soil humidity/acidity/nutrients — soils are still fixed per soil JSON.
- **Grate**: decorative, climbable, waterloggable panel; vines can be added and clipped. No crop effect.

| Config | Default |
|---|---|
| `tank_capacity` | 8000 mB |
| `channel_capacity` | 500 mB |
| `rain_fill_rate` | 5 |
| `sprinkler_water_consumption` | 10 (see §13 quirk) |
| `sprinkler_growth_chance` | 0.2 (capped by roll_mod) |
| `sprinkler_interval` | 40 ticks |
| `sprinkler_particles` | true |

### Greenhouses
- **Create:** sneak-use the **Greenhouse Monitor** from inside (your head block must be air). A flood fill marks the
  interior: `#agricraft:greenhouse_glass`, doors/trapdoors and any block with a sturdy face are walls; everything
  else (air, crops, torches…) is interior. Fails when the interior exceeds `block_limit` (1024; also what an
  unenclosed space hits), touches another greenhouse, or less than `ceiling_glass_fraction` (0.4) of the ceiling is
  glass. Interior air becomes invisible `agricraft:greenhouse_air`.
- Plain use reports inside/outside. Breaking a wall block into something neither glass nor sturdy dissolves the
  whole greenhouse; the interior set is fixed at creation.
- **Effects** on crops whose own position is inside: growth rate × `growth_modifier` (1.05) and, with
  `ignores_seasons` (true), seasons always fertile. No effect on light, weather, weeds or breeding.

| Config | Default |
|---|---|
| `block_limit` | 1024 |
| `ceiling_glass_fraction` | 0.4 |
| `growth_modifier` | 1.05 |
| `ignores_seasons` | true |

