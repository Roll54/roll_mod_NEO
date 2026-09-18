package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Farmer's Delight cooking, fed by the COOK hook. Each nested class below is one task, either a
 * whole tag of dishes or a single one.
 *
 * <p>Only ids and tag keys are used here — never an FD class — so these load safely without the
 * mod, and {@link #requiredMod()} keeps them out of the registry when it is absent.
 */
public abstract class CookDishTask implements DailyTask {

    private static final String FD = "farmersdelight";

    private final String id;
    private final int amount;
    private final Predicate<ItemStack> filter;
    private final DailyTaskIcon icon;

    protected CookDishTask(String id, int amount, Predicate<ItemStack> filter, String iconItem) {
        this.id = id;
        this.amount = amount;
        this.filter = filter;
        // Tasks are built at common setup, after registries are frozen, so the lookup resolves.
        this.icon = DailyTaskIcon.of(new ItemStack(BuiltInRegistries.ITEM.get(fd(iconItem))));
    }

    /** Every dish in {@code farmersdelight:<tag>}. */
    protected CookDishTask(String id, int amount, String tag, String iconItem) {
        this(id, amount, stack -> stack.is(TagKey.create(Registries.ITEM, fd(tag))), iconItem);
    }

    /** Exactly {@code farmersdelight:<item>}, which is also the icon. */
    protected CookDishTask(String id, int amount, String item) {
        this(id, amount, stack -> is(stack, fd(item)), item);
    }

    private static ResourceLocation fd(String path) {
        return ResourceLocation.fromNamespaceAndPath(FD, path);
    }

    private static boolean is(ItemStack stack, ResourceLocation itemId) {
        Item item = stack.getItem();
        return BuiltInRegistries.ITEM.getKey(item).equals(itemId);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.COOK;
    }

    @Override
    public int baseAmount() {
        return amount;
    }

    @Override
    public DailyTaskIcon icon() {
        return icon;
    }

    @Override
    public String requiredMod() {
        return FD;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof ItemStack stack && !stack.isEmpty() && filter.test(stack);
    }

    @AutoDailyTask
    public static final class Meals extends CookDishTask {
        public Meals() {
            super("cook_meals", 10, "meals", "beef_stew");
        }
    }

    @AutoDailyTask
    public static final class Drinks extends CookDishTask {
        public Drinks() {
            super("cook_drinks", 4, "drinks", "hot_cocoa");
        }
    }

    @AutoDailyTask
    public static final class BeefStew extends CookDishTask {
        public BeefStew() {
            super("cook_beef_stew", 6, "beef_stew");
        }
    }

    @AutoDailyTask
    public static final class FriedRice extends CookDishTask {
        public FriedRice() {
            super("cook_fried_rice", 6, "fried_rice");
        }
    }

    @AutoDailyTask
    public static final class PastaWithMeatballs extends CookDishTask {
        public PastaWithMeatballs() {
            super("cook_pasta_with_meatballs", 4, "pasta_with_meatballs");
        }
    }
}
