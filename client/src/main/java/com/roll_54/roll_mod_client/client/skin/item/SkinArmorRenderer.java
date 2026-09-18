package com.roll_54.roll_mod_client.client.skin.item;

import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.items.armor.geckolib.SkinAnchorItem;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Draws a cosmetic skin in place of whatever helmet the player is actually wearing.
 *
 * <p>{@code GeoArmorRenderer} is declared {@code <T extends Item & GeoItem>} and relies on it while
 * rendering: it casts the animatable to {@code GeoItem} for the animation tick, and the model calls
 * {@code getAnimatableInstanceCache()} on it. A skin may be worn on any helmet at all, so the worn
 * item is usually neither — a vanilla diamond helmet least of all.
 *
 * <p>{@link #prepForRender} is the one place that can be fixed cheaply, because it is not generic in
 * {@code T}: overriding it produces no erasure bridge, so the animatable can be swapped for a valid
 * one with no cast anywhere. {@code super} still records the real stack, wearer and slot, which is
 * what the rest of the renderer reads.
 *
 * <p>Nothing else may be overridden here. Any override of a {@code T}-typed method would make javac
 * emit a bridge containing {@code checkcast SkinAnchorItem}, which the worn helmet would fail — the
 * exact bug this class exists to avoid.
 */
public class SkinArmorRenderer extends GeoArmorRenderer<SkinAnchorItem> {

    public SkinArmorRenderer(ItemSkinDefinition skin) {
        super(new SkinnedArmorModel(skin));
    }

    @Override
    public void prepForRender(Entity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> baseModel,
                              MultiBufferSource bufferSource, float partialTick, float limbSwing,
                              float limbSwingAmount, float netHeadYaw, float headPitch) {
        super.prepForRender(entity, stack, slot, baseModel, bufferSource, partialTick, limbSwing,
                limbSwingAmount, netHeadYaw, headPitch);
        // super just pinned the worn helmet, which is not required to be a GeoItem. Replace it before
        // anything reads it; the stack, wearer and slot it also recorded stay pointing at the real one.
        this.animatable = ItemRegistry.SKIN_ANCHOR.get();
    }
}
