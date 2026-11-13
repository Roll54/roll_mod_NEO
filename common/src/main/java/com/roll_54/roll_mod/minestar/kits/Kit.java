package com.roll_54.roll_mod.minestar.kits;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One kit, as {@code minestar/kits/<name>.json} holds it.
 *
 * <p>Items are stored through {@link ItemStack#CODEC}, which is what makes the file both
 * hand-editable and able to carry enchantments and other components verbatim. The codec clamps a
 * count to 1..99, which is no constraint here: a kit slot is an inventory slot.
 *
 * <p>{@code slots} runs alongside {@code items}: where each item sits in the kit window
 * ({@link KitSlots}) — an inventory slot, an armour slot, the offhand or a curio slot. It is only
 * for showing the kit; a claim still just adds the items to the inventory. Older files have no
 * {@code slots} and read as the first inventory slots, in order.
 *
 * <p>The cooldown is in ticks, the unit {@code /kit create} takes, and it is the whole of the
 * answer: no permission and no LuckPerms meta shortens or waives it, so what this file says is what
 * every player waits. Only the stamps in {@code kits/cooldowns.json} are epoch millis.
 */
public record Kit(String name, long cooldownTicks, String permission, List<ItemStack> items, String displayName,
                  List<String> slots, String description) {

    /** A cooldown of this many ticks: the kit can be claimed once, ever (as on Eden). */
    public static final long ONCE = -1L;
    /** How long a description may be, {@code &} colour codes included. */
    public static final int MAX_DESCRIPTION = 256;

    /** How long a display name may be, {@code &} colour codes included. */
    public static final int MAX_DISPLAY = 48;

    public Kit {
        // Exactly one slot per item, whatever was passed: missing ones become free inventory slots.
        slots = KitSlots.fill(items, slots);
        description = description == null ? "" : description;
    }

    /** Without a description. */
    public Kit(String name, long cooldownTicks, String permission, List<ItemStack> items, String displayName,
               List<String> slots) {
        this(name, cooldownTicks, permission, items, displayName, slots, "");
    }

    /** Claimed once and never again. */
    public boolean oneTime() {
        return cooldownTicks < 0;
    }

    /** The same kit with another description; blank clears it. */
    public Kit withDescription(String text) {
        String clean = text == null ? "" : text.strip();
        if (clean.length() > MAX_DESCRIPTION) clean = clean.substring(0, MAX_DESCRIPTION);
        return new Kit(name, cooldownTicks, permission, items, displayName, slots, clean);
    }

    /** The same kit with another cooldown ({@link #ONCE} for a one-time kit). */
    public Kit withCooldown(long ticks) {
        return new Kit(name, Math.max(ONCE, ticks), permission, items, displayName, slots, description);
    }

    /** A kit whose items sit in the first inventory slots, in order. */
    public Kit(String name, long cooldownTicks, String permission, List<ItemStack> items, String displayName) {
        this(name, cooldownTicks, permission, items, displayName, List.of());
    }

    /** What players see the kit as: the name a moderator set, or the id when none was. */
    public String title() {
        return displayName == null || displayName.isBlank() ? name : displayName;
    }

    /** The same kit under another display name; blank clears it back to the id. */
    public Kit withDisplayName(String display) {
        String clean = display == null ? "" : display.strip();
        if (clean.length() > MAX_DISPLAY) clean = clean.substring(0, MAX_DISPLAY);
        return new Kit(name, cooldownTicks, permission, items, clean, slots, description);
    }

    /** What a kit may be called: a safe file name and a safe permission-node segment. */
    public static boolean validName(String name) {
        return name != null && name.matches("[a-z0-9_-]{1,24}");
    }

    public static String clampName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
    }

    /** The node that grants this kit, unless the file names a different one. */
    public String permissionOrDefault() {
        return permission == null || permission.isBlank() ? "rollmod.kit." + name : permission;
    }

    public JsonObject save(RegistryAccess registries) {
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        JsonArray array = new JsonArray();
        JsonArray slotArray = new JsonArray();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            String slot = slots.get(i);
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(encoded -> {
                array.add(encoded);
                slotArray.add(slot);
            });
        }

        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("cooldownTicks", cooldownTicks);
        json.addProperty("permission", permissionOrDefault());
        if (displayName != null && !displayName.isBlank()) json.addProperty("displayName", displayName);
        if (!description.isBlank()) json.addProperty("description", description);
        json.add("items", array);
        json.add("slots", slotArray);
        return json;
    }

    /**
     * Reads one kit, or {@code null} when the file is too broken to use.
     *
     * <p>An item that no longer exists — a removed mod, a renamed id — is dropped and the rest of
     * the kit still works. Refusing the whole kit over one missing item would be the worse failure.
     * Its slot goes with it: {@code slots} is read by the same index as {@code items}.
     */
    public static @Nullable Kit load(JsonObject json, String fallbackName, RegistryAccess registries) {
        String name = clampName(json.has("name") ? json.get("name").getAsString() : fallbackName);
        if (!validName(name)) return null;

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        JsonArray slotArray = json.has("slots") && json.get("slots").isJsonArray() ? json.getAsJsonArray("slots") : null;
        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>();
        if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("items");
            for (int i = 0; i < array.size(); i++) {
                ItemStack stack = ItemStack.CODEC.parse(ops, array.get(i)).result().orElse(null);
                if (stack == null) continue;
                items.add(stack);
                slots.add(slotArray != null && i < slotArray.size() && slotArray.get(i).isJsonPrimitive()
                        ? slotArray.get(i).getAsString() : "");
            }
        }

        long cooldown = json.has("cooldownTicks") ? json.get("cooldownTicks").getAsLong() : 0L;
        String permission = json.has("permission") ? json.get("permission").getAsString() : "";
        // Older files have no display name; the kit then shows as its id, as it always did.
        String display = json.has("displayName") ? json.get("displayName").getAsString() : "";
        String description = json.has("description") ? json.get("description").getAsString() : "";
        return new Kit(name, Math.max(ONCE, cooldown), permission, List.copyOf(items), display, slots, description);
    }
}
