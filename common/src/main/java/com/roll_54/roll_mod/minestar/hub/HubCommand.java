package com.roll_54.roll_mod.minestar.hub;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.mojang.brigadier.CommandDispatcher;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.plot.PlotViewers;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionManager;
import com.roll_54.roll_mod.minestar.hub.home.HomeViewers;
import com.roll_54.roll_mod.minestar.kits.KitViewers;
import com.roll_54.roll_mod.minestar.op.OperatorViewers;
import com.roll_54.roll_mod.minestar.tpa.TpaViewers;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.minestar.hub.warp.WarpViewers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** {@code /rollmod hub} — opens the hub without going through the inventory button. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HubCommand {

    private HubCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("hub").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    open(player);
                    return 1;
                })));
    }

    /**
     * Which tab each player last asked for. The hub is opened on the server but tabs are selected on
     * the client, so the request is parked here and read back through a sync binding as the screen
     * builds. Cleared on read: a later open with no tab means "home".
     */
    private static final Map<UUID, Integer> REQUESTED_TAB = new ConcurrentHashMap<>();

    /** Shared by the command, the packet and every screen command, so all of them behave alike. */
    public static void open(ServerPlayer player) {
        open(player, 0);
    }

    /** Opens the hub on a particular tab — see {@link HubUI#indexOf}. */
    public static void open(ServerPlayer player, int tab) {
        REQUESTED_TAB.put(player.getUUID(), tab);
        // Refreshes the client-side balance cache the home tab reads from.
        CurrencyService.get(player, CurrencyType.MAIN);
        // Registers for updates and sends the first snapshot, so each tab is populated before the
        // player ever clicks it. This has to happen here rather than on tab selection: selecting a
        // tab is a client-side click the server never sees — see HubUI's class javadoc.
        WarpViewers.add(player);
        KitViewers.add(player);
        TpaViewers.add(player);
        OperatorViewers.add(player);
        HomeViewers.add(player);
        AuctionManager.addViewer(player);
        PlotViewers.add(player);
        PlayerUIMenuType.openUI(player, HubUI.UI_ID);
    }

    /** The tab this player asked for, consumed so it does not stick to the next open. */
    public static int requestedTab(ServerPlayer player) {
        Integer tab = REQUESTED_TAB.get(player.getUUID());
        return tab == null ? 0 : tab;
    }

    public static void clearRequestedTab(ServerPlayer player) {
        REQUESTED_TAB.remove(player.getUUID());
    }
}
