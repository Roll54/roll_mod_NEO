package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Fruits Delight cooking, fed by the COOK hook: one task whose target dish is drawn from
 * {@link #DISHES} anew each period. The pick hashes {@link DailyTaskManager#currentPeriodDay()},
 * so it is the same for every group all day and re-rolls with the 06:00 board turnover; being
 * day-derived (a task is a stateless singleton and {@code matches} gets no group context), the
 * client renders the dish from its own clock, so only a machine whose clock straddles 06:00
 * differently than the server's briefly shows the wrong dish.
 *
 * <p>Like {@link CookDishTask}, only ids are used — never a Fruits Delight class — so this loads
 * safely without the mod, and {@link #requiredMod()} keeps it out of the registry when absent.
 */
@AutoDailyTask
public final class RandomFruitDishTask implements DailyTask {

    private static final String FRUITS_DELIGHT = "fruitsdelight";

    private record Dish(ResourceLocation item, int amount) {
        Dish(String item, int amount) {
            this(ResourceLocation.fromNamespaceAndPath(FRUITS_DELIGHT, item), amount);
        }
    }

    private static final Dish[] DISHES = {
            new Dish("lychee_chicken", 16),
            new Dish("pineapple_marinated_pork", 16),
            new Dish("orange_marinated_pork", 16),
            new Dish("fig_chicken_stew", 16),
            new Dish("orange_chicken", 16),
            new Dish("pear_with_rock_sugar", 16),
            new Dish("mangosteen_cake", 16),
            new Dish("pineapple_fried_rice", 64),
    };

    /**
     * Only the dishes whose id actually resolves. An unknown id (typo, removed in a mod update)
     * would otherwise leave an AIR icon and a {@code matches} that can never fire — an
     * uncompletable slot on someone's board. Unknowns are logged and dropped here instead, and if
     * none survive, {@link #baseAmount()} returns 0 so the registry rejects the whole task at
     * bootstrap.
     */
    private final Dish[] dishes;
    private final DailyTaskIcon[] icons;

    public RandomFruitDishTask() {
        // Tasks are built at common setup, after registries are frozen, so the lookups resolve.
        List<Dish> valid = new ArrayList<>(DISHES.length);
        for (Dish dish : DISHES) {
            if (BuiltInRegistries.ITEM.containsKey(dish.item())) {
                valid.add(dish);
            } else if (ModList.get().isLoaded(FRUITS_DELIGHT)) {
                // Without the mod every id is unknown and the registry skips the task anyway;
                // only complain when the mod is present and an id still does not resolve.
                RollMod.LOGGER.error("[DailyTasks] random_fruit_dish: unknown item '{}'; dropping it from the rotation.",
                        dish.item());
            }
        }
        dishes = valid.toArray(new Dish[0]);
        icons = new DailyTaskIcon[dishes.length];
        for (int i = 0; i < dishes.length; i++) {
            icons[i] = DailyTaskIcon.of(new ItemStack(BuiltInRegistries.ITEM.get(dishes[i].item())));
        }
    }

    /** Today's dish. The day is hashed so the list is not just walked in order. */
    private int dishIndex() {
        long mixed = DailyTaskManager.currentPeriodDay() * 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 32;
        return Math.floorMod((int) mixed, dishes.length);
    }

    @Override
    public String id() {
        return "random_fruit_dish";
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.COOK;
    }

    @Override
    public int baseAmount() {
        // No resolvable dishes: 0 makes DailyTaskRegistry reject the task at bootstrap.
        return dishes.length == 0 ? 0 : dishes[dishIndex()].amount();
    }

    @Override
    public DailyTaskIcon icon() {
        return icons[dishIndex()];
    }

    @Override
    public String requiredMod() {
        return FRUITS_DELIGHT;
    }

    @Override
    public Component tooltip(int required) {
        return Component.translatable(descKey(), required,
                icons[dishIndex()].toastStack().getHoverName());
    }

    @Override
    public boolean matches(Object subject) {
        return dishes.length > 0
                && subject instanceof ItemStack stack
                && !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(dishes[dishIndex()].item());
    }
}
