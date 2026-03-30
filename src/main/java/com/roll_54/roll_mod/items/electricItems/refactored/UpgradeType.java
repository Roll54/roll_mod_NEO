package com.roll_54.roll_mod.items.electricItems.refactored;

public enum UpgradeType {
    ENERGY_EFFICIENCY("energy_efficiency", 0.5, true),
    WIDTH("width", 10, false),
    HEIGHT("height", 10, false),
    DEPTH("depth", 15, false);

    public final String id;
    public final double energyCost;
    public final boolean isMultiplier;

    UpgradeType(String id, double energyCost, boolean isMultiplier) {
        this.id = id;
        this.energyCost = energyCost;
        this.isMultiplier = isMultiplier;
    }
}