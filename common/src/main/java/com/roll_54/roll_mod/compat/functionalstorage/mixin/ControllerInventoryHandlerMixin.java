package com.roll_54.roll_mod.compat.functionalstorage.mixin;

import com.buuz135.functionalstorage.inventory.ControllerInventoryHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Turns the drawer controller's per-slot staleness check from a list scan into a set lookup.
 *
 * <p>{@code ControllerInventoryHandler} indexes slots properly — {@code invalidateSlots()} builds a
 * {@code HandlerSlotSelector[]} and {@code selectorForSlot(int)} is a bare array access. But all
 * three accessors then re-validate the selector against the network the slow way:
 *
 * <pre>{@code
 * HandlerSlotSelector selector = selectorForSlot(slot);
 * if (selector == null) return ItemStack.EMPTY;
 * if (!getDrawers().getItemHandlers().contains(selector.handler)) {  // O(n) over every drawer
 *     invalidateSlots();
 *     return ItemStack.EMPTY;
 * }
 * return selector.getStackInSlot();
 * }</pre>
 *
 * <p>{@code itemHandlers} is a plain {@code ArrayList}, so that is {@code indexOfRange} plus an
 * {@code equals} per element, on <em>every</em> slot read, insert and extract. Anything that walks
 * the controller's slots — NeoForge's own {@code ItemHandlerHelper.insertItemStacked} does, which is
 * what Modern Industrialization's item pipes call — therefore costs O(slots x connected drawers).
 *
 * <p>On this server that was the single largest mod cost in an hour of profiling: 357s of tick time
 * inside the controller, 291s of it in {@code ArrayList.contains} alone, ~5 ms/tick. It also put
 * {@code java.lang.Object.equals} at the top of the whole profile's self-time list.
 *
 * <p>The replacement is a cached {@link HashSet} of the same list, and the cache-validity rule is
 * what makes it safe without hooking any mutation. {@code ConnectedDrawers.rebuild()} assigns a
 * <em>brand new</em> {@code ArrayList} to {@code itemHandlers} before filling it, and filling is the
 * only other thing that ever touches it — adds, no removals, no in-place replacement. So the pair
 * (list identity, list size) changes on every mutation this class can observe: a rebuild changes the
 * identity, and an add during a rebuild changes the size. Comparing both is a reference test and a
 * {@code size()} call, and it is exact rather than a heuristic.
 *
 * <p>{@link HashSet} and not an identity set, deliberately: {@code List.contains} uses
 * {@code equals}, so this keeps the semantics character for character. None of Functional Storage's
 * handlers override {@code equals} or {@code hashCode}, so in practice both answer identity anyway.
 *
 * <p>Behaviour is unchanged, including the branch that matters: when a drawer is broken mid-transfer
 * the handler is no longer in the list, the lookup still misses, {@code invalidateSlots()} still
 * runs and the controller still heals itself.
 *
 * <p>Soft config ({@code required = false}, {@code defaultRequire = 0}) — Functional Storage is a
 * pack mod, not a dependency of this one. Checked against 1.5.8.
 */
@Mixin(value = ControllerInventoryHandler.class, remap = false)
public abstract class ControllerInventoryHandlerMixin {

    @Unique
    private List<?> roll_mod$cachedHandlers;

    /** No initializer on purpose: Mixin's handling of instance field initializers is version
     * sensitive, and the {@code roll_mod$handlerSet == null} test below already covers first use. */
    @Unique
    private int roll_mod$cachedSize;

    @Unique
    private Set<Object> roll_mod$handlerSet;

    @WrapOperation(
            method = {"getStackInSlot", "insertItem", "extractItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"
            )
    )
    private boolean roll_mod$containsViaSet(
            List<?> handlers, Object handler, Operation<Boolean> original) {
        int size = handlers.size();
        if (roll_mod$handlerSet == null || roll_mod$cachedHandlers != handlers || roll_mod$cachedSize != size) {
            roll_mod$handlerSet = new HashSet<>(handlers);
            roll_mod$cachedHandlers = handlers;
            roll_mod$cachedSize = size;
        }
        return roll_mod$handlerSet.contains(handler);
    }
}
