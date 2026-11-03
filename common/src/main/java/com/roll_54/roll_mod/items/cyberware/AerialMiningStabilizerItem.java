package com.roll_54.roll_mod.items.cyberware;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import net.minecraft.world.item.Item;

import java.util.Set;

/**
 * An organ implant that keeps a player's footing steady in the air: while it is installed and
 * enabled, breaking blocks with anything is not slowed down when the player is off the ground
 * (vanilla divides mining speed by 5 then). The effect itself lives in {@code FlightMiningSpeed},
 * which checks for this implant alongside creative-style flight. Needs no energy.
 */
public class AerialMiningStabilizerItem extends Item implements ICyberwareItem {

    private final int humanityCost;

    public AerialMiningStabilizerItem(Properties properties, int humanityCost) {
        super(properties);
        this.humanityCost = humanityCost;
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.ORGANS);
    }

    /** An upgrade alongside the organs, not a replacement for one. */
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
        return humanityCost;
    }
}
