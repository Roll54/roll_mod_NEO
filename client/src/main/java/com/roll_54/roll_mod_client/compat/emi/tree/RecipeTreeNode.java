package com.roll_54.roll_mod_client.compat.emi.tree;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One ingredient in the recipe tree: how much of it is needed, the recipe picked for it (if any) and
 * the ingredients that recipe takes. Positions are in tree space, filled in by {@link #layout}.
 */
final class RecipeTreeNode {

    static final int SIZE = 18;
    static final int GAP = 6;
    /** Slot, recipe icon under it, then the bar the children hang from. */
    static final int LEVEL = 42;

    final EmiIngredient ingredient;
    final long amount;
    final @Nullable EmiRecipe recipe;
    final List<RecipeTreeNode> children;
    /** Already an ancestor of this node, so it is not expanded again. */
    final boolean cycle;
    /** EMI knows at least one recipe making it. */
    final boolean craftable;

    int x, y, width;

    RecipeTreeNode(EmiIngredient ingredient, long amount, @Nullable EmiRecipe recipe, List<RecipeTreeNode> children,
                   boolean cycle, boolean craftable) {
        this.ingredient = ingredient;
        this.amount = amount;
        this.recipe = recipe;
        this.children = children;
        this.cycle = cycle;
        this.craftable = craftable;
    }

    /** Lays the subtree out with its left edge at {@code left}; returns its width. */
    int layout(int left, int top) {
        int childrenWidth = 0;
        for (RecipeTreeNode child : children) {
            childrenWidth += child.measure();
        }
        childrenWidth += Math.max(0, children.size() - 1) * GAP;
        width = Math.max(SIZE, childrenWidth);
        x = left + (width - SIZE) / 2;
        y = top;

        int childLeft = left + (width - childrenWidth) / 2;
        for (RecipeTreeNode child : children) {
            childLeft += child.layout(childLeft, top + LEVEL) + GAP;
        }
        return width;
    }

    private int measure() {
        int childrenWidth = 0;
        for (RecipeTreeNode child : children) {
            childrenWidth += child.measure();
        }
        childrenWidth += Math.max(0, children.size() - 1) * GAP;
        return Math.max(SIZE, childrenWidth);
    }

    boolean contains(double treeX, double treeY) {
        return treeX >= x && treeX < x + SIZE && treeY >= y && treeY < y + SIZE;
    }

    @Nullable RecipeTreeNode find(double treeX, double treeY) {
        if (contains(treeX, treeY)) return this;
        for (RecipeTreeNode child : children) {
            RecipeTreeNode hit = child.find(treeX, treeY);
            if (hit != null) return hit;
        }
        return null;
    }
}
