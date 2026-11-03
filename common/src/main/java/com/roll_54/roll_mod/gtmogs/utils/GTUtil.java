package com.roll_54.roll_mod.gtmogs.utils;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class GTUtil {

    public static final Direction[] DIRECTIONS = Direction.values();

    @SuppressWarnings("UnstableApiUsage")
    public static final ImmutableList<BlockPos> NON_CORNER_NEIGHBOURS = Util.make(() -> {
        var builder = ImmutableList.<BlockPos>builderWithExpectedSize(18);
        BlockPos.betweenClosedStream(-1, -1, -1, 1, 1, 1)
                .filter((pos) -> (pos.getX() == 0 || pos.getY() == 0 || pos.getZ() == 0) && !pos.equals(BlockPos.ZERO))
                .map(BlockPos::immutable)
                .forEach(builder::add);
        return builder.build();
    });

    @Nullable
    public static Direction determineWrenchingSide(Direction facing, float x, float y, float z) {
        Direction opposite = facing.getOpposite();
        switch (facing) {
            case DOWN, UP -> {
                if (x < 0.25) {
                    if (z < 0.25) return opposite;
                    if (z > 0.75) return opposite;
                    return Direction.WEST;
                }
                if (x > 0.75) {
                    if (z < 0.25) return opposite;
                    if (z > 0.75) return opposite;
                    return Direction.EAST;
                }
                if (z < 0.25) return Direction.NORTH;
                if (z > 0.75) return Direction.SOUTH;
                return facing;
            }
            case NORTH, SOUTH -> {
                if (x < 0.25) {
                    if (y < 0.25) return opposite;
                    if (y > 0.75) return opposite;
                    return Direction.WEST;
                }
                if (x > 0.75) {
                    if (y < 0.25) return opposite;
                    if (y > 0.75) return opposite;
                    return Direction.EAST;
                }
                if (y < 0.25) return Direction.DOWN;
                if (y > 0.75) return Direction.UP;
                return facing;
            }
            case WEST, EAST -> {
                if (z < 0.25) {
                    if (y < 0.25) return opposite;
                    if (y > 0.75) return opposite;
                    return Direction.NORTH;
                }
                if (z > 0.75) {
                    if (y < 0.25) return opposite;
                    if (y > 0.75) return opposite;
                    return Direction.SOUTH;
                }
                if (y < 0.25) return Direction.DOWN;
                if (y > 0.75) return Direction.UP;
                return facing;
            }
        }
        return null;
    }

    /**
     * Calculates on which side the neighbor is relative to the main pos.
     *
     * @param main     main pos
     * @param neighbor neighbor pos
     * @return position of neighbor relative to main or null the neighbor pos is not a neighbor
     */
    @Nullable
    public static Direction getFacingToNeighbor(@NotNull BlockPos main, @NotNull BlockPos neighbor) {
        int difX = neighbor.getX() - main.getX();
        int difY = neighbor.getY() - main.getY();
        int difZ = neighbor.getZ() - main.getZ();
        if (difX != 0) {
            if (difY != 0 || difZ != 0 || (difX != 1 && difX != -1)) return null;
            return difX > 0 ? Direction.EAST : Direction.WEST;
        }
        if (difY != 0) {
            if (difZ != 0 || (difY != 1 && difY != -1)) return null;
            return difY > 0 ? Direction.UP : Direction.DOWN;
        }
        if (difZ != 0) {
            if (difZ != 1 && difZ != -1) return null;
            return difZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }

    /**
     * @param array Array sorted with natural order
     * @param value Value to search for
     * @return Index of the nearest value lesser or equal than {@code value},
     *         or {@code -1} if there's no entry matching the condition
     */
    public static int nearestLesserOrEqual(long @NotNull [] array, long value) {
        int low = 0, high = array.length - 1;
        while (true) {
            int median = (low + high) / 2;
            if (array[median] <= value) {
                if (low == high) return low;
                low = median + 1;
            } else {
                if (low == high) return low - 1;
                high = median - 1;
            }
        }
    }

    /**
     * @param array Array sorted with natural order
     * @param value Value to search for
     * @return Index of the nearest value lesser than {@code value},
     *         or {@code -1} if there's no entry matching the condition
     */
    public static int nearestLesser(long @NotNull [] array, long value) {
        int low = 0, high = array.length - 1;
        while (true) {
            int median = (low + high) / 2;
            if (array[median] < value) {
                if (low == high) return low;
                low = median + 1;
            } else {
                if (low == high) return low - 1;
                high = median - 1;
            }
        }
    }

    /**
     * Copies first non-empty ItemStack from stacks.
     *
     * @param stacks list of candidates for copying
     * @return a copy of ItemStack, or {@link ItemStack#EMPTY} if all the candidates are empty
     * @throws IllegalArgumentException if {@code stacks} is empty
     */
    public static @NotNull ItemStack copyFirst(@NotNull ItemStack... stacks) {
        if (stacks.length == 0) {
            throw new IllegalArgumentException("Empty ItemStack candidates");
        }
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Copies first non-empty ItemStack from stacks, with new stack size.
     *
     * @param stacks list of candidates for copying
     * @return a copy of ItemStack, or {@link ItemStack#EMPTY} if all the candidates are empty
     * @throws IllegalArgumentException if {@code stacks} is empty
     */
    public static @NotNull ItemStack copyFirst(int newCount, @NotNull ItemStack... stacks) {
        if (stacks.length == 0) {
            throw new IllegalArgumentException("Empty ItemStack candidates");
        }
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return stack.copyWithCount(newCount);
            }
        }
        return ItemStack.EMPTY;
    }

    public static <M> M getItem(List<? extends M> list, int index, M replacement) {
        if (index >= 0 && index < list.size())
            return list.get(index);
        return replacement;
    }

    public static <T extends WeightedEntry> @Nullable T getRandomItem(RandomSource random, List<T> randomList) {
        if (randomList.isEmpty()) return null;
        int size = randomList.size();
        int[] baseOffsets = new int[size];
        int currentIndex = 0;
        for (int i = 0; i < size; i++) {
            int weight = randomList.get(i).weight();
            if (weight <= 0) {
                throw new IllegalArgumentException("Invalid weight: " + weight);
            }
            currentIndex += weight;
            baseOffsets[i] = currentIndex;
        }
        int randomValue = random.nextInt(currentIndex);
        for (int i = 0; i < size; i++) {
            if (randomValue < baseOffsets[i]) return randomList.get(i);
        }
        throw new IllegalArgumentException("Invalid weight");
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Class<T> getActualTypeParameter(Class<? extends R> thisClass, int index) {
        Type type = thisClass.getGenericSuperclass();
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[index];
    }

    public static boolean isShiftDown() {
        if (GTMOGS.isClientSide()) {
            var id = Minecraft.getInstance().getWindow().getWindow();
            return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                    InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT);
        }
        return false;
    }

    public static boolean isCtrlDown() {
        if (GTMOGS.isClientSide()) {
            var id = Minecraft.getInstance().getWindow().getWindow();
            return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                    InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_CONTROL);
        }
        return false;
    }

    public static boolean isAltDown() {
        if (GTMOGS.isClientSide()) {
            var id = Minecraft.getInstance().getWindow().getWindow();
            return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_ALT) ||
                    InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_ALT);
        }
        return false;
    }

    public static boolean isFluidStackAmountDivisible(FluidStack fluidStack, int divisor) {
        return fluidStack.getAmount() % divisor == 0 && fluidStack.getAmount() % divisor != fluidStack.getAmount() &&
                fluidStack.getAmount() / divisor != 0;
    }

    public static boolean isItemStackCountDivisible(ItemStack itemStack, int divisor) {
        return itemStack.getCount() % divisor == 0 && itemStack.getCount() % divisor != itemStack.getCount() &&
                itemStack.getCount() / divisor != 0;
    }

    public static int getItemBurnTime(Item item) {
        return item.getDefaultInstance().getBurnTime(RecipeType.SMELTING);
    }

    public static int getPumpBiomeModifier(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_NETHER)) {
            return -1;
        }

        if (biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) ||
                biome.is(BiomeTags.IS_RIVER)) {
            return FluidType.BUCKET_VOLUME;
        } else if (biome.is(Tags.Biomes.IS_SWAMP) || biome.is(Tags.Biomes.IS_WET)) {
            return FluidType.BUCKET_VOLUME * 4 / 5;
        } else if (biome.is(BiomeTags.IS_JUNGLE)) {
            return FluidType.BUCKET_VOLUME * 35 / 100;
        } else if (biome.is(Tags.Biomes.IS_SNOWY)) {
            return FluidType.BUCKET_VOLUME * 3 / 10;
        } else if (biome.is(Tags.Biomes.IS_PLAINS) || biome.is(BiomeTags.IS_FOREST)) {
            return FluidType.BUCKET_VOLUME / 4;
        } else if (biome.is(Tags.Biomes.IS_COLD)) {
            return FluidType.BUCKET_VOLUME * 175 / 1000;
        } else if (biome.is(Tags.Biomes.IS_SANDY)) {
            return FluidType.BUCKET_VOLUME * 170 / 1000;
        }
        return FluidType.BUCKET_VOLUME / 10;
    }

    /**
     * Determines dye color nearest to specified RGB color
     */
    public static @Nullable DyeColor determineDyeColor(int rgbColor) {
        float[] c = GradientUtil.getRGB(rgbColor);

        double min = Double.MAX_VALUE;
        DyeColor minColor = null;
        for (DyeColor dyeColor : DyeColor.values()) {
            float[] c2 = GradientUtil.getRGB(dyeColor.getTextColor());

            double distance = (c[0] - c2[0]) * (c[0] - c2[0]) + (c[1] - c2[1]) * (c[1] - c2[1]) +
                    (c[2] - c2[2]) * (c[2] - c2[2]);

            if (min > distance) {
                minColor = dyeColor;
                min = distance;
            }
        }
        return minColor;
    }

    public static int convertRGBtoARGB(int colorValue) {
        return convertRGBtoARGB(colorValue, 0xFF);
    }

    public static int convertRGBtoARGB(int colorValue, int opacity) {
        // preserve existing opacity if present
        if (((colorValue >> 24) & 0xFF) != 0) return colorValue;
        return opacity << 24 | colorValue;
    }

    public static int getFluidColor(FluidStack fluid) {
        return IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
    }

    /**
     * Returns the slot type based on the slot group and the index inside that group.
     */
    public static EquipmentSlot equipmentSlotByTypeAndIndex(EquipmentSlot.Type slotType, int slotIndex) {
        for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            if (equipmentslot.getType() == slotType && equipmentslot.getIndex() == slotIndex) {
                return equipmentslot;
            }
        }

        throw new IllegalArgumentException("Invalid slot '" + slotType + "': " + slotIndex);
    }
}
