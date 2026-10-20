package com.roll_54.roll_mod.compat.createcybernetics;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.surgery.DefaultOrgans;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A player's whole Create Cybernetics state as one tag, for the death log: taken before the mod's
 * own death handler swaps implants for default organs and drops them, and written back on restore.
 *
 * <p>Next to the tag, {@link #items} reads the same state as plain stacks — every implant that is
 * not a default organ, and whatever the implants carry (chipware, cyberdeck, spinal injector, arm
 * cannon, heat engine) — which is what a moderator sees, and what is handed over as items when the
 * tag cannot be written back.
 */
public final class CyberwareSnapshots {

    /**
     * Upper bound for the sub-inventories whose size the mod does not publish. Their getters answer
     * {@code EMPTY} past the end, so reading further than needed costs nothing.
     */
    private static final int MAX_SUB_SLOTS = 27;

    private CyberwareSnapshots() {}

    public static CompoundTag capture(ServerPlayer player) {
        return player.getData(ModAttachments.CYBERWARE).serializeNBT(player.registryAccess()).copy();
    }

    /** The stacks inside a tag from {@link #capture}; empty if the tag is null or unreadable. */
    public static List<ItemStack> items(@Nullable CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null || tag.isEmpty()) return List.of();
        PlayerCyberwareData data = new PlayerCyberwareData();
        try {
            data.deserializeNBT(tag.copy(), registries);
        } catch (RuntimeException e) {
            return List.of();
        }
        return items(data);
    }

    private static List<ItemStack> items(PlayerCyberwareData data) {
        List<ItemStack> out = new ArrayList<>();
        for (Map.Entry<CyberwareSlot, InstalledCyberware[]> entry : data.getAll().entrySet()) {
            InstalledCyberware[] row = entry.getValue();
            if (row == null) continue;
            for (InstalledCyberware installed : row) {
                if (installed == null) continue;
                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty()) continue;
                // A default organ is the body, not loot: nobody lost a human heart worth returning.
                if (DefaultOrgans.isOrganForSlot(stack, entry.getKey())) continue;
                out.add(stack.copy());
            }
        }
        for (int i = 0; i < PlayerCyberwareData.CHIPWARE_SLOT_COUNT; i++) add(out, data.getChipwareStack(i));
        for (int i = 0; i < PlayerCyberwareData.CYBERDECK_SLOT_COUNT; i++) add(out, data.getCyberdeckStack(i));
        for (int i = 0; i < PlayerCyberwareData.HEAT_ENGINE_SLOT_COUNT; i++) add(out, data.getHeatEngineStack(i));
        for (int i = 0; i < MAX_SUB_SLOTS; i++) add(out, data.getSpinalInjectorStack(i));
        for (int i = 0; i < MAX_SUB_SLOTS; i++) add(out, data.getArmCannonStack(i));
        return out;
    }

    private static void add(List<ItemStack> out, @Nullable ItemStack stack) {
        if (stack != null && !stack.isEmpty()) out.add(stack.copy());
    }

    /**
     * Writes {@code tag} back onto the player, but only while the body holds nothing but default
     * organs and empty sub-inventories — the state the mod leaves a player in after death. Anything
     * else installed since would be overwritten and lost, so then this refuses and the caller hands
     * the implants over as items instead.
     */
    public static boolean restore(ServerPlayer player, CompoundTag tag) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (!items(data).isEmpty()) return false;
        try {
            data.deserializeNBT(tag.copy(), player.registryAccess());
        } catch (RuntimeException e) {
            return false;
        }
        // deserializeNBT only loads state; the implants' own hooks (attribute modifiers and the like)
        // run on install, so run them as a surgery would.
        for (Map.Entry<CyberwareSlot, InstalledCyberware[]> entry : data.getAll().entrySet()) {
            InstalledCyberware[] row = entry.getValue();
            if (row == null) continue;
            for (InstalledCyberware installed : row) {
                if (installed == null) continue;
                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty() || DefaultOrgans.isOrganForSlot(stack, entry.getKey())) continue;
                if (stack.getItem() instanceof ICyberwareItem cyberware) cyberware.onInstalled(player, stack);
            }
        }
        data.recomputeHumanityBaseFromInstalled(player);
        data.setDirty();
        ModAttachments.syncCyberware(player);
        return true;
    }
}
