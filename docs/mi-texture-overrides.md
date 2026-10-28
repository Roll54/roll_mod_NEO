# MI texture overrides

How Modern Industrialization (MI 2.4.1) builds its item textures at runtime, and where a custom
texture (e.g. `incoloy_gear`) has to go to actually show up.

## How MI generates textures

On the client roll_mod forces MI's runtime datagen on (`MIClientForceRuntimeDatagenMixin`) and
always loads its output (`MIForceRuntimeResourcesMixin`). At startup MI:

1. Reads grayscale part templates (`modern_industrialization:textures/materialsets/common/<part>.png`,
   e.g. `gear.png`, `ring.png`) and tints them per material.
2. Writes the result to `<gamedir>/modern_industrialization/generated_resources`.
3. Loads that folder as a built-in pack at `Pack.Position.TOP`, which is above the mod resources pack.

MI's datagen reads only from vanilla, MI's own jar and
`<gamedir>/modern_industrialization/extra_datagen_resources`. It does **not** read roll_mod's jar.

## What `textures/override` does

`assets/roll_mod/textures/override/modern_industrialization/textures/materialsets/common/` is read by
`MITextureManagerOverrideMixin`. It replaces MI's **part templates** only:

- `gear.png` there changes the gear shape for **every** material, not just one.
- A per-item file such as `incoloy_gear.png` there is **ignored**, because MI never asks for a template by that name.
- A file there is used for that template name in **every** material set, not only `common`: MI checks the set's
  own folder first (e.g. `shiny/gear_overlay.png`), so a `common` override must also win over set-specific ones.
  Templates we have no file for (other set-specific, gem, ore and block templates) are untouched.
- `<part>_underlay.png` / `<part>_overlay.png` are drawn untinted under/over the tinted part (`gear_overlay`,
  `ring_overlay`, `blade_overlay`, `fine_wire_overlay`).
- `casing_4_rivets_ct.png`, `casing_12_rivets_ct.png`, `casing_pipe_ct.png` are 4x4 Fusion "simple" sheets for
  `machine_casing`, `machine_casing_special` and `machine_casing_pipe` ("MI in New" names): `MICasingConnectedTexturesMixin`
  tints them into `<casing>_ct` / `<casing>_mct` connected textures for every material casing (see the class for the
  generated models and mcmetas). The older `machine_casing_ct.png`, `machine_casing_special_ct.png`,
  `machine_casing_pipe_ct.png` are the fallback when one of those is missing (`MiCasingConnectedTextures.ctTemplates`).
- `machine_casing.png`, `machine_casing_special.png`, `machine_casing_pipe.png` and `crushed_dust.png` replace MI's own
  templates of the same name, like `gear.png`.

## Why a per-item texture in our jar loses

A file at `assets/modern_industrialization/textures/item/incoloy_gear.png` in roll_mod's jar sits in
the mod resources pack. MI's generated pack is on TOP, so MI's generated `incoloy_gear.png` hides it.
This applies to any MI item MI generates itself. It may affect the MI item textures already shipped
this way (e.g. `gold_gear.png`, `tin_curved_plate.png`, and from the "MI in New" pack `osmiridium_*`,
`gold_large_plate.png`, YAI's `beryllium_large_plate.png` — shipped because the dev run doesn't generate them; if
the pack's materials do, the generated ones win).

Hand-made, non-generated MI/EI/YAI textures (machine overlays, hulls, casings like `bricked_*`, items such as
`wrench`, `quantum_circuit_board`) are plain overrides in `common/src/main/resources/assets/<modid>/textures/` and
always apply.

## MI's own per-item override

Before writing a generated texture, `TextureManager.addTexture` looks for the same path with
`textures/` replaced by `datagen_texture_overrides/`. If that file exists, MI uses it instead of the
generated one:

```
modern_industrialization:textures/item/incoloy_gear.png
  -> checked first: modern_industrialization:datagen_texture_overrides/item/incoloy_gear.png
```

Because MI looks only in the sources listed above, the file currently has to be placed **per
instance**:

```
<gamedir>/modern_industrialization/extra_datagen_resources/assets/modern_industrialization/datagen_texture_overrides/item/incoloy_gear.png
```

This folder exists in `client/run` and `client/run2`, but it is not part of the mod, so every
client/instance needs the file.

## Option: ship per-item overrides in the mod (not implemented)

Extend `MITextureManagerOverrideMixin` so MI's `datagen_texture_overrides/...` lookup in `addTexture`
also checks roll_mod's jar, the same way part templates are rerouted now. Per-item textures would
then live in a folder inside roll_mod (location to be decided, e.g.
`assets/roll_mod/textures/override/modern_industrialization/datagen_texture_overrides/item/`) and
would always win over MI's generated textures.

Note: `addTexture` queries the resource provider directly (not through `hasAsset` /
`getAssetAsTexture`), so the mixin needs a new injection there. The existing two injections don't
cover it.
