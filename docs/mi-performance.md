# Modern Industrialization performance changes

MI and its addons (EI, Tesseract, MI Tweaks, YAI) used 23% of the live server thread over 13 hours of spark profiling
(`nH00HdZJiW`, Oct 24–25, MI 2.5.11), and 24–29% of the ticks that went over 50 ms (`ipdl7CtI14`, `1vF3XoFxtJ`,
`bI67GFthQj`). The changes below remove work that produced nothing. Machine speed, recipes, EU amounts and outputs
are unchanged except for the cable changes listed at the end.

All of them are written against the versions the live pack runs, which roll_mod's dev build now also uses: MI 2.5.11,
Extended Industrialization 1.16.2, Tesseract API 1.12.16, MI Tweaks 1.9.5.

## What changed

| Change | Where | Cost before (13 h profile / lag ticks) | Behaviour |
|---|---|---|---|
| Neighbour capability lookups are cached (fluid auto-extract, item and fluid auto-insert, energy auto-output). MI already cached item auto-extract only | `mixin/mi/MIInventoryCapabilityCacheMixin`, `EnergyHelperMixin`, `MachineBlockEntityEnergyCacheMixin`, `compat/mi/perf/CapabilityCaches` | 2.8% / 3.3% of the server thread | None. NeoForge invalidates the cache when the neighbouring block or block entity changes |
| Tesseract's multiplied crafters (EI processing array and other "multiplied" machines) check a recipe at x1 before computing its multiplier | `compat/tesseract/mixin/MultipliedCrafterComponentMixin` (soft config `roll_mod.tesseract.mixins.json`) | up to 1.2% / 1.6% | None: a recipe that fails at x1 fails at every multiplier |
| `EnergyComponent.setEu` skips the save mark when the stored EU does not change | `mixin/mi/EnergyComponentMixin` | up to 0.4% / 0.7% | None |
| Recipe caches are rebuilt only when server data is (re)loaded, not every 20 s | DimThreads fork 1.3.1 (`RecipeStamp`, the recipe cache patches) | 0.2% on average, but 17% of the time inside over-50 ms ticks: EI's canning machine and composter rebuild their whole recipe list from the item registry | `/reload` applies immediately (was up to 20 s). Toggling EI's runtime-generated recipes in its config now needs a `/reload` |
| Energy cables: only cable blocks touching a machine are visited, and cables store no EU | `mixin/mi/ElectricityNetworkMixin`, `ElectricityNetworkNodeMixin`, `PipeNetworkMixin`, `compat/mi/energy/DirectEnergyTransfer` | 2.5% / 3.0% for all energy networks | See below |

Not done: replacing MI's max-efficiency loop with a formula (0.02 ms per average tick, about 1% of a lag tick; an
off-by-one would change overclock ramp timing).

## Energy cables

MI visited every cable block of a network every tick to find the machines next to it, and buffered up to the tier's
max transfer in every cable block (extract into the cables, insert from the cables). Now a network keeps a list of the
cable blocks that touch a machine and moves energy straight from the machines that give it to the machines that take
it, in the same tick. Cable blocks in between only connect, like item pipes.

- **Throughput is unchanged:** at most the tier's max transfer per network per tick (LV tin cable: 256 EU/t), split
  between consumers with MI's own fair-split algorithm.
- **Cables hold no EU.** EU stored in cables by the old system is deleted when the network next rebuilds its endpoint
  list (on load, or when cables change). The cable tooltip shows 0 stored.
- **A machine touching one network through two cable blocks gets one share,** not two.
- **No buffer:** if no consumer wants energy this tick, nothing is pulled from generators that tick. Energy arrives in
  the tick it leaves.
- Unchanged from MI: a cable only connects to a machine when the cable is placed or wrenched. A machine placed next to
  an existing cable later still needs the wrench.

## Checked on the dev server (2026-11-07)

- 200-block LV tin cable from a creative storage unit to an LV storage unit: 25,600 EU per 100 ticks (256 EU/t, the
  tier limit). Every cable block reads `eu: 0`; 5,000 EU seeded into each cable as "old" storage was deleted.
- Breaking a cable in the middle stops delivery; placing it back resumes it at the same rate.
- Two LV storage units, one touching the network through two cable blocks: 12,800 EU each per 100 ticks.
- Energy auto-output of a storage unit into a machine, and fluid auto-extract between two configurable tanks, keep
  working after the target block is broken and replaced.
- MI electric furnace (a proxy recipe type) smelts before and after `/reload`.
- DimThreads self-check 56/56 on MI 2.5.11 / EI 1.16.2 / Tesseract 1.12.16; the DimThreads stress scenario, AE2
  autocrafting in four dimensions and a 3,000-tick sprint: no errors.

## Checking it in game

1. A cable network with generators and machines: machines run, the cable tooltip shows 0 stored.
2. An EI processing array: it forms and runs batches with the same multiplier as before.
3. Hatches and machines with auto-extract/auto-insert into and out of AE2 interfaces, tanks and chests.
4. After deploying, take a spark profile on the live server and compare `MIInventory.auto*`, `EnergyHelper.autoOutput`,
   `ElectricityNetwork.tick`, `MultipliedCrafterComponent.canStartRecipe` and `MachineRecipeType.updateRecipeCache`
   with `nH00HdZJiW`.
