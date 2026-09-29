package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod_client.RollModClient;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod.network.packet.armor.MultiProtectingGraviChestItemPacket;
import com.roll_54.roll_mod.network.packet.drill.OpenDrillConfigPacket;
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
        if (KeyMappingRegistry.HUB.get().consumeClick()) {
            // The hub is a LdLib2 container UI, so the server has to open it — and the tab has to
            // travel with the request, because only the client knows which one the hub was left on.
            PacketDistributor.sendToServer(new OpenHubPacket(HubUI.lastTab()));
        }
        if (KeyMappingRegistry.DRILL_CONFIG.get().consumeClick()) {
            // Cheap pre-check only; the server re-checks the hand before opening anything.
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.getMainHandItem().getItem() instanceof ModularDrillItem) {
                PacketDistributor.sendToServer(new OpenDrillConfigPacket());
            }
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