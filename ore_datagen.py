#!/usr/bin/env python3
"""
Minecraft Ore Data Generator
(multi-base + overlay + item-base + crushed/refined/purified + batch mode)
"""

import argparse
import json
from pathlib import Path
from typing import List, Optional
from PIL import Image

VALID_BASES = ["mars", "moon", "stone", "netherrack", "deepslate",
               "venus", "mercury", "end"]
VALID_OVERLAYS = ["coal", "copper", "diamond", "gold", "iron", "lapis",
                  "redstone", "quartz", "lead", "osmium", "tin", "uranium", "zinc"]
VALID_ITEM_BASES = ["copper", "gold", "iridium", "iron", "uranium", "coal",
                    "quartz", "diamond", "tin", "osmium", "zinc", "amethyst"]


class OreDataGenerator:
    def __init__(self, workspace_path: str):
        self.workspace_path = Path(workspace_path)

        # ASSETS output
        self.assets_path = self.workspace_path / "src" / "main" / "resources" / "assets" / "roll_mod"
        self.textures_path = self.assets_path / "textures"
        self.models_path = self.assets_path / "models"
        self.blockstates_path = self.assets_path / "blockstates"

        # DATA output
        self.data_path = self.workspace_path / "src" / "main" / "resources" / "data" / "roll_mod"
        self.loot_tables_blocks_path = self.data_path / "loot_table" / "blocks"

        # Datagen templates
        self.datagen_root = self.assets_path / "py_datagen"
        self.block_sub_layers_dir = self.datagen_root / "blocks" / "sub_layers"
        self.block_overlays_dir = self.datagen_root / "blocks" / "overlays"
        self.item_bases_dir = self.datagen_root / "items" / "bases"
        self.item_layers_dir = self.datagen_root / "items" / "layers"

        self._ensure_directories()

    def _ensure_directories(self):
        (self.textures_path / "block").mkdir(parents=True, exist_ok=True)
        (self.textures_path / "item").mkdir(parents=True, exist_ok=True)
        (self.models_path / "block").mkdir(parents=True, exist_ok=True)
        (self.models_path / "item").mkdir(parents=True, exist_ok=True)
        self.blockstates_path.mkdir(parents=True, exist_ok=True)
        self.loot_tables_blocks_path.mkdir(parents=True, exist_ok=True)

    # =========== IMAGE HELPERS ===========
    @staticmethod
    def hex_to_rgb(hex_color: str):
        hex_color = hex_color.lstrip('#')
        return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

    def apply_color_multiply(self, image: Image.Image, hex_color: str) -> Image.Image:
        if image.mode != 'RGBA':
            image = image.convert('RGBA')

        r, g, b = self.hex_to_rgb(hex_color)
        pix = image.load()

        for y in range(image.height):
            for x in range(image.width):
                pr, pg, pb, pa = pix[x, y]
                if pa == 0:
                    continue
                pix[x, y] = (
                    (pr * r) // 255,
                    (pg * g) // 255,
                    (pb * b) // 255,
                    pa
                )
        return image

    # =========== RESOURCE HELPERS ===========
    def _req(self, path: Path, name: str):
        if not path.exists():
            raise FileNotFoundError(f"Missing {name}: {path}")
        return path

    def get_block_sub_layer(self, base):
        return self._req(self.block_sub_layers_dir / f"{base}.png", f"block sub-layer '{base}'")

    def get_block_overlay(self, name):
        return self._req(self.block_overlays_dir / f"{name}.png", f"block overlay '{name}'")

    def get_item_base(self, base):
        return self._req(self.item_bases_dir / f"{base}.png", f"item base '{base}'")

    def get_item_layer(self, name):
        return self._req(self.item_layers_dir / f"{name}.png", f"item layer '{name}'")

    # =========== MULTILAYER ITEM ===========
    def create_item_multilayer_texture(self, out_name: str, layers: list, tint: Optional[str] = None) -> Path:
        if not layers:
            raise ValueError("No layers provided for multilayer item")

        base = Image.open(layers[0]).convert("RGBA")

        # фарбуємо тільки базовий шар
        if tint:
            base = self.apply_color_multiply(base, tint)

        result = base
        for layer in layers[1:]:
            overlay = Image.open(layer).convert("RGBA")
            result = Image.alpha_composite(result, overlay)

        out_path = self.textures_path / "item" / f"{out_name}.png"
        result.save(out_path)
        print("Created multilayer item:", out_path)
        return out_path

    # =========== CRUSHED / REFINED / PURIFIED / PURE / IMPURE ===========
   # =========== CRUSHED / REFINED / PURIFIED / PURE / IMPURE ===========
    def create_crushed_items(self, ore_name, hex_color):
        return {
            "crushed": self.create_item_multilayer_texture(
                f"crushed_{ore_name}_ore",
                [
                    self.get_item_layer("crushed"),
                    self.get_item_layer("crushed_overlay"),
                ],
                tint=hex_color
            ),

            # ❗ SECONDARY LAYER REMOVED
            "refined": self.create_item_multilayer_texture(
                f"refined_{ore_name}_ore",
                [
                    self.get_item_layer("crushed_refined"),
                    self.get_item_layer("crushed_refined_overlay"),
                ],
                tint=hex_color
            ),

            # ❗ SECONDARY LAYER REMOVED
            "purified": self.create_item_multilayer_texture(
                f"purified_{ore_name}_ore",
                [
                    self.get_item_layer("crushed_purified"),
                ],
                tint=hex_color
            ),

            "pure_dust": self.create_item_multilayer_texture(
                f"pure_{ore_name}_dust",
                [
                    self.get_item_layer("dust_pure"),
                    self.get_item_layer("dust_pure_overlay"),
                ],
                tint=hex_color
            ),

            "impure_dust": self.create_item_multilayer_texture(
                f"impure_{ore_name}_dust",
                [
                    self.get_item_layer("dust_impure"),
                    self.get_item_layer("dust_impure_overlay"),
                ],
                tint=hex_color
            )
        }

    # =========== BASIC ITEMS ===========
    def create_item_texture(self, item_name, item_base, hex_color):
        img = Image.open(self.get_item_base(item_base)).convert("RGBA")
        img = self.apply_color_multiply(img, hex_color)

        out = self.textures_path / "item" / f"{item_name}.png"
        img.save(out)
        print("Created item:", out)
        return out

    def create_item_model(self, item_name):
        data = {
            "parent": "item/generated",
            "textures": {"layer0": f"roll_mod:item/{item_name}"},
        }
        out = self.models_path / "item" / f"{item_name}.json"
        out.write_text(json.dumps(data, indent=2), encoding="utf-8")
        print("Created item model:", out)
        return out

    # =========== BLOCK STUFF ===========
    def create_block_texture(self, base, overlay, ore_name, hex_color):
        sub = Image.open(self.get_block_sub_layer(base)).convert("RGBA")
        ovl = Image.open(self.get_block_overlay(overlay)).convert("RGBA")
        ovl = self.apply_color_multiply(ovl, hex_color)

        result = Image.alpha_composite(sub, ovl)
        out = self.textures_path / "block" / f"{base}_{ore_name}.png"
        result.save(out)
        print("Created block texture:", out)
        return out

    def create_block_model(self, block_id):
        data = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"roll_mod:block/{block_id}"},
        }
        out = self.models_path / "block" / f"{block_id}.json"
        out.write_text(json.dumps(data, indent=2), encoding="utf-8")
        print("Created block model:", out)
        return out

    def create_block_item_model(self, block_id):
        data = {"parent": f"roll_mod:block/{block_id}"}
        out = self.models_path / "item" / f"{block_id}.json"
        out.write_text(json.dumps(data, indent=2), encoding="utf-8")
        print("Created block-item model:", out)
        return out

    def create_blockstates(self, block_id):
        data = {"variants": {"": {"model": f"roll_mod:block/{block_id}"}}}
        out = self.blockstates_path / f"{block_id}.json"
        out.write_text(json.dumps(data, indent=2), encoding="utf-8")
        print("Created blockstates:", out)
        return out

    def create_loot_table(self, block_id, item_name):
        loot = {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
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
                                                            "levels": {"min": 1},
                                                        }
                                                    ]
                                                }
                                            },
                                        }
                                    ],
                                    "name": f"roll_mod:{block_id}",
                                },
                                {
                                    "type": "minecraft:item",
                                    "functions": [
                                        {
                                            "function": "minecraft:apply_bonus",
                                            "enchantment": "minecraft:fortune",
                                            "formula": "minecraft:ore_drops",
                                        },
                                        {"function": "minecraft:explosion_decay"},
                                    ],
                                    "name": f"roll_mod:{item_name}",
                                },
                            ],
                        }
                    ],
                }
            ],
            "random_sequence": f"minecraft:blocks/{block_id}",
        }
        out = self.loot_tables_blocks_path / f"{block_id}.json"
        out.write_text(json.dumps(loot, indent=2), encoding="utf-8")
        print("Created loot table:", out)
        return out

    # =========== MAIN PROCEDURE ===========
    def generate_ore_sets(self, ore_name, item_name, hex_color, bases: List[str],
                          overlay, item_base):
        print("\n=== GENERATING", ore_name, "===")
        print("Bases:", bases)

        # base items
        self.create_item_texture(item_name, item_base, hex_color)
        self.create_item_model(item_name)

        self.create_item_texture(f"{ore_name}_dust", "dust", hex_color)
        self.create_item_model(f"{ore_name}_dust")

        # crushed / refined / purified / pure / impure
        crushed = self.create_crushed_items(ore_name, hex_color)
        for _, path in crushed.items():
            self.create_item_model(path.stem)

        # blocks per base
        for base in bases:
            block_id = f"{base}_{ore_name}"

            self.create_block_texture(base, overlay, ore_name, hex_color)
            self.create_block_model(block_id)
            self.create_block_item_model(block_id)
            self.create_blockstates(block_id)
            self.create_loot_table(block_id, item_name)

        print("=== DONE ===\n")


# ============= HELPERS =============
def parse_bases(arg: str) -> List[str]:
    """
    CLI варіант: рядок типу "stone,deepslate" або "all".
    """
    arg = arg.strip().lower()
    if arg == "all":
        return VALID_BASES.copy()
    bases = [a.strip() for a in arg.split(",") if a.strip()]
    out: List[str] = []
    for b in bases:
        if b not in VALID_BASES:
            raise ValueError(f"Invalid base '{b}', valid: {', '.join(VALID_BASES)}")
        if b not in out:
            out.append(b)
    return out


def normalize_bases_from_json(raw_bases) -> List[str]:
    """
    Приймає те, що прийшло з batch JSON:
    - "all"
    - ["all"]
    - ["stone","deepslate",...]
    і повертає нормальний список баз.
    """
    if isinstance(raw_bases, str):
        # "all" або "stone,deepslate"
        return parse_bases(raw_bases)

    if isinstance(raw_bases, list):
        if len(raw_bases) == 1 and isinstance(raw_bases[0], str) and raw_bases[0].lower() == "all":
            return VALID_BASES.copy()

        out: List[str] = []
        for b in raw_bases:
            if not isinstance(b, str):
                raise ValueError(f"Invalid base entry in JSON: {b!r}")
            name = b.strip().lower()
            if name not in VALID_BASES:
                raise ValueError(f"Invalid base '{name}' in JSON, valid: {', '.join(VALID_BASES)}")
            if name not in out:
                out.append(name)
        return out

    raise TypeError(f"Unsupported 'bases' type in JSON: {type(raw_bases)}")


def detect_default_workspace():
    cur = Path.cwd()
    if cur.name == "roll_mod":
        return str(cur)
    if (cur / "roll_mod").exists():
        return str(cur / "roll_mod")
    return str(cur)


# ============= CLI =============
def main():
    ap = argparse.ArgumentParser()

    ap.add_argument("-w", "--workspace", default=detect_default_workspace())
    ap.add_argument("--batch", help="JSON file with batch ore definitions")

    ap.add_argument("-o", "--ore-name")
    ap.add_argument("-i", "--item-name")
    ap.add_argument("-c", "--hex-color")
    ap.add_argument("-B", "--bases", type=parse_bases)
    ap.add_argument("-O", "--overlay", choices=VALID_OVERLAYS)
    ap.add_argument("-I", "--item-base", choices=VALID_ITEM_BASES)

    args = ap.parse_args()

    gen = OreDataGenerator(args.workspace)

    # ===== BATCH MODE =====
    if args.batch:
        batch_path = Path(args.batch)
        if not batch_path.exists():
            raise FileNotFoundError(f"Batch file not found: {batch_path}")

        batch = json.loads(batch_path.read_text("utf-8"))

        for ore in batch.get("ores", []):
            bases = normalize_bases_from_json(ore["bases"])
            gen.generate_ore_sets(
                ore_name=ore["ore_name"],
                item_name=ore["item_name"],
                hex_color=ore["hex_color"],
                bases=bases,
                overlay=ore["overlay"],
                item_base=ore["item_base"],
            )
        return

    # ===== SINGLE MODE =====
    if not all([args.ore_name, args.item_name, args.hex_color,
                args.bases, args.overlay, args.item_base]):
        ap.error("Need all of: -o -i -c -B -O -I, unless using --batch.")

    gen.generate_ore_sets(
        ore_name=args.ore_name,
        item_name=args.item_name,
        hex_color=args.hex_color,
        bases=args.bases,
        overlay=args.overlay,
        item_base=args.item_base,
    )


if __name__ == "__main__":
    main()
