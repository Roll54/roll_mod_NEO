package com.roll_54.roll_mod.minestar.staff;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.tree.CommandNode;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The command blocker, ported from Eden_LD's Server section: other mods' commands only operators (level 2) may use —
 * the console and command blocks keep them. Brigadier has no way to add a requirement to a registered command, so
 * each listed root's own requirement is wrapped (by reflection) once the server has all its commands: at start, and
 * again after /reload, which rebuilds them. The list lives in {@code config/minestar/op_only_commands.json} and is
 * edited in the Moderation tab ("Commands").
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class OpOnlyCommands {

    private static final Field REQUIREMENT = requirementField();
    private static final int MAX = 200;

    private OpOnlyCommands() {}

    private static Path file() {
        return MinestarFiles.resolve("op_only_commands.json");
    }

    /** The blocked roots, without the slash. */
    public static synchronized List<String> names() {
        List<String> out = new ArrayList<>();
        JsonObject json = MinestarFiles.readObject(file());
        if (json.has("op_only") && json.get("op_only").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("op_only")) {
                if (e.isJsonPrimitive()) {
                    String name = clean(e.getAsString());
                    if (!name.isEmpty() && !out.contains(name)) out.add(name);
                }
            }
        }
        return out;
    }

    static String clean(String name) {
        String s = name == null ? "" : name.strip();
        return s.startsWith("/") ? s.substring(1).strip() : s;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        apply(event.getServer());
    }

    /** A null player means the whole server's data was reloaded (/reload), which rebuilt the commands. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        MinecraftServer server = event.getPlayerList().getServer();
        apply(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) server.getCommands().sendCommands(player);
    }

    /**
     * Replaces the list (the Moderation tab): saved and applied at once — commands taken off it get their own
     * requirement back — then everyone online gets their command list again.
     */
    public static synchronized void set(MinecraftServer server, List<String> names) {
        List<String> clean = new ArrayList<>();
        for (String n : names) {
            String c = clean(n);
            if (!c.isEmpty() && !clean.contains(c) && clean.size() < MAX) clean.add(c);
        }
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        clean.forEach(array::add);
        json.add("op_only", array);
        MinestarFiles.write(file(), json);
        if (REQUIREMENT != null) {
            Set<String> wanted = new HashSet<>(clean);
            for (CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
                if (node.getRequirement() instanceof OpOnly opOnly && !wanted.contains(node.getName())) {
                    try {
                        REQUIREMENT.set(node, opOnly.original());
                    } catch (IllegalAccessException e) {
                        RollMod.LOGGER.error("[Commands] Can't open /{} again.", node.getName(), e);
                    }
                }
            }
        }
        apply(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) server.getCommands().sendCommands(player);
    }

    /** Every root command on the server, sorted: what the Moderation tab offers to block. */
    public static List<String> roots(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        for (CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            out.add(node.getName());
        }
        out.sort(String::compareTo);
        return out;
    }

    private static void apply(MinecraftServer server) {
        if (REQUIREMENT == null) return;
        CommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();
        int restricted = 0;
        for (String literal : names()) {
            CommandNode<CommandSourceStack> node = root.getChild(literal);
            if (node == null) {
                RollMod.LOGGER.info("[Commands] op_only: no command /{} on this server.", literal);
                continue;
            }
            Predicate<CommandSourceStack> original = node.getRequirement();
            if (original instanceof OpOnly) {
                restricted++;
                continue;
            }
            try {
                REQUIREMENT.set(node, new OpOnly(original));
                restricted++;
            } catch (IllegalAccessException e) {
                RollMod.LOGGER.error("[Commands] Can't restrict /{}.", literal, e);
            }
        }
        if (restricted > 0) RollMod.LOGGER.info("[Commands] {} command(s) restricted to ops.", restricted);
    }

    /** The command's own requirement, plus op level 2. */
    private record OpOnly(Predicate<CommandSourceStack> original) implements Predicate<CommandSourceStack> {
        @Override
        public boolean test(CommandSourceStack source) {
            return source.hasPermission(2) && original.test(source);
        }
    }

    private static Field requirementField() {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            RollMod.LOGGER.error("[Commands] Brigadier changed; the command blocker can't work.", e);
            return null;
        }
    }
}
