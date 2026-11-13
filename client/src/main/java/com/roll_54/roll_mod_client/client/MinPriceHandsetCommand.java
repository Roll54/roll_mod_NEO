package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.economy.minprice.SaveMinPriceRecipesPacket;
import com.roll_54.roll_mod_client.RollModClient;
import com.roll_54.roll_mod_client.compat.emi.tree.RecipeTreeScreen;
import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreeScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * {@code /rollmod admin minprice handset} — opens the recipe tree (with the held item, if any): pick the
 * recipes a minimal price is computed from and save them to the server. Under TEI that is
 * {@link TeiRecipeTreeScreen}, under EMI {@link RecipeTreeScreen}.
 *
 * <p>Client-side, because it opens a screen; for operators only, as the save is checked again on the
 * server. Any other {@code /rollmod ...} is unknown to the client dispatcher, so NeoForge still forwards
 * it to the server.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class MinPriceHandsetCommand {

    private MinPriceHandsetCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(SaveMinPriceRecipesPacket.PERMISSION_LEVEL))
                        .then(Commands.literal("minprice")
                                .then(Commands.literal("handset").executes(ctx -> open())))));
    }

    private static int open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        ItemStack held = mc.player.getMainHandItem().copy();
        // TEI first: it replaces EMI in the pack (the two are incompatible, so at most one is loaded). Each tree
        // class is only named inside the branch of the viewer that is loaded.
        if (ModList.get().isLoaded("tei")) {
            // After the chat screen has closed, or closing it would close the tree too.
            mc.tell(() -> TeiRecipeTreeScreen.open(held));
            return 1;
        }
        if (ModList.get().isLoaded("emi")) {
            mc.tell(() -> RecipeTreeScreen.open(held));
            return 1;
        }
        mc.player.sendSystemMessage(Component.translatable("gui.roll_mod.recipe_tree.requires_viewer")
                .withStyle(ChatFormatting.RED));
        return 0;
    }
}
