package com.roll_54.roll_mod_client.compat.fusion;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fusion block model predicate: true when the model is rendered for a FramedBlocks block, i.e. as a camo.
 * FramedBlocks passes its own (enclosing) block state when it asks a camo model for its model data, so block
 * model modifiers on casing blocks can pick a different model for framed blocks.
 * <p>
 * Usage: {@code { "type": "mi_ct:is_framed_block" }}
 */
public class IsFramedBlockModelPredicate implements BlockStateModelPredicate {

    public static final IsFramedBlockModelPredicate INSTANCE = new IsFramedBlockModelPredicate();
    public static final Serializer<IsFramedBlockModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public IsFramedBlockModelPredicate deserialize(JsonObject json){
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(IsFramedBlockModelPredicate value){
            return new JsonObject();
        }
    };

    private IsFramedBlockModelPredicate(){
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        return state != null && "framedblocks".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
