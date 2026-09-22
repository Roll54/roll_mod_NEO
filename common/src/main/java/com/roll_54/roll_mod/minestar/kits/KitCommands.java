package com.roll_54.roll_mod.minestar.kits;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * {@code /kit …} and {@code /rollmod kit …}.
 *
 * <p>Both names are registered here as real commands, not as a redirect onto somebody else's
 * literal: the short names used to be FTB Essentials' and were reachable only while FTB was there
 * to register them.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class KitCommands {

    /** Only the kits the caller may actually claim, so the list is not a directory of what they lack. */
    private static final SuggestionProvider<CommandSourceStack> USABLE = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(
                KitService.visibleTo(player).stream().map(Kit::name), builder);
    };

    /** Every kit, for the operator commands that may act on one the operator cannot claim. */
    private static final SuggestionProvider<CommandSourceStack> ALL = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    KitStore.all(ctx.getSource().getServer()).stream().map(Kit::name), builder);

    private KitCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod").then(tree("kit")));
        dispatcher.register(tree("kit"));
        dispatcher.register(tree("kits"));
    }

    /**
     * The command tree, built fresh per registration.
     *
     * <p>A Brigadier {@code redirect} would forward the remaining input to the target node, which
     * reads badly under a {@code rollmod} parent; the tree is small enough to build twice.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> tree(String literal) {
        return Commands.literal(literal)
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("create")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("cooldownTicks",
                                                IntegerArgumentType.integer(0))
                                        .executes(ctx -> create(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                IntegerArgumentType.getInteger(ctx, "cooldownTicks"))))))
                .then(Commands.literal("delete")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(ALL)
                                .executes(ctx -> delete(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(USABLE)
                        .executes(ctx -> claim(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name"))));
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<Kit> kits = KitService.visibleTo(player);
        if (kits.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("msg.roll_mod.kit.none"), false);
            return 0;
        }
        for (Kit kit : kits) {
            long remaining = KitService.remainingMillis(player, kit);
            source.sendSuccess(() -> remaining > 0
                    ? Component.translatable("msg.roll_mod.kit.listWaiting", kit.name(),
                            Durations.format(remaining)).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("msg.roll_mod.kit.listReady", kit.name()), false);
        }
        return kits.size();
    }

    private static int claim(CommandSourceStack source, String name) throws CommandSyntaxException {
        return KitService.claim(source.getPlayerOrException(), name) ? 1 : 0;
    }

    private static int create(CommandSourceStack source, String name, int cooldownTicks)
            throws CommandSyntaxException {
        return KitService.create(source.getPlayerOrException(), name, cooldownTicks) ? 1 : 0;
    }

    private static int delete(CommandSourceStack source, String name) throws CommandSyntaxException {
        return KitService.delete(source.getPlayerOrException(), name) ? 1 : 0;
    }

    private static int reload(CommandSourceStack source) {
        KitStore.reload();
        KitCooldowns.reload();
        KitViewers.resync(source.getServer());
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.kit.reloaded",
                KitStore.all(source.getServer()).size()), true);
        return 1;
    }
}
