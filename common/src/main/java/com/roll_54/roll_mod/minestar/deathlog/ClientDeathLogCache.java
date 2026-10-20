package com.roll_54.roll_mod.minestar.deathlog;

import com.roll_54.roll_mod.network.packet.DeathLogPackets;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The death log as this client last received it — only ever filled for those who may read it, and
 * only on request, since a death carries a whole inventory.
 */
public final class ClientDeathLogCache {

    public static volatile List<DeathLogStore.Victim> VICTIMS = List.of();

    /** Whose deaths {@link #DEATHS} are. */
    public static volatile @Nullable UUID DEATHS_OF;
    public static volatile List<DeathLogPackets.Summary> DEATHS = List.of();

    /** Which death the stacks below are. */
    public static volatile @Nullable UUID DETAIL_ID;
    public static volatile Map<Integer, ItemStack> INVENTORY = Map.of();
    public static volatile List<ItemStack> CURIOS = List.of();
    public static volatile List<ItemStack> CYBERWARE = List.of();

    /** Bumped on every arrival. */
    public static volatile int VERSION;

    private ClientDeathLogCache() {}

    public static void clear() {
        VICTIMS = List.of();
        DEATHS_OF = null;
        DEATHS = List.of();
        DETAIL_ID = null;
        INVENTORY = Map.of();
        CURIOS = List.of();
        CYBERWARE = List.of();
        VERSION++;
    }
}
