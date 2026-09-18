package com.roll_54.roll_mod.economy.plot.command;

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
 * Registers {@code /rollmod plots}, which opens the hub on the plot-map tab —
 * {@link com.roll_54.roll_mod.economy.plot.gui.PlotPurchaseUI}. Viewer registration and the first
 * snapshot come from {@link HubCommand#open}, so every entry point behaves alike. Mirrors
 * {@code AuctionCommand}.
 */
public final class PlotCommand {

  public static final ResourceLocation PLOT_UI_ID =
      RollMod.id("plot_purchase");

  private PlotCommand() {}

  public static void register(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("rollmod")
            .then(
                Commands.literal("plots")
                    .executes(
                        ctx -> {
                          ServerPlayer player = ctx.getSource().getPlayerOrException();
                          // The plot map is a tab of the hub now, not a screen of its own, and
                          // HubCommand.open registers the viewer for every entry point.
                          HubCommand.open(player, HubUI.indexOf("plots"));
                          return 1;
                        })));
  }
}
