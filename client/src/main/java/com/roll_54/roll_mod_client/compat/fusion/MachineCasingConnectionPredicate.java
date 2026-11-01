package com.roll_54.roll_mod_client.compat.fusion;

import aztech.modern_industrialization.client.machines.models.MachineBakedModel;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.models.MachineCasing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.SensitiveConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Fusion connection predicate: connects to a Modern Industrialization machine (including hatches) only while it
 * shows one of the given machine casings. Casing ids without a namespace use {@code modern_industrialization}.
 * <p>
 * Usage: {@code { "type": "mi_ct:machine_casing", "casings": ["steel", "extended_industrialization:steel_pipe"] }}
 */
public class MachineCasingConnectionPredicate implements SensitiveConnectionPredicate {

    public static final Serializer<MachineCasingConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MachineCasingConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("casings") || !json.get("casings").isJsonArray())
                throw new JsonParseException("Property 'casings' must be an array of machine casing ids!");
            Set<ResourceLocation> casings = new HashSet<>();
            for(JsonElement element : json.getAsJsonArray("casings")){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Entries of 'casings' must be strings!");
                String id = element.getAsString();
                casings.add(id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath("modern_industrialization", id));
            }
            return new MachineCasingConnectionPredicate(casings);
        }

        @Override
        public JsonObject serialize(MachineCasingConnectionPredicate value){
            JsonObject json = new JsonObject();
            JsonArray array = new JsonArray();
            value.casings.forEach(casing -> array.add(casing.toString()));
            json.add("casings", array);
            return json;
        }
    };

    private final Set<ResourceLocation> casings;

    public MachineCasingConnectionPredicate(Set<ResourceLocation> casings){
        this.casings = casings;
    }

    @Override
    public boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        BlockPos neighbor = ConnectionContext.neighbor();
        if(neighbor == null)
            return false;
        BlockEntity blockEntity = level.getBlockEntity(neighbor);
        if(!(blockEntity instanceof MachineBlockEntity machine))
            return false;
        MachineCasing casing = machine.getMachineModelData().casing;
        if(casing == null){
            // Not overridden (e.g. a hatch outside a multiblock): the machine shows its model's base casing
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(otherState);
            if(model instanceof MachineBakedModel machineModel)
                casing = machineModel.getBaseCasing();
        }
        return casing != null && this.casings.contains(casing.key);
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}
