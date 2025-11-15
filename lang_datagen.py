#!/usr/bin/env python3
import json
import argparse
from pathlib import Path

def make_item_names(ore_name: str, ua_name: str, en_name: str):
    """Generate language keys for all 6 items of an ore."""

    # Базові назви
    item_raw      = f"raw_{ore_name}"
    item_dust     = f"{ore_name}_dust"
    item_crushed  = f"crushed_{ore_name}_ore"
    item_refined  = f"refined_{ore_name}_ore"
    item_purified = f"purified_{ore_name}_ore"
    item_pure     = f"pure_{ore_name}_dust"
    item_impure   = f"impure_{ore_name}_dust"

    # UA
    ua = {
        f"item.roll_mod.{item_raw}":        f"Необроблений {ua_name.lower()}",
        f"item.roll_mod.{item_dust}":       f"Пил {ua_name.lower()}",
        f"item.roll_mod.{item_crushed}":    f"Дроблений {ua_name.lower()}",
        f"item.roll_mod.{item_refined}":    f"Очищений {ua_name.lower()}",
        f"item.roll_mod.{item_purified}":   f"Промитий {ua_name.lower()}",
        f"item.roll_mod.{item_pure}":       f"Чистий пил {ua_name.lower()}",
        f"item.roll_mod.{item_impure}":     f"Брудний пил {ua_name.lower()}",
    }

    # EN
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--batch", required=True, help="Path to ores_batch.json")
    parser.add_argument("--out", default="lang_out", help="Output directory")
    args = parser.parse_args()

    batch_file = Path(args.batch)
    out_dir = Path(args.out)

    out_dir.mkdir(exist_ok=True)

    data = json.load(open(batch_file, "r", encoding="utf8"))

    ua_total = {}
    en_total = {}

    for ore in data["ores"]:
        if "uk_ua_name" not in ore or "en_us_name" not in ore:
            print(f"[WARN] Ore {ore['ore_name']} missing UA/EN names — skipped")
            continue

        ore_name = ore["ore_name"]
        ua_name = ore["uk_ua_name"]
        en_name = ore["en_us_name"]

        ua, en = make_item_names(ore_name, ua_name, en_name)

        ua_total.update(ua)
        en_total.update(en)

    # Write UA
    with open(out_dir / "uk_ua.json", "w", encoding="utf8") as f:
        json.dump(ua_total, f, indent=2, ensure_ascii=False)

    # Write EN
    with open(out_dir / "en_us.json", "w", encoding="utf8") as f:
        json.dump(en_total, f, indent=2, ensure_ascii=False)

    print("DONE. Generated:")
    print(" -", out_dir / "uk_ua.json")
    print(" -", out_dir / "en_us.json")


if __name__ == "__main__":
    main()
