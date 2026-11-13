package com.roll_54.roll_mod_client.compat.tei;

import com.roll_54.roll_mod.economy.minprice.ClientMinPrices;
import com.roll_54.roll_mod_client.RollModClient;
import dev.lorp.tei.client.FluidIngredients;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * A fluid's minimal price (per bucket) under its name in TEI, like items get it from the tooltip event. TEI lists
 * fluids (item list, favourites, the handset tree) as stand-in stacks and replaces their tooltip with the fluid's
 * own in its {@link ItemTooltipEvent} listener; this one runs after it and adds the line. Fluid slots of recipes
 * are covered by {@code EmiFluidMinPriceMixin} (EMI plugins; TEI keeps EMI's {@code FluidEmiStack}) and
 * {@code TeiJeiFluidMinPriceMixin} (JEI plugins).
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class TeiFluidMinPriceTooltip {

    private static final boolean TEI = ModList.get().isLoaded("tei");

    private TeiFluidMinPriceTooltip() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        if (TEI) Tei.addLine(event);
    }

    /** Kept apart so TEI's classes are only resolved with TEI loaded. */
    private static final class Tei {
        static void addLine(ItemTooltipEvent event) {
            Fluid fluid = FluidIngredients.fluidOf(event.getItemStack());
            if (fluid == null) return;
            Component line = ClientMinPrices.fluidLine(BuiltInRegistries.FLUID.getKey(fluid).toString());
            if (line == null) return;
            List<Component> tooltip = event.getToolTip();
            tooltip.add(Math.min(1, tooltip.size()), line.copy().withStyle(ChatFormatting.GOLD));
        }
    }
}
