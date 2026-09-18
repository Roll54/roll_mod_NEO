package com.roll_54.roll_mod.cosmetics;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** How a skin is drawn, which decides where its {@code model} resource lives. */
public enum SkinKind implements StringRepresentable {
    /**
     * A baked item model, swapped in at {@code ItemRenderer#getModel}. {@code model} is the raw
     * path under {@code models/} — {@code roll_mod:tool/murasama_on}, <em>not</em>
     * {@code item/tool/murasama_on} — because {@code ModelBakery} side-loads standalone models
     * without prefixing {@code item/}.
     */
    ITEM_MODEL("item_model"),
    /**
     * A GeckoLib armour model, drawn on the wearer by {@code GeoArmorRenderer}. Both {@code model}
     * and {@code texture} are full resource paths ({@code geo/….geo.json},
     * {@code textures/….png}), because {@code GeoModel} returns them to GeckoLib verbatim.
     */
    GEO_ARMOR("geo_armor");

    public static final Codec<SkinKind> CODEC = StringRepresentable.fromEnum(SkinKind::values);

    private final String name;

    SkinKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
