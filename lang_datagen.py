#!/usr/bin/env python3
import json
import argparse
from pathlib import Path

# якщо треба підтримка "bases": ["all"]
ALL_BASES = [
    "stone", "deepslate", "netherrack", "end",
    "moon", "mars", "venus", "mercury"
]

# -----------------------------
#   ITEM NAMES
# -----------------------------
def make_item_names(ore_name: str, ua_name: str, en_name: str):
    """Generate language keys for all items of an ore."""

    item_raw      = f"raw_{ore_name}"
    item_dust     = f"{ore_name}_dust"
    item_crushed  = f"crushed_{ore_name}_ore"
    item_refined  = f"refined_{ore_name}_ore"
    item_purified = f"purified_{ore_name}_ore"
    item_pure     = f"pure_{ore_name}_dust"
    item_impure   = f"impure_{ore_name}_dust"

    # UA items
    ua = {
        f"item.roll_mod.{item_raw}":        f"Необроблений {ua_name.lower()}",
        f"item.roll_mod.{item_dust}":       f"Пил {ua_name.lower()}",
        f"item.roll_mod.{item_crushed}":    f"Дроблений {ua_name.lower()}",
        f"item.roll_mod.{item_refined}":    f"Очищений {ua_name.lower()}",
        f"item.roll_mod.{item_purified}":   f"Промитий {ua_name.lower()}",
        f"item.roll_mod.{item_pure}":       f"Чистий пил {ua_name.lower()}",
        f"item.roll_mod.{item_impure}":     f"Брудний пил {ua_name.lower()}",
    }

    # EN items
    en = {
        f"item.roll_mod.{item_raw}":        f"Raw {en_name}",
        f"item.roll_mod.{item_dust}":       f"{en_name} Dust",
        f"item.roll_mod.{item_crushed}":    f"Crushed {en_name} Ore",
        f"item.roll_mod.{item_refined}":    f"Refined {en_name} Ore",
        f"item.roll_mod.{item_purified}":   f"Purified {en_name} Ore",
        f"item.roll_mod.{item_pure}":       f"Pure {en_name} Dust",
        f"item.roll_mod.{item_impure}":     f"Impure {en_name} Dust",
    }

    return ua, en


# -----------------------------
#   BLOCK NAMES
# -----------------------------
def prettify_base_ua(base: str):
    return {
        "stone": "Кам'яний",
        "deepslate": "Глибосланцевий",
        "netherrack": "Незеритний",
        "end": "Краєвий",
        "moon": "Місячний",
        "mars": "Марсіанський",
        "venus": "Венеріанський",
        "mercury": "Меркуріанський",
    }.get(base, base.capitalize())


def prettify_base_en(base: str):
    return {
        "stone": "Stone",
        "deepslate": "Deepslate",
        "netherrack": "Netherrack",
        "end": "End",
        "moon": "Moon",
        "mars": "Mars",
        "venus": "Venus",
        "mercury": "Mercury",
    }.get(base, base.capitalize())


def make_block_names(ore_name: str, ua_name: str, en_name: str, bases: list):
    ua = {}
    en = {}

    for base in bases:
        key = f"block.roll_mod.{base}_{ore_name}"

        ua_prefix = prettify_base_ua(base)
        en_prefix = prettify_base_en(base)

        ua[key] = f"{ua_prefix} {ua_name.lower()}"
        en[key] = f"{en_prefix} {en_name} Ore"

    return ua, en


# -----------------------------
#   MAIN
# -----------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--batch", required=True, help="Path to ores_batch.json")
    parser.add_argument("--out", default="lang_out", help="Output directory")
    args = parser.parse_args()

    batch_file = Path(args.batch)
    out_dir = Path(args.out)

    out_dir.mkdir(exist_ok=True)

    # Загальні колекції для 4 файлів:
    ua_items_total = {}
    en_items_total = {}
    ua_blocks_total = {}
    en_blocks_total = {}

    data = json.load(open(batch_file, "r", encoding="utf8"))

    for ore in data["ores"]:
        if "uk_ua_name" not in ore or "en_us_name" not in ore:
            print(f"[WARN] Ore {ore['ore_name']} missing UA/EN names — skipped")
            continue

        ore_name = ore["ore_name"]
        ua_name = ore["uk_ua_name"]
        en_name = ore["en_us_name"]

        # --------------------
        # ITEMS
        # --------------------
        ua_items, en_items = make_item_names(ore_name, ua_name, en_name)
        ua_items_total.update(ua_items)
        en_items_total.update(en_items)

        # --------------------
        # BLOCKS
        # --------------------
        bases = ore.get("bases", [])
        if bases == ["all"]:
            bases = ALL_BASES

        ua_blocks, en_blocks = make_block_names(ore_name, ua_name, en_name, bases)
        ua_blocks_total.update(ua_blocks)
        en_blocks_total.update(en_blocks)

    # ------------------------------
    #   ЗАПИС ФАЙЛІВ
    # ------------------------------
    with open(out_dir / "item_uk_ua.json", "w", encoding="utf8") as f:
        json.dump(ua_items_total, f, indent=2, ensure_ascii=False)

    with open(out_dir / "item_en_us.json", "w", encoding="utf8") as f:
        json.dump(en_items_total, f, indent=2, ensure_ascii=False)

    with open(out_dir / "block_uk_ua.json", "w", encoding="utf8") as f:
        json.dump(ua_blocks_total, f, indent=2, ensure_ascii=False)

    with open(out_dir / "block_en_us.json", "w", encoding="utf8") as f:
        json.dump(en_blocks_total, f, indent=2, ensure_ascii=False)

    print("DONE. Generated:")
    print(" - item_uk_ua.json")
    print(" - item_en_us.json")
    print(" - block_uk_ua.json")
    print(" - block_en_us.json")


if __name__ == "__main__":
    main()
