package com.roll_54.roll_mod.minestar.letters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.ItemMentions;
import com.roll_54.roll_mod.util.LegacyText;
import com.roll_54.roll_mod.util.PlayerLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@code /rollmod letter} — personal letters, written from a command. The compose form in the hub
 * only writes letters for everyone; a letter for one player is made here, or from Java through
 * {@link LetterService#sendPersonal}.
 *
 * <pre>
 * /rollmod letter send        &lt;player&gt; &lt;title&gt; &lt;body…&gt;
 * /rollmod letter senditem    &lt;player&gt; &lt;title&gt; &lt;body…&gt;            the stack in your main hand
 * /rollmod letter sendmoney   &lt;player&gt; &lt;amount&gt; &lt;title&gt; &lt;body…&gt;   starcoins
 * /rollmod letter sendcommand &lt;player&gt; &lt;command&gt; &lt;title&gt; &lt;body…&gt;  run as the reader on accept
 * /rollmod letter json        &lt;player&gt; &lt;json…&gt;
 * </pre>
 *
 * <p>The title is one word or a quoted string; the body is the rest of the line, with {@code &}
 * colour codes and {@code \n} for a line break. The player may be offline — they find it at their
 * next login.
 *
 * <p>The JSON form takes {@code {"title", "body", "icon"?, "author"?, "lifetime"?, "rewards"?: [...]}},
 * the rewards in the format {@code letters.json} stores them ({@link LetterReward#load}), e.g.
 * {@code {"type":"command","command":"give %player% diamond","label":"Diamond","repeat":2,"console":true}}.
 * {@code lifetime} is a duration such as {@code 7d}; left out, the letter never expires.
 *
 * <p>Writing needs the letters permission, as the compose form does; a command reward also needs
 * level 4, for the reason {@code LetterService.commandRewardsAllowed} gives. The console has both.
 * An {@code <item:…>} tag in the body ({@link ItemMentions}) needs an operator.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class LetterCommand {

    private static final SuggestionProvider<CommandSourceStack> PLAYERS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                    .map(player -> player.getGameProfile().getName()), builder);

    private LetterCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var money = Commands.argument("amount", LongArgumentType.longArg(1))
                .then(text(ctx -> List.of(LetterReward.money(CurrencyType.MAIN,
                        LongArgumentType.getLong(ctx, "amount")))));
        var command = Commands.argument("command", StringArgumentType.string())
                .then(text(ctx -> List.of(LetterReward.command(StringArgumentType.getString(ctx, "command")))));
        var json = Commands.argument("json", StringArgumentType.greedyString()).executes(LetterCommand::json);

        dispatcher.register(Commands.literal("rollmod").then(Commands.literal("letter")
                .requires(LetterCommand::mayWrite)
                .then(Commands.literal("send").then(player(text(ctx -> List.of()))))
                .then(Commands.literal("senditem").then(player(text(LetterCommand::heldItem))))
                .then(Commands.literal("sendmoney").then(player(money)))
                .then(Commands.literal("sendcommand").requires(source -> source.hasPermission(4)).then(player(command)))
                .then(Commands.literal("json").then(player(json)))));
    }

    /** The letters permission for a player; the console and command blocks at level 2. */
    private static boolean mayWrite(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) return ModerationPermissions.canWriteLetters(player);
        return source.hasPermission(2);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> player(ArgumentBuilder<CommandSourceStack, ?> then) {
        return Commands.argument("player", StringArgumentType.word()).suggests(PLAYERS).then(then);
    }

    /** {@code <title> <body…>}, sending with whatever {@code rewards} makes of the context. */
    private static ArgumentBuilder<CommandSourceStack, ?> text(
            Function<CommandContext<CommandSourceStack>, List<LetterReward>> rewards) {
        return Commands.argument("title", StringArgumentType.string())
                .then(Commands.argument("body", StringArgumentType.greedyString()).executes(ctx -> {
                    List<LetterReward> attached = rewards.apply(ctx);
                    if (attached == null) return 0;
                    send(ctx.getSource(), StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "title"),
                            breaks(StringArgumentType.getString(ctx, "body")),
                            author(ctx.getSource()), "", 0L, attached);
                    return 1;
                }));
    }

    /** The stack in the sender's main hand, or null (with the reason said) when there is none. */
    private static List<LetterReward> heldItem(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player) || player.getMainHandItem().isEmpty()) {
            fail(ctx.getSource(), "msg.roll_mod.letters.command.noItem");
            return null;
        }
        return List.of(LetterReward.item(player.getMainHandItem().copy()));
    }

    private static int json(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        JsonObject json;
        try {
            JsonElement parsed = JsonParser.parseString(StringArgumentType.getString(ctx, "json"));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("not an object");
            json = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            source.sendFailure(Component.translatable("msg.roll_mod.letters.command.badJson", e.getMessage()));
            return 0;
        }

        String title = string(json, "title", "");
        if (title.isBlank()) {
            fail(source, "msg.roll_mod.letters.needTitle");
            return 0;
        }
        long lifetime = 0L;
        if (json.has("lifetime")) {
            lifetime = Durations.parse(json.get("lifetime").getAsString());
            if (lifetime < 0L) {
                fail(source, "msg.roll_mod.letters.command.badLifetime");
                return 0;
            }
        }

        List<LetterReward> rewards = new ArrayList<>();
        if (json.has("rewards") && json.get("rewards").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("rewards");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                LetterReward reward = LetterReward.load(element.getAsJsonObject(), source.getServer().registryAccess());
                if (reward == null) {
                    source.sendFailure(Component.translatable("msg.roll_mod.letters.command.badReward",
                            element.toString()));
                    return 0;
                }
                rewards.add(reward);
            }
        }
        if (!source.hasPermission(4) && rewards.stream().anyMatch(r -> r.type() == LetterReward.Type.COMMAND)) {
            fail(source, "msg.roll_mod.letters.commandNeedsOp");
            return 0;
        }

        send(source, StringArgumentType.getString(ctx, "player"), title, string(json, "body", ""),
                string(json, "author", author(source)), string(json, "icon", ""), lifetime, rewards);
        return 1;
    }

    /** Looks the player up — offline ones too — and sends once it is known who they are. */
    private static void send(CommandSourceStack source, String name, String title, String body, String author,
                             String icon, long lifetime, List<LetterReward> rewards) {
        if (!ItemMentions.allowed(source) && ItemMentions.contains(body)) {
            fail(source, "msg.roll_mod.letters.itemTagsNeedOp");
            return;
        }
        PlayerLookup.resolve(source.getServer(), name, profile -> {
            if (profile == null) {
                source.sendFailure(Component.translatable("msg.roll_mod.letters.command.noPlayer", name));
                return;
            }
            Letter letter = LetterService.sendPersonal(source.getServer(), profile.id(), author, title, body, icon,
                    lifetime, rewards);
            source.sendSuccess(() -> Component.translatable("msg.roll_mod.letters.command.sent",
                    LegacyText.display(letter.title()), profile.name(), letter.rewards().size()), true);
        });
    }

    /** {@code \n} typed in chat is a line break in the letter. */
    private static String breaks(String body) {
        return body.replace("\\n", "\n");
    }

    private static String author(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player.getGameProfile().getName()
                : source.getTextName();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private static void fail(CommandSourceStack source, String key) {
        source.sendFailure(Component.translatable(key).withStyle(ChatFormatting.RED));
    }
}
