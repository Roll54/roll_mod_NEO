package com.roll_54.roll_mod.minestar.deathlog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * One death: where, how, and everything the player had on them a moment before.
 *
 * @param cause        the death message as plain text, in the server's language; the fallback
 * @param causeJson    the same message as a chat component (JSON), so a viewer reads it in their own
 *                     language with names intact; empty on records from before it was kept
 * @param damageType   the damage type's id, e.g. {@code minecraft:player_attack}
 * @param killer       who or what dealt the blow, or empty
 * @param inventory    vanilla inventory by slot index — 0–35 main, 36–39 armour (boots first), 40
 *                     offhand — non-empty slots only
 * @param curios       worn curios, empty when Curios is not installed
 * @param cyberware    the whole Create Cybernetics state as {@code PlayerCyberwareData} saves it, or
 *                     {@code null}; written back as-is on a return when it safely can be
 * @param cyberwareItems the same cyberware as stacks: implants that are not default organs and what
 *                     they carried. For display, and for handing over as items
 * @param keptInventory the player kept their things anyway (keepInventory, Prime), so restoring
 *                     this death would duplicate them
 * @param restores     every time a moderator restored this death, oldest first
 */
public record DeathRecord(UUID id, UUID victim, String victimName, long diedAt, String dimension,
                          int x, int y, int z, String cause, String causeJson, String damageType,
                          String killer, boolean keptInventory,
                          Map<Integer, ItemStack> inventory, List<CurioStack> curios,
                          @Nullable CompoundTag cyberware, List<ItemStack> cyberwareItems,
                          List<Restore> restores) {

    /** A worn curio: its slot type, index in it, and whether it was the cosmetic row. */
    public record CurioStack(String slot, int index, boolean cosmetic, ItemStack stack) {}

    /**
     * One hand-out: {@code by} is the moderator, {@code to} who received the items — the moderator
     * themselves for a retrieve, the player who died for a return. {@code mode} is {@link Mode#id()}.
     */
    public record Restore(String by, String to, long at, String mode) {}

    public enum Mode {
        /** Everything handed to the moderator. */
        RETRIEVE("retrieve"),
        /** Everything handed back to the player who died. */
        RETURN("return");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public int itemCount() {
        return inventory.size() + curios.size() + cyberwareItems.size();
    }

    public DeathRecord withRestore(Restore restore) {
        List<Restore> list = new ArrayList<>(restores);
        list.add(restore);
        return new DeathRecord(id, victim, victimName, diedAt, dimension, x, y, z, cause, causeJson, damageType,
                killer, keptInventory, inventory, curios, cyberware, cyberwareItems, List.copyOf(list));
    }

    /** Every stack in the record, in inventory, curios, cyberware order: what a retrieve hands over. */
    public List<ItemStack> allStacks() {
        List<ItemStack> out = new ArrayList<>();
        inventory.values().forEach(s -> out.add(s.copy()));
        curios.forEach(c -> out.add(c.stack().copy()));
        cyberwareItems.forEach(s -> out.add(s.copy()));
        return out;
    }

    /* ------------------------------------------- json ------------------------------------------- */

    public JsonObject save(HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("victim", victim.toString());
        json.addProperty("victimName", victimName);
        json.addProperty("diedAt", diedAt);
        json.addProperty("dimension", dimension);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        json.addProperty("cause", cause);
        if (!causeJson.isEmpty()) json.addProperty("causeJson", causeJson);
        json.addProperty("damageType", damageType);
        json.addProperty("killer", killer);
        json.addProperty("keptInventory", keptInventory);

        JsonArray inv = new JsonArray();
        inventory.forEach((slot, stack) -> ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(item -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("slot", slot);
            entry.add("item", item);
            inv.add(entry);
        }));
        json.add("inventory", inv);

        JsonArray cur = new JsonArray();
        for (CurioStack curio : curios) {
            ItemStack.CODEC.encodeStart(ops, curio.stack()).result().ifPresent(item -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("slot", curio.slot());
                entry.addProperty("index", curio.index());
                entry.addProperty("cosmetic", curio.cosmetic());
                entry.add("item", item);
                cur.add(entry);
            });
        }
        json.add("curios", cur);

        // SNBT rather than JSON: the tag goes back through deserializeNBT untouched, types and all.
        if (cyberware != null) json.addProperty("cyberware", cyberware.getAsString());
        JsonArray cyb = new JsonArray();
        for (ItemStack stack : cyberwareItems) ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(cyb::add);
        json.add("cyberwareItems", cyb);

        JsonArray res = new JsonArray();
        for (Restore r : restores) {
            JsonObject entry = new JsonObject();
            entry.addProperty("by", r.by());
            entry.addProperty("to", r.to());
            entry.addProperty("at", r.at());
            entry.addProperty("mode", r.mode());
            res.add(entry);
        }
        json.add("restores", res);
        return json;
    }

    /**
     * Reads one record, or {@code null} when it is too broken to use. An item that no longer exists
     * is dropped and the rest kept, as {@code Kit.load} does.
     */
    public static @Nullable DeathRecord load(JsonObject json, HolderLookup.Provider registries) {
        try {
            RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
            UUID id = UUID.fromString(json.get("id").getAsString());
            UUID victim = UUID.fromString(json.get("victim").getAsString());

            Map<Integer, ItemStack> inventory = new TreeMap<>();
            for (JsonElement e : array(json, "inventory")) {
                JsonObject entry = e.getAsJsonObject();
                int slot = entry.get("slot").getAsInt();
                ItemStack.CODEC.parse(ops, entry.get("item")).result().ifPresent(s -> inventory.put(slot, s));
            }

            List<CurioStack> curios = new ArrayList<>();
            for (JsonElement e : array(json, "curios")) {
                JsonObject entry = e.getAsJsonObject();
                String slot = entry.get("slot").getAsString();
                int index = entry.get("index").getAsInt();
                boolean cosmetic = entry.has("cosmetic") && entry.get("cosmetic").getAsBoolean();
                ItemStack.CODEC.parse(ops, entry.get("item")).result()
                        .ifPresent(s -> curios.add(new CurioStack(slot, index, cosmetic, s)));
            }

            CompoundTag cyberware = null;
            if (json.has("cyberware")) {
                cyberware = TagParser.parseTag(json.get("cyberware").getAsString());
            }
            List<ItemStack> cyberwareItems = new ArrayList<>();
            for (JsonElement e : array(json, "cyberwareItems")) {
                ItemStack.CODEC.parse(ops, e).result().ifPresent(cyberwareItems::add);
            }

            List<Restore> restores = new ArrayList<>();
            for (JsonElement e : array(json, "restores")) {
                JsonObject entry = e.getAsJsonObject();
                String mode = entry.get("mode").getAsString();
                String by = entry.get("by").getAsString();
                // Older entries have no "to": a retrieve went to whoever did it, a return to the victim.
                String to = entry.has("to") ? entry.get("to").getAsString()
                        : Mode.RETRIEVE.id().equals(mode) ? by : string(json, "victimName");
                restores.add(new Restore(by, to, entry.get("at").getAsLong(), mode));
            }

            return new DeathRecord(id, victim, string(json, "victimName"), json.get("diedAt").getAsLong(),
                    string(json, "dimension"), json.get("x").getAsInt(), json.get("y").getAsInt(),
                    json.get("z").getAsInt(), string(json, "cause"), string(json, "causeJson"),
                    string(json, "damageType"), string(json, "killer"),
                    json.has("keptInventory") && json.get("keptInventory").getAsBoolean(),
                    java.util.Collections.unmodifiableMap(inventory), List.copyOf(curios), cyberware,
                    List.copyOf(cyberwareItems), List.copyOf(restores));
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonArray array(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : new JsonArray();
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }
}
