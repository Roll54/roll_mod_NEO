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


# --- запуск ---
if __name__ == "__main__":
    generate_tags("ores_batch.json")
