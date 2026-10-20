package com.roll_54.roll_mod.compat.createcybernetics;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.surgery.DefaultOrgans;
import com.perigrine3.createcybernetics.item.cyberware.arm.ArmCannonItem;
import com.perigrine3.createcybernetics.item.cyberware.bone.SpinalInjectorItem;
import com.perigrine3.createcybernetics.item.cyberware.brain.CerebralProcessingUnitItem;
import com.perigrine3.createcybernetics.item.cyberware.brain.ChipwareSlotsItem;
import com.perigrine3.createcybernetics.item.cyberware.brain.CyberdeckItem;
import com.perigrine3.createcybernetics.item.cyberware.organs.HeatEngineItem;
import com.perigrine3.createcybernetics.item.cyberware.organs.OregrinderItem;
import com.perigrine3.createcybernetics.util.ModTags;
import com.roll_54.roll_mod.minestar.moderation.invsee.InvSeeEdit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * A player's implants as slots, for the moderator's inventory window: every body-part slot of
 * every {@link CyberwareSlot}, then the inventories some implants carry (chipware, cyberdeck, heat
 * engine, spinal injector, arm cannon).
 *
 * <p>Like {@code DrillModuleSlotHandler}, it holds no items. Every read goes to the player's
 * {@link PlayerCyberwareData}, and every write is an install or a removal done the way the
 * Robosurgeon does it in {@code SurgeryController.performSurgery}. An install has to pass the
 * same checks: the slot accepts the item, the per-slot limit, required and incompatible implants,
 * the Oregrinder's no-wetware rule, and a bound biochip for the CPU. Then the hooks run and
 * humanity is recomputed. A removal ejects what the implant carried, runs its hook, and removes
 * whatever depended on it, the way surgery does. Those dependents go into the player's own
 * inventory, since no surgeon is standing there to hand them to.
 *
 * <p>{@link #setStackInSlot} is a real uninstall and install, not a plain write, for the reason
 * {@code DrillModuleSlotHandler} gives: a shift-click or a hotbar swap copies the stack out and
 * then clears the slot with {@code set(EMPTY)}.
 *
 * <p>The layout is fixed by the mod's own constants, so the client builds the same slot count from
 * {@link #sections()} without knowing anything about the player.
 */
public final class CyberwareSlotHandler implements IItemHandlerModifiable {

    /** The implant inventories, in slot order after the body. */
    public enum Sub {
        CHIPWARE(PlayerCyberwareData.CHIPWARE_SLOT_COUNT, 1),
        CYBERDECK(PlayerCyberwareData.CYBERDECK_SLOT_COUNT, 1),
        HEAT_ENGINE(PlayerCyberwareData.HEAT_ENGINE_SLOT_COUNT, 64),
        SPINAL_INJECTOR(SpinalInjectorItem.SLOT_COUNT, 64),
        ARM_CANNON(ArmCannonItem.SLOT_COUNT, 64);

        public final int size;
        final int limit;

        Sub(int size, int limit) {
            this.size = size;
            this.limit = limit;
        }
    }

    /** One labelled row of the window: a body part or an implant inventory. */
    public record Section(String key, int start, int size) {}

    private static final List<Section> SECTIONS;
    private static final int SLOTS;

    static {
        List<Section> sections = new ArrayList<>();
        int at = 0;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            sections.add(new Section("body." + slot.name().toLowerCase(), at, slot.size));
            at += slot.size;
        }
        for (Sub sub : Sub.values()) {
            sections.add(new Section("sub." + sub.name().toLowerCase(), at, sub.size));
            at += sub.size;
        }
        SECTIONS = List.copyOf(sections);
        SLOTS = at;
    }

    private final ServerPlayer target;
    /** Whether a write may go through right now: the moderator may edit and the player is still here. */
    private final BooleanSupplier writable;
    private final InvSeeEdit edits;

    public CyberwareSlotHandler(ServerPlayer target, BooleanSupplier writable, InvSeeEdit edits) {
        this.target = target;
        this.writable = writable;
        this.edits = edits;
    }

    /** Every row in slot order, for laying the window out on either side. */
    public static List<Section> sections() {
        return SECTIONS;
    }

    public static int slotCount() {
        return SLOTS;
    }

    /** The player's humanity as their HUD shows it. */
    public int humanity() {
        return data().getHumanity(target);
    }

    private PlayerCyberwareData data() {
        return target.getData(ModAttachments.CYBERWARE);
    }

    /* ------------------------------------------- addressing ------------------------------------------- */

    /** A body slot as (part, index), or a sub-inventory slot as (sub, index); exactly one is non-null. */
    private record Address(CyberwareSlot part, Sub sub, int index) {}

    private static Address address(int slot) {
        int at = 0;
        for (CyberwareSlot part : CyberwareSlot.values()) {
            if (slot < at + part.size) return new Address(part, null, slot - at);
            at += part.size;
        }
        for (Sub sub : Sub.values()) {
            if (slot < at + sub.size) return new Address(null, sub, slot - at);
            at += sub.size;
        }
        return null;
    }

    private static String describe(Address a) {
        return (a.part() != null ? a.part().name() : a.sub().name()) + "#" + a.index();
    }

    /* ---------------------------------------------- reads --------------------------------------------- */

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        Address a = address(slot);
        if (a == null) return ItemStack.EMPTY;
        if (a.part() != null) {
            InstalledCyberware installed = data().get(a.part(), a.index());
            ItemStack stack = installed == null ? null : installed.getItem();
            return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        ItemStack stack = subGet(data(), a.sub(), a.index());
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        return limit(slot);
    }

    /** The most one slot holds: one implant per body slot, a shard per chip slot, a stack elsewhere. */
    public static int limit(int slot) {
        Address a = address(slot);
        if (a == null || a.part() != null) return 1;
        return a.sub().limit;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        Address a = address(slot);
        if (a == null || stack.isEmpty()) return false;
        if (a.part() != null) return canInstall(data(), a.part(), a.index(), stack);
        return subAccepts(data(), a.sub(), stack);
    }

    /* ---------------------------------------------- writes -------------------------------------------- */

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !writable.getAsBoolean()) return stack;
        Address a = address(slot);
        if (a == null || !isItemValid(slot, stack)) return stack;
        ItemStack current = getStackInSlot(slot);

        if (a.part() != null) {
            if (!current.isEmpty()) return stack;
            if (!simulate) {
                install(a.part(), a.index(), stack.copyWithCount(1));
                finish();
                edits.changed(describe(a), ItemStack.EMPTY, stack.copyWithCount(1));
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        int cap = subLimit(a.sub(), stack);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int room = cap - current.getCount();
        if (room <= 0) return stack;
        int moved = Math.min(room, stack.getCount());
        if (!simulate) {
            ItemStack after = stack.copyWithCount(current.getCount() + moved);
            subSet(data(), a.sub(), a.index(), after);
            finish();
            edits.changed(describe(a), current, after);
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !writable.getAsBoolean()) return ItemStack.EMPTY;
        Address a = address(slot);
        if (a == null) return ItemStack.EMPTY;
        ItemStack current = getStackInSlot(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;

        if (a.part() != null) {
            if (simulate) return current;
            ItemStack removed = uninstall(a.part(), a.index());
            finish();
            edits.changed(describe(a), removed, ItemStack.EMPTY);
            return removed;
        }

        int taken = Math.min(amount, current.getCount());
        if (simulate) return current.copyWithCount(taken);
        ItemStack after = current.copyWithCount(current.getCount() - taken);
        subSet(data(), a.sub(), a.index(), after);
        finish();
        edits.changed(describe(a), current, after);
        return current.copyWithCount(taken);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!writable.getAsBoolean()) return;
        Address a = address(slot);
        if (a == null) return;
        ItemStack before = getStackInSlot(slot);
        // Vanilla writes a slot back unchanged when a click adds nothing (the same implant on the
        // cursor over a full slot); taken literally, that would be a removal and a fresh install.
        if (ItemStack.matches(before, stack)) return;

        if (a.part() != null) {
            if (!before.isEmpty()) uninstall(a.part(), a.index());
            if (!stack.isEmpty()) {
                // The menu only calls this after mayPlace agreed, but the stack has already left the
                // cursor by now, so a refusal here hands it to the player rather than losing it.
                if (canInstall(data(), a.part(), a.index(), stack)) {
                    install(a.part(), a.index(), stack.copyWithCount(1));
                    if (stack.getCount() > 1) give(stack.copyWithCount(stack.getCount() - 1));
                } else {
                    give(stack.copy());
                }
            }
        } else if (stack.isEmpty() || subAccepts(data(), a.sub(), stack)) {
            subSet(data(), a.sub(), a.index(), stack.copy());
        } else {
            give(stack.copy());
        }
        finish();
        edits.changed(describe(a), before, getStackInSlot(slot));
    }

    /* --------------------------------------------- surgery -------------------------------------------- */

    private void install(CyberwareSlot part, int index, ItemStack stack) {
        int humanityCost = stack.getItem() instanceof ICyberwareItem cw ? cw.getHumanityCost() : 0;
        InstalledCyberware installed = new InstalledCyberware(stack, part, index, humanityCost);
        installed.setPowered(true);
        data().set(part, index, installed);
        if (stack.getItem() instanceof ICyberwareItem cw) cw.onInstalled(target, installed.getItem());
    }

    /** Takes one implant out, then everything that no longer has what it requires. */
    private ItemStack uninstall(CyberwareSlot part, int index) {
        PlayerCyberwareData data = data();
        ItemStack removed = removeOne(data, part, index);
        if (removed.isEmpty()) return removed;

        // SurgeryController.forceRemoveDependents, without a surgeon: repeat until nothing changes,
        // since taking one dependent out can strand another.
        Set<Item> removedItems = new HashSet<>();
        removedItems.add(removed.getItem());
        boolean changed;
        do {
            changed = false;
            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    InstalledCyberware inst = data.get(slot, i);
                    ItemStack stack = inst == null ? null : inst.getItem();
                    if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ICyberwareItem cw)) continue;
                    Set<Item> needItems = orEmpty(cw.requiresCyberware(stack, slot));
                    Set<TagKey<Item>> needTags = orEmpty(cw.requiresCyberwareTags(stack, slot));
                    if (needItems.isEmpty() && needTags.isEmpty()) continue;
                    if (!requirementRemoved(needItems, needTags, removedItems)) continue;
                    boolean itemsOk = !needItems.isEmpty() && anyInstalled(data, needItems);
                    boolean tagsOk = !needTags.isEmpty() && anyInstalledTagged(data, needTags);
                    if (itemsOk || tagsOk) continue;
                    ItemStack dependent = removeOne(data, slot, i);
                    if (dependent.isEmpty()) continue;
                    removedItems.add(dependent.getItem());
                    give(dependent);
                    edits.changed(slot.name() + "#" + i + " (dependent)", dependent, ItemStack.EMPTY);
                    changed = true;
                }
            }
        } while (changed);
        return removed;
    }

    private ItemStack removeOne(PlayerCyberwareData data, CyberwareSlot part, int index) {
        InstalledCyberware old = data.remove(part, index);
        ItemStack stack = old == null ? null : old.getItem();
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        data.ejectInventoryForRemovedCyberware(target, stack);
        if (stack.getItem() instanceof ICyberwareItem cw) cw.onRemoved(target, stack);
        return stack.copy();
    }

    /** After every change, what a finished surgery does: humanity, energy, and the sync. */
    private void finish() {
        PlayerCyberwareData data = data();
        data.recomputeHumanityBaseFromInstalled(target);
        data.clampEnergyToCapacity(target);
        data.setDirty();
        ModAttachments.syncCyberware(target);
    }

    private void give(ItemStack stack) {
        if (!stack.isEmpty()) target.getInventory().placeItemBackInInventory(stack);
    }

    /* ---------------------------------------------- rules --------------------------------------------- */

    /**
     * Whether {@code stack} may go into {@code part}'s slot {@code index}, judged as if that slot were
     * empty (a swap takes the old implant out first). The checks follow
     * {@code SurgeryController.performSurgery} and {@code RobosurgeonSlotItemHandler.mayPlace}.
     */
    private static boolean canInstall(PlayerCyberwareData data, CyberwareSlot part, int index, ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean organ = DefaultOrgans.isOrganForSlot(stack, part);
        if (!(stack.getItem() instanceof ICyberwareItem cw)) return organ;
        if (!organ && !cw.supportsSlot(part)) return false;
        if (!cw.surgeryInstallable()) return false;
        if (stack.getItem() instanceof CerebralProcessingUnitItem
                && !CerebralProcessingUnitItem.hasCompletedBiochipBinding(stack)) return false;

        int cap = Math.max(1, cw.maxStacksPerSlotType(stack, part));
        if (countSame(data, part, index, stack) >= cap) return false;

        Set<Item> needItems = orEmpty(cw.requiresCyberware(stack, part));
        if (!needItems.isEmpty() && !hasInPart(data, part, index, s -> needItems.contains(s.getItem()))) return false;
        Set<TagKey<Item>> needTags = orEmpty(cw.requiresCyberwareTags(stack, part));
        if (!needTags.isEmpty() && !hasInPart(data, part, index, s -> isAny(s, needTags))) return false;

        if (stack.getItem() instanceof OregrinderItem && hasAnywhere(data, part, index,
                (s, slot) -> s.is(ModTags.Items.WETWARE_ITEM))) return false;
        if (incompatible(data, part, index, stack, cw)) return false;
        return !sameSlotIncompatible(data, part, index, stack, cw);
    }

    private static boolean incompatible(PlayerCyberwareData data, CyberwareSlot part, int index,
                                        ItemStack stack, ICyberwareItem cw) {
        Set<Item> badItems = orEmpty(cw.incompatibleCyberware(stack, part));
        Set<TagKey<Item>> badTags = orEmpty(cw.incompatibleCyberwareTags(stack, part));
        return hasAnywhere(data, part, index, (other, otherSlot) -> {
            if (badItems.contains(other.getItem()) || isAny(other, badTags)) return true;
            if (other.getItem() instanceof ICyberwareItem otherCw) {
                if (orEmpty(otherCw.incompatibleCyberware(other, otherSlot)).contains(stack.getItem())) return true;
                return isAny(stack, orEmpty(otherCw.incompatibleCyberwareTags(other, otherSlot)));
            }
            return false;
        });
    }

    private static boolean sameSlotIncompatible(PlayerCyberwareData data, CyberwareSlot part, int index,
                                                ItemStack stack, ICyberwareItem cw) {
        Set<TagKey<Item>> badTags = orEmpty(cw.sameSlotIncompatibleCyberwareTags(stack, part));
        if (badTags.isEmpty()) return false;
        return hasInPart(data, part, index, other -> {
            if (isAny(other, badTags)) return true;
            return other.getItem() instanceof ICyberwareItem otherCw
                    && isAny(stack, orEmpty(otherCw.sameSlotIncompatibleCyberwareTags(other, part)));
        });
    }

    private static int countSame(PlayerCyberwareData data, CyberwareSlot part, int skip, ItemStack needle) {
        int count = 0;
        for (int i = 0; i < part.size; i++) {
            if (i == skip) continue;
            ItemStack s = installedAt(data, part, i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, needle)) count++;
        }
        return count;
    }

    private static boolean hasInPart(PlayerCyberwareData data, CyberwareSlot part, int skip,
                                     java.util.function.Predicate<ItemStack> test) {
        for (int i = 0; i < part.size; i++) {
            if (i == skip) continue;
            ItemStack s = installedAt(data, part, i);
            if (!s.isEmpty() && test.test(s)) return true;
        }
        return false;
    }

    private static boolean hasAnywhere(PlayerCyberwareData data, CyberwareSlot skipPart, int skipIndex,
                                       java.util.function.BiPredicate<ItemStack, CyberwareSlot> test) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            for (int i = 0; i < slot.size; i++) {
                if (slot == skipPart && i == skipIndex) continue;
                ItemStack s = installedAt(data, slot, i);
                if (!s.isEmpty() && test.test(s, slot)) return true;
            }
        }
        return false;
    }

    private static boolean anyInstalled(PlayerCyberwareData data, Set<Item> items) {
        return hasAnywhere(data, null, -1, (s, slot) -> items.contains(s.getItem()));
    }

    private static boolean anyInstalledTagged(PlayerCyberwareData data, Set<TagKey<Item>> tags) {
        return hasAnywhere(data, null, -1, (s, slot) -> isAny(s, tags));
    }

    /** SurgeryController.removedRequirementMatches: does a removed item satisfy any requirement? */
    private static boolean requirementRemoved(Set<Item> needItems, Set<TagKey<Item>> needTags, Set<Item> removed) {
        for (Item item : removed) {
            if (needItems.contains(item)) return true;
            for (TagKey<Item> tag : needTags) {
                if (tag != null && item.builtInRegistryHolder().is(tag)) return true;
            }
        }
        return false;
    }

    private static ItemStack installedAt(PlayerCyberwareData data, CyberwareSlot part, int index) {
        InstalledCyberware inst = data.get(part, index);
        ItemStack s = inst == null ? null : inst.getItem();
        return s == null ? ItemStack.EMPTY : s;
    }

    private static boolean isAny(ItemStack stack, Set<TagKey<Item>> tags) {
        for (TagKey<Item> tag : tags) {
            if (tag != null && stack.is(tag)) return true;
        }
        return false;
    }

    private static <T> Set<T> orEmpty(Set<T> set) {
        return set == null ? Set.of() : set;
    }

    /* ------------------------------------------ sub-inventories ---------------------------------------- */

    /**
     * Whether {@code stack} may go into one of {@code sub}'s slots. The implant that carries the
     * inventory has to be installed, and the item has to be one its own setter keeps. The setters
     * replace anything else with EMPTY, which here would delete the item.
     */
    private static boolean subAccepts(PlayerCyberwareData data, Sub sub, ItemStack stack) {
        if (!carries(data, sub)) return false;
        return switch (sub) {
            case CHIPWARE -> stack.is(ModTags.Items.DATA_SHARDS);
            case CYBERDECK -> stack.is(ModTags.Items.QUICKHACK_SHARDS);
            case HEAT_ENGINE -> true;
            case SPINAL_INJECTOR -> SpinalInjectorItem.isInjectable(stack);
            case ARM_CANNON -> ArmCannonItem.isValidStoredItem(stack);
        };
    }

    private static int subLimit(Sub sub, ItemStack stack) {
        return switch (sub) {
            case CHIPWARE, CYBERDECK -> 1;
            case SPINAL_INJECTOR -> Math.max(1, SpinalInjectorItem.maxStackFor(stack));
            case HEAT_ENGINE, ARM_CANNON -> Math.max(1, stack.getMaxStackSize());
        };
    }

    private static boolean carries(PlayerCyberwareData data, Sub sub) {
        return hasAnywhere(data, null, -1, (s, slot) -> switch (sub) {
            case CHIPWARE -> s.getItem() instanceof ChipwareSlotsItem;
            case CYBERDECK -> s.getItem() instanceof CyberdeckItem;
            case HEAT_ENGINE -> s.getItem() instanceof HeatEngineItem;
            case SPINAL_INJECTOR -> s.getItem() instanceof SpinalInjectorItem;
            case ARM_CANNON -> s.getItem() instanceof ArmCannonItem;
        });
    }

    private static ItemStack subGet(PlayerCyberwareData data, Sub sub, int index) {
        return switch (sub) {
            case CHIPWARE -> data.getChipwareStack(index);
            case CYBERDECK -> data.getCyberdeckStack(index);
            case HEAT_ENGINE -> data.getHeatEngineStack(index);
            case SPINAL_INJECTOR -> data.getSpinalInjectorStack(index);
            case ARM_CANNON -> data.getArmCannonStack(index);
        };
    }

    private static void subSet(PlayerCyberwareData data, Sub sub, int index, ItemStack stack) {
        switch (sub) {
            case CHIPWARE -> data.setChipwareStack(index, stack);
            case CYBERDECK -> data.setCyberdeckStack(index, stack);
            case HEAT_ENGINE -> data.setHeatEngineStack(index, stack);
            case SPINAL_INJECTOR -> data.setSpinalInjectorStack(index, stack);
            case ARM_CANNON -> data.setArmCannonStack(index, stack);
        }
    }
}
