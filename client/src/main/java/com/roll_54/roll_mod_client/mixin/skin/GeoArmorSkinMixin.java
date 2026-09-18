package com.roll_54.roll_mod_client.mixin.skin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod_client.client.skin.item.HelmetSkinRenderers;
import com.roll_54.roll_mod_client.client.skin.item.SkinArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.util.InternalUtil;

/**
 * Lets a cosmetic helmet skin render on <em>any</em> helmet, not only the mod's own.
 *
 * <p>GeckoLib draws armour from {@code HumanoidArmorLayer} through
 * {@link InternalUtil#tryRenderGeoArmorPiece}, which asks {@code GeoRenderProvider.of(item)} for a
 * renderer. That method is {@code item instanceof GeoItem ? item.getRenderProvider() : DEFAULT}, and
 * {@code DEFAULT} returns null — so for a vanilla diamond helmet, or any helmet from a mod that does
 * not use GeckoLib, the whole path bails before the cosmetics code is ever consulted. Skins bind per
 * slot, so the data layer would happily report a skin as active on that helmet while nothing drew it.
 *
 * <p>Replacing the value of that call is the smallest possible intervention: GeckoLib still owns the
 * orchestration around it — {@code copyPropertiesTo}, {@code setPartVisibility}, {@code prepForRender},
 * {@code renderToBuffer} — so none of that is duplicated here and it cannot drift out of step.
 *
 * <p>Injecting <em>after</em> the call rather than before it also means a skin outranks the piece's
 * own model, which is the rule the mod already applied to its own armour, now stated once for every
 * helmet in the game.
 *
 * <p>Held items get the equivalent treatment one layer down, in {@code ItemRendererSkinMixin}.
 */
@Mixin(InternalUtil.class)
public class GeoArmorSkinMixin {

    /**
     * The handler is static because the target is. {@code @Local(argsOnly = true)} resolves by
     * descriptor, and {@code LivingEntity}, {@code ItemStack} and {@code EquipmentSlot} each occur
     * exactly once in the erased parameter list, so no ordinal is needed. The two
     * {@code HumanoidModel} parameters would be ambiguous, which is why nothing here asks for them.
     */
    @ModifyExpressionValue(
            method = "tryRenderGeoArmorPiece",
            at = @At(value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/animatable/client/GeoRenderProvider;"
                            + "getGeoArmorRenderer("
                            + "Lnet/minecraft/world/entity/LivingEntity;"
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + "Lnet/minecraft/world/entity/EquipmentSlot;"
                            + "Lnet/minecraft/client/model/HumanoidModel;)"
                            + "Lnet/minecraft/client/model/HumanoidModel;"))
    private static HumanoidModel<?> roll_mod$applyHelmetSkin(HumanoidModel<?> original,
                                                             @Local(argsOnly = true) LivingEntity wearer,
                                                             @Local(argsOnly = true) ItemStack stack,
                                                             @Local(argsOnly = true) EquipmentSlot slot) {
        // Only the head slot is skinnable; chestplate, leggings and boots must pass through untouched.
        if (slot != EquipmentSlot.HEAD) {
            return original;
        }
        SkinArmorRenderer skin = HelmetSkinRenderers.forWearer(wearer, stack);
        return skin != null ? skin : original;
    }
}
