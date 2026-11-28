package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.prospecting.Discovery;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Comparator;
import java.util.List;

/**
 * A chunk's prospected ore as one flat icon centred on the pose's origin, as the minimap shows a mob: the raw item of
 * the chunk's biggest vein. Flat (the item's own sprite, not its 3D model), so it reads the same whichever way the map
 * turns, with a black outline. An ore also found in deepslate carries a small dark mark in its corner. Shared by both maps; {@code size} is
 * the icon's side in GUI pixels.
 */
@OnlyIn(Dist.CLIENT)
final class ProspectIcons {

    private static final int DEEPSLATE_MARK = 0xFF2B2B33;

    private ProspectIcons() {}

    /** The ore a chunk is shown by: its biggest vein, the first found on a tie. */
    static Discovery main(List<Discovery> ores) {
        return ores.stream().max(Comparator.comparingInt((Discovery d) -> d.veinCount)).orElse(null);
    }

    /** Draws the chunk's icon {@code size} GUI pixels wide (any size, it is scaled), outlined one pixel in black. */
    static void draw(GuiGraphics graphics, List<Discovery> ores, float size) {
        Discovery d = main(ores);
        if (d == null || size < 1) return;
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(d.rawItem));
        var sprite = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).getParticleIcon();
        var pose = graphics.pose();
        // The black outline: the sprite's silhouette, one GUI pixel out each way, under the sprite itself.
        RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
        for (int[] o : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }) sprite(graphics, sprite, size, o[0], o[1]);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        sprite(graphics, sprite, size, 0, 0);
        if (d.hasDeepslate()) {
            pose.pushPose();
            pose.scale(size / 16f, size / 16f, 1f);
            graphics.fill(4, 4, 8, 8, DEEPSLATE_MARK);
            pose.popPose();
        }
    }

    private static void sprite(GuiGraphics graphics, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                               float size, int dx, int dy) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(dx, dy, 0);
        pose.scale(size / 16f, size / 16f, 1f);
        graphics.blit(-8, -8, 0, 16, 16, sprite);
        pose.popPose();
    }
}
