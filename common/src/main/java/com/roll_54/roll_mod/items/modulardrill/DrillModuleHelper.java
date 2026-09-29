package com.roll_54.roll_mod.items.modulardrill;

import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.ResourceLocation;

/**
 * Every rule and number that reads a drill's installed modules, in one place: the installation
 * table asks it whether a module may go in, the drill asks it what mining now costs, and both
 * trust nothing else.
 */
public final class DrillModuleHelper {

    /**
     * The throttle ceiling with Overcharge installed: none. The surcharge is the only limiter —
     * every point above 100 adds 10% of the base cost per block, so the economics price out an
     * absurd throttle long before any number here could.
     */
    public static final int OVERCHARGE_THROTTLE_LIMIT = Integer.MAX_VALUE;

    /** Energy usage grows by this fraction of the base cost per throttle point above 100. */
    private static final double OVERCHARGE_SURCHARGE = 0.10;

    private static final ResourceLocation REACH_MODIFIER_ID = RollMod.id("drill_reach");

    private DrillModuleHelper() {}

    /* ------------------------------------------ component ------------------------------------------- */

    public static DrillModules get(ItemStack drill) {
        return drill.getOrDefault(ComponentsRegistry.DRILL_MODULES.get(), DrillModules.EMPTY);
    }

    public static void set(ItemStack drill, DrillModules modules) {
        drill.set(ComponentsRegistry.DRILL_MODULES.get(), modules);
    }

    /* ----------------------------------------- installation ----------------------------------------- */

    /**
     * Whether this module may be installed right now.
     *
     * <p>The rules: a free slot; at most one module of each type; Silk Touch, Fortune and
     * Auto-Smelt shape the drops in ways that fight each other, so Silk Touch stands alone while
     * Fortune+Smelt (multiply, then smelt) is the one legal pair; Burn voids the drops, so
     * nothing that processes them may ride along. No voltage gating — module availability is
     * balanced by the crafting recipes.
     */
    public static boolean canInstall(ItemStack drill, DrillModuleItem module) {
        if (!(drill.getItem() instanceof ModularTool tool)) return false;
        // Drill modules stay out of sabers and saber modules out of drills; Battery, Eco and the
        // XP Reactor fit both.
        if (!module.type.fits(tool.toolKind())) return false;

        DrillModules modules = get(drill);
        if (modules.modules().size() >= tool.moduleSlots()) return false;
        if (modules.has(module.type)) return false;
        // The complexity budget, which binds even with slots free. A module that gives complexity
        // back (Speak) always passes.
        if (module.complexity > 0 && complexity(drill) + module.complexity > tool.maxComplexity()) return false;

        for (ModuleType conflict : conflictsOf(module.type)) {
            if (modules.has(conflict)) return false;
        }
        return true;
    }

    /**
     * The module types that may never share a tool with this one. The pairs are symmetric, so
     * install order does not matter, and the module tooltip lists exactly these.
     *
     * <p>Silk Touch, Fortune and Auto-Smelt shape the drops in ways that fight each other, so Silk
     * Touch stands alone while Fortune+Smelt (multiply, then smelt) is the one legal pair; Burn
     * voids the drops, so nothing that processes them may ride along. The deprecated Looting items
     * still install into sabers, but never beside the merged Fortune/Looting module — the two
     * levels would stack.
     */
    public static Set<ModuleType> conflictsOf(ModuleType type) {
        return switch (type) {
            case SILK_TOUCH -> EnumSet.of(ModuleType.FORTUNE, ModuleType.SMELT, ModuleType.BURN);
            case FORTUNE -> EnumSet.of(ModuleType.SILK_TOUCH, ModuleType.BURN, ModuleType.LOOTING);
            case LOOTING -> EnumSet.of(ModuleType.FORTUNE);
            // Same story for the deprecated Attack Speed items beside the merged Speed module.
            case SPEED -> EnumSet.of(ModuleType.ATTACK_SPEED);
            case ATTACK_SPEED -> EnumSet.of(ModuleType.SPEED);
            case SMELT -> EnumSet.of(ModuleType.SILK_TOUCH, ModuleType.BURN);
            case TRASH_FILTER -> EnumSet.of(ModuleType.BURN);
            case BURN -> EnumSet.of(ModuleType.SILK_TOUCH, ModuleType.FORTUNE, ModuleType.SMELT,
                    ModuleType.TRASH_FILTER);
            default -> EnumSet.noneOf(ModuleType.class);
        };
    }

    /** The complexity the installed modules add up to; Speak's negative share included. */
    public static int complexity(ItemStack tool) {
        int total = 0;
        for (DrillModuleItem module : get(tool).moduleItems()) {
            total += module.complexity;
        }
        return total;
    }

    /**
     * Whether the module at {@code index} may come out. Only one that gives complexity back (Speak)
     * can be refused: taking it out must not leave the tool over its budget, so the player removes
     * something else first.
     */
    public static boolean canRemove(ItemStack tool, int index) {
        DrillModules modules = get(tool);
        if (index < 0 || index >= modules.modules().size()) return false;
        if (!(tool.getItem() instanceof ModularTool modular)) return true;
        // By id, not moduleItems(): that list skips unresolved ids, so its indices can drift.
        if (!(BuiltInRegistries.ITEM.get(modules.modules().get(index)) instanceof DrillModuleItem module)) return true;
        if (module.complexity >= 0) return true;
        return complexity(tool) - module.complexity <= modular.maxComplexity();
    }

    /** Installs the module. The caller has already checked {@link #canInstall}. */
    public static void install(ItemStack drill, DrillModuleItem module) {
        set(drill, get(drill).withModuleAdded(BuiltInRegistries.ITEM.getKey(module)));
        rebuildAttributes(drill);
    }

    /**
     * {@link #install(ItemStack, DrillModuleItem)} from the module's stack, so what the module
     * carries comes along: the Trash Filter's list moves onto the drill.
     */
    public static void install(ItemStack drill, ItemStack moduleStack) {
        if (!(moduleStack.getItem() instanceof DrillModuleItem module)) return;
        install(drill, module);
        if (module.type == ModuleType.TRASH_FILTER) {
            ItemContainerContents filter = moduleStack.get(ComponentsRegistry.TRASH_FILTER.get());
            if (filter != null) {
                drill.set(ComponentsRegistry.TRASH_FILTER.get(), filter);
            } else {
                drill.remove(ComponentsRegistry.TRASH_FILTER.get());
            }
        }
    }

    /**
     * The module installed at {@code index} as an item, carrying what it stores while in the
     * drill (the Trash Filter's list) — what the table's slot shows and what removal hands back.
     */
    public static ItemStack moduleStack(ItemStack drill, int index) {
        DrillModules modules = get(drill);
        if (index < 0 || index >= modules.modules().size()) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(modules.modules().get(index)));
        if (stack.getItem() instanceof DrillModuleItem module && module.type == ModuleType.TRASH_FILTER) {
            ItemContainerContents filter = drill.get(ComponentsRegistry.TRASH_FILTER.get());
            if (filter != null && !filter.equals(ItemContainerContents.EMPTY)) {
                stack.set(ComponentsRegistry.TRASH_FILTER.get(), filter);
            }
        }
        return stack;
    }

    /**
     * Removes the module at {@code index} and hands back the item it was.
     *
     * <p>Removing a module's headroom clamps what leaned on it: a drill that loses Overcharge at
     * 400% must not keep mining at 400%, and one that loses its AOE module shrinks its volume.
     */
    public static ItemStack removeModule(ItemStack drill, int index) {
        DrillModules modules = get(drill);
        if (index < 0 || index >= modules.modules().size()) return ItemStack.EMPTY;

        ItemStack removed = moduleStack(drill, index);
        if (removed.getItem() instanceof DrillModuleItem module && module.type == ModuleType.TRASH_FILTER) {
            drill.remove(ComponentsRegistry.TRASH_FILTER.get());
        }
        DrillModules after = modules.withModuleRemoved(index);
        if (drill.getItem() instanceof ModularDrillItem) {
            after = after.withThrottle(Math.min(after.throttle(), maxThrottleOf(after)));
            set(drill, after);
            Volume clamped = volume(drill);
            set(drill, get(drill).withVolume(clamped.x(), clamped.y(), clamped.z()));
        } else {
            set(drill, after);
        }
        rebuildAttributes(drill);

        return removed;
    }

    /* ------------------------------------------ aggregates ------------------------------------------ */

    /** The speed multiplier from a Speed module, 1.0 without one. */
    public static double speedFactor(ItemStack drill) {
        DrillModuleItem speed = get(drill).ofType(ModuleType.SPEED);
        return speed != null ? speed.value : 1.0;
    }

    /** The capacity multiplier from a Battery module, 1.0 without one. */
    public static double capacityFactor(ItemStack drill) {
        DrillModuleItem battery = get(drill).ofType(ModuleType.BATTERY);
        return battery != null ? battery.value : 1.0;
    }

    /** The mining box the player dialed in, clamped per axis to what the AOE module allows. */
    public record Volume(int x, int y, int z) {
        public int blocks() {
            return x * y * z;
        }

        public boolean single() {
            return x == 1 && y == 1 && z == 1;
        }
    }

    /** The longest edge the installed AOE module permits per axis; 1 without a module. */
    public static int maxEdge(ItemStack drill) {
        DrillModuleItem aoe = get(drill).ofType(ModuleType.AOE);
        return aoe != null ? (int) aoe.value * 2 + 1 : 1;
    }

    /**
     * The effective mining volume: the stored choice, normalized through {@link #clampAxis} on
     * every read so a pulled module shrinks the box even if the stored numbers were never
     * rewritten, and an even number from anywhere becomes the odd one below it.
     */
    public static Volume volume(ItemStack drill) {
        DrillModules modules = get(drill);
        int max = maxEdge(drill);
        return new Volume(
                clampAxis(modules.volX(), max),
                clampAxis(modules.volY(), max),
                clampAxis(modules.volZ(), max));
    }

    /**
     * One axis of a volume, made legal: clamped to {@code [1, max]} and odd. Only odd sizes
     * exist — an even box has no middle block to center on — so 1, 3, 5… up to the module's
     * edge, which is itself always odd.
     */
    public static int clampAxis(int value, int max) {
        int clamped = Mth.clamp(value, 1, max);
        return (clamped & 1) == 1 ? clamped : clamped - 1;
    }

    public static int maxThrottle(ItemStack drill) {
        return maxThrottleOf(get(drill));
    }

    private static int maxThrottleOf(DrillModules modules) {
        return modules.has(ModuleType.OVERCHARGE) ? OVERCHARGE_THROTTLE_LIMIT : 100;
    }

    public static boolean hasXpReactor(ItemStack drill) {
        return get(drill).has(ModuleType.XP_REACTOR);
    }

    /** Slots in the Trash Filter's list. */
    public static final int TRASH_FILTER_SLOTS = 18;

    /**
     * The Trash Filter's list as the item types it voids; empty without the module or with an
     * empty list. Matched by item alone, components ignored.
     */
    public static Set<Item> trashFilter(ItemStack drill) {
        if (!get(drill).has(ModuleType.TRASH_FILTER)) return Set.of();
        return filterItems(drill.get(ComponentsRegistry.TRASH_FILTER.get()));
    }

    /** The distinct item types in a filter list; empty for {@code null}. */
    public static Set<Item> filterItems(@Nullable ItemContainerContents filter) {
        if (filter == null) return Set.of();
        Set<Item> items = new HashSet<>();
        filter.nonEmptyItems().forEach(stack -> items.add(stack.getItem()));
        return items;
    }

    public static boolean burnsDrops(ItemStack drill) {
        return get(drill).has(ModuleType.BURN);
    }

    /**
     * What one block costs to break with the current configuration:
     * {@code base × Π(module cost factors) × (1 + 0.10 × max(0, throttle − 100))}.
     *
     * <p>Throttling below 100% does not discount the cost — slower, same energy per block.
     */
    public static long costPerBlock(ItemStack drill, DrillVoltage voltage) {
        DrillModules modules = get(drill);

        double cost = voltage.baseCostPerBlock;
        for (DrillModuleItem module : modules.moduleItems()) {
            cost *= module.costFactor(ToolKind.DRILL);
        }

        int over = Math.max(0, modules.throttle() - 100);
        cost *= 1.0 + OVERCHARGE_SURCHARGE * over;

        return (long) Math.ceil(cost);
    }

    /** The multiplier of the module of this type, 1.0 without one — for saber stat modules. */
    public static double factor(ItemStack tool, ModuleType type) {
        DrillModuleItem module = get(tool).ofType(type);
        return module != null ? module.value : 1.0;
    }

    /** The {@code value} of the module of this type, 0 without one. */
    public static double value(ItemStack tool, ModuleType type) {
        DrillModuleItem module = get(tool).ofType(type);
        return module != null ? module.value : 0.0;
    }

    /** {@link #value} as a saber reads it ({@link DrillModuleItem#saberValue}); 0 without the module. */
    public static double saberValue(ItemStack tool, ModuleType type) {
        DrillModuleItem module = get(tool).ofType(type);
        return module != null ? module.saberValue : 0.0;
    }

    /**
     * The XP Reactor's bookkeeping, for any modular tool: {@code eu} fills the tool first (a direct
     * component write — its own input is locked), and what does not fit is offered to every energy
     * item in the inventory. Whatever nothing wants is lost, which the tooltips say.
     */
    public static void absorbEnergy(ItemStack stack, ISimpleEnergyItem item, ServerPlayer player, long eu) {
        long stored = item.getStoredEnergy(stack);
        long space = item.getEnergyCapacity(stack) - stored;
        long intoTool = Math.min(space, eu);
        if (intoTool > 0) {
            item.setStoredEnergy(stack, stored + intoTool);
        }

        long surplus = eu - intoTool;
        if (surplus <= 0) return;

        for (ItemStack other : player.getInventory().items) {
            if (other == stack || other.isEmpty()) continue;
            ILongEnergyStorage storage = other.getCapability(ILongEnergyStorage.ITEM);
            if (storage == null) continue;
            surplus -= storage.receive(surplus, false);
            if (surplus <= 0) return;
        }
    }

    /* -------------------------------------------- speak --------------------------------------------- */

    /** The Speak module talks once per this many blocks mined. */
    public static final int SPEAK_INTERVAL = 10_000;

    /** Players within this many blocks of the miner, in the same dimension, hear the drill. */
    private static final double SPEAK_RADIUS = 32.0;

    /**
     * How many {@code message.roll_mod.speak.<n>} lang keys exist, numbered from 1. To add a line,
     * add the next key to en_us/uk_ua and bump this.
     */
    private static final int SPEAK_MESSAGE_COUNT = 3;

    /**
     * Adds {@code broken} to the drill's Speak counter and, when the count crosses a multiple of
     * {@link #SPEAK_INTERVAL}, says one random line to the players nearby. One swing of a big AOE
     * box crossing the mark still speaks only once.
     */
    public static void countSpeakBlocks(ItemStack drill, ServerPlayer player, int broken) {
        if (broken <= 0 || !get(drill).has(ModuleType.SPEAK)) return;

        int before = drill.getOrDefault(ComponentsRegistry.SPEAK_BLOCKS.get(), 0);
        int after = before + broken;
        drill.set(ComponentsRegistry.SPEAK_BLOCKS.get(), after);
        if (before / SPEAK_INTERVAL == after / SPEAK_INTERVAL) return;

        int line = player.getRandom().nextInt(SPEAK_MESSAGE_COUNT) + 1;
        Component message = Component.translatable("message.roll_mod.speak.format",
                drill.getHoverName(), Component.translatable("message.roll_mod.speak." + line));

        double radiusSq = SPEAK_RADIUS * SPEAK_RADIUS;
        for (ServerPlayer listener : player.serverLevel().players()) {
            if (listener.distanceToSqr(player) <= radiusSq) {
                listener.sendSystemMessage(message);
            }
        }
    }

    /* ------------------------------------------ attributes ------------------------------------------ */

    /**
     * Rewrites the stack's attribute modifiers to match the installed modules. Only the Reach
     * module lives here; the drill has no default attack attributes to preserve, so replacing the
     * whole component loses nothing.
     */
    public static void rebuildAttributes(ItemStack drill) {
        // A saber's damage and speed come from its getDefaultAttributeModifiers, which a component
        // written here would override outright.
        if (!(drill.getItem() instanceof ModularDrillItem)) return;
        DrillModuleItem reach = get(drill).ofType(ModuleType.REACH);
        if (reach == null) {
            drill.remove(DataComponents.ATTRIBUTE_MODIFIERS);
            return;
        }

        drill.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.BLOCK_INTERACTION_RANGE,
                        new AttributeModifier(REACH_MODIFIER_ID, reach.value,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build());
    }
}
