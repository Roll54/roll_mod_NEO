package com.roll_54.roll_mod_client.compat.tei.tree;

import dev.lorp.tei.api.TeiDisplay;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One ingredient in the TEI handset tree: how much of it is needed, the step picked for it (a recipe, or the one
 * stack of a tag / list it stands for) and the ingredients that step takes. Positions are in tree space, filled in
 * by {@link #layout}. Same layout as the EMI tree's node.
 */
final class TeiRecipeTreeNode {

    static final int SIZE = 18;
    static final int GAP = 6;
    /** Slot, recipe icon under it, then the bar the children hang from. */
    static final int LEVEL = 42;

    /** How a node is made: a TEI recipe, or (for a tag or list) the one stack chosen for it. */
    sealed interface Step permits RecipeStep, NarrowStep {}

    record RecipeStep(TeiDisplay display) implements Step {}

    record NarrowStep(ItemStack chosen) implements Step {}

    final TreeIngredient ingredient;
    final long amount;
    final @Nullable Step step;
    final List<TeiRecipeTreeNode> children;
    /** Already an ancestor of this node, so it is not expanded again. */
    final boolean cycle;
    /** TEI knows a recipe making it (or it is a tag / list, which can be narrowed to one stack). */
    final boolean craftable;

    int x, y, width;

    TeiRecipeTreeNode(TreeIngredient ingredient, long amount, @Nullable Step step, List<TeiRecipeTreeNode> children,
                      boolean cycle, boolean craftable) {
        this.ingredient = ingredient;
        this.amount = amount;
        this.step = step;
        this.children = children;
        this.cycle = cycle;
        this.craftable = craftable;
    }

    @Nullable TeiDisplay recipe() {
        return step instanceof RecipeStep r ? r.display() : null;
    }

    /** Lays the subtree out with its left edge at {@code left}; returns its width. */
    int layout(int left, int top) {
        int childrenWidth = 0;
        for (TeiRecipeTreeNode child : children) {
            childrenWidth += child.measure();
        }
        childrenWidth += Math.max(0, children.size() - 1) * GAP;
        width = Math.max(SIZE, childrenWidth);
        x = left + (width - SIZE) / 2;
        y = top;

        int childLeft = left + (width - childrenWidth) / 2;
        for (TeiRecipeTreeNode child : children) {
            childLeft += child.layout(childLeft, top + LEVEL) + GAP;
        }
        return width;
    }

    private int measure() {
        int childrenWidth = 0;
        for (TeiRecipeTreeNode child : children) {
            childrenWidth += child.measure();
        }
        childrenWidth += Math.max(0, children.size() - 1) * GAP;
        return Math.max(SIZE, childrenWidth);
    }

    boolean contains(double treeX, double treeY) {
        return treeX >= x && treeX < x + SIZE && treeY >= y && treeY < y + SIZE;
    }

    @Nullable TeiRecipeTreeNode find(double treeX, double treeY) {
        if (contains(treeX, treeY)) return this;
        for (TeiRecipeTreeNode child : children) {
            TeiRecipeTreeNode hit = child.find(treeX, treeY);
            if (hit != null) return hit;
        }
        return null;
    }
}
