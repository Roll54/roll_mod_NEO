# GTMOGS — Ore Generation Reference

Everything GTMOGS generates comes from **ore vein definitions**: JSON files in a datapack registry.
No veins ship with the mod, and vanilla ore generation is disabled by default, so a pack that adds
GTMOGS without adding vein files will generate no ores at all.

This document covers the current state of the system, including the additions over upstream GT:M:
the **`gtmogs:cuboid`** vein generator, **surface indicators**, and the extra **world gen layers**
for Ad Astra planets and the `roll_mod:mining_overworld` dimension.

---

## 1. Where vein files go

The registry is `gtmogs:ore_vein`, so files live at:

```
data/<your_namespace>/gtmogs/ore_vein/<vein_name>.json
```

Example: `data/roll_mod/gtmogs/ore_vein/moon_borax.json` registers the vein `roll_mod:moon_borax`.

Files are reloaded with the world (they are a *datapack* registry, not a reloadable resource — use
`/reload` for tags, but re-enter the world after changing vein files).

To test a vein in-game:

```
/gtmogs place_vein <vein_id> [x y z]
```

---

## 2. How a vein gets placed

1. **Grid.** Veins are only rolled on a chunk grid — every `oreVeinGridSize` chunks (default 3),
   with up to `oreVeinRandomOffset` blocks (default 12) of horizontal jitter.
2. **One roll per layer.** For each *world gen layer* applicable to the current dimension, one vein
   is rolled. In the Overworld that means one `stone` vein *and* one `deepslate` vein per grid point;
   on the Moon only `moon_stone` rolls, so a moon vein competes only with other moon veins.
3. **Biome filter.** Only veins whose `biomes` list matches the biome at the vein center are eligible
   (empty/absent = every biome).
4. **Weight.** Among eligible veins for that layer, one is chosen weighted by
   `weight` + any `weight_modifier` for that biome. `weight` ≤ 0 with no modifier = never generates.
5. **Height.** `height_range` picks the vein's Y center.
6. **Shape.** The `generator` block decides which positions get which blocks.
7. **Replacement check.** Each candidate position is tested against the target's `target` predicate,
   then against `discard_chance_on_air_exposure`.
8. **Indicators.** If the vein has `indicators`, decorative blocks are scattered afterwards.

---

## 3. Top-level fields

| Field | Required | Type | Meaning |
|---|---|---|---|
| `cluster_size` | ✅ | non-negative `IntProvider` | Rough vein size in blocks. Sampled per vein. Also drives the indicator scatter radius. |
| `density` | ✅ | float `0.0`–`1.0` | How densely the shape is filled. Interpreted per generator. |
| `weight` | ✅ | int | Selection weight within its layer. `0` disables the vein. |
| `layer` | ✅ | string | World gen layer name — see §4. Controls which dimensions roll it and (for `veined`) what it replaces. |
| `dimension_filter` | ✅ | list of dimension ids | Dimensions the vein may appear in. Must contain the layer's dimension or nothing generates. |
| `height_range` | ✅ | `HeightRangePlacement` | Vertical placement of the vein center. |
| `discard_chance_on_air_exposure` | ✅ | float `0.0`–`1.0` | Chance to *skip* the "don't place next to air" check. `0` = always check (no ore exposed in caves), `1` = never check. |
| `biomes` | ❌ | biome tag or list | Restricts eligible biomes. Default: all. |
| `weight_modifier` | ❌ | object | Per-biome weight adjustments. |
| `indicators` | ❌ | list | Surface markers hinting at the vein — see §6. |
| `generator` | ✅ | object | Vein shape — see §5. |

### `cluster_size`

Any vanilla int provider:

```json
"cluster_size": 40
```
```json
"cluster_size": { "type": "minecraft:uniform", "min_inclusive": 34, "max_inclusive": 52 }
```

### `height_range`

Wraps vanilla's `HeightRangePlacement`, so the value goes under a `height` key:

```json
"height_range": {
  "height": {
    "type": "minecraft:uniform",
    "min_inclusive": { "absolute": -40 },
    "max_inclusive": { "absolute": 70 }
  }
}
```

`minecraft:trapezoid` and `minecraft:biased_to_bottom` work too, as do `{ "above_bottom": n }` /
`{ "below_top": n }` anchors.

### `biomes`

```json
"biomes": "#ad_astra:is_moon"
```
```json
"biomes": ["minecraft:desert", "minecraft:badlands"]
```

### `weight_modifier`

```json
"weight_modifier": {
  "type": "add",
  "biomes": "#minecraft:is_mountain",
  "amount": 40
}
```

---

## 4. World gen layers

`layer` selects both the set of dimensions that roll the vein and the default "replaceable" rule test.

| Layer | Replaceable blocks | Dimension |
|---|---|---|
| `stone` | `#minecraft:stone_ore_replaceables` | `minecraft:overworld` |
| `deepslate` | `#minecraft:deepslate_ore_replaceables` | `minecraft:overworld` |
| `netherrack` | `#minecraft:nether_carver_replaceables` | `minecraft:the_nether` |
| `endstone` | `#c:end_stones` | `minecraft:the_end` |
| `moon_stone` | `#c:moon_stones` | `ad_astra:moon` |
| `mars_stone` | `#c:mars_stones` | `ad_astra:mars` |
| `mercury_stone` | `#c:mercury_stones` | `ad_astra:mercury` |
| `venus_stone` | `#c:venus_stones` | `ad_astra:venus` |
| `glacio_stone` | `#c:glacio_stones` | `ad_astra:glacio` |
| `mining_overworld_stone` | `#minecraft:stone_ore_replaceables` | `roll_mod:mining_overworld` |
| `mining_overworld_deepslate` | `#minecraft:deepslate_ore_replaceables` | `roll_mod:mining_overworld` |

The planet tags ship with the mod and can be extended from any datapack —
`data/c/tags/block/moon_stones.json`:

```json
{
  "replace": false,
  "values": [
    "ad_astra:moon_stone",
    "minecraft:deepslate",
    "minecraft:basalt",
    "minecraft:soul_soil"
  ]
}
```

> ⚠️ The layer's rule test is only applied automatically by the `veined` generator. Every other
> generator uses the `target` predicate written on each individual target entry (see §5.1). Adding a
> stone block to `#c:moon_stones` does **not** by itself stop an `always_true` target from
> overwriting it — it is what you point a `tag_match` target at.

---

## 5. Vein generators

Registered types: `gtmogs:cuboid`, `gtmogs:classic`, `gtmogs:layer`, `gtmogs:standard`,
`gtmogs:dike`, `gtmogs:veined`, `gtmogs:geode`, `gtmogs:no_op`.

### 5.1 Target entries

`cuboid`, `classic`, `standard`, `dike` and `veined` all place blocks through vanilla
`TargetBlockState` entries:

```json
{
  "target": { "predicate_type": "minecraft:always_true" },
  "state": { "Name": "roll_mod:moon_borax" }
}
```

Entries in a list are tried **in order**, and the first one whose predicate matches wins — so put
your most specific predicate first. Common predicates:

```json
{ "predicate_type": "minecraft:always_true" }
{ "predicate_type": "minecraft:tag_match", "tag": "c:moon_stones" }
{ "predicate_type": "minecraft:block_match", "block": "minecraft:deepslate" }
{ "predicate_type": "minecraft:blockstate_match", "block_state": { "Name": "minecraft:stone" } }
```

`always_true` will happily replace air, water and player-built blocks inside the vein footprint.
It's convenient for dedicated dimensions where the whole column is stone; use `tag_match` when you
want the vein to respect terrain.

Block states with properties:

```json
"state": { "Name": "minecraft:redstone_ore", "Properties": { "lit": "false" } }
```

### 5.2 `gtmogs:cuboid`

A blocky, layered slab of ore — the shape used for the planet veins. Ore is banded vertically
(bottom → middle → top) with a "spread" filler mixed throughout, and density falls off with
horizontal distance from the vein center, so the middle of a vein is much richer than its edges.

| Field | Required | Meaning |
|---|---|---|
| `top` / `middle` / `bottom` / `spread` | ✅ | Layer objects: `targets` (list) + optional `layers` (int). |
| `min_y` / `max_y` | ✅ | Absolute Y band the slab starts in. |

- `layers` defaults: `top` 2, `middle` 3, `bottom` 2 (omit or pass `-1`). `spread`'s `layers` value
  is unused for thickness — spread fills gaps across the whole vein.
- Total vein height = `top.layers + middle.layers + bottom.layers`.
- Constraint: `top.layers + bottom.layers >= middle.layers`, otherwise world gen throws.
- The vein's starting Y is picked inside `[min_y, max_y]`, so keep `max_y` at least ~6 above `min_y`.
- `min_y`/`max_y` are separate from `height_range`; set both consistently.

**Full sample** — `data/roll_mod/gtmogs/ore_vein/moon_borax.json`:

```json
{
  "biomes": "#ad_astra:is_moon",
  "cluster_size": {
    "min_inclusive": 34,
    "max_inclusive": 52,
    "type": "minecraft:uniform"
  },
  "density": 1,
  "dimension_filter": [
    "ad_astra:moon"
  ],
  "discard_chance_on_air_exposure": 0.6,
  "generator": {
    "type": "gtmogs:cuboid",
    "top": {
      "layers": 6,
      "targets": [
        {
          "target": { "predicate_type": "minecraft:always_true" },
          "state": { "Name": "roll_mod:moon_borax" }
        }
      ]
    },
    "middle": {
      "layers": 8,
      "targets": [
        {
          "target": { "predicate_type": "minecraft:always_true" },
          "state": { "Name": "roll_mod:moon_rock_salt" }
        },
        {
          "target": { "predicate_type": "minecraft:always_true" },
          "state": { "Name": "roll_mod:moon_lepidolite" }
        }
      ]
    },
    "bottom": {
      "layers": 3,
      "targets": [
        {
          "target": { "predicate_type": "minecraft:always_true" },
          "state": { "Name": "roll_mod:moon_lepidolite" }
        }
      ]
    },
    "spread": {
      "layers": 6,
      "targets": [
        {
          "target": { "predicate_type": "minecraft:always_true" },
          "state": { "Name": "roll_mod:moon_borax" }
        }
      ]
    },
    "min_y": -30,
    "max_y": 60
  },
  "height_range": {
    "height": {
      "type": "minecraft:uniform",
      "max_inclusive": { "absolute": 70 },
      "min_inclusive": { "absolute": -40 }
    }
  },
  "indicators": [
    {
      "type": "gtmogs:surface",
      "blocks": [
        "roll_mod:moon_borax_ore_sample",
        "roll_mod:moon_rock_salt_ore_sample",
        "roll_mod:moon_lepidolite_ore_sample"
      ],
      "chance": 0.05
    }
  ],
  "layer": "moon_stone",
  "weight": 100
}
```

The `indicators` block is optional — omit it and the vein generates identically, just without surface
samples. See §6 for what the field does.

Note that only the *first* matching target in a list is ever placed at a position — the two entries
under `middle` above mean "rock salt, or lepidolite if rock salt's predicate fails". Since both use
`always_true`, only rock salt will actually appear. To genuinely mix two ores in one band, give them
disjoint predicates:

```json
"middle": {
  "layers": 8,
  "targets": [
    {
      "target": { "predicate_type": "minecraft:block_match", "block": "ad_astra:moon_stone" },
      "state": { "Name": "roll_mod:moon_rock_salt" }
    },
    {
      "target": { "predicate_type": "minecraft:block_match", "block": "minecraft:deepslate" },
      "state": { "Name": "roll_mod:moon_lepidolite" }
    }
  ]
}
```

### 5.2.1 Mixing several ores in one band, by weight

`cuboid` and `classic` targets have no `weight` field, but vanilla's
`minecraft:random_block_match` predicate is probabilistic — it matches when the existing block is
`block` **and** a random roll lands below `probability`. Chaining those, with an unconditional entry
last, gives weighted mixing without touching any existing vein file.

A 60/40 split between rock salt and lepidolite:

```json
"middle": {
  "layers": 8,
  "targets": [
    {
      "target": { "predicate_type": "minecraft:random_block_match", "block": "ad_astra:moon_stone", "probability": 0.6 },
      "state": { "Name": "roll_mod:moon_rock_salt" }
    },
    {
      "target": { "predicate_type": "minecraft:always_true" },
      "state": { "Name": "roll_mod:moon_lepidolite" }
    }
  ]
}
```

**The probabilities are conditional.** Each entry is only tested on the positions the previous
entries rejected, so for weights `w₁ : w₂ : … : wₙ` the *i*-th probability is

```
pᵢ = wᵢ / (wᵢ + wᵢ₊₁ + … + wₙ)
```

and the last entry is an unconditional `always_true` (or a plain `tag_match`). Weights 5 : 3 : 2:

```json
"targets": [
  { "target": { "predicate_type": "minecraft:random_block_match", "block": "ad_astra:moon_stone", "probability": 0.5 }, "state": { "Name": "roll_mod:moon_rock_salt" } },
  { "target": { "predicate_type": "minecraft:random_block_match", "block": "ad_astra:moon_stone", "probability": 0.6 }, "state": { "Name": "roll_mod:moon_lepidolite" } },
  { "target": { "predicate_type": "minecraft:always_true" }, "state": { "Name": "roll_mod:moon_borax" } }
]
```

5/10 of positions get rock salt; of the remaining 5/10, 0.6 → 3/10 get lepidolite; the last 2/10 get
borax.

> ⚠️ `random_block_match` only fires when the existing block is that *exact* block, so anything else
> in the column — the other members of `#c:moon_stones`, or air — falls through to the catch-all and
> always receives the last ore. To keep the ratio uniform across the whole vein, repeat the weighted
> entry once per stone type before the catch-all:

```json
"targets": [
  { "target": { "predicate_type": "minecraft:random_block_match", "block": "ad_astra:moon_stone", "probability": 0.6 }, "state": { "Name": "roll_mod:moon_rock_salt" } },
  { "target": { "predicate_type": "minecraft:random_block_match", "block": "minecraft:deepslate",  "probability": 0.6 }, "state": { "Name": "roll_mod:moon_rock_salt" } },
  { "target": { "predicate_type": "minecraft:random_block_match", "block": "minecraft:basalt",     "probability": 0.6 }, "state": { "Name": "roll_mod:moon_rock_salt" } },
  { "target": { "predicate_type": "minecraft:random_block_match", "block": "minecraft:soul_soil",  "probability": 0.6 }, "state": { "Name": "roll_mod:moon_rock_salt" } },
  { "target": { "predicate_type": "minecraft:always_true" }, "state": { "Name": "roll_mod:moon_lepidolite" } }
]
```

`minecraft:random_blockstate_match` works the same way but takes a `block_state` object instead of a
`block` id, for ores that care about block properties.

Existing veins are unaffected: a target list with no `random_*` predicate keeps its current
first-match-wins behaviour exactly.

If you'd rather have honest `"weight": 3` fields than a probability chain, `gtmogs:dike` and
`gtmogs:veined` already roll their block definitions through a real weighted pick per position — at
the cost of the cuboid's crisp banding.

A minimal Overworld cuboid vein:

```json
{
  "cluster_size": 32,
  "density": 0.8,
  "weight": 60,
  "layer": "mining_overworld_deepslate",
  "dimension_filter": ["roll_mod:mining_overworld"],
  "discard_chance_on_air_exposure": 0.4,
  "height_range": {
    "height": {
      "type": "minecraft:uniform",
      "min_inclusive": { "absolute": -60 },
      "max_inclusive": { "absolute": 10 }
    }
  },
  "generator": {
    "type": "gtmogs:cuboid",
    "min_y": -60,
    "max_y": 10,
    "top":    { "layers": 3, "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }, "state": { "Name": "minecraft:deepslate_iron_ore" } } ] },
    "middle": { "layers": 4, "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }, "state": { "Name": "minecraft:deepslate_gold_ore" } } ] },
    "bottom": { "layers": 2, "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }, "state": { "Name": "minecraft:deepslate_redstone_ore" } } ] },
    "spread": { "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }, "state": { "Name": "minecraft:deepslate_coal_ore" } } ] }
  }
}
```

### 5.3 `gtmogs:classic`

The GT ellipsoid vein: `primary` / `secondary` / `between` / `sporadic` layers plus a `y_radius`.
Same `Layer` shape as `cuboid` (`targets` + optional `layers`).

```json
"generator": {
  "type": "gtmogs:classic",
  "y_radius": 4,
  "primary":   { "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:iron_ore" } } ] },
  "secondary": { "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:copper_ore" } } ] },
  "between":   { "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:gold_ore" } } ] },
  "sporadic":  { "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:coal_ore" } } ] }
}
```

### 5.4 `gtmogs:standard`

Vanilla-style blob. Either a list of targets, or the three-block short form.

```json
"generator": {
  "type": "gtmogs:standard",
  "targets": [
    { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:iron_ore" } },
    { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }, "state": { "Name": "minecraft:deepslate_iron_ore" } }
  ]
}
```

```json
"generator": {
  "type": "gtmogs:standard",
  "block": "minecraft:iron_ore",
  "deep_block": "minecraft:deepslate_iron_ore",
  "nether_block": "minecraft:nether_gold_ore"
}
```

### 5.5 `gtmogs:layer`

Stacked, randomly-picked horizontal layer patterns. `layer_patterns` is a list of *patterns*, and each
pattern is itself a list of *layer* objects (no wrapper key). Every layer needs `targets`, `min_size`,
`max_size` and `weight`.

`targets` here is a **list of target lists**: one inner list is picked at random per placement, and
within it the usual first-match-wins rule applies.

```json
"generator": {
  "type": "gtmogs:layer",
  "layer_patterns": [
    [
      {
        "weight": 3,
        "min_size": 1,
        "max_size": 2,
        "targets": [
          [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:iron_ore" } } ],
          [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:copper_ore" } } ]
        ]
      },
      {
        "weight": 1,
        "min_size": 1,
        "max_size": 1,
        "targets": [
          [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:gold_ore" } } ]
        ]
      }
    ]
  ]
}
```

### 5.6 `gtmogs:dike`

Vertical dike/pipe, with each block bound to its own Y band.

```json
"generator": {
  "type": "gtmogs:dike",
  "min_y": -50,
  "max_y": 60,
  "blocks": [
    {
      "weight": 3,
      "min_y": -50,
      "max_y": 10,
      "block": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }, "state": { "Name": "minecraft:deepslate_iron_ore" } } ]
    },
    {
      "weight": 2,
      "min_y": 0,
      "max_y": 60,
      "block": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:iron_ore" } } ]
    }
  ]
}
```

### 5.7 `gtmogs:veined`

Noise-driven, vanilla-large-vein style. This is the one generator that falls back to the **layer's**
rule test — for the filler block — so the filler respects `#c:moon_stones` and friends without a
per-target predicate. Ore/rare blocks still use their own target predicates.

`ore_blocks` and `rare_blocks` are both required; each entry's `block` is a *list* of target entries.

```json
"generator": {
  "type": "gtmogs:veined",
  "min_y": -60,
  "max_y": 50,
  "veininess_threshold": 0.4,
  "min_richness": 0.1,
  "max_richness": 0.3,
  "rare_block_chance": 0.02,
  "filler_block": { "Name": "minecraft:tuff" },
  "ore_blocks": [
    {
      "weight": 3,
      "block": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:copper_ore" } } ]
    }
  ],
  "rare_blocks": [
    {
      "weight": 1,
      "block": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }, "state": { "Name": "minecraft:raw_copper_block" } } ]
    }
  ]
}
```

Other optional fields: `edge_roundoff_begin` (20), `max_edge_roundoff` (0.2),
`max_richness_threshold` (0.6).

### 5.8 `gtmogs:geode` / `gtmogs:no_op`

`geode` takes vanilla's geode configuration (`blocks`, `layers`, `crack`, `outer_wall_distance`, …).
`no_op` generates nothing and is used to disable an inherited vein:

```json
"generator": { "type": "gtmogs:no_op" }
```

---

## 6. Indicators

`indicators` scatters decorative blocks on the surface above a vein — a prospecting hint, placed only
into air/replaceable spots resting on solid ground, never over player-built blocks. The scatter
radius is derived from the vein's `cluster_size` (roughly a quarter of it, minimum 4), and blocks are
chosen uniformly from the list.

Ore samples for the moon vein — one indicator block per ore the vein contains:

```json
"indicators": [
  {
    "type": "gtmogs:surface",
    "blocks": [
      "roll_mod:moon_borax_ore_sample",
      "roll_mod:moon_rock_salt_ore_sample",
      "roll_mod:moon_lepidolite_ore_sample"
    ],
    "chance": 0.05
  }
]
```

| Field | Required | Meaning |
|---|---|---|
| `type` | ✅ | `gtmogs:surface` |
| `blocks` | ✅ | Candidate blocks, picked with equal weight. Empty list = nothing placed. |
| `chance` | ❌ | Per-position placement chance, `0.0`–`1.0`. Default `0.05`. |

All entries in `blocks` are equally likely — there is no weight field. To bias the samples toward the
vein's main ore, list it more than once:

```json
"blocks": [
  "roll_mod:moon_borax_ore_sample",
  "roll_mod:moon_borax_ore_sample",
  "roll_mod:moon_rock_salt_ore_sample",
  "roll_mod:moon_lepidolite_ore_sample"
]
```

Multiple indicator entries stack, each rolled independently — useful when one ore should be common
across the whole footprint and another rare:

```json
"indicators": [
  { "type": "gtmogs:surface", "blocks": ["roll_mod:moon_borax_ore_sample"], "chance": 0.06 },
  { "type": "gtmogs:surface", "blocks": ["roll_mod:moon_rock_salt_ore_sample", "roll_mod:moon_lepidolite_ore_sample"], "chance": 0.015 }
]
```

In dimensions with a ceiling (e.g. the Nether) there is no world surface, so a column near the vein is
scanned up and then down for the first open spot on solid ground.

The sample blocks need to survive world gen on their own: they are placed into an air/replaceable spot
resting on solid ground, and nothing re-checks them afterwards. A block with survival rules of its own
(a `BushBlock`, anything needing a specific soil) may pop off on the first block update, so plain
full/solid blocks work best.

---

## 7. Config

`config/gtmogs.json` → `worldgen.oreVeins`:

| Option | Default | Effect |
|---|---|---|
| `oreVeinGridSize` | `3` | Chunks between vein grid points. Bigger = rarer veins. |
| `oreVeinRandomOffset` | `12` | Max horizontal jitter (blocks) from the grid point. |
| `removeVanillaOreGen` | `true` | Strips vanilla ore features. |
| `removeVanillaLargeOreVeins` | `true` | Strips vanilla large ore veins. |
| `oreGenerationChunkCacheSize` | `512` | Chunk cache for vein generation. Raise for very large veins/grids. |
| `oreIndicatorChunkCacheSize` | `2048` | Chunk cache for indicators. Raise for large indicator ranges. |

`dev.debugWorldgen` logs each vein as it is generated, which is the quickest way to see whether a
vein is being *selected* (a definition problem) or being selected but placing nothing (a predicate or
Y-range problem).

---

## 8. Troubleshooting

| Symptom | Likely cause |
|---|---|
| Vein never appears | `dimension_filter` doesn't contain the `layer`'s dimension, or `weight` is `0`. |
| Vein appears in the wrong dimension | `dimension_filter` lists a dimension the layer doesn't serve — the layer wins for rolling, the filter for eligibility; they must agree. |
| Vein selected but no blocks placed | Target predicates never match the surrounding stone, or `min_y`/`max_y` don't overlap the terrain. |
| Ore replacing air/water in caves | `always_true` targets — switch to `tag_match`, and/or lower `discard_chance_on_air_exposure`. |
| Only the first ore of a band shows up | Targets are first-match-wins; give them disjoint predicates (§5.2) or weight them with `random_block_match` (§5.2.1). |
| Weighted mix is lopsided toward the last ore | `random_block_match` only matches its exact block — everything else falls through to the catch-all (§5.2.1). |
| Crash: "cannot have more `middle` layers than top and bottom layers combined" | `cuboid` requires `top.layers + bottom.layers >= middle.layers`. |
| Ores on other planets replace nothing | The planet's stone isn't in `#c:<planet>_stones`, or targets don't reference it. |
