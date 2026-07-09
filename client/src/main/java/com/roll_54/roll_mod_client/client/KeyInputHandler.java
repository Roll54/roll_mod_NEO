package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod_client.RollModClient;
import com.roll_54.roll_mod.network.packet.armor.MultiProtectingGraviChestItemPacket;
import com.roll_54.roll_mod_client.registry.KeyMappingRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

//@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyMappingRegistry.CHESTPLATE_TOGGLE_ONE.get().consumeClick()) {
            PacketDistributor.sendToServer(new MultiProtectingGraviChestItemPacket(
                    EquipmentSlot.CHEST,
                    0,
                    toggleState(0)
            ));
        }
        if (KeyMappingRegistry.CHESTPLATE_TOGGLE_TWO.get().consumeClick()) {
            PacketDistributor.sendToServer(new MultiProtectingGraviChestItemPacket(
                    EquipmentSlot.CHEST,
                    1,
                    toggleState(1)
            ));
        }
        if (KeyMappingRegistry.CHESTPLATE_TOGGLE_THREE.get().consumeClick()) {
            PacketDistributor.sendToServer(new MultiProtectingGraviChestItemPacket(
                    EquipmentSlot.CHEST,
                    2,
                    toggleState(2)
            ));
        }
    }
    private static final boolean[] stateStatus = {false, false, false};

    private static boolean toggleState(int index) {
        if (index >= 0 && index < stateStatus.length) {
            stateStatus[index] = !stateStatus[index];
            return stateStatus[index];
        }
        return false;
    }
}