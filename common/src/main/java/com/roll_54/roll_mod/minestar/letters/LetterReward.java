package com.roll_54.roll_mod.minestar.letters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One thing a letter gives when accepted: money, an item stack, or a command run as the reader.
 *
 * <p>One record with a type rather than three classes, because all three travel through the same
 * packet and the same JSON array, and the unused fields cost nothing.
 *
 * <p>{@code COMMAND} runs as the reader at permission level 4, so {@code @s} is always them. That is
 * the point of it and also the risk: anyone who may write letters can run any command as anyone who
 * reads one. {@code rollmod.moderation.letters} is granted narrowly for that reason, and every run
 * is logged.
 *
 * <p>A command reward may hold several commands, one per line, run {@code repeat} times over — a
 * shop product is a list of commands bought some number of times. {@code label} is what the reader
 * sees instead of the raw commands (the product's name); blank shows the command itself.
 * {@code console} runs them from the server console rather than as the reader, with {@code %player%}
 * replaced — exactly how {@code /shop-receive} used to run a product.
 */
public record LetterReward(Type type, CurrencyType currency, long amount, ItemStack item, String command,
                           String label, int repeat, boolean console) {

    /** Append only: the codec sends the ordinal. */
    public enum Type { MONEY, ITEM, COMMAND }

    public static final int MAX_COMMAND = 2048;
    public static final int MAX_LABEL = 128;
    /** A product bought this many times is still one reward; more than this is a typo or an exploit. */
    public static final int MAX_REPEAT = 1000;
    public static final int MAX_STACK = 6400;

    public static LetterReward money(CurrencyType currency, long amount) {
        return new LetterReward(Type.MONEY, currency, Math.max(1L, amount), ItemStack.EMPTY, "", "", 1, false);
    }

    public static LetterReward item(ItemStack stack) {
        return new LetterReward(Type.ITEM, CurrencyType.MAIN, 0L, stack.copy(), "", "", 1, false);
    }

    /** One command, run once as the reader — what the compose form makes. */
    public static LetterReward command(String command) {
        return command(command, "", 1, false);
    }

    /**
     * {@code commands}: one per line, each with or without its leading {@code /}. Blank lines are
     * dropped.
     */
    public static LetterReward command(String commands, String label, int repeat, boolean console) {
        StringBuilder clean = new StringBuilder();
        for (String line : (commands == null ? "" : commands).split("\\R")) {
            String one = line.strip();
            if (one.startsWith("/")) one = one.substring(1);
            if (one.isEmpty()) continue;
            if (!clean.isEmpty()) clean.append('\n');
            clean.append(one);
        }
        String text = clean.length() > MAX_COMMAND ? clean.substring(0, MAX_COMMAND) : clean.toString();
        String name = label == null ? "" : label.strip();
        if (name.length() > MAX_LABEL) name = name.substring(0, MAX_LABEL);
        return new LetterReward(Type.COMMAND, CurrencyType.MAIN, 0L, ItemStack.EMPTY, text, name,
                Math.clamp(repeat, 1, MAX_REPEAT), console);
    }

    /** The commands, one per entry. */
    public java.util.List<String> commands() {
        return command.isEmpty() ? java.util.List.of() : java.util.List.of(command.split("\n"));
    }

    /** Whether this is worth keeping: a zero payout, an empty stack or a blank command is not. */
    public boolean valid() {
        return switch (type) {
            case MONEY -> amount > 0L;
            case ITEM -> !item.isEmpty();
            case COMMAND -> !command.isBlank();
        };
    }

    /* --------------------------------------------- wire --------------------------------------------- */

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        switch (type) {
            case MONEY -> {
                buf.writeUtf(currency.id(), 32);
                buf.writeVarLong(Math.max(0L, amount));
            }
            case ITEM -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, item);
            case COMMAND -> {
                buf.writeUtf(command, MAX_COMMAND);
                buf.writeUtf(label, MAX_LABEL);
                buf.writeVarInt(repeat);
                buf.writeBoolean(console);
            }
        }
    }

    public static LetterReward decode(RegistryFriendlyByteBuf buf) {
        Type type = Type.values()[Math.floorMod(buf.readVarInt(), Type.values().length)];
        return switch (type) {
            case MONEY -> {
                CurrencyType currency = CurrencyType.byId(buf.readUtf(32)).orElse(CurrencyType.MAIN);
                yield money(currency, buf.readVarLong());
            }
            case ITEM -> {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                yield item(stack.copyWithCount(Math.min(stack.getCount(), MAX_STACK)));
            }
            case COMMAND -> command(buf.readUtf(MAX_COMMAND), buf.readUtf(MAX_LABEL), buf.readVarInt(),
                    buf.readBoolean());
        };
    }

    /* --------------------------------------------- file --------------------------------------------- */

    public @Nullable JsonObject save(RegistryAccess registries) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name().toLowerCase());
        switch (type) {
            case MONEY -> {
                json.addProperty("currency", currency.id());
                json.addProperty("amount", amount);
            }
            case ITEM -> {
                RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
                JsonElement stack = ItemStack.CODEC.encodeStart(ops, item).result().orElse(null);
                if (stack == null) return null;
                json.add("item", stack);
            }
            case COMMAND -> {
                json.addProperty("command", command);
                if (!label.isEmpty()) json.addProperty("label", label);
                if (repeat != 1) json.addProperty("repeat", repeat);
                if (console) json.addProperty("console", true);
            }
        }
        return json;
    }

    /**
     * One reward from the file, or {@code null} when it cannot be used — an unknown type, a
     * currency this build does not have, an item from a removed mod. The rest of the letter still
     * loads; one bad reward is not a reason to lose the message.
     */
    public static @Nullable LetterReward load(JsonObject json, RegistryAccess registries) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        LetterReward reward = switch (type) {
            case "money" -> CurrencyType.byId(json.has("currency") ? json.get("currency").getAsString() : "main")
                    .map(c -> money(c, json.has("amount") ? json.get("amount").getAsLong() : 0L))
                    .orElse(null);
            case "item" -> {
                if (!json.has("item")) yield null;
                RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
                yield ItemStack.CODEC.parse(ops, json.get("item")).result().map(LetterReward::item).orElse(null);
            }
            // label, repeat and console are newer than the format; a file without them loads as before.
            case "command" -> command(json.has("command") ? json.get("command").getAsString() : "",
                    json.has("label") ? json.get("label").getAsString() : "",
                    json.has("repeat") ? json.get("repeat").getAsInt() : 1,
                    json.has("console") && json.get("console").getAsBoolean());
            default -> null;
        };
        return reward != null && reward.valid() ? reward : null;
    }
}
