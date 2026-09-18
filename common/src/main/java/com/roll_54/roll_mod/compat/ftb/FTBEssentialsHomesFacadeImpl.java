package com.roll_54.roll_mod.compat.ftb;

import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.SavedTeleportManager;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The FTB Essentials half of {@link HomesFacade}. Never loaded unless {@code ftbessentials} is
 * present — see {@link HomesFacade.Holder}.
 */
public final class FTBEssentialsHomesFacadeImpl implements HomesFacade {

    @Override
    public List<Legacy> homesOf(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player)
                .map(data -> {
                    List<Legacy> homes = new ArrayList<>();
                    data.homeManager().destinations().forEach(entry -> {
                        Legacy legacy = toLegacy(entry);
                        if (legacy != null) homes.add(legacy);
                    });
                    return homes;
                })
                .orElseGet(List::of);
    }

    @Override
    public boolean forget(ServerPlayer player, String name) {
        return FTBEPlayerData.getOrCreate(player)
                .map(data -> data.homeManager().deleteDestination(name))
                .orElse(false);
    }

    /**
     * {@code null} when the home names a dimension that will not parse, which is the one field that
     * can be nonsense: FTB writes it as a plain string and the world it named may be long gone.
     *
     * <p>The dimension has to come out of {@link TeleportPos#write()} because the field itself is
     * private and FTB exposes no getter for it — {@code getPos()} covers the coordinates and the two
     * rotations are public fields, but the dimension is only reachable through the tag.
     */
    private static Legacy toLegacy(SavedTeleportManager.DestinationEntry entry) {
        TeleportPos pos = entry.destination();
        if (pos == null) return null;

        ResourceLocation dimension = ResourceLocation.tryParse(pos.write().getString("dim"));
        if (dimension == null) return null;

        BlockPos block = pos.getPos();
        // Block centre on the horizontal axes: a home pinned to a corner drops the player against
        // the neighbouring block's face, which is not where they stood when they set it.
        return new Legacy(entry.name(), dimension,
                block.getX() + 0.5, block.getY(), block.getZ() + 0.5,
                pos.yRot == null ? 0f : pos.yRot,
                pos.xRot == null ? 0f : pos.xRot);
    }
}
