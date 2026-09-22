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
 * <p>The cooldown is in ticks, the unit {@code /kit create} takes, and it is the whole of the
 * answer: no permission and no LuckPerms meta shortens or waives it, so what this file says is what
 * every player waits. Only the stamps in {@code kits/cooldowns.json} are epoch millis.
 */
public record Kit(String name, long cooldownTicks, String permission, List<ItemStack> items) {

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
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(array::add);
        }

        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("cooldownTicks", cooldownTicks);
        json.addProperty("permission", permissionOrDefault());
        json.add("items", array);
        return json;
    }

    /**
     * Reads one kit, or {@code null} when the file is too broken to use.
     *
     * <p>An item that no longer exists — a removed mod, a renamed id — is dropped and the rest of
     * the kit still works. Refusing the whole kit over one missing item would be the worse failure.
     */
    public static @Nullable Kit load(JsonObject json, String fallbackName, RegistryAccess registries) {
        String name = clampName(json.has("name") ? json.get("name").getAsString() : fallbackName);
        if (!validName(name)) return null;

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<ItemStack> items = new ArrayList<>();
        if (json.has("items") && json.get("items").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("items")) {
                ItemStack.CODEC.parse(ops, element).result().ifPresent(items::add);
            }
        }

        long cooldown = json.has("cooldownTicks") ? json.get("cooldownTicks").getAsLong() : 0L;
        String permission = json.has("permission") ? json.get("permission").getAsString() : "";
        return new Kit(name, Math.max(0L, cooldown), permission, List.copyOf(items));
    }
}
