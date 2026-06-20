package com.roll_54.roll_mod.blocks.entity;

import com.roll_54.roll_mod.registry.BlockEntites;
import com.roll_54.roll_mod.registry.RecipeRegistry;
import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import com.roll_54.roll_mod.recipe.ItemResearchRecipeInput;
import com.roll_54.roll_mod.gui.menu.ResearchWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Slots:
 *   0 – MAIN_INPUT  (the expensive item, consumed only on success)
 *   1 – CATALYST    (the item the player inserts to trigger research)
 *   2 – OUTPUT      (blueprint is placed here on success)
 */
public class ResearchWorkbenchBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    // slot indices
    public static final int MAIN_INPUT_SLOT = 0;
    public static final int CATALYST_SLOT   = 1;
    public static final int OUTPUT_SLOT     = 2;

    public final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    // ContainerData indices: 0 = progress, 1 = maxProgress
    private int progress    = 0;
    private int maxProgress = 100;

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> progress    = value;
                case 1 -> maxProgress = value;
            }
        }
        @Override public int getCount() { return 2; }
    };

    public ResearchWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntites.RESEARCH_WORKBENCH_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.roll_mod.research_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ResearchWorkbenchMenu(id, inv, this, data);
    }

    // ── Tick ─────────────────────────────────────────────────────────

    public void tick(Level level, BlockPos pos, BlockState state) {
        Optional<RecipeHolder<ItemResearchRecipe>> recipeOpt = getCurrentRecipe();

        if (recipeOpt.isEmpty()) {
            resetProgress();
            return;
        }

        ItemResearchRecipe recipe = recipeOpt.get().value();

        // Update maxProgress from recipe duration
        maxProgress = recipe.duration();

        // Make sure main_input slot has the required item
        ItemStack mainStack = itemHandler.getStackInSlot(MAIN_INPUT_SLOT);
        if (!mainStack.is(recipe.mainInput().item())) {
            resetProgress();
            return;
        }

        // Make sure output slot can accept the result
        if (!canOutput(recipe.output())) {
            resetProgress();
            return;
        }

        // Advance progress
        progress++;
        setChanged(level, pos, state);

        if (progress >= maxProgress) {
            // Roll the dice
            ItemStack catalyst = itemHandler.getStackInSlot(CATALYST_SLOT);
            ItemResearchRecipe.CatalystEntry entry = recipe.findCatalyst(catalyst);
            if (entry != null) {
                boolean success = level.getRandom().nextFloat() < entry.successChance();

                // Always consume the catalyst
                itemHandler.extractItem(CATALYST_SLOT, 1, false);

                if (success) {
                    // Consume main input if configured
                    if (recipe.mainInput().consumeOnSuccess()) {
                        itemHandler.extractItem(MAIN_INPUT_SLOT, 1, false);
                    }
                    // Place blueprint in output
                    ItemStack outputStack = recipe.output().copy();
                    ItemStack existing = itemHandler.getStackInSlot(OUTPUT_SLOT);
                    if (existing.isEmpty()) {
                        itemHandler.setStackInSlot(OUTPUT_SLOT, outputStack);
                    } else {
                        existing.grow(outputStack.getCount());
                    }
                }
            }
            resetProgress();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Optional<RecipeHolder<ItemResearchRecipe>> getCurrentRecipe() {
        ItemStack catalyst = itemHandler.getStackInSlot(CATALYST_SLOT);
        if (catalyst.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(
                RecipeRegistry.ITEM_RESEARCH_RECIPE_TYPE.get(),
                new ItemResearchRecipeInput(catalyst),
                level);
    }

    private boolean canOutput(ItemStack output) {
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) return true;
        if (!current.is(output.getItem())) return false;
        int maxStack = current.isEmpty() ? 64 : current.getMaxStackSize();
        return current.getCount() + output.getCount() <= maxStack;
    }

    private void resetProgress() {
        progress    = 0;
        maxProgress = 100;
    }

    // ── Drop contents on break ────────────────────────────────────────

    public void drops() {
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    // ── NBT persistence ───────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("research_workbench.progress", progress);
        tag.putInt("research_workbench.max_progress", maxProgress);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress    = tag.getInt("research_workbench.progress");
        maxProgress = tag.getInt("research_workbench.max_progress");
    }

    // ── Sync ─────────────────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.DOWN) {
			return new int[]{OUTPUT_SLOT};
		}
		if (side == Direction.UP) {
			return new int[]{MAIN_INPUT_SLOT};
		}
		return new int[]{CATALYST_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
		if (direction == Direction.UP && index == MAIN_INPUT_SLOT) {
			return true;
		}
		if (direction != Direction.UP && direction != Direction.DOWN && index == CATALYST_SLOT) {
			return true;
		}
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return direction == Direction.DOWN && index == OUTPUT_SLOT;
	}

	@Override
	public int getContainerSize() {
		return itemHandler.getSlots();
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < itemHandler.getSlots(); i++) {
			if (!itemHandler.getStackInSlot(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		return itemHandler.getStackInSlot(index);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		return itemHandler.extractItem(index, count, false);
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack stack = getItem(index);
		setItem(index, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		itemHandler.setStackInSlot(index, stack);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < itemHandler.getSlots(); i++) {
			itemHandler.setStackInSlot(i, ItemStack.EMPTY);
		}
	}
}
