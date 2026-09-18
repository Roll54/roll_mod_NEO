package com.roll_54.roll_mod.items;

import com.roll_54.roll_mod.items.armor.geckolib.GeoArmorRendererRegistry;
import com.roll_54.roll_mod.util.TooltipManager;
import com.roll_54.roll_mod.util.TooltipOptions;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * The mod's general-purpose armour item.
 *
 * <p>It implements {@link GeoItem} without animating anything of its own, so that a subclass with a
 * geo model of its own — {@code HazmatHelmetItem} — inherits the plumbing. Nothing changes for a
 * piece that has no provider registered: {@link GeoArmorRendererRegistry#apply} is a no-op then, and
 * vanilla renders it.
 *
 * <p>This is <em>not</em> what makes a helmet eligible for a cosmetic skin, though it used to be.
 * Skins are applied in {@code GeoArmorSkinMixin}, at GeckoLib's own call site, and reach every helmet
 * in the game whether or not it is a {@code GeoItem}. Do not re-add a blanket registration loop for
 * the mod's helmets on the strength of this interface.
 */
public class TooltipArmorItem extends ArmorItem implements GeoItem {
    private final TooltipOptions opts;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TooltipArmorItem(Holder<ArmorMaterial> material, Type type, Properties props, TooltipOptions opts) {
        super(material, type, props);
        this.opts = (opts == null) ? TooltipOptions.NONE : opts;
    }

    @Override
    public Component getName(ItemStack stack) {
        return TooltipManager.colorName(super.getName(stack), opts);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        TooltipManager.addLore(stack, opts, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return opts != null && opts.glow();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        GeoArmorRendererRegistry.apply(this, consumer);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Skins supply their own models, not animations; the base armour has none.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---------------- BUILDER ----------------
    public static class Builder {
        private final Holder<ArmorMaterial> material;
        private final ArmorItem.Type type;
        private final Properties props;
        private int tooltipLines = 0;
        private Integer nameColorHex = null;
        private Integer loreColorHex = null;
        private boolean textureGlow = false;

        public Builder(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties props) {
            this.material = material;
            this.type = type;
            this.props = props;
        }

        public Builder tooltipLines(int lines) {
            this.tooltipLines = lines;
            return this;
        }

        public Builder nameColor(int hex) {
            this.nameColorHex = hex;
            return this;
        }

        public Builder loreColor(int hex) {
            this.loreColorHex = hex;
            return this;
        }

        /** Вмикає або вимикає ванільне світіння текстури */
        public Builder textureGlow(boolean glow) {
            this.textureGlow = glow;
            return this;
        }

        public TooltipArmorItem build() {
            return new TooltipArmorItem(material, type, props,
                    new TooltipOptions(tooltipLines, nameColorHex, loreColorHex, textureGlow));
        }
    }
}
