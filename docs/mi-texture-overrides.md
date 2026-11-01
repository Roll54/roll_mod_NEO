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

## Shipped textures beat MI's generated ones

A file at e.g. `assets/modern_industrialization/textures/item/incoloy_gear.png` in roll_mod's jar sits in the
mod resources pack, below MI's generated pack, so on its own MI's generated `incoloy_gear.png` would hide it.
`MITextureManagerOverrideMixin#roll_mod$shippedTextureWins` fixes that: whenever MI's `TextureManager.addTexture`
is about to generate `<ns>:textures/<path>` and roll_mod's jar has `assets/<ns>/textures/<path>`, our file is
written into the generated pack instead of MI's. So every per-material item/block texture we ship -- the whole
"MI in New" pack, synced into `common/src/main/resources/assets/` -- wins over MI's datagen, for MI and for the
addons whose textures MI's runtime datagen generates.

Hand-made, non-generated MI/EI/YAI/HNI textures (machine overlays, hulls, casings like `bricked_*`, items such as
`wrench`, `quantum_circuit_board`) were already plain overrides in `common/src/main/resources/assets/<modid>/textures/`.

To update from a new version of the pack: copy its `assets/` over `common/src/main/resources/assets/`, except
`assets/modern_industrialization/textures/materialsets/`, which goes to
`assets/roll_mod/textures/override/modern_industrialization/textures/materialsets/` (see above).

## MI's own per-item override

MI itself also looks for `<ns>:datagen_texture_overrides/<path>` before generating, but only in vanilla, MI's
jar and `<gamedir>/modern_industrialization/extra_datagen_resources` -- not in roll_mod's jar, which is why the
mixin above exists.
