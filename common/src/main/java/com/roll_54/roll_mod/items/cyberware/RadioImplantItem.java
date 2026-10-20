package com.roll_54.roll_mod.items.cyberware;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * A radio in the head: while installed, the player gets the hub's Radio tab, where they tune into a
 * station and choose whether they hear it, and whether players around them do. The item itself does
 * nothing but be installed; {@code minestar/radio/RadioService} watches for it.
 */
public class RadioImplantItem extends Item implements ICyberwareItem {

    public static final int HUMANITY_COST = 3;
    public static final int ENERGY_PER_TICK = 1;

    public RadioImplantItem(Properties properties) {
        super(properties);
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.BRAIN);
    }

    @Override
    public boolean replacesOrgan() {
        return false;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of();
    }

    @Override
    public int getHumanityCost() {
        return HUMANITY_COST;
    }

    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return ENERGY_PER_TICK;
    }
}
