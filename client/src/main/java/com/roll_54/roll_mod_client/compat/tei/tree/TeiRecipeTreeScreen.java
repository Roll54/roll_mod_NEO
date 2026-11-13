package com.roll_54.roll_mod_client.compat.tei.tree;

import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import com.roll_54.roll_mod.economy.minprice.SaveMinPriceRecipesPacket;
import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreeNode.NarrowStep;
import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreeNode.RecipeStep;
import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreeNode.Step;
import dev.lorp.tei.api.CategoryIcons;
import dev.lorp.tei.api.TeiDisplay;
import dev.lorp.tei.api.TeiSlot;
import dev.lorp.tei.core.IndexManager;
import dev.lorp.tei.core.RecipeTree;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@code /rollmod admin minprice handset} under TEI: the same recipe tree as the EMI one
 * ({@code compat.emi.tree.RecipeTreeScreen}), built on TEI's recipe index. The held item, or one dragged from TEI's
 * item list into the slot, is the root; "Recipes" opens TEI's recipe screen in resolve mode for it, and the picked
 * recipe's ingredients hang below it. Click an ingredient to pick its recipe in TEI's recipe screen, Shift+click to
 * let TEI pick one (its default recipe, or the shortest), right-click to remove it. A tag or list is first narrowed
 * to one stack: click takes the stack shown at that moment, Shift+click the one in the player's inventory (or the
 * first). "Save" sends the picked recipes to the server (config {@code itemRecipes}); a price typed into the field
 * becomes the root's fixed price.
 *
 * <p>A container screen over a client-only, slotless menu, because TEI shows its item list over container screens.
 * A pick applies to every node of the same ingredient; the root and the picks are kept until the game closes.
 */
public class TeiRecipeTreeScreen extends AbstractContainerScreen<TeiRecipeTreeScreen.Menu> {

    private static final int HEADER = 64;
    private static final int FOOTER = 24;
    private static final int MARGIN = 6;
    private static final int MAX_DEPTH = 24;
    private static final float MIN_ZOOM = 0.25F;
    private static final float MAX_ZOOM = 2.0F;
    private static final long STATUS_MS = 3000;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int LINE = 0xFF373737;
    private static final int CRAFTABLE = 0xFFE0B040;
    private static final int CYCLE = 0xFFC04040;
    private static final int TITLE = 0x404040;

    // Kept between openings of the screen.
    private static @Nullable TreeIngredient root;
    private static final List<Resolution> RESOLUTIONS = new ArrayList<>();
    /** A display's inputs / outputs as tree ingredients (reading fluid slots asks the display, so keep them). */
    private static final Map<TeiDisplay, List<TreeIngredient>> INPUTS = new WeakHashMap<>();
    private static final Map<TeiDisplay, List<TreeIngredient>> OUTPUTS = new WeakHashMap<>();

    private record Resolution(TreeIngredient ingredient, Step step) {}

    private @Nullable TeiRecipeTreeNode tree;
    /** Where the root's minimal price is shown (as the hint) and a fixed one can be typed in. */
    private @Nullable EditBox priceBox;
    private final List<TreeIngredient> totals = new ArrayList<>();
    private boolean viewPlaced;
    private double panX, panY;
    private float zoom = 1.0F;
    private boolean panning;
    private @Nullable Component status;
    private long statusUntil;

    public static void open(ItemStack held) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Just the item: prices go by item id, and the held stack's own data (names, tags) would make it
        // match none of the recipes TEI lists for it.
        if (!held.isEmpty()) root = TreeIngredient.of(new ItemStack(held.getItem()), 1);
        mc.setScreen(new TeiRecipeTreeScreen(mc.player.getInventory()));
    }

    public TeiRecipeTreeScreen(Inventory inventory) {
        super(new Menu(), inventory, Component.translatable("gui.roll_mod.recipe_tree.title"));
    }

    @Override
    protected void init() {
        imageWidth = Math.min(width - 40, Math.max(240, width / 2));
        imageHeight = height - 30;
        super.init();

        int cx = leftPos + imageWidth / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.roll_mod.recipe_tree.recipes"), b -> {
                    if (root != null) pick(root);
                })
                .bounds(cx - 118, topPos + 38, 56, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.roll_mod.recipe_tree.auto"), b -> autoResolveAll())
                .tooltip(Tooltip.create(Component.translatable("gui.roll_mod.recipe_tree.auto.tooltip")))
                .bounds(cx - 58, topPos + 38, 56, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.roll_mod.recipe_tree.reset"), b -> {
                    RESOLUTIONS.clear();
                    rebuild();
                })
                .bounds(cx + 2, topPos + 38, 56, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.roll_mod.recipe_tree.save"), b -> save(true))
                .tooltip(Tooltip.create(Component.translatable("gui.roll_mod.recipe_tree.save.tooltip")))
                .bounds(cx + 62, topPos + 38, 56, 18).build());
        int boxX = rootX() + 24;
        priceBox = new EditBox(font, boxX, rootY() + 1, Math.max(60, leftPos + imageWidth - MARGIN - boxX), 16,
                Component.translatable("gui.roll_mod.recipe_tree.price"));
        priceBox.setMaxLength(19);
        priceBox.setFilter(s -> s.isEmpty() || s.equals("-") || s.chars().allMatch(Character::isDigit));
        priceBox.setTooltip(Tooltip.create(Component.translatable("gui.roll_mod.recipe_tree.price.tooltip")));
        addRenderableWidget(priceBox);
        rebuild();
    }

    // ---- tree ----

    /** Called by {@link TeiRecipeTreePicker} once a recipe was picked in TEI for {@code ingredient}. */
    void choose(TreeIngredient ingredient, TeiDisplay recipe) {
        resolve(ingredient, new RecipeStep(recipe));
    }

    private void resolve(TreeIngredient ingredient, Step step) {
        RESOLUTIONS.removeIf(r -> r.ingredient().sameAs(ingredient));
        RESOLUTIONS.add(new Resolution(ingredient, step));
        rebuild();
    }

    /** The root from a stack dropped from TEI's item list (null clears it). */
    public void setRoot(@Nullable TreeIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            root = null;
        } else {
            long amount = ingredient.amount() > 0 ? ingredient.amount() : ingredient.isFluid() ? 1000 : 1;
            // A fluid dropped as one stack of its list: start at a bucket.
            if (ingredient.isFluid() && amount < 1000 && ingredient.single()) amount = 1000;
            root = ingredient.withAmount(amount);
        }
        viewPlaced = false;
        rebuild();
    }

    /** Removes the step picked for {@code ingredient}, leaving it a base resource. */
    void unpick(TreeIngredient ingredient) {
        RESOLUTIONS.removeIf(r -> r.ingredient().sameAs(ingredient));
        rebuild();
    }

    /**
     * Click: a single stack opens TEI's recipes for it in resolve mode; a tag or list is narrowed to the stack it
     * shows right now (TEI's resolve mode works on one stack).
     */
    private void pick(TreeIngredient ingredient) {
        if (!ingredient.single()) {
            resolve(ingredient, new NarrowStep(ingredient.displayed().copyWithCount(1)));
            return;
        }
        Step current = resolution(ingredient);
        TeiDisplay currentRecipe = current instanceof RecipeStep r ? r.display() : null;
        if (!TeiRecipeTreePicker.pick(this, ingredient, currentRecipe)) {
            showStatus(Component.translatable("gui.roll_mod.recipe_tree.no_recipes"));
        }
    }

    /**
     * Shift+click: a tag or list takes the stack the player has (or the first one); a single stack TEI's default
     * recipe for it (the heart), else the one TEI's own tree would use, else the shortest one.
     */
    private void autoPick(TreeIngredient ingredient) {
        Step step;
        if (!ingredient.single()) {
            step = new NarrowStep(heldOrFirst(ingredient));
        } else {
            TeiDisplay recipe = treePlan(ingredient);
            if (recipe == null) recipe = shortestRecipe(ingredient, List.of());
            step = recipe == null ? null : new RecipeStep(recipe);
        }
        if (step != null) {
            resolve(ingredient, step);
        } else {
            showStatus(Component.translatable("gui.roll_mod.recipe_tree.no_recipes"));
        }
    }

    private ItemStack heldOrFirst(TreeIngredient ingredient) {
        Inventory inventory = minecraft.player == null ? null : minecraft.player.getInventory();
        if (inventory != null) {
            for (ItemStack stack : ingredient.stacks()) {
                if (inventory.contains(stack)) return stack;
            }
        }
        return ingredient.first();
    }

    /** TEI's choice for the item (its heart, or the precomputed plan), if it makes the ingredient. */
    private static @Nullable TeiDisplay treePlan(TreeIngredient ingredient) {
        try {
            ItemStack lookup = TreeIngredient.lookupStack(ingredient.first());
            RecipeTree.Plan plan = IndexManager.get().tree().plan(lookup.getItem());
            if (plan != null && made(plan.display(), ingredient) > 0) return plan.display();
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ---- save ----

    /**
     * Sends the recipes picked in this tree to the server ({@code withRecipes}), item (or {@code fluid:<id>}) →
     * recipe id, as the recipes their minimal prices are computed from; and the root's price typed into the price
     * field, which then holds whatever its recipes say ("-" takes it away again). Only real recipes count: the choice
     * of one stack of a tag, and pages without a recipe id, have no recipe the server knows.
     */
    private void save(boolean withRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();
        if (withRecipes && tree != null) collectPicked(tree, recipes);
        Map<String, Long> prices = new LinkedHashMap<>();
        String key = root == null ? null : root.priceKey();
        String typed = priceBox == null ? "" : priceBox.getValue().trim();
        if (key != null && !typed.isEmpty()) {
            if (typed.equals("-")) {
                prices.put(key, SaveMinPriceRecipesPacket.CLEAR);
            } else {
                try {
                    prices.put(key, Long.parseLong(typed));
                } catch (NumberFormatException e) {
                    showStatus(Component.translatable("gui.roll_mod.recipe_tree.bad_price"));
                    return;
                }
            }
        }
        if (recipes.isEmpty() && prices.isEmpty()) {
            showStatus(Component.translatable("gui.roll_mod.recipe_tree.nothing_to_save"));
            return;
        }
        PacketDistributor.sendToServer(new SaveMinPriceRecipesPacket(recipes, prices));
        if (priceBox != null) {
            priceBox.setValue("");
            priceBox.setFocused(false);
        }
        showStatus(Component.translatable("gui.roll_mod.recipe_tree.saving", recipes.size() + prices.size())
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    /** "Min. price: N" of the root (for its amount when more than one), or null when it has none. */
    private static @Nullable Component rootPrice() {
        if (root == null || !root.single()) return null;
        ItemStack stack = root.first();
        Fluid fluid = TreeIngredient.fluidOf(stack);
        if (fluid != null) {
            String key = root.priceKey();
            return key == null ? null : ClientMinPrices.fluidLine(key.substring("fluid:".length()));
        }
        long amount = Math.max(1, root.amount());
        double unit = ClientMinPrices.unit(stack);
        if (unit <= 0) return null;
        return amount == 1
                ? Component.translatable("tooltip.roll_mod.minprice.item", ClientMinPrices.format(unit))
                : Component.translatable("tooltip.roll_mod.minprice.stack", ClientMinPrices.format(unit),
                        amount, ClientMinPrices.format(unit * amount));
    }

    private static void collectPicked(TeiRecipeTreeNode node, Map<String, String> out) {
        if (node.step == null) return;
        TeiDisplay recipe = node.recipe();
        ResourceLocation recipeId = recipe == null ? null : recipe.id();
        String key = node.ingredient.priceKey();
        if (key != null && recipeId != null) out.putIfAbsent(key, recipeId.toString());
        for (TeiRecipeTreeNode child : node.children) collectPicked(child, out);
    }

    // ---- auto ----

    /** Passes over the tree; each one picks recipes for the ingredients the previous one added. */
    private static final int AUTO_PASSES = 64;

    /**
     * The "Auto" button: every ingredient that has no recipe yet (the root included) gets the shortest one, down the
     * whole tree, until only base resources are left. Recipes already picked stay as they are.
     */
    private void autoResolveAll() {
        if (root == null) return;
        List<TreeIngredient> tried = new ArrayList<>();
        int picked = 0;
        for (int pass = 0; pass < AUTO_PASSES; pass++) {
            List<Open> open = new ArrayList<>();
            collectOpen(build(root, root.amount(), new ArrayList<>()), new ArrayList<>(), open);
            boolean progress = false;
            for (Open node : open) {
                if (resolution(node.ingredient()) != null) continue; // picked earlier in this pass
                if (tried.stream().anyMatch(t -> t.sameAs(node.ingredient()))) continue;
                tried.add(node.ingredient());
                Step step;
                if (!node.ingredient().single()) {
                    step = new NarrowStep(heldOrFirst(node.ingredient()));
                } else {
                    TeiDisplay recipe = shortestRecipe(node.ingredient(), node.path());
                    step = recipe == null ? null : new RecipeStep(recipe);
                }
                if (step == null) continue;
                RESOLUTIONS.add(new Resolution(node.ingredient(), step));
                picked++;
                progress = true;
            }
            if (!progress) break;
        }
        rebuild();
        showStatus(Component.translatable("gui.roll_mod.recipe_tree.auto_done", picked).withStyle(ChatFormatting.DARK_GREEN));
    }

    private record Open(TreeIngredient ingredient, List<TreeIngredient> path) {}

    /** Nodes without a step that could have one, with the ingredients above them. */
    private static void collectOpen(TeiRecipeTreeNode node, List<TreeIngredient> path, List<Open> out) {
        if (node.step == null) {
            if (!node.cycle && node.craftable && path.size() < MAX_DEPTH) out.add(new Open(node.ingredient, List.copyOf(path)));
            return;
        }
        path.add(node.ingredient);
        for (TeiRecipeTreeNode child : node.children) collectOpen(child, path, out);
        path.remove(path.size() - 1);
    }

    /**
     * The shortest recipe for a single stack: the fewest different inputs, then the fewest input items per item
     * made. Recipes that take something above it in the tree are skipped (they would loop), and recipes that only
     * undo another one (ingot from block, block from ingots) come last, so iron ends at its ore, not in a block loop.
     */
    private static @Nullable TeiDisplay shortestRecipe(TreeIngredient ingredient, List<TreeIngredient> path) {
        if (!ingredient.single()) return null;
        TeiDisplay best = null;
        double[] bestScore = null;
        for (TeiDisplay recipe : recipesFor(ingredient)) {
            if (!recipe.usableForRecipeTree()) continue;
            long made = made(recipe, ingredient);
            if (made <= 0) continue;
            List<TreeIngredient> inputs = inputsOf(recipe);
            if (inputs.isEmpty()) continue;
            if (inputs.stream().anyMatch(in -> in.mentions(ingredient) || path.stream().anyMatch(in::mentions))) continue;
            long items = inputs.stream().mapToLong(in -> Math.max(1, in.amount())).sum();
            double[] score = {undoes(recipe, inputs, ingredient) ? 1 : 0, inputs.size(), (double) items / made};
            if (bestScore == null || compare(score, bestScore) < 0) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    private static int compare(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            int c = Double.compare(a[i], b[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    /** Whether one of the recipe's inputs is itself made from {@code made}: the recipe just reverses another. */
    private static boolean undoes(TeiDisplay recipe, List<TreeIngredient> inputs, TreeIngredient made) {
        for (TreeIngredient input : inputs) {
            if (!input.single()) continue;
            for (TeiDisplay back : recipesFor(input)) {
                if (back != recipe && inputsOf(back).stream().anyMatch(in -> in.mentions(made))) return true;
            }
        }
        return false;
    }

    /** TEI's recipes making the stack (a fluid's are indexed on its bucket). */
    private static List<TeiDisplay> recipesFor(TreeIngredient ingredient) {
        if (ingredient.isEmpty()) return List.of();
        try {
            return IndexManager.get().recipesFor(TreeIngredient.lookupStack(ingredient.first()));
        } catch (Throwable t) {
            return List.of();
        }
    }

    private void rebuild() {
        totals.clear();
        if (root == null) {
            tree = null;
            return;
        }
        tree = build(root, root.amount(), new ArrayList<>());
        tree.layout(0, 0);
        collectTotals(tree);
        if (!viewPlaced && width > 0) placeView();
    }

    private static TeiRecipeTreeNode build(TreeIngredient ingredient, long amount, List<TreeIngredient> path) {
        boolean cycle = path.stream().anyMatch(p -> p.sameAs(ingredient));
        Step step = cycle || path.size() >= MAX_DEPTH ? null : resolution(ingredient);
        List<TeiRecipeTreeNode> children = new ArrayList<>();
        if (step instanceof RecipeStep(TeiDisplay recipe)) {
            long crafts = Math.ceilDiv(amount, Math.max(1, made(recipe, ingredient)));
            path.add(ingredient);
            for (TreeIngredient input : inputsOf(recipe)) {
                children.add(build(input, Math.max(1, input.amount()) * crafts, path));
            }
            path.remove(path.size() - 1);
        } else if (step instanceof NarrowStep(ItemStack chosen)) {
            path.add(ingredient);
            children.add(build(TreeIngredient.of(chosen, amount), amount, path));
            path.remove(path.size() - 1);
        }
        return new TeiRecipeTreeNode(ingredient, amount, step, children, cycle, step != null || craftable(ingredient));
    }

    private static @Nullable Step resolution(TreeIngredient ingredient) {
        for (Resolution r : RESOLUTIONS) {
            if (r.ingredient().sameAs(ingredient)) return r.step();
        }
        return null;
    }

    /** How much of {@code ingredient} one run of the recipe makes (0 when it makes none). */
    private static long made(TeiDisplay recipe, TreeIngredient ingredient) {
        long best = 0;
        for (TreeIngredient output : outputsOf(recipe)) {
            if (!output.mentions(ingredient)) continue;
            long amount = output.amount();
            // A bucket output standing for the fluid wanted: one bucket per item.
            if (ingredient.isFluid() && !output.isFluid()) amount *= 1000;
            best = Math.max(best, amount);
        }
        return best;
    }

    /** The recipe's inputs with repeats (a shaped grid lists every slot) added together. */
    private static List<TreeIngredient> inputsOf(TeiDisplay recipe) {
        List<TreeIngredient> cached = INPUTS.get(recipe);
        if (cached != null) return cached;
        List<TreeIngredient> merged = new ArrayList<>();
        outer:
        for (TeiSlot slot : recipe.slots()) {
            if (slot.role() != TeiSlot.Role.INPUT) continue;
            TreeIngredient input = TreeIngredient.ofSlot(recipe, slot);
            if (input == null) continue;
            for (int i = 0; i < merged.size(); i++) {
                TreeIngredient existing = merged.get(i);
                if (existing.sameAs(input)) {
                    merged.set(i, existing.withAmount(existing.amount() + input.amount()));
                    continue outer;
                }
            }
            merged.add(input);
        }
        List<TreeIngredient> result = List.copyOf(merged);
        INPUTS.put(recipe, result);
        return result;
    }

    private static List<TreeIngredient> outputsOf(TeiDisplay recipe) {
        List<TreeIngredient> cached = OUTPUTS.get(recipe);
        if (cached != null) return cached;
        List<TreeIngredient> out = new ArrayList<>();
        for (TeiSlot slot : recipe.slots()) {
            if (slot.role() != TeiSlot.Role.OUTPUT) continue;
            TreeIngredient output = TreeIngredient.ofSlot(recipe, slot);
            if (output != null) out.add(output);
        }
        List<TreeIngredient> result = List.copyOf(out);
        OUTPUTS.put(recipe, result);
        return result;
    }

    private static boolean craftable(TreeIngredient ingredient) {
        // A tag or list first has to be narrowed to one stack.
        if (!ingredient.single()) return true;
        return !recipesFor(ingredient).isEmpty();
    }

    private void collectTotals(TeiRecipeTreeNode node) {
        if (node.step != null) {
            for (TeiRecipeTreeNode child : node.children) collectTotals(child);
            return;
        }
        for (int i = 0; i < totals.size(); i++) {
            TreeIngredient existing = totals.get(i);
            if (existing.sameAs(node.ingredient)) {
                totals.set(i, existing.withAmount(existing.amount() + node.amount));
                return;
            }
        }
        totals.add(node.ingredient.withAmount(node.amount));
    }

    // ---- geometry ----

    private int areaLeft() { return leftPos + MARGIN; }
    private int areaTop() { return topPos + HEADER; }
    private int areaRight() { return leftPos + imageWidth - MARGIN; }
    private int areaBottom() { return topPos + imageHeight - FOOTER; }

    private boolean inArea(double mx, double my) {
        return mx >= areaLeft() && mx < areaRight() && my >= areaTop() && my < areaBottom();
    }

    private int rootX() { return leftPos + imageWidth / 2 - 9; }
    private int rootY() { return topPos + 16; }

    /** The whole panel {x, y, width, height}, for TEI to lay its sidebars out around. */
    public int[] panelRect() {
        return new int[]{leftPos, topPos, imageWidth, imageHeight};
    }

    /** Where a stack may be dropped from TEI's item list: the root slot {x, y, width, height}. */
    public int[] rootRect() {
        return new int[]{rootX(), rootY(), 18, 18};
    }

    private boolean onRoot(double mx, double my) {
        return mx >= rootX() && mx < rootX() + 18 && my >= rootY() && my < rootY() + 18;
    }

    private void placeView() {
        zoom = 1.0F;
        int areaWidth = areaRight() - areaLeft();
        panX = tree == null ? 0 : (areaWidth - tree.width) / 2.0;
        panY = 8;
        viewPlaced = true;
    }

    private double toTreeX(double mx) { return (mx - areaLeft() - panX) / zoom; }
    private double toTreeY(double my) { return (my - areaTop() - panY) / zoom; }

    private @Nullable TeiRecipeTreeNode nodeAt(double mx, double my) {
        if (tree == null || !inArea(mx, my)) return null;
        return tree.find(toTreeX(mx), toTreeY(my));
    }

    /** Stack under the mouse for TEI's own keys (recipes, uses, favourites). */
    public @Nullable TreeIngredient ingredientAt(double mx, double my) {
        if (root != null && onRoot(mx, my)) return root;
        TeiRecipeTreeNode node = nodeAt(mx, my);
        if (node != null) return node.ingredient.withAmount(node.amount);
        int index = totalAt(mx, my);
        return index >= 0 ? totals.get(index) : null;
    }

    private int totalsX() {
        return leftPos + MARGIN + font.width(Component.translatable("gui.roll_mod.recipe_tree.totals")) + 4;
    }

    private int totalsY() { return areaBottom() + 3; }

    private int visibleTotals() {
        return Math.max(0, Math.min(totals.size(), (areaRight() - totalsX()) / 18));
    }

    private int totalAt(double mx, double my) {
        if (my < totalsY() || my >= totalsY() + 18 || mx < totalsX()) return -1;
        int index = (int) ((mx - totalsX()) / 18);
        return index < visibleTotals() ? index : -1;
    }

    // ---- rendering ----

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int l = leftPos, t = topPos, r = leftPos + imageWidth, b = topPos + imageHeight;
        g.fill(l, t, r, b, DARK);
        g.fill(l + 1, t + 1, r - 1, b - 1, LIGHT);
        g.fill(l + 2, t + 2, r - 1, b - 1, 0xFF555555);
        g.fill(l + 2, t + 2, r - 2, b - 2, PANEL);
        recess(g, areaLeft(), areaTop(), areaRight(), areaBottom(), SLOT);
        recess(g, rootX(), rootY(), rootX() + 18, rootY() + 18, SLOT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 5, TITLE, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (priceBox != null) {
            priceBox.visible = root != null && root.priceKey() != null;
            // The server's current minimal price as the field's hint, read every frame, so a save shows up
            // as soon as the rebuilt table reaches the client.
            Component price = rootPrice();
            priceBox.setHint((price != null ? price : Component.translatable("gui.roll_mod.recipe_tree.price"))
                    .copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        super.render(g, mouseX, mouseY, partialTick);

        if (root != null) {
            g.renderItem(root.displayed(), rootX() + 1, rootY() + 1);
            drawAmount(g, root, root.amount(), rootX(), rootY());
        }

        if (tree != null) {
            g.enableScissor(areaLeft() + 1, areaTop() + 1, areaRight() - 1, areaBottom() - 1);
            g.pose().pushPose();
            g.pose().translate(areaLeft() + panX, areaTop() + panY, 0);
            g.pose().scale(zoom, zoom, 1);
            drawNode(g, tree);
            g.pose().popPose();
            g.disableScissor();
        } else {
            Component hint = Component.translatable("gui.roll_mod.recipe_tree.empty");
            g.drawString(font, hint, (areaLeft() + areaRight() - font.width(hint)) / 2,
                    (areaTop() + areaBottom()) / 2 - 4, 0xFFE0E0E0, true);
        }

        renderFooter(g);
        renderHover(g, mouseX, mouseY);
    }

    private void drawNode(GuiGraphics g, TeiRecipeTreeNode node) {
        int border = node.cycle ? CYCLE : node.step == null && node.craftable ? CRAFTABLE : 0;
        if (border != 0) g.fill(node.x - 1, node.y - 1, node.x + 19, node.y + 19, border);
        recess(g, node.x, node.y, node.x + 18, node.y + 18, SLOT);
        g.renderItem(node.ingredient.displayed(), node.x + 1, node.y + 1);
        drawAmount(g, node.ingredient, node.amount, node.x, node.y);

        if (node.step == null) return;
        int cx = node.x + 9;
        TeiDisplay recipe = node.recipe();
        if (recipe != null) {
            g.pose().pushPose();
            g.pose().translate(cx - 5, node.y + 20, 0);
            g.pose().scale(0.625F, 0.625F, 1);
            try {
                CategoryIcons.draw(g, recipe.category(), 0, 0);
            } catch (Throwable ignored) {
            }
            g.pose().popPose();
        }

        if (node.children.isEmpty()) return;
        int barY = node.y + 35;
        TeiRecipeTreeNode first = node.children.get(0);
        TeiRecipeTreeNode last = node.children.get(node.children.size() - 1);
        g.fill(cx, recipe != null ? node.y + 31 : node.y + 18, cx + 1, barY, LINE);
        g.fill(Math.min(first.x + 9, cx), barY, Math.max(last.x + 9, cx) + 1, barY + 1, LINE);
        for (TeiRecipeTreeNode child : node.children) {
            g.fill(child.x + 9, barY, child.x + 10, child.y, LINE);
            drawNode(g, child);
        }
    }

    private void renderFooter(GuiGraphics g) {
        int y = totalsY();
        if (status != null && Util.getMillis() < statusUntil) {
            g.drawString(font, status, leftPos + MARGIN, y + 5, 0xFFA02020, false);
            return;
        }
        if (totals.isEmpty() || tree == null || tree.step == null) return;
        g.drawString(font, Component.translatable("gui.roll_mod.recipe_tree.totals"), leftPos + MARGIN, y + 5, TITLE, false);
        int shown = visibleTotals();
        for (int i = 0; i < shown; i++) {
            int x = totalsX() + i * 18;
            recess(g, x, y, x + 18, y + 18, SLOT);
            TreeIngredient total = totals.get(i);
            g.renderItem(total.displayed(), x + 1, y + 1);
            drawAmount(g, total, total.amount(), x, y);
        }
    }

    private void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (onRoot(mouseX, mouseY)) {
            if (root == null) {
                lines.add(Component.translatable("gui.roll_mod.recipe_tree.empty"));
            } else {
                lines.addAll(describe(root));
                lines.add(amountLine(root, root.amount()));
                lines.add(hint(root.single() ? "gui.roll_mod.recipe_tree.hint.pick" : "gui.roll_mod.recipe_tree.hint.narrow"));
                lines.add(hint("gui.roll_mod.recipe_tree.hint.amount"));
                lines.add(hint("gui.roll_mod.recipe_tree.hint.root_clear"));
            }
        } else {
            TeiRecipeTreeNode node = nodeAt(mouseX, mouseY);
            if (node != null) {
                lines.addAll(describe(node.ingredient));
                lines.add(amountLine(node.ingredient, node.amount));
                TeiDisplay recipe = node.recipe();
                if (recipe != null) {
                    lines.add(Component.translatable("gui.roll_mod.recipe_tree.recipe", recipe.category().title())
                            .withStyle(ChatFormatting.BLUE));
                }
                if (node.cycle) {
                    lines.add(Component.translatable("gui.roll_mod.recipe_tree.cycle").withStyle(ChatFormatting.RED));
                } else if (node.craftable) {
                    if (!node.ingredient.single()) lines.add(hint("gui.roll_mod.recipe_tree.hint.narrow"));
                    else lines.add(hint(node.step == null ? "gui.roll_mod.recipe_tree.hint.pick" : "gui.roll_mod.recipe_tree.hint.change"));
                    lines.add(hint("gui.roll_mod.recipe_tree.hint.auto"));
                }
                if (node.step != null) lines.add(hint("gui.roll_mod.recipe_tree.hint.clear"));
            } else {
                int index = totalAt(mouseX, mouseY);
                if (index >= 0 && tree != null && tree.step != null) {
                    TreeIngredient total = totals.get(index);
                    lines.addAll(describe(total));
                    lines.add(amountLine(total, total.amount()));
                    lines.add(hint(total.single() ? "gui.roll_mod.recipe_tree.hint.pick" : "gui.roll_mod.recipe_tree.hint.narrow"));
                }
            }
        }
        if (!lines.isEmpty()) g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private List<Component> describe(TreeIngredient ingredient) {
        if (ingredient.single()) return new ArrayList<>(Screen.getTooltipFromItem(minecraft, ingredient.first()));
        return List.of(Component.translatable("gui.roll_mod.recipe_tree.any_of", ingredient.displayed().getHoverName()));
    }

    private static Component amountLine(TreeIngredient ingredient, long amount) {
        String value = ingredient.isFluid() ? amount + " mB" : String.valueOf(amount);
        return Component.translatable("gui.roll_mod.recipe_tree.amount", value).withStyle(ChatFormatting.GRAY);
    }

    private static Component hint(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY);
    }

    private void drawAmount(GuiGraphics g, TreeIngredient ingredient, long amount, int x, int y) {
        boolean fluid = ingredient.isFluid();
        if (amount == 1 && !fluid) return;
        String text = fluid ? compact(amount) + "mB" : compact(amount);
        float scale = font.width(text) > 16 ? 0.5F : 1.0F;
        g.pose().pushPose();
        g.pose().translate(x + 17 - font.width(text) * scale, y + 17 - 8 * scale, 200);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    private static String compact(long n) {
        if (n < 10_000) return String.valueOf(n);
        if (n < 1_000_000) return trim(n / 1_000.0) + "k";
        if (n < 1_000_000_000) return trim(n / 1_000_000.0) + "M";
        return trim(n / 1_000_000_000.0) + "G";
    }

    private static String trim(double v) {
        return v >= 100 ? String.valueOf((long) v) : String.valueOf(Math.floor(v * 10) / 10).replace(".0", "");
    }

    /** A vanilla-style sunken box: dark top-left, light bottom-right. */
    private static void recess(GuiGraphics g, int l, int t, int r, int b, int fill) {
        g.fill(l, t, r, b, DARK);
        g.fill(l + 1, t + 1, r, b, LIGHT);
        g.fill(l + 1, t + 1, r - 1, b - 1, fill);
    }

    private void showStatus(Component text) {
        // Red unless the caller gave it a colour of its own.
        status = text.getStyle().getColor() == null ? text.copy().withStyle(ChatFormatting.DARK_RED) : text;
        statusUntil = Util.getMillis() + STATUS_MS;
    }

    // ---- input ----
    // The same mouse keys as the EMI tree: click picks a recipe in TEI's recipe screen, Shift+click lets TEI pick
    // one, right-click removes it. Mouse handling never reaches AbstractContainerScreen: with no slots, its click
    // logic would only send "clicked outside" packets for a menu the server doesn't know.

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (priceBox != null && !priceBox.isMouseOver(mouseX, mouseY)) priceBox.setFocused(false);
        for (GuiEventListener child : children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                setFocused(child);
                if (button == 0) setDragging(true);
                return true;
            }
        }
        if (onRoot(mouseX, mouseY)) {
            if (button == 1) setRoot(null);
            else if (button == 0 && root != null) {
                if (hasShiftDown()) autoPick(root);
                else pick(root);
            }
            return true;
        }
        if (inArea(mouseX, mouseY)) {
            TeiRecipeTreeNode node = nodeAt(mouseX, mouseY);
            if (node != null) {
                if (button == 0 && !node.cycle) {
                    if (hasShiftDown()) autoPick(node.ingredient);
                    else pick(node.ingredient);
                } else if (button == 1 && node.step != null) {
                    unpick(node.ingredient);
                }
            } else if (button == 0) {
                panning = true;
            }
            return true;
        }
        int index = totalAt(mouseX, mouseY);
        if (index >= 0 && button == 0) {
            TreeIngredient total = totals.get(index);
            if (hasShiftDown()) autoPick(total);
            else pick(total);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        GuiEventListener focused = getFocused();
        return focused != null && isDragging() && button == 0 && focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panning = false;
        setDragging(false);
        GuiEventListener focused = getFocused();
        if (focused != null) focused.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        if (root != null && onRoot(mouseX, mouseY)) {
            long step = (root.isFluid() ? 1000 : 1) * (hasShiftDown() ? 10 : 1);
            root = root.withAmount(Math.max(1, root.amount() + (scrollY > 0 ? step : -step)));
            rebuild();
            return true;
        }
        if (tree != null && inArea(mouseX, mouseY)) {
            double treeX = toTreeX(mouseX), treeY = toTreeY(mouseY);
            zoom = Mth.clamp(zoom * (scrollY > 0 ? 1.1F : 1 / 1.1F), MIN_ZOOM, MAX_ZOOM);
            // Keep the point under the mouse where it is.
            panX = mouseX - areaLeft() - treeX * zoom;
            panY = mouseY - areaTop() - treeY * zoom;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (priceBox != null && priceBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                save(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                priceBox.setFocused(false);
                return true;
            }
            // Everything else is typing, not the inventory key closing the screen.
            priceBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // Not AbstractContainerScreen's: it tells the server to close a container the server never opened.
        minecraft.setScreen(null);
    }

    /** Client-only and slotless; it exists because TEI shows its item list over container screens. */
    public static final class Menu extends AbstractContainerMenu {
        Menu() {
            super(null, -1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
