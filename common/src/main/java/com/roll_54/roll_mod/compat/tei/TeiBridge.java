package com.roll_54.roll_mod.compat.tei;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * TEI's "show recipes" / "show uses", for screens TEI does not attach to on its own — it only hooks
 * container screens and its recipe screen, so roll_mod's LdLib creative screen asks here. Reached by
 * reflection: the common module does not compile against TEI, and without TEI every call is a no-op.
 * Uses TEI's own key bindings (R / U by default, rebindable in TEI).
 */
@OnlyIn(Dist.CLIENT)
public final class TeiBridge {

    private static boolean resolved;
    private static MethodHandle matches;      // TeiKeys.matches(TeiBind, int, int)
    private static MethodHandle openRecipes;  // ItemListOverlay.openRecipes(ItemStack, boolean uses)
    private static Object showRecipes;        // TeiKeys.SHOW_RECIPES
    private static Object showUses;           // TeiKeys.SHOW_USES

    private TeiBridge() {}

    private static synchronized boolean ready() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> keys = Class.forName("dev.lorp.tei.client.TeiKeys");
                Class<?> bind = Class.forName("dev.lorp.tei.client.TeiBind");
                Class<?> overlay = Class.forName("dev.lorp.tei.client.gui.ItemListOverlay");
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                matches = lookup.findStatic(keys, "matches", MethodType.methodType(boolean.class, bind, int.class, int.class));
                openRecipes = lookup.findStatic(overlay, "openRecipes", MethodType.methodType(boolean.class, ItemStack.class, boolean.class));
                showRecipes = keys.getField("SHOW_RECIPES").get(null);
                showUses = keys.getField("SHOW_USES").get(null);
            } catch (ClassNotFoundException e) {
                matches = null; // TEI not installed
            } catch (ReflectiveOperationException | RuntimeException e) {
                matches = null;
                RollMod.LOGGER.warn("[TEI] could not reach TEI's recipe screen; R/U stay off on roll_mod screens", e);
            }
        }
        return matches != null;
    }

    /**
     * If {@code key} is TEI's show-recipes or show-uses key, opens that for {@code stack} and returns
     * true; otherwise false.
     */
    public static boolean handleKey(int key, int scanCode, ItemStack stack) {
        if (stack.isEmpty() || !ready()) return false;
        try {
            if ((boolean) matches.invoke(showRecipes, key, scanCode)) return (boolean) openRecipes.invoke(stack, false);
            if ((boolean) matches.invoke(showUses, key, scanCode)) return (boolean) openRecipes.invoke(stack, true);
        } catch (Throwable t) {
            RollMod.LOGGER.warn("[TEI] opening recipes failed", t);
        }
        return false;
    }
}
