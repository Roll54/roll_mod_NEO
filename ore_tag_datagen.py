import json
import os

# Усі можливі бази, якщо "bases": ["all"]
ALL_BASES = [
    "stone", "deepslate", "netherrack", "end",
    "mars", "moon", "venus", "mercury"
]

# Шлях до вихідної папки (твій точний)
OUTPUT_DIR = "src/main/resources/data/c/tags/item/ores"


def ensure_dir(path):
    os.makedirs(path, exist_ok=True)


def load_batch(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def generate_tag_json(ore_name, item_name, bases):
    values = []

    for base in bases:
        values.append(f"roll_mod:{base}_{ore_name}")

    values.append(f"roll_mod:{item_name}")

    return {
        "replace": False,
        "values": values
    }


def process_single_ore(entry):
    ore_name = entry["ore_name"]
    item_name = entry["item_name"]
    bases = entry["bases"]

    # підтримка "all"
    if bases == ["all"]:
        bases = ALL_BASES

    return ore_name, generate_tag_json(ore_name, item_name, bases)


def generate_tags(batch_file):
    ensure_dir(OUTPUT_DIR)

    batch = load_batch(batch_file)

    for entry in batch["ores"]:
        ore_name, tag_json = process_single_ore(entry)

        out_path = os.path.join(OUTPUT_DIR, f"{ore_name}.json")

        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(tag_json, f, ensure_ascii=False, indent=2)

        print(f"✔ Створено тег: {out_path}")

        DUSTS_OUTPUT_DIR = "src/main/resources/data/c/tags/item/dusts"
        ensure_dir(DUSTS_OUTPUT_DIR)

        dust_tag = generate_dust_tag_json(f"{ore_name}_dust")
        dust_path = os.path.join(DUSTS_OUTPUT_DIR, f"{ore_name}.json")

        with open(dust_path, "w", encoding="utf-8") as f:
            json.dump(dust_tag, f, ensure_ascii=False, indent=2)

        print(f"✔ Створено тег пилу: {dust_path}")
def generate_dust_tag_json(item_name):
    return {
        "replace": False,
        "values": [
            f"roll_mod:{item_name}"
        ]
    }


# --- запуск ---
if __name__ == "__main__":
    generate_tags("ores_batch.json")
