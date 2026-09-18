package com.roll_54.roll_mod.cosmetics;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every cosmetic skin the mod knows about, keyed by id.
 *
 * <p>Definitions are declared here in Java rather than loaded from a datapack: at runtime both
 * resolve to the same record, and JSON would only add a reload listener, a sync packet and a
 * decode step. {@link ItemSkinDefinition#CODEC} exists so that decision can be revisited without
 * the renderer noticing.
 *
 * <p>{@link #bootstrap()} runs at common setup rather than in a static initialiser, because the
 * definitions dereference {@link ItemRegistry} holders and those are only safe to resolve once
 * registration has finished. Afterwards the maps are never mutated, which is what makes them safe
 * to read from the render thread.
 */
public final class ItemSkinRegistry {
    private ItemSkinRegistry() {}

    /**
     * The murasama blade, the mod's first player-level skin. It is a {@link SkinCategory#SWORD}
     * skin, but is explicitly allowed on the meteorite metal blockogriz — a paxel, and therefore a
     * {@link SkinCategory#PICKAXE} — which is the case the whole feature was asked for.
     */
    public static final ResourceLocation MURASAMA = RollMod.id("murasama");

    /** The nanoaxe, the first {@link SkinCategory#AXE} skin. Converted from nanowaraxe.bbmodel. */
    public static final ResourceLocation NANOAXE = RollMod.id("nanoaxe");

    /** Helmet skins, drawn by GeckoLib. Both reuse geo models the mod already ships. */
    public static final ResourceLocation CLOWN_HAT = RollMod.id("clown_hat");
    public static final ResourceLocation HAZMAT_HOOD = RollMod.id("hazmat_hood");

    private static final Map<ResourceLocation, ItemSkinDefinition> BY_ID = new LinkedHashMap<>();
    private static volatile boolean bootstrapped;

    /** Called once from {@code RollMod#onCommonSetup}. Idempotent. */
    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }

        // models/tool/murasama_on.json — the model already wired into the nano saber's overrides.
        // It carries a full display block and an animated murasama_on.png.mcmeta, so it renders
        // correctly in every perspective with no extra work. Note the id has no "item/" prefix:
        // side-loaded standalone models resolve straight under models/.
        register(new ItemSkinDefinition(
                MURASAMA,
                SkinCategory.SWORD,
                SkinKind.ITEM_MODEL,
                RollMod.id("tool/murasama_on"),
                Optional.empty(),
                Set.of(ItemRegistry.METEORITE_METAL_BLOCKOGRIZ.get())
        ));

        // models/skin/nanoaxe.json — converted from blockbench/WIP/nanowaraxe.bbmodel. Three
        // textures, two of them 9-frame animation strips with their own .png.mcmeta, and a full
        // display block, so it needs no override entry to sit correctly in every perspective.
        register(new ItemSkinDefinition(
                NANOAXE,
                SkinCategory.AXE,
                SkinKind.ITEM_MODEL,
                RollMod.id("skin/nanoaxe"),
                Optional.empty(),
                Set.of()
        ));

        // Helmet skins take a geo model and a texture, both as full resource paths, because that is
        // what GeoModel hands back verbatim. Nothing here is a new asset — they are the armour
        // models the mod already renders, made wearable on any helmet.
        register(new ItemSkinDefinition(
                CLOWN_HAT,
                SkinCategory.HELMET,
                SkinKind.GEO_ARMOR,
                RollMod.id("geo/clown_hat.geo.json"),
                Optional.of(RollMod.id("textures/armor/clown_hat.png")),
                Set.of()
        ));
        register(new ItemSkinDefinition(
                HAZMAT_HOOD,
                SkinCategory.HELMET,
                SkinKind.GEO_ARMOR,
                RollMod.id("geo/item/armor/hazmat_helmet.geo.json"),
                Optional.of(RollMod.id("textures/item/armor/hazmat_helmet.png")),
                Set.of()
        ));

        bootstrapped = true;
    }

    private static void register(ItemSkinDefinition definition) {
        ItemSkinDefinition previous = BY_ID.put(definition.skinId(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate item skin id " + definition.skinId());
        }
    }

    @Nullable
    public static ItemSkinDefinition get(ResourceLocation skinId) {
        return BY_ID.get(skinId);
    }

    public static Collection<ItemSkinDefinition> all() {
        return BY_ID.values();
    }

    /**
     * Whether {@code skin} may render on {@code item}: the item resolves to the skin's own slot, or
     * the skin names it as an exception.
     *
     * <p>The exception list is what keeps Murasama — a {@link SkinCategory#SWORD} skin — on the
     * meteorite Blockogriz, which resolves to {@link SkinCategory#PICKAXE} under the paxel rule.
     * That pairing is the case the whole feature was originally asked for.
     */
    public static boolean canApply(ItemSkinDefinition skin, Item item) {
        return skin.category() == SkinCategory.of(item) || skin.alsoAllowed().contains(item);
    }

    /**
     * The skin to draw on {@code item} given a player's chosen slots, or {@code null} for none.
     *
     * <p>Two steps, because a slot lookup alone would drop the {@code alsoAllowed} exceptions: the
     * item's own slot first, then a scan for a chosen skin that names this item explicitly. The scan
     * is over at most one entry per slot, so it stays cheap enough for the render path.
     */
    @Nullable
    public static ResourceLocation resolveActive(Map<SkinCategory, ResourceLocation> active, Item item) {
        if (active.isEmpty()) {
            return null;
        }
        SkinCategory category = SkinCategory.of(item);
        if (category != null) {
            ResourceLocation direct = active.get(category);
            if (direct != null) {
                return direct;
            }
        }
        for (ResourceLocation skinId : active.values()) {
            ItemSkinDefinition skin = BY_ID.get(skinId);
            if (skin != null && skin.alsoAllowed().contains(item)) {
                return skinId;
            }
        }
        return null;
    }

    /** Every skin that may legally be applied to {@code item}. Used by the hub to build its list. */
    public static List<ItemSkinDefinition> applicableTo(Item item) {
        return BY_ID.values().stream().filter(skin -> canApply(skin, item)).toList();
    }
}
