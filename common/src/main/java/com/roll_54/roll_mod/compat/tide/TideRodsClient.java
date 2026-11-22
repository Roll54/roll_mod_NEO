package com.roll_54.roll_mod.compat.tide;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The rods' "cast" model property: the line is out while the holder has a hook in the water, as for vanilla's rod (whose
 * property is registered for {@code minecraft:fishing_rod} alone). Our models switch on {@code cast}.
 */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class TideRodsClient {

    private TideRodsClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("tide")) return;
        event.enqueueWork(() -> {
            ResourceLocation cast = ResourceLocation.withDefaultNamespace("cast");
            for (var rod : TideRods.all()) {
                ItemProperties.register(rod.get(), cast, (stack, level, holder, seed) -> {
                    if (!(holder instanceof Player player)) return 0;
                    boolean main = holder.getMainHandItem() == stack;
                    boolean off = holder.getOffhandItem() == stack && !(holder.getMainHandItem().getItem() instanceof FishingRodItem);
                    return (main || off) && player.fishing != null ? 1 : 0;
                });
            }
        });
    }
}
