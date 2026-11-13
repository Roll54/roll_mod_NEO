package com.roll_54.roll_mod_client.compat.tei;

import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreeScreen;
import com.roll_54.roll_mod_client.compat.tei.tree.TreeIngredient;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen hooks of the TEI handset tree ({@link TeiRecipeTreeScreen}). TEI's own API has no stack providers or
 * drag-drop handlers, but TEI runs EMI plugins and implements those parts of the EMI API, so they are registered
 * through it: stacks dragged out of TEI's item list land in the root slot, TEI's keys (recipes, uses, favourite)
 * work on the tree's nodes, and TEI lays its sidebars out around the panel.
 *
 * <p>Does nothing unless TEI is loaded (under real EMI the EMI tree in {@code compat.emi} is used instead), and
 * touches {@link TeiRecipeTreeScreen} only after that check.
 */
@EmiEntrypoint
public class TeiTreeEmiBridge implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded("tei")) return;
        Hooks.register(registry);
    }

    /** Kept apart so nothing here is resolved without TEI. */
    private static final class Hooks {
        static void register(EmiRegistry registry) {
            registry.addScreenBoundsProvider(TeiRecipeTreeScreen.class, screen -> bounds(screen.panelRect()));
            registry.addDragDropHandler(TeiRecipeTreeScreen.class, new EmiDragDropHandler.BoundsBased<TeiRecipeTreeScreen>(
                    (screen, consumer) -> consumer.accept(bounds(screen.rootRect()), ing -> screen.setRoot(fromEmi(ing)))));
            // Not clickable: a click on a node is the tree's own (pick in resolve mode), not "view recipes".
            registry.addStackProvider(TeiRecipeTreeScreen.class, (screen, x, y) -> {
                TreeIngredient hovered = screen.ingredientAt(x, y);
                return hovered == null ? EmiStackInteraction.EMPTY : new EmiStackInteraction(toEmi(hovered), null, false);
            });
        }

        private static Bounds bounds(int[] rect) {
            return new Bounds(rect[0], rect[1], rect[2], rect[3]);
        }

        /** TEI hands item-list fluids over as their stand-in stacks; EMI fluid stacks become stand-ins too. */
        private static TreeIngredient fromEmi(EmiIngredient ingredient) {
            if (ingredient == null || ingredient.isEmpty()) return null;
            List<ItemStack> stacks = new ArrayList<>();
            for (EmiStack stack : ingredient.getEmiStacks()) {
                if (stack.getKey() instanceof Fluid fluid) {
                    stacks.add(TreeIngredient.of(fluid, 1).first());
                } else if (!stack.getItemStack().isEmpty()) {
                    stacks.add(stack.getItemStack().copyWithCount(1));
                }
            }
            if (stacks.isEmpty()) return null;
            return new TreeIngredient(stacks, ingredient.getAmount());
        }

        private static EmiIngredient toEmi(TreeIngredient ingredient) {
            List<EmiStack> stacks = new ArrayList<>();
            for (ItemStack stack : ingredient.stacks()) stacks.add(EmiStack.of(stack));
            return EmiIngredient.of(stacks);
        }
    }
}
