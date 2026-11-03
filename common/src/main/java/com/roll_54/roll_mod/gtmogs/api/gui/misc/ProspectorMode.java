package com.roll_54.roll_mod.gtmogs.api.gui.misc;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags;

import com.roll_54.roll_mod.gtmogs.api.gui.texture.ProspectingTexture;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class ProspectorMode<T> {

    public static ProspectorMode<String> ORE = new ProspectorMode<>("metaitem.prospector.mode.ores", 16) {

        private final Map<BlockState, String> BLOCK_CACHE = new HashMap<>();
        private final Map<String, IGuiTexture> ICON_CACHE = new HashMap<>();

        @Override
        public void scan(String[][][] storage, LevelChunk chunk) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                        pos.set(x, y, z);
                        var state = chunk.getBlockState(pos);
                        if (state.is(Tags.Blocks.ORES)) {
                            var itemName = BLOCK_CACHE.computeIfAbsent(state, blockState -> {
                                var name = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
                                return name;
                            });
                            storage[x][z] = ArrayUtils.add(storage[x][z], itemName);
                        }
                    }
                }
            }
        }

        @Override
        public int getItemColor(String item) {
            return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(item)).defaultMapColor().col;
        }

        @Override
        public IGuiTexture getItemIcon(String item) {
            return ICON_CACHE.computeIfAbsent(item, name -> {
                return new ItemStackTexture(new ItemStack(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name))))
                        .scale(0.8f);
            });
        }

        @Override
        public MutableComponent getDescription(String item) {
            return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(item)).getName();
        }

        @Override
        public String getUniqueID(String item) {
            return item;
        }

        @Override
        public void serialize(String item, FriendlyByteBuf buf) {
            buf.writeUtf(item);
        }

        @Override
        public String deserialize(FriendlyByteBuf buf) {
            return buf.readUtf();
        }

        @Override
        public Class<String> getItemClass() {
            return String.class;
        }

        @Override
        public void appendTooltips(List<String[]> items, List<Component> tooltips, String selected) {
            Object2IntOpenHashMap<String> counter = new Object2IntOpenHashMap<>();
            for (var array : items) {
                for (String item : array) {
                    if (ProspectingTexture.SELECTED_ALL.equals(selected) || selected.equals(getUniqueID(item))) {
                        counter.addTo(item, 1);
                    }
                }
            }
            counter.forEach((item, count) -> tooltips.add(getDescription(item).append(" --- " + count)));
        }
    };

    public final String unlocalizedName;
    public final int cellSize;

    ProspectorMode(@NotNull String unlocalizedName, int cellSize) {
        this.unlocalizedName = unlocalizedName;
        this.cellSize = cellSize;
    }

    public abstract void scan(T[][][] storage, LevelChunk chunk);

    public abstract int getItemColor(T item);

    public abstract IGuiTexture getItemIcon(T item);

    public abstract MutableComponent getDescription(T item);

    public abstract String getUniqueID(T item);

    public abstract void serialize(T item, FriendlyByteBuf buf);

    public abstract T deserialize(FriendlyByteBuf buf);

    public abstract Class<T> getItemClass();

    public abstract void appendTooltips(List<T[]> items, List<Component> tooltips, String selected);

    @OnlyIn(Dist.CLIENT)
    public void drawSpecialGrid(GuiGraphics graphics, T[] items, int x, int y, int width, int height) {}
}
