# Hydroponic Garden Bed — reagent data maps

The Hydroponic Garden Bed takes four resources: **energy** (EU/FE, from cables), **water** (anything
in `c:water`), **acidity** and **fertilizer**. The last two are fully data-driven, so real-life
materials can be added from a datapack without touching code.

`roll_mod` registers four data map types — two concepts, each over both `ITEM` and `FLUID`. The same
id is used for both registries; that is legal, because data maps are keyed per registry, and it is
why the item and fluid files have the same name in different folders.

| Data map id | Registry | File |
| --- | --- | --- |
| `roll_mod:acidity` | `minecraft:item` | `data/<namespace>/data_maps/item/acidity.json` |
| `roll_mod:acidity` | `minecraft:fluid` | `data/<namespace>/data_maps/fluid/acidity.json` |
| `roll_mod:fertilizer` | `minecraft:item` | `data/<namespace>/data_maps/item/fertilizer.json` |
| `roll_mod:fertilizer` | `minecraft:fluid` | `data/<namespace>/data_maps/fluid/fertilizer.json` |

All four are synced to clients. That is required, not cosmetic: the bed's input slots decide whether
to accept a stack in `Slot#mayPlace`, which runs client-side for slot prediction, and an unsynced
data map is empty there — valid reagents would look rejected.

## `roll_mod:acidity`

```json
{
  "values": {
    "roll_mod:sulfur_dust": 250,
    "roll_mod:quicklime": { "value": -250 }
  }
}
```

`value` is **signed**, in acidity units — the same unit as the bed's acidity buffer, which runs from
−10000 to +10000 and is the one buffer that never grows with node size.

- **positive = acid** (sulfur, sulfuric acid, phosphoric acid). Stored above zero. Consumed when the
  bed is set to an acidic pH (slider 1–3).
- **negative = base** (sodium hydroxide, quicklime, potash). Stored below zero. Consumed when the
  bed is set to an alkaline pH (slider 5–7).
- pH 4 (neutral) consumes nothing.

Running the bed always drags the buffer back toward zero: pH 3/2/1 spend 10/100/500 units per tick
per bed off the positive side, pH 5/6/7 spend the same off the negative side. A bed whose reservoir
cannot cover a whole step that tick is simply not supplied — it spends nothing and falls back to
1x growth.

## `roll_mod:fertilizer`

```json
{
  "values": {
    "minecraft:bone_meal": 100,
    "roll_mod:liquid_fertilizer": { "amount": 1000 }
  }
}
```

`amount` is millibuckets of fertilizer added to the buffer (base capacity 5 buckets). Fertility
slider 1 consumes nothing; sliders 2–6 consume 2/3/4/5/6 mB per tick per bed.

## Units: items vs fluids

- **Item maps** — the value is what **one item** contributes.
- **Fluid maps** — the value is what **one bucket (1000 mB)** contributes, scaled linearly. A fluid
  worth `1000` fertilizer converts 1 mB of fluid into 1 mB of buffer; a diluted one worth `250`
  needs four buckets to fill one. This is the single easiest thing to get wrong by a factor of 1000.

## Shorthand

Both maps accept a bare number in place of the object form, so `"minecraft:bone_meal": 100` and
`"minecraft:bone_meal": {"amount": 100}` are identical. Use whichever reads better.

## Where reagents can go in

Each of the three shared slots on the GUI (acidity, water, fertilizer) accepts **either**:

- a dry item registered in the matching data map — one item is eaten per tick, and its crafting
  remainder (an empty bottle, say) is left in the slot; or
- any fluid container whose fluid is registered in that map — it is drained in place and the empty
  container stays in the slot. Slots holding a container are capped at one item, because there is
  nowhere else for the emptied container to go.

Pipes can also push straight into the block on any face through `Capabilities.FluidHandler.BLOCK`.
The block routes by what the fluid is, not by which face or tank was addressed, so a fluid only
needs to be in the right data map. The bed is a sink: it never lets anything be drained back out.
