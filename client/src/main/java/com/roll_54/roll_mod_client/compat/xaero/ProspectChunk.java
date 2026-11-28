package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.prospecting.ClientProspects;
import com.roll_54.roll_mod.prospecting.Discovery;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** One chunk where the player prospected ores, as both maps draw it. */
@OnlyIn(Dist.CLIENT)
public record ProspectChunk(int x, int z, List<Discovery> ores) {

    /** The prospected chunks of the player's dimension, or none while hidden. */
    static Iterator<ProspectChunk> current() {
        if (!ProspectMapState.visible()) return java.util.Collections.emptyIterator();
        return all();
    }

    /** The prospected chunks of the player's dimension, shown or not. */
    static Iterator<ProspectChunk> all() {
        var chunks = ClientProspects.FINDS.get(ProspectMapState.dimension());
        if (chunks == null) return java.util.Collections.emptyIterator();
        List<ProspectChunk> out = new ArrayList<>(chunks.size());
        chunks.forEach((key, ores) -> {
            if (ores.isEmpty()) return;
            out.add(new ProspectChunk(ChunkPos.getX(key), ChunkPos.getZ(key), List.copyOf(ores.values())));
        });
        return out.iterator();
    }
}
