package com.roll_54.roll_mod.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.armor.TwotypedActivateItemPacket;
import com.roll_54.roll_mod.registry.KeyMappingRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

//@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyMappingRegistry.CHESTPLATE_TOGGLE_ONE.get().consumeClick()) {
            PacketDistributor.sendToServer(new TwotypedActivateItemPacket(
                    EquipmentSlot.CHEST,
                    1,
                    changeType()
            ));
        }
        if (KeyMappingRegistry.CHESTPLATE_TOGGLE_TWO.get().consumeClick()) {
            PacketDistributor.sendToServer(new TwotypedActivateItemPacket(
                    EquipmentSlot.CHEST,
                    2,
                    changeType()
            ));
        }
        if (KeyMappingRegistry.CHESTPLATE_TOGGLE_THREE.get().consumeClick()) {
            PacketDistributor.sendToServer(new TwotypedActivateItemPacket(
                    EquipmentSlot.CHEST,
                    3,
                    changeType()
            ));
        }
    }
    private static boolean flightEnabled = true;

    private static boolean changeType() {

        flightEnabled = !flightEnabled;
        return flightEnabled;
    }
}