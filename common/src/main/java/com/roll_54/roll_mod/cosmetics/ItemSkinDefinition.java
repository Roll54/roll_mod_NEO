package com.roll_54.roll_mod.cosmetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One cosmetic skin. Definitions live in {@link ItemSkinRegistry} as plain Java today; the codec
 * exists so a datapack loader can be added later without the renderer having to change.
 *
 * @param skinId      unique id, also the lang key suffix ({@code skin.roll_mod.<path>})
 * @param category    the slot this skin belongs to
 * @param kind        how it is drawn
 * @param model       {@link SkinKind#ITEM_MODEL}: path under {@code models/}.
 *                    {@link SkinKind#GEO_ARMOR}: the {@code geo/} model id.
 * @param texture     required for {@link SkinKind#GEO_ARMOR}, unused for item models (the baked
 *                    model carries its own texture references)
 * @param alsoAllowed items outside {@code category} this skin may still be applied to. The escape
 *                    hatch that lets the murasama sword skin sit on a paxel without loosening the
 *                    category rule for everything else.
 */
public record ItemSkinDefinition(
        ResourceLocation skinId,
        SkinCategory category,
        SkinKind kind,
        ResourceLocation model,
        Optional<ResourceLocation> texture,
        Set<Item> alsoAllowed
) {
    public static final Codec<ItemSkinDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("skin_id").forGetter(ItemSkinDefinition::skinId),
            SkinCategory.CODEC.fieldOf("category").forGetter(ItemSkinDefinition::category),
            SkinKind.CODEC.optionalFieldOf("kind", SkinKind.ITEM_MODEL).forGetter(ItemSkinDefinition::kind),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemSkinDefinition::model),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(ItemSkinDefinition::texture),
            BuiltInRegistries.ITEM.byNameCodec().listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("also_allowed", Set.of()).forGetter(ItemSkinDefinition::alsoAllowed)
    ).apply(instance, ItemSkinDefinition::new));

    public ItemSkinDefinition {
        if (kind == SkinKind.GEO_ARMOR && texture.isEmpty()) {
            throw new IllegalArgumentException("GEO_ARMOR skin " + skinId + " needs a texture");
        }
    }

    /** {@code skin.roll_mod.murasama} — resolved client-side by the hub. */
    public String translationKey() {
        return "skin." + this.skinId.getNamespace() + "." + this.skinId.getPath();
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }
}
