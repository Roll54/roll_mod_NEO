package com.roll_54.roll_mod.minestar.hub.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.HubCommand;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The homes commands, kept for people who would rather type than open the hub.
 *
 * <p>Registered twice: under {@code /rollmod}, and at {@code /home} and friends. The short names
 * used to be FTB Essentials' and were handed back by a {@code CommandEvent} interceptor in the
 * server module; that only ever worked because FTB registered the literals for Brigadier to parse,
 * so with FTB gone they have to be registered here in their own right.
 *
 * <p>Each subtree is built by its own method and registered from both places, rather than one node
 * being redirected at the other: Brigadier's {@code redirect} forwards the rest of the input to the
 * target, which reads badly under a {@code rollmod} parent.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HomeCommands {

    /** Completes on the caller's own homes — the ones they may delete or share. */
    private static final SuggestionProvider<CommandSourceStack> OWN_HOMES = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(
                // Suggest what the name reads as: the codes are not part of what you type.
                HomeData.get(player.server).ownedBy(player.getUUID()).stream()
                        .map(home -> LegacyText.plain(home.name())),
                builder);
    };

    /** Completes on everywhere the caller can actually go: their own, plus accepted shares. */
    private static final SuggestionProvider<CommandSourceStack> REACHABLE_HOMES = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return builder.buildFuture();
        List<String> names = new ArrayList<>();
        for (PlayerHome home : HomeData.get(player.server).visibleTo(player.getUUID())) {
            if (home.canTeleport(player.getUUID())) names.add(LegacyText.plain(home.name()));
        }
        return SharedSuggestionProvider.suggest(names.stream().distinct(), builder);
    };

    private HomeCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(home())
                .then(homes())
                .then(sethome())
                .then(delhome())
                .then(invitehome()));

        dispatcher.register(home());
        dispatcher.register(homes());
        dispatcher.register(sethome());
        dispatcher.register(delhome());
        dispatcher.register(invitehome());
    }

    /**
     * Bare {@code /home} opens the tab, the way {@code /rollmod ah} and {@code /rollmod plots} do —
     * unless the player has exactly one home, in which case they mean that one and nothing else.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> home() {
        return Commands.literal("home")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    List<PlayerHome> own = HomeData.get(player.server).ownedBy(player.getUUID());
                    if (own.size() == 1) {
                        return teleport(player, LegacyText.plain(own.get(0).name()));
                    }
                    HubCommand.open(player, HubUI.indexOf("homes"));
                    return 1;
                })
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(REACHABLE_HOMES)
                        .executes(ctx -> teleport(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "name"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> homes() {
        return Commands.literal("homes")
                .executes(ctx -> list(ctx.getSource().getPlayerOrException()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> sethome() {
        return Commands.literal("sethome")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            HomeService.create(player, StringArgumentType.getString(ctx, "name"));
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> delhome() {
        return Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(OWN_HOMES)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            PlayerHome home = HomeData.get(player.server)
                                    .byOwnerAndName(player.getUUID(), name);
                            if (home == null) return notFound(player, name);
                            HomeService.delete(player, home.id());
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> invitehome() {
        return Commands.literal("invitehome")
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(OWN_HOMES)
                        // A plain word, not EntityArgument.player(): the whole point of
                        // PlayerLookup is that an offline player can be invited.
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer owner = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    PlayerHome home = HomeData.get(owner.server)
                                            .byOwnerAndName(owner.getUUID(), name);
                                    if (home == null) return notFound(owner, name);
                                    HomeService.invite(owner, home.id(),
                                            StringArgumentType.getString(ctx, "player"));
                                    return 1;
                                })));
    }

    /**
     * Own homes win over shared ones, so a player can always reach their own "base" even if someone
     * shared a "base" with them. A tie between two shares is refused rather than guessed at.
     */
    private static int teleport(ServerPlayer player, String name) {
        HomeData data = HomeData.get(player.server);
        PlayerHome own = data.byOwnerAndName(player.getUUID(), name);
        if (own != null) {
            HomeService.teleport(player, own.id());
            return 1;
        }

        List<PlayerHome> shared = data.sharedWithByName(player.getUUID(), name);
        if (shared.isEmpty()) return notFound(player, name);
        if (shared.size() > 1) {
            String owners = String.join(", ", shared.stream().map(PlayerHome::ownerName).toList());
            player.sendSystemMessage(
                    Component.translatable("msg.roll_mod.homes.ambiguous", name, owners)
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        HomeService.teleport(player, shared.get(0).id());
        return 1;
    }

    private static int list(ServerPlayer player) {
        List<PlayerHome> visible = HomeData.get(player.server).visibleTo(player.getUUID());
        if (visible.isEmpty()) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.listEmpty")
                    .withStyle(ChatFormatting.GRAY));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.listHeader",
                HomeData.get(player.server).countFor(player.getUUID()),
                HomeService.limitFor(player)));
        for (PlayerHome home : visible) {
            Component line = home.isOwner(player.getUUID())
                    ? LegacyText.display(home.name())
                    : Component.translatable(
                            home.canTeleport(player.getUUID())
                                    ? "msg.roll_mod.homes.listShared"
                                    : "msg.roll_mod.homes.listInvited",
                            LegacyText.display(home.name()), home.ownerName());
            player.sendSystemMessage(line.copy().withStyle(ChatFormatting.GRAY));
        }
        return visible.size();
    }

    private static int notFound(ServerPlayer player, String name) {
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.notFound", name)
                .withStyle(ChatFormatting.RED));
        return 0;
    }
}
