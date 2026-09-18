package com.roll_54.roll_mod.minestar.hub.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.RollMod;
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

import java.util.List;

/**
 * The warp moderation commands: {@code /rollmod admin warps approve} for staff, and
 * {@code /rollmod reportWarp} for everyone else.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class WarpModerationCommand {

    /** Completes on the names of warps that actually exist, so nobody has to remember them. */
    private static final SuggestionProvider<CommandSourceStack> WARP_NAMES = (ctx, builder) -> {
        if (ctx.getSource().getServer() == null) return builder.buildFuture();
        // Suggest what the name reads as: the colour codes are not part of what you type, and
        // WarpData.byName matches on the same stripped form.
        return SharedSuggestionProvider.suggest(
                WarpData.get(ctx.getSource().getServer()).all().stream()
                        .map(warp -> LegacyText.plain(warp.name())).distinct(),
                builder);
    };

    private WarpModerationCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .then(Commands.literal("warps")
                                .then(Commands.literal("approve")
                                        // Operator-only: vouching for a warp is what makes it safe
                                        // to use, so it must not be delegable by accident.
                                        .requires(src -> src.hasPermission(2))
                                        .then(Commands.argument("warp", StringArgumentType.string())
                                                .suggests(WARP_NAMES)
                                                .then(Commands.argument("state", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider
                                                                .suggest(List.of("true", "false", "admin"), builder))
                                                        .executes(ctx -> approve(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "warp"),
                                                                StringArgumentType.getString(ctx, "state"))))))))
                .then(Commands.literal("reportWarp")
                        .then(Commands.argument("warp", StringArgumentType.string())
                                .suggests(WARP_NAMES)
                                .executes(ctx -> report(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "warp"))))));
    }

    private static int approve(CommandSourceStack source, String name, String state)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer moderator = source.getPlayerOrException();
        Warp warp = resolve(source, name);
        if (warp == null) return 0;

        WarpApproval approval = WarpApproval.byCommandArgument(state);
        WarpService.setApproval(moderator, warp, approval);
        return 1;
    }

    private static int report(CommandSourceStack source, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer reporter = source.getPlayerOrException();
        Warp warp = resolve(source, name);
        if (warp == null) return 0;

        WarpService.report(reporter, warp);
        return 1;
    }

    /**
     * Finds the one warp with this name. Names are not unique, so an ambiguous one is refused with
     * the owners listed rather than acted on arbitrarily — moderating the wrong player's warp would
     * be worse than making someone disambiguate.
     */
    private static Warp resolve(CommandSourceStack source, String name) {
        List<Warp> matches = WarpData.get(source.getServer()).byName(name);
        if (matches.isEmpty()) {
            source.sendFailure(Component.translatable("msg.roll_mod.warp.notFound", name));
            return null;
        }
        if (matches.size() > 1) {
            String owners = matches.stream().map(Warp::ownerName).distinct()
                    .reduce((a, b) -> a + ", " + b).orElse("");
            source.sendFailure(Component.translatable("msg.roll_mod.warp.ambiguous", name, owners)
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        return matches.getFirst();
    }
}
