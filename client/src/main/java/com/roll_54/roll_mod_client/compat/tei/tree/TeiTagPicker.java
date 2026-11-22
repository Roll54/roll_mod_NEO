package com.roll_54.roll_mod_client.compat.tei.tree;

import dev.lorp.tei.client.gui.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Picks which item a tag node of the {@link TeiRecipeTreeScreen} stands for through TEI's own tag page: a click on a
 * tag node opens {@link RecipeScreen#openTag} for the tag the node's items make up, and the first click there on one
 * of those items (which TEI would answer by opening that item's recipes) is taken as the choice instead
 * ({@code TeiTagPickMixin}): the tree narrows the node to it and comes back. A list that is no tag has no page, and
 * keeps the old click (the stack shown at that moment).
 */
public final class TeiTagPicker {

    private static @Nullable TeiRecipeTreeScreen owner;
    private static @Nullable TreeIngredient target;
    private static Set<Item> members = Set.of();

    private TeiTagPicker() {}

    /** Opens TEI's page for the tag {@code ingredient} is made of; false when it is no tag or TEI shows nothing. */
    static boolean pick(TeiRecipeTreeScreen screen, TreeIngredient ingredient) {
        Set<Item> items = new HashSet<>();
        for (ItemStack stack : ingredient.stacks()) if (!stack.isEmpty()) items.add(stack.getItem());
        TagKey<Item> tag = tagOf(items);
        if (tag == null) return false;
        owner = screen;
        target = ingredient;
        members = Set.copyOf(items);
        if (!RecipeScreen.openTag(tag)) {
            cancel();
            return false;
        }
        return true;
    }

    /**
     * Called by the mixin for every stack TEI is about to open recipes for. True when a tag page opened by
     * {@link #pick} is waiting and {@code stack} is one of its items: the tree takes it and TEI's call is cancelled.
     */
    public static boolean offer(ItemStack stack) {
        if (owner == null || target == null || stack.isEmpty() || !members.contains(stack.getItem())) return false;
        TeiRecipeTreeScreen screen = owner;
        TreeIngredient ingredient = target;
        cancel();
        Minecraft.getInstance().setScreen(screen);
        screen.narrow(ingredient, stack.copyWithCount(1));
        return true;
    }

    /** Forgets a pick still waiting: the tree is showing again without one having been made. */
    static void cancel() {
        owner = null;
        target = null;
        members = Set.of();
    }

    /** The item tag whose items are exactly {@code items}; the smallest when several are. */
    private static @Nullable TagKey<Item> tagOf(Set<Item> items) {
        if (items.size() < 2) return null;
        TagKey<Item>[] found = new TagKey[1];
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            if (found[0] != null || pair.getSecond().size() != items.size()) return;
            for (var holder : pair.getSecond()) if (!items.contains(holder.value())) return;
            found[0] = pair.getFirst();
        });
        return found[0];
    }
}
