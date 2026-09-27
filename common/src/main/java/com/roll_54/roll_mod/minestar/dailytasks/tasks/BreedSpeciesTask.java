package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Breeding one kind of farm animal; the subject is one of the parents. Not a task itself — each
 * nested class below is one, because the registry instantiates tasks through a no-arg constructor.
 */
public abstract class BreedSpeciesTask implements DailyTask {

    private final String id;
    private final EntityType<?> type;
    private final int amount;
    private final DailyTaskIcon icon;

    protected BreedSpeciesTask(String id, EntityType<?> type, int amount, ItemLike icon) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.icon = DailyTaskIcon.of(icon);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public DailyTaskHook hook() {
        return DailyTaskHook.BREED;
    }

    @Override
    public int baseAmount() {
        return amount;
    }

    @Override
    public double teamMultiplier() {
        return 1.5;
    }

    @Override
    public DailyTaskIcon icon() {
        return icon;
    }

    @Override
    public boolean matches(Object subject) {
        return subject instanceof LivingEntity entity && entity.getType() == type;
    }

    @AutoDailyTask
    public static final class Cows extends BreedSpeciesTask {
        public Cows() {
            super("breed_cows", EntityType.COW, 20, Items.WHEAT);
        }
    }

    @AutoDailyTask
    public static final class Sheep extends BreedSpeciesTask {
        public Sheep() {
            super("breed_sheep", EntityType.SHEEP, 15, Items.WHITE_WOOL);
        }
    }

    @AutoDailyTask
    public static final class Pigs extends BreedSpeciesTask {
        public Pigs() {
            super("breed_pigs", EntityType.PIG, 20, Items.CARROT);
        }
    }

    @AutoDailyTask
    public static final class Chickens extends BreedSpeciesTask {
        public Chickens() {
            super("breed_chickens", EntityType.CHICKEN, 20, Items.WHEAT_SEEDS);
        }
    }

    @AutoDailyTask
    public static final class Rabbits extends BreedSpeciesTask {
        public Rabbits() {
            super("breed_rabbits", EntityType.RABBIT, 12, Items.DANDELION);
        }
    }

    @AutoDailyTask
    public static final class Goats extends BreedSpeciesTask {
        public Goats() {
            super("breed_goats", EntityType.GOAT, 10, Items.GOAT_HORN);
        }
    }

    @AutoDailyTask
    public static final class Mooshrooms extends BreedSpeciesTask {
        public Mooshrooms() {
            super("breed_mooshrooms", EntityType.MOOSHROOM, 6, Items.RED_MUSHROOM);
        }
    }
}
