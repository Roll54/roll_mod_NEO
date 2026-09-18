package com.roll_54.roll_mod.cosmetics;

import com.mojang.serialization.Codec;
import com.roll_54.roll_mod.registry.TagRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The slot a cosmetic skin occupies. A player picks at most one skin per slot, and it then renders on
 * every item that resolves to that slot — see {@link ItemSkinRegistry#canApply}.
 *
 * <p>Membership is decided by {@link #of(Item)} in three layers, because no single test covers the
 * field:
 *
 * <ol>
 *   <li>{@link TagRegistry#SKIN_BLACKLIST}, then the per-slot tags. Datapack JSON, so a server can
 *       retag items live with {@code /reload}, and vanilla syncs item tags to clients for free.
 *   <li>{@code instanceof}, for tools that never overrode {@code canPerformAction} — {@code
 *       DiggerItem} itself has no override.
 *   <li>{@link ItemAbility}, which catches a well-behaved modded tool of any class. Vanilla {@code
 *       SwordItem}, {@code AxeItem} and {@code PickaxeItem} all report their default action sets.
 * </ol>
 *
 * <p>Tags remain the authority rather than a mere override because the mod's own {@code
 * EnergySwordItem} and {@code ComponentEnergyDrill} extend plain {@link Item} and answer no ability:
 * both later layers are blind to them. {@code RollItemTagProvider} keeps emitting the tags that cover
 * them.
 */
public enum SkinCategory implements StringRepresentable {
    // Order is the resolution priority for layers 2 and 3 — see the paxel rule on PICKAXE.
    HELMET("helmet", TagRegistry.SKIN_SLOT_HELMET,
            item -> item instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.HELMET,
            null),

    /**
     * Sits above {@link #AXE} on purpose: <b>a multi-tool is a pickaxe</b>. A paxel answers true to
     * pickaxe, axe and shovel abilities alike ({@code BlockogrizItem#canPerformAction}), so without a
     * fixed winner its slot would depend on enum declaration order — which a later edit could silently
     * flip, moving every paxel onto the axe skin. The mod already treats them this way: {@code
     * RollItemTagProvider} tags {@code BlockogrizItem} as a pickaxe, and Murasama's {@code
     * alsoAllowed} exception for the meteorite Blockogriz assumes it.
     */
    PICKAXE("pickaxe", TagRegistry.SKIN_SLOT_PICKAXE,
            item -> item instanceof PickaxeItem, ItemAbilities.PICKAXE_DIG),

    AXE("axe", TagRegistry.SKIN_SLOT_AXE,
            item -> item instanceof AxeItem, ItemAbilities.AXE_DIG),

    SWORD("sword", TagRegistry.SKIN_SLOT_SWORD,
            item -> item instanceof SwordItem, ItemAbilities.SWORD_DIG);

    public static final Codec<SkinCategory> CODEC = StringRepresentable.fromEnum(SkinCategory::values);

    public static final StreamCodec<ByteBuf, SkinCategory> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(SkinCategory::byNameOrSword, SkinCategory::getSerializedName);

    /** {@code values()} clones its array on every call, and {@link #of} runs on the render thread. */
    private static final SkinCategory[] VALUES = values();

    /**
     * Layer 2 and 3 allocate an {@link ItemStack} to probe abilities, which must not happen per frame.
     * Cleared by {@code ItemSkinEvents} on {@code TagsUpdatedEvent} — without that, a {@code /reload}
     * that retags an item would appear to do nothing.
     */
    private static final Map<Item, Optional<SkinCategory>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final TagKey<Item> tag;
    private final Predicate<Item> classTest;
    @Nullable
    private final ItemAbility ability;

    SkinCategory(String name, TagKey<Item> tag, Predicate<Item> classTest, @Nullable ItemAbility ability) {
        this.name = name;
        this.tag = tag;
        this.classTest = classTest;
        this.ability = ability;
    }

    public TagKey<Item> tag() {
        return this.tag;
    }

    /** The category {@code item} belongs to, or {@code null} when it is not skinnable at all. */
    @Nullable
    public static SkinCategory of(Item item) {
        // Optional, because ConcurrentHashMap forbids null values and "not skinnable" is the answer
        // for most of the registry — it has to be cached too, or every stick re-runs the whole chain.
        return CACHE.computeIfAbsent(item, key -> Optional.ofNullable(resolve(key))).orElse(null);
    }

    /** Drops every memoised classification. Call whenever the item tags change. */
    public static void invalidate() {
        CACHE.clear();
    }

    /** The slot named {@code name}, or {@code null}. For command input, which may be anything. */
    @Nullable
    public static SkinCategory byName(String name) {
        for (SkinCategory category : VALUES) {
            if (category.name.equals(name)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Which layer of {@link #of} decided this item, as a word. Diagnostic only — it re-runs the
     * chain rather than reading the cache, so it must stay off the render path.
     */
    public static String explain(Item item) {
        if (item.builtInRegistryHolder().is(TagRegistry.SKIN_BLACKLIST)) {
            return "blacklisted";
        }
        for (SkinCategory category : VALUES) {
            if (item.builtInRegistryHolder().is(category.tag)) {
                return "tag " + category.tag.location();
            }
        }
        ItemStack probe = new ItemStack(item);
        for (SkinCategory category : VALUES) {
            if (category.classTest.test(item)) {
                return "class";
            }
            if (category.ability != null && probe.canPerformAction(category.ability)) {
                return "ability " + category.ability.name();
            }
        }
        return "no rule matched";
    }

    @Nullable
    private static SkinCategory resolve(Item item) {
        if (item.builtInRegistryHolder().is(TagRegistry.SKIN_BLACKLIST)) {
            return null;
        }
        for (SkinCategory category : VALUES) {
            if (item.builtInRegistryHolder().is(category.tag)) {
                return category;
            }
        }
        // Class and ability are tested together per category rather than as two separate passes, so
        // that the declaration order above is what decides a tool answering to more than one of them.
        ItemStack probe = null;
        for (SkinCategory category : VALUES) {
            if (category.classTest.test(item)) {
                return category;
            }
            if (category.ability != null) {
                if (probe == null) {
                    probe = new ItemStack(item);
                }
                if (probe.canPerformAction(category.ability)) {
                    return category;
                }
            }
        }
        return null;
    }

    private static SkinCategory byNameOrSword(String name) {
        // Malformed input from a client is clamped rather than thrown, matching SkinActionPacket's
        // floorMod on the action byte: a bad packet picks a valid slot instead of killing the decoder.
        SkinCategory category = byName(name);
        return category == null ? SWORD : category;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
