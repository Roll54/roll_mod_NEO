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

## 8. Machines in Java (no editor, no `.sm`)

A machine does not have to come from the MBD2 editor. `roll_mod:crop_manager_mk2` is assembled in
Java in
[`RollMBD2Machines.java`](../common/src/main/java/com/roll_54/roll_mod/compat/MBD2/machine/RollMBD2Machines.java),
which is the pattern to copy.

### Where custom machine logic goes
MBD2 has **no machine class to subclass**, and `MachineEvent.postCustomEvent()` only forwards to
KubeJS — there is no NeoForge event for per-machine ticks. The one supported hook for custom Java
behaviour is **`ITrait.serverTick()`**. So machine logic lives in a custom trait
(`TraitDefinition` + `Trait`), registered from `RollMBD2Plugin.registerTraitTypes()` exactly like
`mi_energy_storage`. See
[`CropHarvesterTrait`](../common/src/main/java/com/roll_54/roll_mod/compat/MBD2/crops/CropHarvesterTrait.java).

### Building the definition
```java
@SubscribeEvent
public void onRegisterMachines(MBDRegistryEvent.Machine event) {
    event.register(MBDMachineDefinition.builder()
            .id(RollMod.id("crop_manager_mk2"))
            .rootState(StateMachine.createSingleDefault(builderSupplier, rendererSupplier))
            .blockProperties(ConfigBlockProperties.builder()
                    .rotationState(RotationState.NON_Y_AXIS)   // gives the block a facing
                    .build())
            .machineSettings(MyMachines::settings)              // a Supplier, re-invoked per load
            .recipeLogicSettings(ConfigRecipeLogicSettings.builder().enable(false).build())
            .build());
}
```

Every `MBDMachineDefinition.Builder` field is null-checked in the constructor, so anything you leave
unset falls back to a default. Two defaults bite:

| Default | Why it matters |
|---|---|
| `ConfigRecipeLogicSettings.enable = true` | A machine with no recipe type still ticks `RecipeLogic`. Set `enable(false)` for a trait-driven machine. |
| `ConfigBlockProperties.rotationState = NONE` | `MBDMachine#getFrontFacing()` is then empty, so "behind the machine" has no meaning. |

MBD2 registers the block, item and block entity under the definition's own id, so the lang key is
`block.<namespace>.<path>` and the block is **not** in the mod's `DeferredRegister` — datagen that
needs it must use `addOptional(id)` rather than `add(block)`.

### ⚠️ Registration runs before items exist
MBD2 fires `MBDRegistryEvent.Machine` from `FMLConstructModEvent`, i.e. **before `RegisterEvent`**.
Nothing in a definition may resolve a `DeferredHolder` — `ItemRegistry.FOO.get()` throws there. That
is why the Crop Manager Mk2's herbicide slot filters on the item *tag* `roll_mod:herbicides`
(a `ResourceLocation`, resolved lazily at runtime) instead of on item instances.

### The GUI
`ConfigMachineSettings.builder().uiTemplate(UITemplate.of(root))` takes an LdLib2 element tree —
build it the same way as the economy UIs (see `VendorUIHelper`). MBD2 binds it afterwards **by id**:

| Widget | Required id |
|---|---|
| Item slot | `<trait definition>.uiId() + "_" + slotIndex` — i.e. `ui:<trait name>_<n>` |
| Trait bar (energy, fluid, …) | the trait definition's own `uiId()` |
| Machine name label | `ui:machine_name` |
| Progress / fuel bar | `ui:progress_bar` / `ui:fuel_bar` |

Note the `ui:` prefix — `IUIProviderTrait#uiId()` returns `"ui:" + definition.getName()`, so derive
ids from `uiId()` rather than writing them out. Because binding is by id, a layout later re-authored
in the F4 editor binds identically, and switching to it is one line:
`event.registerFromResource(getClass(), RollMod.MODID, "machine/crop_manager_mk2.sm")`.

The trade-off of building in Java: the definition is not a project file, so the F4 editor **cannot
edit it in place**.

---

## 9. Machine looks: per-state models, and trait-owned settings

### Idle vs active, and facing
A machine's visual state is a **`MachineState` with its own renderer**, not a blockstate. MBD2 resolves
`machine.getMachineState().getRealRenderer()` on every `getQuads` call, and **a child state with no
renderer of its own inherits its parent's**:

```java
MachineState.Builder<MachineState> builder =
        (MachineState.Builder<MachineState>) MachineState.baseBuilder();
return builder
        .modelRenderer(RollMod.id("block/crop_manager_mk2"))            // base  -> idle
        .child("working", w -> w.modelRenderer(RollMod.id("block/crop_manager_mk2_active"))
                                .child("waiting"))                      // inherits -> active
        .child("suspend")                                               // inherits -> idle
        .build();
```
Switch at runtime with `machine.setMachineState("working" | "base")`. It **early-outs when the state is
unchanged**, so calling it every tick is free; on a real change it fires `notifyBlockUpdate()` and the
field is `@DescSynced`, so clients follow.

**Facing is free.** With `ConfigBlockProperties.rotationState(RotationState.NON_Y_AXIS)`, MBD2 bakes a
rotated copy of the model per facing and caches it (`MBDMachineBlock.getModelState` →
`ModelFactory.getRotation`). Author the model **facing NORTH** and use `minecraft:block/cube` when you
need a distinct back face — `block/orientable` has no `south` slot. No blockstate JSON is needed or
used: LDLib's `BlockStateModelLoaderMixin` short-circuits blockstate loading for these blocks.

> **Not the MI way, deliberately.** Modern Industrialization does the opposite — it never rotates the
> model and instead index-swaps a sprite per face (`MachineBakedModel.getSprite`), with active/idle as
> a `MachineModelClientData` flag. MI's model loader also resolves machines by
> `BuiltInRegistries.BLOCK.get(MI.id(name))`, so it only works for blocks registered in MI's own
> namespace through MI's pipeline — an MBD2 machine can never be one. Same visual result, different
> engine.

An animated face is an ordinary vertical strip PNG **plus a `.png.mcmeta`**; without the meta file
Minecraft renders the whole strip squashed onto one face.

**Overlay art needs a casing under it.** `ConfigBlockProperties.RenderTypes` defaults to **cutout**
(applied through `ItemBlockRenderTypes.setRenderLayer`), so a transparent pixel in a face texture is
discarded — and if that texture is the only thing on the face, you get a hole straight through the
block. Front/back art in the MI style is ~half transparent, so the model needs two elements: a full
`0..16` cube carrying the opaque side texture on all six faces, then the overlay faces nudged `0.01`
proud of it so they win the depth test instead of z-fighting:

```jsonc
"elements": [
  { "from": [0, 0, 0], "to": [16, 16, 16],
    "faces": { "north": {"texture": "#side", "cullface": "north"}, /* ...all six... */ } },
  { "from": [0, 0, -0.01], "to": [16, 16, 16.01],
    "faces": { "north": {"texture": "#front", "cullface": "north"},
               "south": {"texture": "#back",  "cullface": "south"} } }
]
```
The `_active` texture *replaces* its overlay rather than stacking on it, matching MI's
`front_active → front → side` fallback order.

### Per-machine settings on a trait
Put the state on the **trait** (per block), not the definition (shared config):

```java
@Persisted @DescSynced private boolean collectCrops = true;
```
`@Persisted` lands it in the machine's NBT, namespaced by trait name automatically
(`MBDMachine.loadAdditionalTraits`); `@DescSynced` pushes it to clients. The trait's existing
`ManagedFieldHolder` picks the fields up with no extra registration.

To give the trait widgets, make its **definition** `implements IUIProviderTrait`:
`createTraitUITemplate(UIElement)` builds them, `initTraitUI(ITrait, UI)` binds them — `bindMachineUI`
calls the latter for every trait definition implementing the interface. LDLib has **no `Checkbox`**;
the boolean widgets are `Switch` and `Toggle`, bound with
`DataBindingBuilder.bool(getter, setter)` (also `boolS2C` / `boolC2S`).

> **Trap:** seed with `switch.setOn(value, false)`. `setOn(value)` defaults `notify = true`, fires the
> change listener as the GUI opens and bounces a spurious write back at the server.

#### A `UITemplate` is data — listeners set at build time are lost

This is the one that bites hardest, because it fails silently:

```java
UITemplate.of(root)   // -> root.serializeNBT(...)  : the tree becomes a CompoundTag
template.createUI()   // -> new UIElement() + deserializeNBT(data) : a BRAND NEW tree
```
The element the machine actually shows is **never the element you built**. Element types survive
(every widget is `@LDLRegister`-ed into `ldlib2:ui_element`), and so do ids, classes, layout and
styles — but a `setOnClick`, `setOnSwitchChanged` or `addEventListener` lambda attached while
building the template is dropped on the floor. The symptom is a widget that does nothing, with
**nothing whatsoever in the log**.

So the template may only carry *appearance and ids*. Everything behavioural is re-attached to the
live UI in `initTraitUI(ITrait, UI)`, found by id:

```java
UIElement panel = ui.selectId(settingId(SETTINGS_PANEL), UIElement.class).findFirst().orElse(null);
ui.selectId(settingId(SETTINGS_TOGGLE), Button.class)
        .forEach(gear -> gear.setOnClick(e -> panel.setDisplay(!panel.isDisplayed())));
```
This is also why MBD2 itself binds *everything* by id (`ui:machine_name`, `ui:progress_bar`,
`ui:<trait>_<slot>`) rather than wiring widgets as it creates them.

A panel that overhangs the main window needs `overflowVisible(true)` on the root, or it is clipped.

### In-world overlays without touching the client module
`TraitDefinition.getBESRenderer(IMachine)` returns an `IRenderer` that `MBDBlockRenderer` already calls
for every trait — **no `RenderLevelStageEvent`, no client-module registration**. Implement
`hasBlockEntityRenderer`, `render`, and usually `shouldRenderOffScreen` +
`getRenderBoundingBox` when the overlay is larger than the block. Draw with
`RenderBufferUtils.drawCubeFrame(...)` / `shapeCube(...)` on `LDLibRenderTypes.noDepthLines()`.

> **One renderer instance is shared by every machine of the definition**, so read the facing and any
> toggles off the `BlockEntity` passed to `render(...)` (via `IMachine.ofMachine(be)` →
> `getTraitByName(...)`) — never off the definition. Keep the renderer in a lazily-created field so the
> class is never loaded on a dedicated server.

#### `hasBlockEntityRenderer` is sampled when the chunk compiles, not per frame

LDLib's `BlockEntityRendererDispatcherMixin` makes vanilla's
`BlockEntityRenderDispatcher.getRenderer(be)` return **null** whenever
`IRenderer.hasBlockEntityRenderer(be)` is false — and vanilla calls `getRenderer` **while compiling a
chunk section**, to decide whether a block entity belongs in that section's renderable list at all.

So `hasBlockEntityRenderer` (and `shouldRenderOffScreen`, same story) must be **constant**. Gate them
on a mutable per-machine flag and the machine is baked out of the section: flipping the flag on then
changes nothing until something forces a chunk rebuild — placing or breaking a block nearby. The
symptom is an overlay that "only appears after I update a block".

```java
public boolean hasBlockEntityRenderer(BlockEntity be) { return true; }   // constant
public boolean shouldRenderOffScreen(BlockEntity be)  { return true; }   // constant
public int     getViewDistance()                      { return 64; }     // bound the cost
public void render(BlockEntity be, ...) {
    if (!enabledFor(be)) return;                                         // per-frame decision HERE
    ...
}
```
`shouldRenderOffScreen == true` also puts the machine in the level's *global* block-entity list, which
is drawn without a frustum check — that is what keeps an overlay visible while the player stands
inside it with the machine itself behind them. Bound `getViewDistance()` to pay for that.

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
