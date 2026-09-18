package com.roll_54.roll_mod_client.mixin.skin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_client.client.skin.item.ItemSkinResolver;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws a player's chosen cosmetic skin in place of the item they are actually holding.
 *
 * <p>Swapping the {@link BakedModel} at this one call is the cheapest hook that exists for it: the
 * model is already baked and cached, so the change costs a pointer, and the item stays inside
 * vanilla's batching. A {@code BlockEntityWithoutLevelRenderer} would have to be bound per-Item and
 * statically, dragging every saber and paxel in the world onto the unbatched path even for players
 * who own no skins.
 *
 * <p>The inventory is untouched for free, without a display-context check: {@code GuiGraphics}
 * calls {@code getModel} and {@code render} itself rather than going through {@code renderStatic},
 * so slots, JEI and tooltips all keep showing the real item.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererSkinMixin {

    @WrapOperation(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;"))
    private BakedModel roll_mod$applyItemSkin(ItemRenderer self, ItemStack stack, @Nullable Level level,
                                              @Nullable LivingEntity holder, int seed,
                                              Operation<BakedModel> original) {
        BakedModel skin = ItemSkinResolver.resolve(holder, stack);
        // Falling through to the original keeps the existing per-stack component skins working:
        // murasama, flame and the rest are resolved by the item model's own overrides in there.
        return skin != null ? skin : original.call(self, stack, level, holder, seed);
    }
}
