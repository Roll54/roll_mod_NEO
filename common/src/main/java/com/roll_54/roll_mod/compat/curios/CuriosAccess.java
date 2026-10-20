package com.roll_54.roll_mod.compat.curios;

import com.roll_54.roll_mod.minestar.deathlog.DeathRecord.CurioStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The Curios side of {@link CuriosCompat}. Only ever loaded with Curios present. */
final class CuriosAccess {

    private CuriosAccess() {}

    static List<CurioStack> snapshot(LivingEntity entity) {
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
        if (inventory.isEmpty()) return List.of();
        List<CurioStack> out = new ArrayList<>();
        for (Map.Entry<String, ICurioStacksHandler> entry : inventory.get().getCurios().entrySet()) {
            collect(out, entry.getKey(), entry.getValue().getStacks(), false);
            collect(out, entry.getKey(), entry.getValue().getCosmeticStacks(), true);
        }
        return out;
    }

    private static void collect(List<CurioStack> out, String slot, IDynamicStackHandler stacks, boolean cosmetic) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (!stack.isEmpty()) out.add(new CurioStack(slot, i, cosmetic, stack.copy()));
        }
    }

    static boolean restore(LivingEntity entity, CurioStack curio) {
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
        if (inventory.isEmpty()) return false;
        Optional<ICurioStacksHandler> handler = inventory.get().getStacksHandler(curio.slot());
        if (handler.isEmpty()) return false;
        IDynamicStackHandler stacks = curio.cosmetic()
                ? handler.get().getCosmeticStacks() : handler.get().getStacks();
        if (curio.index() < 0 || curio.index() >= stacks.getSlots()) return false;
        if (!stacks.getStackInSlot(curio.index()).isEmpty()) return false;
        stacks.setStackInSlot(curio.index(), curio.stack().copy());
        return true;
    }

    static List<CuriosCompat.CurioRow> rows(LivingEntity entity) {
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
        if (inventory.isEmpty()) return List.of();
        List<CuriosCompat.CurioRow> out = new ArrayList<>();
        for (Map.Entry<String, ICurioStacksHandler> entry : inventory.get().getCurios().entrySet()) {
            ICurioStacksHandler handler = entry.getValue();
            int size = handler.getStacks().getSlots();
            if (size <= 0) continue;
            out.add(new CuriosCompat.CurioRow(entry.getKey(), size, false));
            if (handler.hasCosmetic()) {
                out.add(new CuriosCompat.CurioRow(entry.getKey(), handler.getCosmeticStacks().getSlots(), true));
            }
        }
        return out;
    }

    @Nullable
    static IItemHandlerModifiable handler(LivingEntity entity, CuriosCompat.CurioRow row) {
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
        if (inventory.isEmpty()) return null;
        Optional<ICurioStacksHandler> handler = inventory.get().getStacksHandler(row.id());
        if (handler.isEmpty()) return null;
        IDynamicStackHandler stacks = row.cosmetic()
                ? handler.get().getCosmeticStacks() : handler.get().getStacks();
        return new Guarded(entity, row, stacks, handler.get());
    }

    /**
     * A curio row that checks what Curios' own slot checks before an item goes in: the slot type's
     * validators, then the item's own {@code canEquip}. Reads go to the live handler, so a slot
     * that shrank while the window was open shows as empty instead of throwing.
     */
    private record Guarded(LivingEntity entity, CuriosCompat.CurioRow row, IDynamicStackHandler stacks,
                           ICurioStacksHandler type) implements IItemHandlerModifiable {

        private boolean inRange(int slot) {
            return slot >= 0 && slot < stacks.getSlots();
        }

        private SlotContext context(int slot) {
            boolean renders = slot < type.getRenders().size() && type.getRenders().get(slot);
            return new SlotContext(row.id(), entity, slot, row.cosmetic(), renders);
        }

        @Override
        public int getSlots() {
            return row.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inRange(slot) ? stacks.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (inRange(slot)) stacks.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!inRange(slot) || !isItemValid(slot, stack)) return stack;
            return stacks.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inRange(slot) ? stacks.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inRange(slot) ? stacks.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!inRange(slot) || stack.isEmpty()) return false;
            SlotContext context = context(slot);
            if (!CuriosApi.isStackValid(context, stack)) return false;
            return CuriosApi.getCurio(stack).map(c -> c.canEquip(context)).orElse(true);
        }
    }
}
