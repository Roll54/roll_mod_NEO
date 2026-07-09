package com.roll_54.roll_mod.items.cyberware;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

import static com.roll_54.roll_mod.netherstorm.StormHandler.isStormActive;

public class SulfurResistantLungsItem extends Item implements ICyberwareItem {

    private final int humanityCost;

    public SulfurResistantLungsItem(Properties properties, int humanityCost) {
        super(properties);
        this.humanityCost = humanityCost;
    }


    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.LUNGS);
    }

    @Override
    public boolean replacesOrgan() {
        return true;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of(CyberwareSlot.LUNGS);
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        if(entity != null && entity.level().dimension() == Level.NETHER && isStormActive() ){
            return 30;
        }
        return 10;
    }

    public Set<Item> incompatibleCyberware(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModItems.WETWARE_SCULKLUNGS.get(), ModItems.BODYPART_LUNGS.get(), ModItems.WETWARE_AEROSTASISGYROBLADDER.get());
    }

    public boolean requiresEnergyToFunction(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return true;
    }

    public void onInstalled(LivingEntity entity) {
        CyberwareAttributeHelper.applyModifier(entity, "sulfur_resistant_lungs");
    }

    public void onRemoved(LivingEntity entity) {
        CyberwareAttributeHelper.removeModifier(entity, "sulfur_resistant_lungs");
    }

    static {
        CyberwareAttributeHelper.registerModifierDynamicAttribute(
                "sulfur_resistant_lungs",
                AttributeRegistry.SULFUR_ARMOR.unwrapKey().orElseThrow().location(), // roll_mod:sulfur_armor
                ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "sulfur_resistant_lungs"),
                16,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
