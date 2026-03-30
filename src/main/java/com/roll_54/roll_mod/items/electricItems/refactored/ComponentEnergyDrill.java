package com.roll_54.roll_mod.items.electricItems.refactored;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.data.ToggleableItem;
import com.roll_54.roll_mod.data.UpgradeComponent;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.roll_54.roll_mod.registry.ComponentsRegistry.UPGRADES;

public class ComponentEnergyDrill  extends Item implements ISimpleEnergyItem, ToggleableItem {
    private final int tier;

    public ComponentEnergyDrill(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public double getEnergyConsumptionMultiplier() {
        double multiplier = 1.0;
        int totalCost = 0;
        for (UpgradeComponent upgrade : getUpgrades(new ItemStack(this))) {
            for (UpgradeType type : UpgradeType.values()) {
                if (upgrade.id().equals(type.id)) {
                    if (type == UpgradeType.ENERGY_EFFICIENCY) {
                        multiplier *= 0.9;
                    } else {
                        totalCost += type.energyCost;
                    }
                }
            }
        }
        return multiplier * (1 + totalCost / 100.0);
    }

    public int getWidth() {
        int width = 1;
        for (UpgradeComponent upgrade : getUpgrades(new ItemStack(this))) {
            if (upgrade.id().equals(UpgradeType.WIDTH.id)) {
                width++;
            }
        }
        return width;
    }

    public int getHeight() {
        int height = 1;
        for (UpgradeComponent upgrade : getUpgrades(new ItemStack(this))) {
            if (upgrade.id().equals(UpgradeType.HEIGHT.id)) {
                height++;
            }
        }
        return height;
    }

    public int getDepth() {
        int depth = 1;
        for (UpgradeComponent upgrade : getUpgrades(new ItemStack(this))) {
            if (upgrade.id().equals(UpgradeType.DEPTH.id)) {
                depth++;
            }
        }
        return depth;
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    public int getMaxUpgrades() {
        return tier * 3;
    }

    public List<UpgradeComponent> getUpgrades(ItemStack stack) {
        return stack.getOrDefault(UPGRADES, List.of());
    }

    public static void addUpgrade(ItemStack stack, UpgradeType type) {
        List<UpgradeComponent> upgrades =
                new ArrayList<>(stack.getOrDefault(UPGRADES.get(), List.of()));

        int tier = ((ComponentEnergyDrill) stack.getItem()).getMaxUpgrades();

        if (upgrades.size() >= tier) return;

        upgrades.add(new UpgradeComponent(type.id));

        stack.set(UPGRADES.get(), upgrades);
    }

    /**
     * @param stack Current stack.
     * @return The max energy that can be stored in this item stack (ignoring current stack size).
     */
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return 100000L * tier;
    }

    /**
     * @param stack Current stack.
     * @return The max amount of energy that can be inserted in this item stack (ignoring current stack size) in a single operation.
     */
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return 60000;
    }

    /**
     * @param stack Current stack.
     * @return The max amount of energy that can be extracted from this item stack (ignoring current stack size) in a single operation.
     */
    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }
}
