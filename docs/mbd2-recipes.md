# MBD2 Recipe Types & Recipes (with EU)

How to create Multiblocked2 (MBD2) **recipe types** and **recipes** for `roll_mod`, including recipes
that consume **EU** (Modern Industrialization energy) via our custom
[`mi_energy_storage` trait](../common/src/main/java/com/roll_54/roll_mod/compat/MBD2/energy).

The worked example lives at
[`ExampleLavaRecipe.java`](../common/src/main/java/com/roll_54/roll_mod/compat/MBD2/recipe/ExampleLavaRecipe.java)
— EU + water → lava.

---

## 1. How the recipe system fits together

```
Machine (built in the MBD2 editor)
 ├── Recipe Logic component      ← makes the machine actually run recipes
 ├── Recipe Type = roll_mod:...  ← which recipe set this machine runs
 └── Traits (capabilities)       ← the machine's I/O: EU store, fluid tanks, item slots…

Recipe Type  (MBDRecipeType)      ← a ResourceLocation-keyed group of recipes + a JEI/XEI UI
 └── Recipe  (MBDRecipe)          ← inputs/outputs expressed as "recipe capabilities"

Recipe capabilities (what a recipe can require/produce):
  item          ItemRecipeCapability.CAP            (Ingredient / ItemStack)
  fluid         FluidRecipeCapability.CAP           (SizedFluidIngredient / FluidStack)
  forge_energy  ForgeEnergyRecipeCapability.CAP     (Integer)  ← EU goes here (see §2)
  entity        EntityRecipeCapability.CAP
  item_durability ItemDurabilityRecipeCapability.CAP
```

A recipe **runs** when the machine's traits can satisfy every input capability and store every output.
So the recipe and the machine's traits must line up: a recipe that needs `fluid` input requires the
machine to have a fluid tank trait with IO = `IN`, etc.

### EU is `forge_energy`
Our `mi_energy_storage` trait deliberately reuses MBD2's built-in `forge_energy` recipe capability.
Its recipe handler consumes the recipe's `forge_energy` amount from the machine's EU buffer **1:1 as
EU** (`MIEnergyTrait.MIEnergyRecipeHandler`). So to make a recipe "cost EU", you add a
`ForgeEnergyRecipeCapability.CAP` input — the number is EU. (JEI will label it with the FE icon; the
value is EU.)

---

## 2. Creating a recipe type

### Option A — In the MBD2 editor (recommended for a polished JEI UI)
1. Open the MBD2 editor (creative-mode gadget / `/mbd` UI).
2. Create or open a **Machine project**, or a standalone **Recipe Type project**.
3. Give the recipe type a registry name, e.g. `roll_mod:lava_smeltery`, set its icon and JEI layout.
4. Assign that recipe type (and a **Recipe Logic**) to your machine, and add the traits the recipes
   need (see §6).
5. Export the project to `common/src/main/resources/assets/roll_mod/…` and load it (§3, `registerFromResource`).

The editor is the only way to author the fancy JEI/XEI layout. The recipes themselves can still be
written in Java (§4).

### Option B — In Java (code-only recipe type)
Simple and fully functional for the machine logic; uses a default JEI layout.

```java
MBDRecipeType type = new MBDRecipeType(RollMod.id("lava_smeltery"));
type.setXEIVisible(true);
// optional: type.setIcon(...); type.prepareBuilder(b -> b.duration(200)); // template applied to every recipe
```

---

## 3. Registering a recipe type

Recipe types are registered on the **mod event bus** via `MBDRegistryEvent.MBDRecipeType`:

```java
public class MyMBDRecipes {
    @SubscribeEvent
    public void onRegisterRecipeType(MBDRegistryEvent.MBDRecipeType event) {
        MBDRecipeType type = new MBDRecipeType(RollMod.id("lava_smeltery"));
        // ... add builtin recipes to `type` here (see §4) ...
        event.register(type);

        // OR load a recipe-type project exported from the editor:
        // event.registerFromResource(getClass(), "roll_mod/recipe_types/lava_smeltery.rt");
    }
}
```

Enable it by registering the handler on the mod bus (in `RollMod`'s constructor):

```java
eventBus.register(new MyMBDRecipes());
```

> Note: this is the same `MBDRegistryEvent` family used for machines
> (`MBDRegistryEvent.Machine` → `register` / `registerFromResource`).

---

## 4. Writing recipes in Java

Get a builder from the type, describe inputs/outputs, then finish. Two ways to finish:

| Method | Result | Use when |
|---|---|---|
| `.saveAsBuiltinRecipe()` | Stored on the recipe type, loaded when the recipe manager loads. No datapack. | Code-defined recipes shipped with the mod. |
| `.save(RecipeOutput)` | Emits JSON to `data/<modid>/recipe/…` from a `RecipeProvider` (datagen). | Datapack-style, reloadable, pack-overridable. |

### Builder cheat sheet
```java
MBDRecipeBuilder.of(id, recipeType)      // or recipeType.recipeBuilder("id")
    .duration(200)                        // total ticks
    .perTick(true)                        // inputs/outputs added AFTER this are per-tick…
    .input(ForgeEnergyRecipeCapability.CAP, 32)   // 32 EU/tick  (EU = forge_energy)
    .perTick(false)                       // …and these are consumed once per craft
    .inputFluids(new FluidStack(Fluids.WATER, 1000))
    .inputItems(Items.COBBLESTONE)        // also: SizedIngredient, TagKey, ItemStack, Supplier<Item>
    .notConsumable(someCatalystItem)      // required but not consumed
    .outputFluids(new FluidStack(Fluids.LAVA, 1000))
    .outputItems(new ItemStack(Items.NETHERRACK))
    .chance(0.5f)                         // output chance
    .dimension(Level.NETHER.location())   // condition: only in a dimension (also biome/rain/thunder/posY)
    .saveAsBuiltinRecipe();               // or .save(recipeOutput)
```

**`perTick` is stateful and ordering-sensitive.** Every `input`/`output` call captures the builder's
*current* `perTick` flag. Put `.perTick(true)` before the EU input (energy is spent every tick), then
`.perTick(false)` before fluids/items that are consumed once for the whole craft.

### Datagen variant
Inside a `RecipeProvider.buildRecipes(RecipeOutput output)` (registered in `RollDataGenerators` via
`GatherDataEvent`), swap the finisher:

```java
MBDRecipeBuilder.of(RollMod.id("water_to_lava"), lavaSmeltery)
    .duration(200)
    .perTick(true).input(ForgeEnergyRecipeCapability.CAP, 32).perTick(false)
    .inputFluids(new FluidStack(Fluids.WATER, 1000))
    .outputFluids(new FluidStack(Fluids.LAVA, 1000))
    .save(output);   // -> data/roll_mod/recipe/lava_smeltery/water_to_lava.json
```

---

## 5. Worked example: EU + water → lava

Full, compiling source:
[`ExampleLavaRecipe.java`](../common/src/main/java/com/roll_54/roll_mod/compat/MBD2/recipe/ExampleLavaRecipe.java)

```java
public class ExampleLavaRecipe {
    public static final ResourceLocation LAVA_SMELTERY = RollMod.id("lava_smeltery");

    @SubscribeEvent
    public void onRegisterRecipeType(MBDRegistryEvent.MBDRecipeType event) {
        MBDRecipeType lavaSmeltery = new MBDRecipeType(LAVA_SMELTERY);
        lavaSmeltery.setXEIVisible(true);

        MBDRecipeBuilder.of(RollMod.id("water_to_lava"), lavaSmeltery)
                .duration(200)
                .perTick(true)
                .input(ForgeEnergyRecipeCapability.CAP, 32)      // 32 EU/tick -> 6400 EU total
                .perTick(false)
                .inputFluids(new FluidStack(Fluids.WATER, 1000)) // 1000 mB water
                .outputFluids(new FluidStack(Fluids.LAVA, 1000)) // 1000 mB lava
                .saveAsBuiltinRecipe();

        event.register(lavaSmeltery);
    }
}
```

It is **not registered by default** (so it doesn't add a dangling recipe type to your game). To turn
it on, add to `RollMod`'s constructor:

```java
eventBus.register(new ExampleLavaRecipe());
```

---

## 6. Wiring the machine (editor side)

The recipe only runs if a machine uses the recipe type and has matching traits. In the editor, the
`lava_smeltery` machine needs:

| Recipe needs | Machine trait | IO |
|---|---|---|
| `forge_energy` input (EU) | **MI Energy Storage (EU)** (`mi_energy_storage`) | `IN` |
| `fluid` input (water) | Fluid Storage / tank | `IN` |
| `fluid` output (lava) | Fluid Storage / tank | `OUT` |
| (the machine running at all) | **Recipe Logic** + Recipe Type = `roll_mod:lava_smeltery` | — |

Make sure the EU trait's configured **capacity** ≥ the recipe's total EU (here 6400) and its **cable
tier** matches the cables feeding it.

---

## 7. Testing

- **Builtin recipes**: just launch the game (`./gradlew :client:runClient`). Place the machine, feed it
  EU (MI cable at the right tier) and water, and watch it output lava. Confirm JEI shows the recipe
  under the `lava_smeltery` category.
- **Datagen recipes**: run `./gradlew :client:runData` first to emit the JSON, then launch.
- **Sanity-check registration** headlessly: `./gradlew :server:runServer` and look for the recipe-type
  / trait registration log lines.

---

## Key classes (for reference)

| Purpose | Class |
|---|---|
| Recipe type | `com.lowdragmc.mbd2.api.recipe.MBDRecipeType` |
| Recipe builder | `com.lowdragmc.mbd2.api.recipe.MBDRecipeBuilder` |
| Register recipe types | `com.lowdragmc.mbd2.common.event.MBDRegistryEvent.MBDRecipeType` |
| EU / energy recipe capability | `com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability.CAP` |
| Fluid recipe capability | `com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability.CAP` |
| Item recipe capability | `com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability.CAP` |
| Recipe registry | `com.lowdragmc.mbd2.api.registry.MBDRegistries.RECIPE_TYPES` |
| Our EU trait | `com.roll_54.roll_mod.compat.MBD2.energy.MIEnergyTraitDefinition` |
