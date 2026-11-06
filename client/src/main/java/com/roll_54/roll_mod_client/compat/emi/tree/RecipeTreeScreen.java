package com.roll_54.roll_mod_client.compat.emi.tree;

import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import com.roll_54.roll_mod.economy.minprice.SaveMinPriceRecipesPacket;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
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

/**
 * {@code /rollmod admin minprice handset}: a recipe tree in the style of EMI's own, for choosing the
 * recipes minimal prices are computed from. The held item, or one dragged from EMI into the slot, is the root; "Recipes" opens EMI's recipe screen for it, and the picked recipe's ingredients hang below it.
 * Any ingredient takes a recipe the way EMI's tree does — click to pick it in EMI's recipe screen,
 * Shift+click to let EMI pick one, right-click to remove it — so the tree grows down to whatever should
 * stay a base resource; the footer sums those up.
 * "Save" sends the picked recipes to the server (config {@code itemRecipes}), which rebuilds the prices at
 * once; the root's minimal price next to its slot follows the server's table live.
 *
 * <p>It is a container screen (over a client-only, slotless menu) because EMI only shows its item list
 * and opens recipe screens over container screens. A pick applies to every node of the same ingredient,
 * and both the root and the picks are kept until the game closes.
 */
public class RecipeTreeScreen extends AbstractContainerScreen<RecipeTreeScreen.Menu> {

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
    private static @Nullable EmiIngredient root;
    private static final List<Resolution> RESOLUTIONS = new ArrayList<>();

    private record Resolution(EmiIngredient ingredient, EmiRecipe recipe) {}

    private @Nullable RecipeTreeNode tree;
    /** Where the root's minimal price is shown (as the hint) and a fixed one can be typed in. */
    private @Nullable EditBox priceBox;
    private final List<EmiIngredient> totals = new ArrayList<>();
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
        // match none of the recipes EMI lists for it.
        if (!held.isEmpty()) root = EmiStack.of(held.getItem()).setAmount(1);
        mc.setScreen(new RecipeTreeScreen(mc.player.getInventory()));
    }

    public RecipeTreeScreen(Inventory inventory) {
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

    /** Called by {@link RecipeTreePicker} once a recipe was picked in EMI for {@code ingredient}. */
    void choose(EmiIngredient ingredient, EmiRecipe recipe) {
        RESOLUTIONS.removeIf(r -> EmiIngredient.areEqual(r.ingredient(), ingredient));
        RESOLUTIONS.add(new Resolution(ingredient, recipe));
        rebuild();
    }

    public void setRoot(@Nullable EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            root = null;
        } else {
            long amount = ingredient.getAmount() > 0 ? ingredient.getAmount() : isFluid(ingredient) ? 1000 : 1;
            root = ingredient.copy().setAmount(amount);
        }
        viewPlaced = false;
        rebuild();
    }

    /** Removes the recipe picked for {@code ingredient}, leaving it a base resource. */
    void unpick(EmiIngredient ingredient) {
        RESOLUTIONS.removeIf(r -> EmiIngredient.areEqual(r.ingredient(), ingredient));
        rebuild();
    }

    private void pick(EmiIngredient ingredient) {
        if (!RecipeTreePicker.pick(this, ingredient)) {
            showStatus(Component.translatable("gui.roll_mod.recipe_tree.no_recipes"));
        }
    }

    /**
     * EMI's Shift+click in its tree: a tag or list takes the stack the player has (or the first one), a
     * single stack the recipe EMI would craft it with from the player's inventory.
     */
    private void autoPick(EmiIngredient ingredient) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        EmiRecipe recipe;
        if (stacks.size() > 1) {
            EmiPlayerInventory inventory = EmiPlayerInventory.of(minecraft.player);
            EmiStack chosen = stacks.stream().filter(inventory.inventory::containsKey).findFirst().orElse(stacks.get(0));
            recipe = new EmiResolutionRecipe(ingredient, chosen);
        } else {
            recipe = EmiUtil.getRecipeResolution(ingredient, EmiPlayerInventory.of(minecraft.player));
        }
        if (recipe != null) {
            choose(ingredient, recipe);
        } else {
            showStatus(Component.translatable("gui.roll_mod.recipe_tree.no_recipes"));
        }
    }

    // ---- save ----

    /**
     * Sends the recipes picked in this tree to the server ({@code withRecipes}), item (or {@code fluid:<id>}) →
     * recipe id, as the recipes their minimal prices are computed from; and the root's price typed into the
     * price field, which then holds whatever its recipes say ("-" takes it away again). Only real recipes count: the choice of one stack of a
     * tag, and EMI's own synthetic pages, have no recipe the server knows.
     */
    private void save(boolean withRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();
        if (withRecipes && tree != null) collectPicked(tree, recipes);
        Map<String, Long> prices = new LinkedHashMap<>();
        String key = rootKey();
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

    /** The root's key on the server: its item id, or {@code fluid:<id>}; null without a single stack. */
    private static @Nullable String rootKey() {
        if (root == null || root.getEmiStacks().size() != 1) return null;
        EmiStack stack = root.getEmiStacks().get(0);
        return stack.getKey() instanceof Fluid ? "fluid:" + stack.getId() : stack.getId().toString();
    }

    /** "Min. price: N" of the root (for its amount when more than one), or null when it has none. */
    private static @Nullable Component rootPrice() {
        if (root == null || root.getEmiStacks().size() != 1) return null;
        EmiStack stack = root.getEmiStacks().get(0);
        long amount = Math.max(1, root.getAmount());
        if (stack.getKey() instanceof Fluid) {
            return ClientMinPrices.fluidLine(stack.getId().toString());
        }
        ItemStack item = stack.getItemStack();
        if (item.isEmpty()) return null;
        double unit = ClientMinPrices.unit(item);
        if (unit <= 0) return null;
        return amount == 1
                ? Component.translatable("tooltip.roll_mod.minprice.item", ClientMinPrices.format(unit))
                : Component.translatable("tooltip.roll_mod.minprice.stack", ClientMinPrices.format(unit),
                        amount, ClientMinPrices.format(unit * amount));
    }

    private static void collectPicked(RecipeTreeNode node, Map<String, String> out) {
        if (node.recipe == null) return;
        List<EmiStack> stacks = node.ingredient.getEmiStacks();
        ResourceLocation recipeId = node.recipe.getId();
        if (stacks.size() == 1 && recipeId != null && !(node.recipe instanceof EmiResolutionRecipe)) {
            EmiStack stack = stacks.get(0);
            String key = stack.getKey() instanceof Fluid ? "fluid:" + stack.getId() : stack.getId().toString();
            out.putIfAbsent(key, recipeId.toString());
        }
        for (RecipeTreeNode child : node.children) collectPicked(child, out);
    }

    // ---- auto ----

    /** Passes over the tree; each one picks recipes for the ingredients the previous one added. */
    private static final int AUTO_PASSES = 64;

    /**
     * The "Auto" button: every ingredient that has no recipe yet (the root included) gets the shortest one,
     * down the whole tree, until only base resources are left. Recipes already picked stay as they are.
     */
    private void autoResolveAll() {
        if (root == null) return;
        List<EmiIngredient> tried = new ArrayList<>();
        int picked = 0;
        for (int pass = 0; pass < AUTO_PASSES; pass++) {
            List<Open> open = new ArrayList<>();
            collectOpen(build(root, root.getAmount(), new ArrayList<>()), new ArrayList<>(), open);
            boolean progress = false;
            for (Open node : open) {
                if (resolution(node.ingredient()) != null) continue; // picked earlier in this pass
                if (tried.stream().anyMatch(t -> EmiIngredient.areEqual(t, node.ingredient()))) continue;
                tried.add(node.ingredient());
                EmiRecipe recipe = shortestRecipe(node.ingredient(), node.path());
                if (recipe == null) continue;
                RESOLUTIONS.add(new Resolution(node.ingredient(), recipe));
                picked++;
                progress = true;
            }
            if (!progress) break;
        }
        rebuild();
        showStatus(Component.translatable("gui.roll_mod.recipe_tree.auto_done", picked).withStyle(ChatFormatting.DARK_GREEN));
    }

    private record Open(EmiIngredient ingredient, List<EmiIngredient> path) {}

    /** Nodes without a recipe that could have one, with the ingredients above them. */
    private static void collectOpen(RecipeTreeNode node, List<EmiIngredient> path, List<Open> out) {
        if (node.recipe == null) {
            if (!node.cycle && node.craftable && path.size() < MAX_DEPTH) out.add(new Open(node.ingredient, List.copyOf(path)));
            return;
        }
        path.add(node.ingredient);
        for (RecipeTreeNode child : node.children) collectOpen(child, path, out);
        path.remove(path.size() - 1);
    }

    /**
     * The shortest recipe for {@code ingredient}: the fewest different inputs, then the fewest input items
     * per item made. A tag or list is narrowed to the stack the player has, or its first one. Recipes that
     * take something above it in the tree are skipped (they would loop), and recipes that only undo another
     * one (ingot from block, block from ingots) come last, so iron ends at its ore, not in a block loop.
     */
    private @Nullable EmiRecipe shortestRecipe(EmiIngredient ingredient, List<EmiIngredient> path) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks.isEmpty()) return null;
        if (stacks.size() > 1) {
            EmiPlayerInventory inventory = EmiPlayerInventory.of(minecraft.player);
            EmiStack chosen = stacks.stream().filter(inventory.inventory::containsKey).findFirst().orElse(stacks.get(0));
            return new EmiResolutionRecipe(ingredient, chosen);
        }
        EmiStack stack = stacks.get(0);
        EmiRecipe best = null;
        double[] bestScore = null;
        for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
            if (!recipe.supportsRecipeTree()) continue;
            long made = recipe.getOutputs().stream().filter(out -> sameKey(out, stack)).mapToLong(EmiStack::getAmount).max().orElse(0);
            if (made <= 0) continue;
            List<EmiIngredient> inputs = mergedInputs(recipe);
            if (inputs.isEmpty()) continue;
            if (inputs.stream().anyMatch(in -> mentions(in, stack) || path.stream().anyMatch(p -> mentions(in, p)))) continue;
            long items = inputs.stream().mapToLong(in -> Math.max(1, in.getAmount())).sum();
            double[] score = {undoes(recipe, inputs, stack) ? 1 : 0, inputs.size(), (double) items / made};
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

    /** The same item or fluid, whatever data the stacks carry (names, tags): prices go by id. */
    private static boolean sameKey(EmiStack a, EmiStack b) {
        return a.getKey().equals(b.getKey());
    }

    /** Whether {@code input} could be {@code other} (shares a stack with it). */
    private static boolean mentions(EmiIngredient input, EmiIngredient other) {
        for (EmiStack a : input.getEmiStacks()) {
            for (EmiStack b : other.getEmiStacks()) {
                if (sameKey(a, b)) return true;
            }
        }
        return false;
    }

    /** Whether one of the recipe's inputs is itself made from {@code made}: the recipe just reverses another. */
    private static boolean undoes(EmiRecipe recipe, List<EmiIngredient> inputs, EmiStack made) {
        for (EmiIngredient input : inputs) {
            if (input.getEmiStacks().size() != 1) continue;
            for (EmiRecipe back : EmiApi.getRecipeManager().getRecipesByOutput(input.getEmiStacks().get(0))) {
                if (back != recipe && back.getInputs().stream().anyMatch(in -> mentions(in, made))) return true;
            }
        }
        return false;
    }

    private void rebuild() {
        totals.clear();
        if (root == null) {
            tree = null;
            return;
        }
        tree = build(root, root.getAmount(), new ArrayList<>());
        tree.layout(0, 0);
        collectTotals(tree);
        if (!viewPlaced && width > 0) placeView();
    }

    private RecipeTreeNode build(EmiIngredient ingredient, long amount, List<EmiIngredient> path) {
        boolean cycle = path.stream().anyMatch(p -> EmiIngredient.areEqual(p, ingredient));
        EmiRecipe recipe = cycle || path.size() >= MAX_DEPTH ? null : resolution(ingredient);
        List<RecipeTreeNode> children = new ArrayList<>();
        if (recipe != null) {
            long crafts = crafts(recipe, ingredient, amount);
            path.add(ingredient);
            for (EmiIngredient input : mergedInputs(recipe)) {
                children.add(build(input, Math.max(1, input.getAmount()) * crafts, path));
            }
            path.remove(path.size() - 1);
        }
        return new RecipeTreeNode(ingredient, amount, recipe, children, cycle, recipe != null || craftable(ingredient));
    }

    private static @Nullable EmiRecipe resolution(EmiIngredient ingredient) {
        for (Resolution r : RESOLUTIONS) {
            if (EmiIngredient.areEqual(r.ingredient(), ingredient)) return r.recipe();
        }
        return null;
    }

    /** How many times the recipe has to run for {@code amount} of {@code ingredient}. */
    private static long crafts(EmiRecipe recipe, EmiIngredient ingredient, long amount) {
        long perCraft = 1;
        for (EmiStack output : recipe.getOutputs()) {
            if (ingredient.getEmiStacks().stream().anyMatch(s -> sameKey(s, output))) {
                perCraft = Math.max(1, output.getAmount());
                break;
            }
        }
        return Math.ceilDiv(amount, perCraft);
    }

    /** The recipe's inputs with repeats (a shaped grid lists every slot) added together. */
    private static List<EmiIngredient> mergedInputs(EmiRecipe recipe) {
        List<EmiIngredient> merged = new ArrayList<>();
        outer:
        for (EmiIngredient input : recipe.getInputs()) {
            if (input.isEmpty()) continue;
            for (int i = 0; i < merged.size(); i++) {
                EmiIngredient existing = merged.get(i);
                if (EmiIngredient.areEqual(existing, input)) {
                    merged.set(i, existing.copy().setAmount(existing.getAmount() + input.getAmount()));
                    continue outer;
                }
            }
            merged.add(input.copy());
        }
        return merged;
    }

    private static boolean craftable(EmiIngredient ingredient) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        // A tag or list first has to be narrowed to one stack, which EMI offers as a recipe of its own.
        if (stacks.size() != 1) return true;
        return !EmiApi.getRecipeManager().getRecipesByOutput(stacks.get(0)).isEmpty();
    }

    private void collectTotals(RecipeTreeNode node) {
        if (node.recipe != null) {
            for (RecipeTreeNode child : node.children) collectTotals(child);
            return;
        }
        for (int i = 0; i < totals.size(); i++) {
            EmiIngredient existing = totals.get(i);
            if (EmiIngredient.areEqual(existing, node.ingredient)) {
                totals.set(i, existing.copy().setAmount(existing.getAmount() + node.amount));
                return;
            }
        }
        totals.add(node.ingredient.copy().setAmount(node.amount));
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

    /** The whole panel, for EMI to lay its sidebars out around. */
    public Bounds panelBounds() {
        return new Bounds(leftPos, topPos, imageWidth, imageHeight);
    }

    /** Where EMI may drop a stack: the root slot. */
    public Bounds rootBounds() {
        return new Bounds(rootX(), rootY(), 18, 18);
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

    private @Nullable RecipeTreeNode nodeAt(double mx, double my) {
        if (tree == null || !inArea(mx, my)) return null;
        return tree.find(toTreeX(mx), toTreeY(my));
    }

    /** Stack under the mouse for EMI's own keys (recipes, uses, favourites). */
    public @Nullable EmiIngredient ingredientAt(double mx, double my) {
        if (root != null && onRoot(mx, my)) return root;
        RecipeTreeNode node = nodeAt(mx, my);
        if (node != null) return node.ingredient;
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
            priceBox.visible = rootKey() != null;
            // The server's current minimal price as the field's hint, read every frame, so a save shows up
            // as soon as the rebuilt table reaches the client.
            Component price = rootPrice();
            priceBox.setHint((price != null ? price : Component.translatable("gui.roll_mod.recipe_tree.price"))
                    .copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        super.render(g, mouseX, mouseY, partialTick);

        if (root != null) {
            root.render(g, rootX() + 1, rootY() + 1, partialTick, EmiIngredient.RENDER_ICON | EmiIngredient.RENDER_INGREDIENT);
            drawAmount(g, root, root.getAmount(), rootX(), rootY());
        }

        if (tree != null) {
            g.enableScissor(areaLeft() + 1, areaTop() + 1, areaRight() - 1, areaBottom() - 1);
            g.pose().pushPose();
            g.pose().translate(areaLeft() + panX, areaTop() + panY, 0);
            g.pose().scale(zoom, zoom, 1);
            drawNode(g, tree, partialTick);
            g.pose().popPose();
            g.disableScissor();
        } else {
            Component hint = Component.translatable("gui.roll_mod.recipe_tree.empty");
            g.drawString(font, hint, (areaLeft() + areaRight() - font.width(hint)) / 2,
                    (areaTop() + areaBottom()) / 2 - 4, 0xFFE0E0E0, true);
        }

        renderFooter(g, partialTick);
        renderHover(g, mouseX, mouseY);
    }

    private void drawNode(GuiGraphics g, RecipeTreeNode node, float partialTick) {
        int border = node.cycle ? CYCLE : node.recipe == null && node.craftable ? CRAFTABLE : 0;
        if (border != 0) g.fill(node.x - 1, node.y - 1, node.x + 19, node.y + 19, border);
        recess(g, node.x, node.y, node.x + 18, node.y + 18, SLOT);
        node.ingredient.render(g, node.x + 1, node.y + 1, partialTick, EmiIngredient.RENDER_ICON | EmiIngredient.RENDER_INGREDIENT);
        drawAmount(g, node.ingredient, node.amount, node.x, node.y);

        if (node.recipe == null) return;
        int cx = node.x + 9;
        g.pose().pushPose();
        g.pose().translate(cx - 5, node.y + 20, 0);
        g.pose().scale(0.625F, 0.625F, 1);
        node.recipe.getCategory().render(g, 0, 0, partialTick);
        g.pose().popPose();

        if (node.children.isEmpty()) return;
        int barY = node.y + 35;
        RecipeTreeNode first = node.children.get(0);
        RecipeTreeNode last = node.children.get(node.children.size() - 1);
        g.fill(cx, node.y + 31, cx + 1, barY, LINE);
        g.fill(Math.min(first.x + 9, cx), barY, Math.max(last.x + 9, cx) + 1, barY + 1, LINE);
        for (RecipeTreeNode child : node.children) {
            g.fill(child.x + 9, barY, child.x + 10, child.y, LINE);
            drawNode(g, child, partialTick);
        }
    }

    private void renderFooter(GuiGraphics g, float partialTick) {
        int y = totalsY();
        if (status != null && Util.getMillis() < statusUntil) {
            g.drawString(font, status, leftPos + MARGIN, y + 5, 0xFFA02020, false);
            return;
        }
        if (totals.isEmpty() || tree == null || tree.recipe == null) return;
        g.drawString(font, Component.translatable("gui.roll_mod.recipe_tree.totals"), leftPos + MARGIN, y + 5, TITLE, false);
        int shown = visibleTotals();
        for (int i = 0; i < shown; i++) {
            int x = totalsX() + i * 18;
            recess(g, x, y, x + 18, y + 18, SLOT);
            EmiIngredient total = totals.get(i);
            total.render(g, x + 1, y + 1, partialTick, EmiIngredient.RENDER_ICON | EmiIngredient.RENDER_INGREDIENT);
            drawAmount(g, total, total.getAmount(), x, y);
        }
    }

    private void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (onRoot(mouseX, mouseY)) {
            if (root == null) {
                lines.add(Component.translatable("gui.roll_mod.recipe_tree.empty"));
            } else {
                lines.addAll(describe(root));
                lines.add(amountLine(root, root.getAmount()));
                lines.add(hint("gui.roll_mod.recipe_tree.hint.pick"));
                lines.add(hint("gui.roll_mod.recipe_tree.hint.amount"));
                lines.add(hint("gui.roll_mod.recipe_tree.hint.root_clear"));
            }
        } else {
            RecipeTreeNode node = nodeAt(mouseX, mouseY);
            if (node != null) {
                lines.addAll(describe(node.ingredient));
                lines.add(amountLine(node.ingredient, node.amount));
                if (node.recipe != null) {
                    lines.add(Component.translatable("gui.roll_mod.recipe_tree.recipe", node.recipe.getCategory().getName())
                            .withStyle(ChatFormatting.BLUE));
                }
                if (node.cycle) {
                    lines.add(Component.translatable("gui.roll_mod.recipe_tree.cycle").withStyle(ChatFormatting.RED));
                } else if (node.craftable) {
                    lines.add(hint(node.recipe == null ? "gui.roll_mod.recipe_tree.hint.pick" : "gui.roll_mod.recipe_tree.hint.change"));
                    lines.add(hint("gui.roll_mod.recipe_tree.hint.auto"));
                }
                if (node.recipe != null) lines.add(hint("gui.roll_mod.recipe_tree.hint.clear"));
            } else {
                int index = totalAt(mouseX, mouseY);
                if (index >= 0 && tree != null && tree.recipe != null) {
                    EmiIngredient total = totals.get(index);
                    lines.addAll(describe(total));
                    lines.add(amountLine(total, total.getAmount()));
                    lines.add(hint("gui.roll_mod.recipe_tree.hint.pick"));
                }
            }
        }
        if (!lines.isEmpty()) g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static List<Component> describe(EmiIngredient ingredient) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks.size() == 1) return new ArrayList<>(stacks.get(0).getTooltipText());
        return List.of(Component.translatable("gui.roll_mod.recipe_tree.any_of", stacks.get(0).getName()));
    }

    private static Component amountLine(EmiIngredient ingredient, long amount) {
        String value = isFluid(ingredient) ? amount + " mB" : String.valueOf(amount);
        return Component.translatable("gui.roll_mod.recipe_tree.amount", value).withStyle(ChatFormatting.GRAY);
    }

    private static Component hint(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY);
    }

    private void drawAmount(GuiGraphics g, EmiIngredient ingredient, long amount, int x, int y) {
        boolean fluid = isFluid(ingredient);
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

    private static boolean isFluid(EmiIngredient ingredient) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        return !stacks.isEmpty() && stacks.get(0).getKey() instanceof Fluid;
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
    // The same mouse keys as EMI's own tree: click picks a recipe in EMI's recipe screen, Shift+click
    // lets EMI pick one, right-click removes it. Mouse handling never reaches AbstractContainerScreen:
    // with no slots, its click logic would only send "clicked outside" packets for a menu the server
    // doesn't know.

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
            RecipeTreeNode node = nodeAt(mouseX, mouseY);
            if (node != null) {
                if (button == 0 && !node.cycle) {
                    if (hasShiftDown()) autoPick(node.ingredient);
                    else pick(node.ingredient);
                } else if (button == 1 && node.recipe != null) {
                    unpick(node.ingredient);
                }
            } else if (button == 0) {
                panning = true;
            }
            return true;
        }
        int index = totalAt(mouseX, mouseY);
        if (index >= 0 && button == 0) {
            EmiIngredient total = totals.get(index);
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
            long step = (isFluid(root) ? 1000 : 1) * (hasShiftDown() ? 10 : 1);
            root = root.copy().setAmount(Math.max(1, root.getAmount() + (scrollY > 0 ? step : -step)));
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

    /** Client-only and slotless; it exists because EMI only works over container screens. */
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
