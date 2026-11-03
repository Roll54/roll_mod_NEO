package com.roll_54.roll_mod.gtmogs.integration.xei.entry.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public sealed interface ItemEntryList permits ItemStackList, ItemTagList {

    List<ItemStack> stacks();

    boolean isEmpty();
}
