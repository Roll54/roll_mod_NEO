package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod_client.RollModClient;
import com.roll_54.roll_mod_client.compat.emi.tree.RecipeTreeScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * {@code /recipetree} — opens the {@link RecipeTreeScreen}, built from EMI's recipes.
 * Client-side; it never talks to the server.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class RecipeTreeCommand {

    private RecipeTreeCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("recipetree").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (!ModList.get().isLoaded("emi")) {
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.translatable("gui.roll_mod.recipe_tree.requires_emi")
                            .withStyle(ChatFormatting.RED));
                }
                return 0;
            }
            // After the chat screen has closed, or closing it would close the tree too.
            mc.tell(RecipeTreeScreen::open);
            return 1;
        }));
    }
}
