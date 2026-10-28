package com.roll_54.roll_mod.items.modulardrill;

/**
 * The module complexity rules for drills and sabers alike: linear with a per-module tax. A module
 * costs {@link #TAX} + tier and a tool tier allows {@link #BUDGET_BASE} + {@link #BUDGET_STEP} ×
 * tier, so each extra module pays the tax and a tool tier buys room for roughly one more module
 * than the last — a tool cannot max everything, the player picks a focus.
 */
public final class ModuleComplexity {

    /** What every module pays on top of its tier. */
    public static final int TAX = 2;
    /** The budget before the per-tier step. */
    public static final int BUDGET_BASE = 4;
    /** What each tool tier adds to the budget. */
    public static final int BUDGET_STEP = 4;

    private ModuleComplexity() {}

    /** A module's cost when it names none of its own. */
    public static int cost(int tier) {
        return TAX + tier;
    }

    /** A tool tier's budget, {@code tier} counted from 1 (LV) to 5 (IV). */
    public static int budget(int tier) {
        return BUDGET_BASE + BUDGET_STEP * tier;
    }
}
