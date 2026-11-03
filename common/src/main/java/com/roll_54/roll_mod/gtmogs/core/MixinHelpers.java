package com.roll_54.roll_mod.gtmogs.core;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.integration.kjs.GTMOGSServerEvents;
import com.roll_54.roll_mod.gtmogs.integration.kjs.events.GTOreVeinKubeEvent;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class MixinHelpers {

    private static <T> Collector<T, ?, ArrayList<T>> toArrayList() {
        return Collectors.toCollection(ArrayList::new);
    }

    public static TagLoader.EntryWithSource makeItemEntry(ItemLike item) {
        return makeElementEntry(item.asItem().builtInRegistryHolder().key().location());
    }

    public static TagLoader.EntryWithSource makeBlockEntry(Supplier<? extends Block> block) {
        return makeBlockEntry(block.get());
    }

    public static TagLoader.EntryWithSource makeBlockEntry(Block block) {
        return makeElementEntry(block.builtInRegistryHolder().key().location());
    }

    public static TagLoader.EntryWithSource makeFluidEntry(Fluid fluid) {
        return makeElementEntry(fluid.builtInRegistryHolder().key().location());
    }

    public static TagLoader.EntryWithSource makeElementEntry(ResourceLocation id) {
        return new TagLoader.EntryWithSource(TagEntry.element(id), GTValues.CUSTOM_TAG_SOURCE);
    }

    public static TagLoader.EntryWithSource makeTagEntry(TagKey<?> tag) {
        return new TagLoader.EntryWithSource(TagEntry.tag(tag.location()), GTValues.CUSTOM_TAG_SOURCE);
    }

    public static void postKJSVeinEvents(WritableRegistry<?> registry) {
        if (!GTMOGS.Mods.isKubeJSLoaded()) {
            return;
        }

        if (registry.key() == GTRegistries.ORE_VEIN_REGISTRY) {
            KJSCallWrapper.postOreVeinEvent();
        }
    }

    private static final class KJSCallWrapper {

        private static void postOreVeinEvent() {
            GTMOGSServerEvents.ORE_VEIN_MODIFICATION.post(new GTOreVeinKubeEvent());
        }
    }

    public static final class ClientCallWrapper {

        public static Level getClientLevel() {
            return Minecraft.getInstance().level;
        }
    }
}
