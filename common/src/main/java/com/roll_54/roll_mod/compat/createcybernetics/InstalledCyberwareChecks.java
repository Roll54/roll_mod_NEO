package com.roll_54.roll_mod.compat.createcybernetics;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Server-safe "is this implant installed and working" check, the same test Create Cybernetics' HUD
 * uses: installed, enabled in its slot, and powered when the implant needs energy to function.
 */
public final class InstalledCyberwareChecks {

    private InstalledCyberwareChecks() {}

    public static boolean hasWorking(Player player, Item item) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;
        for (Map.Entry<CyberwareSlot, InstalledCyberware[]> entry : data.getAll().entrySet()) {
            InstalledCyberware[] installed = entry.getValue();
            if (installed == null) continue;
            for (int i = 0; i < installed.length; i++) {
                InstalledCyberware cw = installed[i];
                if (cw == null) continue;
                ItemStack stack = cw.getItem();
                if (stack == null || stack.isEmpty() || !stack.is(item)) continue;
                if (!data.isEnabled(entry.getKey(), i)) continue;
                if (stack.getItem() instanceof ICyberwareItem ci
                        && ci.requiresEnergyToFunction(player, stack, entry.getKey())
                        && !cw.isPowered()) continue;
                return true;
            }
        }
        return false;
    }
}
