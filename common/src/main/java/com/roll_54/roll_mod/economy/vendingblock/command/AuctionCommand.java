package com.roll_54.roll_mod.economy.vendingblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.HubCommand;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers {@code /rollmod ah}, which opens the hub on the auction-house tab —
 * {@link com.roll_54.roll_mod.economy.vendingblock.gui.auction.AuctionUI}. Viewer registration and
 * the first snapshot come from {@link HubCommand#open}, so every entry point behaves alike.
 */
public final class AuctionCommand {

  public static final ResourceLocation AH_UI_ID =
      RollMod.id("auction_house");

  private AuctionCommand() {}

  public static void register(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("rollmod")
            .then(
                Commands.literal("ah")
                    .executes(
                        ctx -> {
                          ServerPlayer player = ctx.getSource().getPlayerOrException();
                          // The auction is a tab of the hub now, not a screen of its own, and
                          // HubCommand.open registers the viewer for every entry point.
                          HubCommand.open(player, HubUI.indexOf("auction"));
                          return 1;
                        })));
  }
}
