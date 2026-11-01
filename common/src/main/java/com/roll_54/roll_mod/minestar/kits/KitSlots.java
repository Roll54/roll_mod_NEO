package com.roll_54.roll_mod.minestar.kits;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where an item sits in a kit window, as written in a kit's {@code slots}:
 * <ul>
 *   <li>{@code inv:N} — an inventory slot in {@code Inventory}'s own numbering, 0–8 the hotbar,
 *       9–35 the three rows above it;</li>
 *   <li>{@code armor:head}, {@code armor:chest}, {@code armor:legs}, {@code armor:feet};</li>
 *   <li>{@code offhand};</li>
 *   <li>{@code curio:<slot type>:<index>} — a curio slot, by Curios' slot type id.</li>
 * </ul>
 * The window's main handler uses the inventory's numbering for all of the first three (armour at
 * 36–39 feet to head, the offhand at 40), the same way the moderation inventory window does.
 */
public final class KitSlots {

    /** The window's main handler: 36 inventory slots, 4 armour, the offhand. */
    public static final int MAIN_SLOTS = 41;
    public static final int INVENTORY = 36;
    public static final int OFFHAND = 40;
    private static final String[] ARMOR = {"feet", "legs", "chest", "head"};

    private static final String INV = "inv:";
    private static final String ARMOR_PREFIX = "armor:";
    private static final String CURIO = "curio:";

    private KitSlots() {}

    /** The key of main-handler slot {@code index}. */
    public static String main(int index) {
        if (index < INVENTORY) return INV + index;
        if (index < OFFHAND) return ARMOR_PREFIX + ARMOR[index - INVENTORY];
        return "offhand";
    }

    /** The main-handler slot of {@code key}, or -1 when it is a curio slot or not a key at all. */
    public static int main(String key) {
        if (key.startsWith(INV)) {
            try {
                int index = Integer.parseInt(key.substring(INV.length()));
                return index >= 0 && index < INVENTORY ? index : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (key.startsWith(ARMOR_PREFIX)) {
            String piece = key.substring(ARMOR_PREFIX.length());
            for (int i = 0; i < ARMOR.length; i++) {
                if (ARMOR[i].equals(piece)) return INVENTORY + i;
            }
            return -1;
        }
        return "offhand".equals(key) ? OFFHAND : -1;
    }

    public static String curio(String type, int index) {
        return CURIO + type + ":" + index;
    }

    /** A curio key's slot type and index, or {@code null} when {@code key} is not one. */
    @Nullable
    public static Curio curio(String key) {
        if (!key.startsWith(CURIO)) return null;
        int colon = key.lastIndexOf(':');
        if (colon <= CURIO.length()) return null;
        try {
            int index = Integer.parseInt(key.substring(colon + 1));
            return index < 0 ? null : new Curio(key.substring(CURIO.length(), colon), index);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record Curio(String type, int index) {}

    /**
     * Exactly one slot per item: a known, unused key stays; anything else — a missing key, a typo, a
     * slot already taken — becomes the next free inventory slot. Items past the 36th that find none
     * keep an empty key: they are still given on a claim, the window just has nowhere to show them.
     */
    static List<String> fill(List<ItemStack> items, @Nullable List<String> slots) {
        List<String> out = new ArrayList<>(items.size());
        Set<String> used = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            String key = slots != null && i < slots.size() && slots.get(i) != null ? slots.get(i) : "";
            boolean known = main(key) >= 0 || curio(key) != null;
            out.add(known && used.add(key) ? key : null);
        }
        int next = 0;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i) != null) continue;
            while (next < INVENTORY && used.contains(INV + next)) next++;
            if (next < INVENTORY) {
                String key = INV + next++;
                used.add(key);
                out.set(i, key);
            } else {
                out.set(i, "");
            }
        }
        return List.copyOf(out);
    }
}
