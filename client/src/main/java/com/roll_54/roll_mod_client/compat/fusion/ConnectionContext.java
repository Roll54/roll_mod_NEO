package com.roll_54.roll_mod_client.compat.fusion;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Position of the neighbour Fusion is currently checking a connection with. Fusion only passes the position
 * of the block being rendered to predicates; {@code ConnectingTextureTypeMixin} fills this in around each check.
 */
public final class ConnectionContext {

    private static final ThreadLocal<BlockPos> NEIGHBOR = new ThreadLocal<>();

    public static @Nullable BlockPos neighbor(){
        return NEIGHBOR.get();
    }

    public static @Nullable BlockPos set(@Nullable BlockPos pos){
        BlockPos previous = NEIGHBOR.get();
        NEIGHBOR.set(pos);
        return previous;
    }

    private ConnectionContext(){
    }
}
