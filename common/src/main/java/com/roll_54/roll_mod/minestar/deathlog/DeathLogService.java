package com.roll_54.roll_mod.minestar.deathlog;

import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.compat.createcybernetics.CyberwareSnapshots;
import com.roll_54.roll_mod.compat.curios.CuriosCompat;
import com.roll_54.roll_mod.minestar.deathlog.DeathRecord.Mode;
import com.roll_54.roll_mod.minestar.letters.Letter;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import com.roll_54.roll_mod.minestar.letters.LetterService;
import com.roll_54.roll_mod.minestar.moderation.ModerationLog;
import com.roll_54.roll_mod.minestar.moderation.PunishmentLetters;
import com.roll_54.roll_mod.util.ServerLang;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.ModList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Taking a death down, and giving it back.
 *
 * <p>Restoring is deliberately repeatable: a record is a copy, not a vault, and a moderator may need
 * a second go (the first went to the wrong person, a letter expired). Every restore is noted on the
 * record and in the moderation log, and the tab warns before a repeat, which is what stops it being
 * a duplication button.
 */
public final class DeathLogService {

    private static final String KEY = "letters.roll_mod.deathlog.";
    private static final String CYBERNETICS = "createcybernetics";

    /**
     * Whether the player keeps their things through this death regardless of the gamerule — set by the
     * server jar to Prime, which lives there. A record is still made; it is only marked, since
     * restoring it would hand the player a second copy.
     */
    private static volatile Predicate<ServerPlayer> keepsInventory = player -> false;

    private DeathLogService() {}

    public static void setKeepsInventory(Predicate<ServerPlayer> predicate) {
        keepsInventory = predicate == null ? player -> false : predicate;
    }

    /* ------------------------------------------ capture ------------------------------------------ */

    /** Everything the player has right now, as a record. Called before anything is dropped. */
    public static DeathRecord capture(ServerPlayer player, DamageSource source) {
        Inventory inventory = player.getInventory();
        Map<Integer, ItemStack> stacks = new TreeMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) stacks.put(i, stack.copy());
        }

        CompoundTag cyberware = null;
        List<ItemStack> cyberwareItems = List.of();
        if (ModList.get().isLoaded(CYBERNETICS)) {
            cyberware = CyberwareSnapshots.capture(player);
            cyberwareItems = CyberwareSnapshots.items(cyberware, player.registryAccess());
        }

        boolean kept = player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                || keepsInventory.test(player);

        Component message = source.getLocalizedDeathMessage(player);
        String messageJson;
        try {
            messageJson = Component.Serializer.toJson(message, player.registryAccess());
        } catch (RuntimeException e) {
            messageJson = "";
        }
        String damageType = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
        Entity killerEntity = source.getEntity() != null ? source.getEntity() : player.getKillCredit();
        String killer = killerEntity == null || killerEntity == player ? "" : killerEntity.getName().getString();
        return new DeathRecord(UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(),
                System.currentTimeMillis(), player.level().dimension().location().toString(),
                player.getBlockX(), player.getBlockY(), player.getBlockZ(), message.getString(), messageJson,
                damageType, killer, kept,
                java.util.Collections.unmodifiableMap(stacks), List.copyOf(CuriosCompat.snapshot(player)),
                cyberware, List.copyOf(cyberwareItems), List.of());
    }

    /* ------------------------------------------ restore ------------------------------------------ */

    /** Every stack of the death into the moderator's inventory; what does not fit comes by letter. */
    public static void retrieve(ServerPlayer moderator, DeathRecord record) {
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : record.allStacks()) {
            if (!moderator.getInventory().add(stack) && !stack.isEmpty()) left.add(stack);
        }
        if (!left.isEmpty()) {
            sendLetters(moderator.server, moderator.getUUID(), ServerLang.uk(KEY + "title.retrieve",
                    record.victimName()), ServerLang.uk(KEY + "body.retrieve", record.victimName(),
                    date(record.diedAt()), record.cause()), left);
        }
        noted(moderator, record, Mode.RETRIEVE, moderator.getGameProfile().getName(), left.size());
    }

    /**
     * The death back to the player who died. Online: each stack to the slot it was in when that slot
     * is free, else anywhere; curios to their slots; cyberware written back if the body is still bare.
     * Whatever is left — or everything, when they are offline — comes by letter.
     */
    public static void returnToPlayer(ServerPlayer moderator, DeathRecord record) {
        MinecraftServer server = moderator.server;
        ServerPlayer victim = server.getPlayerList().getPlayer(record.victim());
        List<ItemStack> left = new ArrayList<>();

        if (victim == null) {
            left.addAll(record.allStacks());
        } else {
            Inventory inventory = victim.getInventory();
            List<ItemStack> loose = new ArrayList<>();
            record.inventory().forEach((slot, stack) -> {
                if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
                    inventory.setItem(slot, stack.copy());
                } else {
                    loose.add(stack.copy());
                }
            });
            loose.addAll(CuriosCompat.restoreAll(victim, record.curios()));

            boolean wroteCyberware = record.cyberware() != null && ModList.get().isLoaded(CYBERNETICS)
                    && !record.cyberwareItems().isEmpty()
                    && CyberwareSnapshots.restore(victim, record.cyberware());
            if (!wroteCyberware) record.cyberwareItems().forEach(s -> loose.add(s.copy()));

            for (ItemStack stack : loose) {
                if (!inventory.add(stack) && !stack.isEmpty()) left.add(stack);
            }
            inventory.setChanged();
            victim.inventoryMenu.broadcastChanges();
        }

        if (!left.isEmpty()) {
            sendLetters(server, record.victim(), ServerLang.uk(KEY + "title.return"),
                    ServerLang.uk(KEY + "body.return", date(record.diedAt()), record.cause(),
                            moderator.getGameProfile().getName()), left);
        }
        if (victim != null) {
            victim.sendSystemMessage(Component.translatable(left.isEmpty()
                            ? "chat.roll_mod.deathlog.returned.victim" : "chat.roll_mod.deathlog.returned.victim.letter",
                    moderator.getGameProfile().getName(), date(record.diedAt())));
        }
        noted(moderator, record, Mode.RETURN, record.victimName(), left.size());
    }

    private static void noted(ServerPlayer moderator, DeathRecord record, Mode mode, String to, int byLetter) {
        String name = moderator.getGameProfile().getName();
        DeathLogStore.replace(moderator.server,
                record.withRestore(new DeathRecord.Restore(name, to, System.currentTimeMillis(), mode.id())));
        ModerationLog.append(name, "deathlog " + mode.id(), record.victimName(),
                "death " + record.id() + " of " + date(record.diedAt()) + " given to " + to + ", "
                        + record.itemCount() + " stack(s), " + byLetter + " by letter");
    }

    /** The stacks as item rewards, as many letters as it takes — a letter carries {@link Letter#MAX_REWARDS}. */
    private static void sendLetters(MinecraftServer server, UUID recipient, String title, String body,
                                    List<ItemStack> stacks) {
        int letters = (stacks.size() + Letter.MAX_REWARDS - 1) / Letter.MAX_REWARDS;
        for (int n = 0; n < letters; n++) {
            List<LetterReward> rewards = new ArrayList<>();
            int from = n * Letter.MAX_REWARDS;
            for (int i = from; i < Math.min(stacks.size(), from + Letter.MAX_REWARDS); i++) {
                rewards.add(LetterReward.item(stacks.get(i)));
            }
            String numbered = letters == 1 ? title : title + " (" + (n + 1) + "/" + letters + ")";
            LetterService.sendPersonal(server, recipient, ServerLang.uk(KEY + "author"), numbered, body,
                    PunishmentLetters.HAMMER_ICON, MyConfig.INSTANCE.letters.deathLogLifetime(), rewards, true);
        }
    }

    static String date(long millis) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(millis));
    }
}
