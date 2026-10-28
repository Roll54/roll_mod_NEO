package com.roll_54.roll_mod_client.compat.fusion;

import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Client setup for MI connected textures. Call {@link #init()} from the mod constructor on the client
 * (Fusion only accepts predicates before models get loaded).
 */
public final class FusionCompatClient {

    public static void init(){
        if(!ModList.get().isLoaded("fusion") || !ModList.get().isLoaded("modern_industrialization"))
            return;
        FusionConnectionPredicateRegistry.registerConnectionPredicate(
            ResourceLocation.fromNamespaceAndPath("mi_ct", "is_mi_machine"),
            IsMiMachineConnectionPredicate.SERIALIZER
        );
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(
            ResourceLocation.fromNamespaceAndPath("mi_ct", "is_framed_block"),
            IsFramedBlockModelPredicate.SERIALIZER
        );
    }

    private FusionCompatClient(){
    }
}
