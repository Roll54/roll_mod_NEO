package com.roll_54.roll_mod.minestar.dailytasks.tasks;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.TideFish;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A task made in the Moderation tab's task editor ({@code TaskSettings}) rather than in code. Everything a code task
 * says can be said here, so any of them can be made again from the editor:
 * <ul>
 *     <li>a hook, and what it counts: targets separated by commas, each an id or a {@code #tag} of a block, entity or
 *     item, or {@code rarity:common|uncommon|rare|very_rare|legendary} for Tide's fish; none counts anything;</li>
 *     <li>"one a day": only one of the targets counts each day, the same for everyone (the dish of the day);</li>
 *     <li>the amount, the draw weight, whether a party is asked for more and by what factor;</li>
 *     <li>a mod it needs, and "battle pass only" (a weekly or seasonal challenge, never on the daily board);</li>
 *     <li>a title, a description ({@code %s} is the amount, {@code %2$s} the target of the day) and an icon.</li>
 * </ul>
 * Not {@code @AutoDailyTask}: {@code DailyTaskRegistry} builds these from the synced settings on both sides.
 */
public final class ConfiguredTask implements DailyTask {

    private final String id;
    private final DailyTaskHook hook;
    private final List<String> targets;
    private final int amount;
    private final int weight;
    private final String title;
    private final String desc;
    private final String icon;
    private final boolean scales;
    private final double teamMultiplier;
    @Nullable
    private final String requiredMod;
    private final boolean passOnly;
    private final boolean oneADay;
    private Conditions conditions = Conditions.NONE;

    /**
     * Where and how the player must be for an event to count; blank means anywhere. {@code dimension} and
     * {@code biome} are ids ({@code #tag} for a biome tag), {@code held} an item id or {@code #tag} in the main hand,
     * {@code time} {@code day} / {@code night}, {@code weather} {@code clear} / {@code rain} / {@code thunder}, and the
     * height a range of Y the player stands in ({@code Integer.MIN_VALUE} / {@code MAX_VALUE}: open).
     */
    public record Conditions(String dimension, String biome, String held, String time, String weather, int minY,
                             int maxY, boolean sneaking) {
        public static final Conditions NONE = new Conditions("", "", "", "", "", Integer.MIN_VALUE, Integer.MAX_VALUE, false);

        boolean any() {
            return !this.equals(NONE);
        }
    }

    public ConfiguredTask withConditions(Conditions conditions) {
        this.conditions = conditions == null ? Conditions.NONE : conditions;
        return this;
    }

    /** The targets as written, for the registry (the statistics to sample). */
    public List<String> targets() {
        return targets;
    }

    @Override
    public boolean allowedFor(net.minecraft.server.level.ServerPlayer player) {
        Conditions c = conditions;
        if (!c.any()) return true;
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        if (!c.dimension().isBlank()
                && !level.dimension().location().toString().equals(full(c.dimension()))) return false;
        if (!c.biome().isBlank()) {
            var biome = level.getBiome(player.blockPosition());
            boolean ok = false;
            for (String part : c.biome().split(",")) {
                part = part.strip();
                if (part.isEmpty()) continue;
                ResourceLocation rl = ResourceLocation.tryParse(part.startsWith("#") ? part.substring(1) : part);
                if (rl == null) continue;
                if (part.startsWith("#") ? biome.is(TagKey.create(Registries.BIOME, rl)) : biome.is(rl)) ok = true;
            }
            if (!ok) return false;
        }
        if (!c.held().isBlank()) {
            ItemStack hand = player.getMainHandItem();
            boolean ok = false;
            for (String part : c.held().split(",")) {
                part = part.strip();
                if (!part.isEmpty() && matches(part, hand)) ok = true;
            }
            if (!ok) return false;
        }
        if (!c.time().isBlank()) {
            boolean night = !level.isDay();
            if (c.time().equals("night") != night) return false;
        }
        switch (c.weather()) {
            case "clear" -> {
                if (level.isRaining()) return false;
            }
            case "rain" -> {
                if (!level.isRaining()) return false;
            }
            case "thunder" -> {
                if (!level.isThundering()) return false;
            }
            default -> {
            }
        }
        int y = player.getBlockY();
        if (y < c.minY() || y > c.maxY()) return false;
        return !c.sneaking() || player.isShiftKeyDown();
    }

    private static String full(String id) {
        id = id.strip();
        return id.contains(":") ? id : "minecraft:" + id;
    }

    public ConfiguredTask(String id, DailyTaskHook hook, String target, int amount, int weight, String title, String icon) {
        this(id, hook, target, amount, weight, title, "", icon, true, 0, "", false, false);
    }

    public ConfiguredTask(String id, DailyTaskHook hook, String target, int amount, int weight, String title,
                          String desc, String icon, boolean scales, double teamMultiplier, String requiredMod,
                          boolean passOnly, boolean oneADay) {
        this.id = id;
        this.hook = hook;
        List<String> list = new ArrayList<>();
        if (target != null) for (String part : target.split(",")) if (!part.isBlank()) list.add(part.strip());
        this.targets = List.copyOf(list);
        this.amount = Math.max(1, amount);
        this.weight = Math.max(1, weight);
        this.title = title == null ? "" : title;
        this.desc = desc == null ? "" : desc;
        this.icon = icon == null ? "" : icon.strip();
        this.scales = scales;
        this.teamMultiplier = Math.max(0, teamMultiplier);
        this.requiredMod = requiredMod == null || requiredMod.isBlank() ? null : requiredMod.strip();
        this.passOnly = passOnly;
        this.oneADay = oneADay;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public DailyTaskHook hook() {
        return hook;
    }

    @Override
    public int baseAmount() {
        return amount;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public boolean scalesWithTeam() {
        return scales;
    }

    @Override
    public double teamMultiplier() {
        return teamMultiplier;
    }

    @Nullable
    @Override
    public String requiredMod() {
        return requiredMod;
    }

    @Override
    public boolean battlePassOnly() {
        return passOnly;
    }

    /** The target that counts today when only one a day does; otherwise null (all of them count). */
    @Nullable
    private String today() {
        if (!oneADay || targets.isEmpty()) return null;
        long mixed = DailyTaskManager.currentPeriodDay() * 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 32;
        return targets.get(Math.floorMod((int) mixed, targets.size()));
    }

    @Override
    public boolean matches(Object subject) {
        if (subject == null) return false;
        if (targets.isEmpty()) return !(subject instanceof ItemStack stack) || !stack.isEmpty();
        String only = today();
        if (only != null) return matches(only, subject);
        for (String target : targets) if (matches(target, subject)) return true;
        return false;
    }

    private static boolean matches(String target, Object subject) {
        if (target.startsWith("rarity:")) {
            try {
                return TideFish.is(subject, TideFish.Rarity.valueOf(target.substring(7).toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        boolean tag = target.startsWith("#");
        ResourceLocation rl = ResourceLocation.tryParse(tag ? target.substring(1) : target);
        if (rl == null) return false;
        return switch (subject) {
            case BlockState state -> tag ? state.is(TagKey.create(Registries.BLOCK, rl))
                    : BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(rl);
            case LivingEntity entity -> tag ? entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, rl))
                    : EntityType.getKey(entity.getType()).equals(rl);
            case ItemStack stack -> !stack.isEmpty() && (tag ? stack.is(TagKey.create(Registries.ITEM, rl))
                    : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(rl));
            case String id -> !tag && id.equals(rl.toString());
            // Statistic hooks (jump, playtime, distance, raids) count the player: nothing to narrow.
            default -> true;
        };
    }

    @Override
    public Component name() {
        return title.isBlank() ? Component.translatable("dailytask.roll_mod.custom.title") : Component.literal(title);
    }

    @Override
    public Component tooltip(int required) {
        String only = today();
        Component what = only != null ? targetName(only) : Component.empty();
        if (!desc.isBlank()) return Component.translatableWithFallback("", desc, required, what);
        return only != null ? Component.translatable("dailytask.roll_mod.custom.desc_today", required, what)
                : Component.translatable("dailytask.roll_mod.custom.desc", required);
    }

    /** A target as a player reads it: the item's or block's name, else the id. */
    private static Component targetName(String target) {
        Item item = item(target);
        return item != null ? item.getDescription() : Component.literal(target);
    }

    @Override
    public DailyTaskIcon icon() {
        Item item = iconItem();
        // A picture instead of an item: "roll_mod:textures/gui/....png", as some code tasks have.
        if (icon.endsWith(".png")) {
            ResourceLocation png = ResourceLocation.tryParse(icon);
            if (png != null) return DailyTaskIcon.of(png, item != null ? item : Items.PAPER);
        }
        return DailyTaskIcon.of(item != null ? item : Items.PAPER);
    }

    @Nullable
    private Item iconItem() {
        if (!icon.isEmpty() && !icon.endsWith(".png")) {
            ResourceLocation rl = ResourceLocation.tryParse(icon);
            if (rl != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null && item != Items.AIR) return item;
            }
        }
        // No icon set: the target of the day, or the first plain target that has an item.
        String only = today();
        if (only != null) return item(only);
        for (String target : targets) {
            Item item = item(target);
            if (item != null) return item;
        }
        return null;
    }

    @Nullable
    private static Item item(String target) {
        if (target.startsWith("#") || target.startsWith("rarity:")) return null;
        ResourceLocation rl = ResourceLocation.tryParse(target);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item != null && item != Items.AIR) return item;
        Block block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
        return block != null && block.asItem() != Items.AIR ? block.asItem() : null;
    }
}
