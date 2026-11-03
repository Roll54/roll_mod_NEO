package com.roll_54.roll_mod.gtmogs.utils.memoization;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class MemoizedBlockSupplier<T extends Block> extends MemoizedSupplier<T> {

    protected MemoizedBlockSupplier(Supplier<T> delegate) {
        super(delegate);
    }
}
