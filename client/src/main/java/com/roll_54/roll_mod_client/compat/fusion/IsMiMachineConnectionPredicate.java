package com.roll_54.roll_mod_client.compat.fusion;

import aztech.modern_industrialization.machines.MachineBlock;
import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fusion connection predicate that connects to any Modern Industrialization machine, including machines
 * registered through KubeJS or MI Tweaks. Used by casing blocks so they drop their border towards machines.
 * <p>
 * Usage: {@code { "type": "mi_ct:is_mi_machine" }}
 */
public class IsMiMachineConnectionPredicate implements ConnectionPredicate {

    public static final IsMiMachineConnectionPredicate INSTANCE = new IsMiMachineConnectionPredicate();
    public static final Serializer<IsMiMachineConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public IsMiMachineConnectionPredicate deserialize(JsonObject json){
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(IsMiMachineConnectionPredicate value){
            return new JsonObject();
        }
    };

    private IsMiMachineConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return otherState.getBlock() instanceof MachineBlock;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}
