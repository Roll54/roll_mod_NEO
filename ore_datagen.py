#!/usr/bin/env python3
"""
Minecraft Ore Data Generator (multi-base + overlay + item-base)

- Bases: one or many of [mars, moon, stone, netherrack] or 'all'
- Overlay: exactly one of [coal, copper, diamond, gold, iron, lapis, redstone, quartz]
- Item base (raw template): exactly one of [copper, gold, iridium, iron, uranium]
- Output block texture name: <base>_<overlay>_<ore_name>.png
- Models/block, blockstates, item models are generated per-base (distinct block IDs).
- Loot tables are generated per-base: data/roll_mod/loot_tables/blocks/<block_id>.json

Source templates are loaded from:
  src/main/resources/assets/roll_mod/py_datagen/blocks/(sub_layers|overlays)
  src/main/resources/assets/roll_mod/py_datagen/items/bases
"""

import argparse
import json
import os
import platform
from pathlib import Path
from typing import List, Optional
from PIL import Image

VALID_BASES = ["mars", "moon", "stone", "netherrack"]
VALID_OVERLAYS = ["coal", "copper", "diamond", "gold", "iron", "lapis", "redstone", "quartz"]
VALID_ITEM_BASES = ["copper", "gold", "iridium", "iron", "uranium"]

class OreDataGenerator:
    def __init__(self, workspace_path: str):
        self.workspace_path = Path(workspace_path)

        # ASSETS output (standard MC layout)
        self.assets_path = self.workspace_path / "src" / "main" / "resources" / "assets" / "roll_mod"
        self.textures_path = self.assets_path / "textures"
        self.models_path = self.assets_path / "models"
        self.blockstates_path = self.assets_path / "blockstates"

        # DATA output (loot tables)
        self.data_path = self.workspace_path / "src" / "main" / "resources" / "data" / "roll_mod"
        self.loot_tables_blocks_path = self.data_path / "loot_tables" / "blocks"

        # Datagen sources
        self.datagen_root = self.workspace_path / "src" / "main" / "resources" / "assets" / "roll_mod" / "py_datagen"
        self.block_sub_layers_dir = self.datagen_root / "blocks" / "sub_layers"
        self.block_overlays_dir   = self.datagen_root / "blocks" / "overlays"
        self.item_bases_dir       = self.datagen_root / "items"  / "bases"

        self._ensure_directories()

    def _ensure_directories(self):
        # Outputs
        (self.textures_path / "block").mkdir(parents=True, exist_ok=True)
        (self.textures_path / "item").mkdir(parents=True, exist_ok=True)
        (self.models_path / "block").mkdir(parents=True, exist_ok=True)
        (self.models_path / "item").mkdir(parents=True, exist_ok=True)
        self.blockstates_path.mkdir(parents=True, exist_ok=True)
        self.loot_tables_blocks_path.mkdir(parents=True, exist_ok=True)

    # ------------ image helpers ------------
    @staticmethod
    def hex_to_rgb(hex_color: str):
        hex_color = hex_color.lstrip('#')
        return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

    def apply_color_multiply(self, image: Image.Image, hex_color: str) -> Image.Image:
        if image.mode != 'RGBA':
            image = image.convert('RGBA')
        r, g, b = self.hex_to_rgb(hex_color)

        base_pixels = image.load()
        w, h = image.size
        for y in range(h):
            for x in range(w):
                pr, pg, pb, pa = base_pixels[x, y]
                if pa == 0:
                    continue
                base_pixels[x, y] = (
                    (pr * r) // 255,
                    (pg * g) // 255,
                    (pb * b) // 255,
                    pa
                )
        return image

    # ------------ source locating ------------
    def _require_file(self, path: Path, purpose: str):
        if not path.exists():
            raise FileNotFoundError(f"Missing {purpose}: {path}")
        return path

    def get_block_sub_layer_path(self, base_name: str) -> Path:
        # primary
        p = self.block_sub_layers_dir / f"{base_name}.png"
        if p.exists(): return p
        # fallback (flat dir)
        p2 = self.datagen_root / "blocks" / f"{base_name}.png"
        return self._require_file(p2, f"block sub-layer '{base_name}'")

    def get_block_overlay_path(self, overlay_name: str) -> Path:
        p = self.block_overlays_dir / f"{overlay_name}.png"
        if p.exists(): return p
        p2 = self.datagen_root / "blocks" / f"{overlay_name}.png"
        return self._require_file(p2, f"block overlay '{overlay_name}'")

    def get_item_base_path(self, item_base: str) -> Path:
        p = self.item_bases_dir / f"{item_base}.png"
        if p.exists(): return p
        p2 = self.datagen_root / "items" / f"{item_base}.png"
        return self._require_file(p2, f"item base '{item_base}'")

    # ------------ create outputs ------------
    def create_block_texture(self, base_name: str, overlay_name: str, ore_name: str, hex_color: str) -> Path:
        sub_layer_path = self.get_block_sub_layer_path(base_name)
        overlay_path   = self.get_block_overlay_path(overlay_name)

        base_layer = Image.open(sub_layer_path).convert('RGBA')
        overlay    = Image.open(overlay_path).convert('RGBA')

        # tint overlay
        overlay_tinted = self.apply_color_multiply(overlay, hex_color)

        result = Image.alpha_composite(base_layer, overlay_tinted)
        out_name = f"{base_name}_{overlay_name}_{ore_name}.png"
        out_path = self.textures_path / "block" / out_name
        result.save(out_path)
        print(f"Created block texture: {out_path}")
        return out_path

    def create_item_texture(self, item_name: str, item_base: str, hex_color: str) -> Path:
        base_path = self.get_item_base_path(item_base)
        base_img = Image.open(base_path).convert('RGBA')
        tinted = self.apply_color_multiply(base_img, hex_color)
        out_path = self.textures_path / "item" / f"{item_name}.png"
        tinted.save(out_path)
        print(f"Created item texture: {out_path}")
        return out_path

    def create_block_model(self, block_id: str, texture_key: str) -> Path:
        # texture_key like "roll_mod:block/<base>_<overlay>_<ore>"
        model_data = {
            "parent": "minecraft:block/cube_all",
            "textures": {
                "all": texture_key
            }
        }
        out_path = self.models_path / "block" / f"{block_id}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(model_data, f, indent=2)
        print(f"Created block model: {out_path}")
        return out_path

    def create_block_item_model(self, block_id: str) -> Path:
        # Block item model just points to the block model
        model_data = {
            "parent": f"roll_mod:block/{block_id}"
        }
        out_path = self.models_path / "item" / f"{block_id}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(model_data, f, indent=2)
        print(f"Created block-item model: {out_path}")
        return out_path

    def create_raw_item_model(self, item_name: str) -> Path:
        model_data = {
            "parent": "item/generated",
            "textures": {
                "layer0": f"roll_mod:item/{item_name}"
            }
        }
        out_path = self.models_path / "item" / f"{item_name}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(model_data, f, indent=2)
        print(f"Created raw item model: {out_path}")
        return out_path

    def create_blockstates(self, block_id: str) -> Path:
        # Звичайний blockstate без параметрів
        blockstates_data = {
            "variants": {
                "": {"model": f"roll_mod:block/{block_id}"}
            }
        }

        out_path = self.blockstates_path / f"{block_id}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(blockstates_data, f, indent=2)
        print(f"Created simple blockstate: {out_path}")
        return out_path

    def create_loot_table(self, block_id: str, item_name: str) -> Path:
        # Follows the provided template
        loot = {
            "type": "minecraft:block",
            "pools": [
                {
                    "bonus_rolls": 0.0,
                    "entries": [
                        {
                            "type": "minecraft:alternatives",
                            "children": [
                                {
                                    "type": "minecraft:item",
                                    "conditions": [
                                        {
                                            "condition": "minecraft:match_tool",
                                            "predicate": {
                                                "predicates": {
                                                    "minecraft:enchantments": [
                                                        {
                                                            "enchantments": "minecraft:silk_touch",
                                                            "levels": {"min": 1}
                                                        }
                                                    ]
                                                }
                                            }
                                        }
                                    ],
                                    "name": f"roll_mod:{block_id}"
                                },
                                {
                                    "type": "minecraft:item",
                                    "functions": [
                                        {
                                            "enchantment": "minecraft:fortune",
                                            "formula": "minecraft:ore_drops",
                                            "function": "minecraft:apply_bonus"
                                        },
                                        {"function": "minecraft:explosion_decay"}
                                    ],
                                    "name": f"roll_mod:{item_name}"
                                }
                            ]
                        }
                    ],
                    "rolls": 1.0
                }
            ],
            "random_sequence": f"minecraft:blocks/{block_id}"
        }
        out_path = self.loot_tables_blocks_path / f"{block_id}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(loot, f, indent=2)
        print(f"Created loot table: {out_path}")
        return out_path

    # ------------ main orchestration ------------
    def generate_ore_sets(
        self,
        ore_name: str,
        item_name: str,
        hex_color: str,
        bases: List[str],
        overlay: str,
        item_base: str
    ):
        print("\n=== Generating ore sets ===")
        print(f"Ore Name      : {ore_name}")
        print(f"Item Name     : {item_name}")
        print(f"Hex Color     : {hex_color}")
        print(f"Bases         : {', '.join(bases)}")
        print(f"Overlay       : {overlay}")
        print(f"Item Base     : {item_base}")

        # one shared raw item texture/model
        item_texture = self.create_item_texture(item_name, item_base, hex_color)
        raw_item_model = self.create_raw_item_model(item_name)

        results = []

        for base in bases:
            # texture filename: <base>_<overlay>_<ore_name>.png
            block_texture = self.create_block_texture(base, overlay, ore_name, hex_color)

            # block id per base: <base>_<overlay>_<ore_name>
            block_id = f"{base}_{overlay}_{ore_name}"

            # block model & block-item model point to texture "roll_mod:block/<base>_<overlay>_<ore_name>"
            texture_key = f"roll_mod:block/{base}_{overlay}_{ore_name}"
            block_model = self.create_block_model(block_id, texture_key)
            block_item_model = self.create_block_item_model(block_id)

            # blockstates: minable switch; model for minable=0 must be same as block_id
            blockstates = self.create_blockstates(block_id)

            # loot table for this block
            loot_table = self.create_loot_table(block_id, item_name)

            results.append({
                "base": base,
                "block_id": block_id,
                "block_texture": block_texture,
                "block_model": block_model,
                "block_item_model": block_item_model,
                "blockstates": blockstates,
                "loot_table": loot_table
            })

        print("\n✅ Done.")
        return {
            "item_texture": item_texture,
            "raw_item_model": raw_item_model,
            "blocks": results
        }

# ----------------- CLI -----------------
def detect_default_workspace() -> str:
    current_dir = Path.cwd()
    if current_dir.name == "roll_mod":
        return str(current_dir)
    roll_mod_path = current_dir / "roll_mod"
    if roll_mod_path.exists() and roll_mod_path.is_dir():
        return str(roll_mod_path)

    if platform.system() == "Windows":
        possible_paths = [
            Path.home() / "Desktop" / "Github" / "roll_mod",
            Path.home() / "Documents" / "roll_mod",
            Path("C:") / "workspace" / "roll_mod",
            current_dir
        ]
    else:
        possible_paths = [
            Path.home() / "workspace" / "roll_mod",
            Path.home() / "projects" / "roll_mod",
            Path.home() / "git" / "roll_mod",
            Path.home() / "roll_mod",
            Path("/workspace/roll_mod"),
            current_dir
        ]
    for p in possible_paths:
        if p.exists() and p.is_dir():
            return str(p)
    return str(current_dir)

def parse_bases(arg: str) -> List[str]:
    arg = arg.strip().lower()
    if arg == "all":
        return VALID_BASES.copy()
    bases = [x.strip().lower() for x in arg.split(",") if x.strip()]
    for b in bases:
        if b not in VALID_BASES:
            raise argparse.ArgumentTypeError(f"Unknown base '{b}'. Valid: {', '.join(VALID_BASES)} or 'all'.")
    # dedup preserving order
    seen, out = set(), []
    for b in bases:
        if b not in seen:
            seen.add(b)
            out.append(b)
    return out

def main():
    ap = argparse.ArgumentParser(description="Generate ore textures/models/blockstates/loot for multiple bases + overlay + item base")
    ap.add_argument("-w", "--workspace", default=detect_default_workspace(), help="Path to workspace (root of your mod)")

    # індивідуальний режим
    ap.add_argument("-o", "--ore-name", help="Ore material name, e.g. tungsten")
    ap.add_argument("-i", "--item-name", help="Drop item registry name, e.g. raw_tungsten")
    ap.add_argument("-c", "--hex-color", help="Hex color for tint (e.g. #3b2ab8 or 3b2ab8)")
    ap.add_argument("-B", "--bases", type=parse_bases, help="Comma-separated bases (mars,moon,stone,netherrack) or 'all'")
    ap.add_argument("-O", "--overlay", choices=VALID_OVERLAYS, help="Overlay (exactly one)")
    ap.add_argument("-I", "--item-base", choices=VALID_ITEM_BASES, help="Item base (exactly one)")

    # режим batch
    ap.add_argument("-b", "--batch", help="Path to batch JSON file with multiple ores")

    args = ap.parse_args()
    gen = OreDataGenerator(args.workspace)

    if args.batch:
        batch_path = Path(args.batch)
        if not batch_path.exists():
            raise FileNotFoundError(f"Batch file not found: {batch_path}")
        with open(batch_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        for entry in data.get("ores", []):
            ore_name = entry["ore_name"]
            item_name = entry["item_name"]
            hex_color = entry["hex_color"]
            bases = parse_bases(",".join(entry["bases"]) if isinstance(entry["bases"], list) else entry["bases"])
            overlay = entry["overlay"]
            item_base = entry["item_base"]

            print(f"\n=== Processing {ore_name} ===")
            gen.generate_ore_sets(
                ore_name=ore_name,
                item_name=item_name,
                hex_color=hex_color,
                bases=bases,
                overlay=overlay,
                item_base=item_base
            )
    else:
        # звичайний CLI-режим
        if not all([args.ore_name, args.item_name, args.hex_color, args.bases, args.overlay, args.item_base]):
            ap.error("Either --batch or all of (-o, -i, -c, -B, -O, -I) must be provided.")
        gen.generate_ore_sets(
            ore_name=args.ore_name,
            item_name=args.item_name,
            hex_color=args.hex_color,
            bases=args.bases,
            overlay=args.overlay,
            item_base=args.item_base
        )

if __name__ == "__main__":
    main()