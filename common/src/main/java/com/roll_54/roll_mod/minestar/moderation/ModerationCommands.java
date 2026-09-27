package com.roll_54.roll_mod.minestar.moderation;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.PlayerLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.UUID;

/**
 * {@code /rollmod admin warn|ban|unban|whitelist|rules}.
 *
 * <p>The moderation tab is the comfortable way to do all of this, but it must not be the only way.
 * A GUI needs a client that can open it, and the moment worth reaching for a ban is often the
 * moment the server is too busy to render one — the same reason {@code /mute} still exists beside
 * the mute store. These also make the whole module testable from a server console with no client
 * attached at all.
 *
 * <p>Gated on {@code hasPermission(2)} at the Brigadier level, which is what every other admin
 * command in this mod does, and re-checked against {@link ModerationPermissions} for the actions
 * that want the stricter node.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ModerationCommands {

    /** Rule ids, so a moderator does not have to remember which number is which. */
    private static final SuggestionProvider<CommandSourceStack> RULE_IDS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(RulesStore.ids(), builder);

    private ModerationCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Gate each unique child, not the shared "admin" literal: Brigadier merges
        // same-named literals keeping only the first-registered node's requires, so a gate
        // on "admin" is dropped whenever another class's ungated "rollmod admin" registers
        // first (see the comments in ItemSkinCommand and DailyTasksCommand).
        event.getDispatcher().register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .then(warn().requires(source -> source.hasPermission(2)))
                        .then(ban().requires(source -> source.hasPermission(2)))
                        .then(unban().requires(source -> source.hasPermission(2)))
                        .then(whitelist().requires(source -> source.hasPermission(2)))
                        .then(rules().requires(source -> source.hasPermission(2)))));
    }

    /* --------------------------------------------- warn --------------------------------------------- */

    private static LiteralArgumentBuilder<CommandSourceStack> warn() {
        return Commands.literal("warn")
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearWarns(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> warn(ctx, "", ""))
                        .then(Commands.argument("rule", StringArgumentType.word())
                                .suggests(RULE_IDS)
                                .executes(ctx -> warn(ctx, StringArgumentType.getString(ctx, "rule"), ""))
                                .then(Commands.argument("note", StringArgumentType.greedyString())
                                        .executes(ctx -> warn(ctx,
                                                StringArgumentType.getString(ctx, "rule"),
                                                StringArgumentType.getString(ctx, "note"))))));
    }

    private static int warn(CommandContext<CommandSourceStack> ctx, String rule, String note)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer moderator = source.getPlayer();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        if (moderator == null) {
            // Console has no ServerPlayer, and warn() wants one to credit. Fall back to the target
            // itself only for the count; the audit line still says who typed it.
            source.sendFailure(Component.translatable("msg.roll_mod.moderation.needPlayer"));
            return 0;
        }

        int count = ModerationService.warn(moderator, target, rule, note);
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.moderation.warnedBy",
                target.getGameProfile().getName(), count), true);
        if (count >= ModerationService.WARNS_BEFORE_BAN) {
            source.sendSuccess(() -> Component.translatable("msg.roll_mod.moderation.autoBanned",
                    target.getGameProfile().getName()).withStyle(ChatFormatting.GOLD), true);
        }
        return count;
    }

    private static int clearWarns(CommandSourceStack source, ServerPlayer target) {
        int cleared = WarnStore.clear(target.getUUID());
        ModerationStatus.sendTo(target);
        ModerationLog.append(source.getTextName(), "cleared warnings for",
                target.getGameProfile().getName(), cleared + " removed");
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.moderation.warnsCleared",
                target.getGameProfile().getName(), cleared), true);
        return cleared;
    }

    /* ---------------------------------------------- ban --------------------------------------------- */

    private static LiteralArgumentBuilder<CommandSourceStack> ban() {
        return Commands.literal("ban")
                .requires(ModerationCommands::mayBan)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                ctx.getSource().getOnlinePlayerNames(), builder))
                        .executes(ctx -> ban(ctx, 0, "", ""))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(0))
                                .executes(ctx -> ban(ctx,
                                        IntegerArgumentType.getInteger(ctx, "minutes"), "", ""))
                                .then(Commands.argument("rule", StringArgumentType.word())
                                        .suggests(RULE_IDS)
                                        .executes(ctx -> ban(ctx,
                                                IntegerArgumentType.getInteger(ctx, "minutes"),
                                                StringArgumentType.getString(ctx, "rule"), ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(ctx -> ban(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "minutes"),
                                                        StringArgumentType.getString(ctx, "rule"),
                                                        StringArgumentType.getString(ctx, "reason")))))));
    }

    /** {@code minutes = 0} means permanent, which is what a ban with no duration should be. */
    private static int ban(CommandContext<CommandSourceStack> ctx, int minutes, String rule,
                           String reason) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "target");
        long millis = minutes <= 0 ? 0L : minutes * 60_000L;

        // Offline-capable on purpose: the player who earns a ban is very often the one who just
        // logged off. resolve() answers on the server thread, so the store write is safe.
        PlayerLookup.resolve(source.getServer(), name, profile -> {
            if (profile == null) {
                source.sendFailure(Component.translatable("msg.roll_mod.moderation.noSuchPlayer", name));
                return;
            }
            ModerationService.ban(source.getServer(), source.getTextName(), profile.id(),
                    profile.name(), millis, rule, reason);
            String duration = millis == 0L
                    ? Component.translatable("msg.roll_mod.moderation.permanent").getString()
                    : Durations.format(millis);
            source.sendSuccess(() -> Component.translatable("msg.roll_mod.moderation.bannedBy",
                    profile.name(), duration), true);
        });
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> unban() {
        return Commands.literal("unban")
                .requires(ModerationCommands::mayBan)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                BanStore.active().values().stream().map(BanStore.Ban::name)
                                        .filter(n -> !n.isBlank()).toList(), builder))
                        .executes(ctx -> unban(ctx.getSource(),
                                StringArgumentType.getString(ctx, "target"))));
    }

    private static int unban(CommandSourceStack source, String name) {
        // Matched against the stored name first, so a ban can be lifted without Mojang being
        // reachable — which is exactly when someone is most likely to need lifting.
        for (Map.Entry<UUID, BanStore.Ban> entry : BanStore.active().entrySet()) {
            if (entry.getValue().name().equalsIgnoreCase(name)) {
                BanStore.unban(entry.getKey());
                ModerationLog.append(source.getTextName(), "unbanned", entry.getValue().name(), "");
                source.sendSuccess(() -> Component.translatable(
                        "msg.roll_mod.moderation.unbanned", entry.getValue().name()), true);
                return 1;
            }
        }
        source.sendFailure(Component.translatable("msg.roll_mod.moderation.notBanned", name));
        return 0;
    }

    /* ------------------------------------------- whitelist ------------------------------------------ */

    private static LiteralArgumentBuilder<CommandSourceStack> whitelist() {
        return Commands.literal("whitelist")
                .requires(ModerationCommands::mayBan)
                .executes(ctx -> whitelistStatus(ctx.getSource()))
                .then(Commands.literal("on")
                        .executes(ctx -> setWhitelist(ctx.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(ctx -> setWhitelist(ctx.getSource(), false)))
                .then(Commands.literal("message")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> setMessage(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "text")))));
    }

    private static int whitelistStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable(WhitelistStore.enabled()
                ? "msg.roll_mod.moderation.whitelist.on"
                : "msg.roll_mod.moderation.whitelist.off"), false);
        return 1;
    }

    private static int setWhitelist(CommandSourceStack source, boolean enabled) {
        ModerationService.setWhitelist(source.getServer(), source.getTextName(), enabled);
        return whitelistStatus(source);
    }

    private static int setMessage(CommandSourceStack source, String text) {
        WhitelistStore.setKickMessage(text);
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.moderation.whitelist.message",
                ModerationService.whitelistScreen()), false);
        return 1;
    }

    /* --------------------------------------------- rules -------------------------------------------- */

    /** Lists the rules a punishment can cite, with the ids the commands take. */
    private static LiteralArgumentBuilder<CommandSourceStack> rules() {
        return Commands.literal("rules").executes(ctx -> {
            var all = RulesStore.all();
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "msg.roll_mod.moderation.rules.count", all.size()), false);
            for (Rule rule : all) {
                ctx.getSource().sendSuccess(
                        () -> rule.label().withStyle(ChatFormatting.GRAY), false);
            }
            return all.size();
        });
    }

    /**
     * Banning and closing the server want the stricter node. The console has no player, and a
     * console that cannot ban would be a strange kind of console, so it passes.
     */
    private static boolean mayBan(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? source.hasPermission(2) : ModerationPermissions.canBan(player);
    }
}
