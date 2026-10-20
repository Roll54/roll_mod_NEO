package com.roll_54.roll_mod_client.client.storm;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.netherstorm.ClientStormCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the vanilla nether portal model and, while the storm is active, redraws every quad that uses
 * the vanilla portal sprite with {@code roll_mod:block/nether_portal_caution} instead. Also swaps the
 * particle icon, which is what the in-portal screen overlay and portal break particles draw.
 */
public final class StormPortalModel extends BakedModelWrapper<BakedModel> {

    private static final ResourceLocation CAUTION = RollMod.id("block/nether_portal_caution");

    /** Resolved on first use — the atlas is not stitched yet when the model is wrapped. */
    private volatile TextureAtlasSprite caution;

    public StormPortalModel(BakedModel original) {
        super(original);
    }

    private TextureAtlasSprite caution() {
        TextureAtlasSprite sprite = caution;
        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(CAUTION);
            caution = sprite;
        }
        return sprite;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> quads = super.getQuads(state, side, rand);
        return ClientStormCache.isActive() ? retexture(quads) : quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        return ClientStormCache.isActive() ? retexture(quads) : quads;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return ClientStormCache.isActive() ? caution() : super.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return ClientStormCache.isActive() ? caution() : super.getParticleIcon(data);
    }

    private List<BakedQuad> retexture(List<BakedQuad> quads) {
        if (quads.isEmpty()) return quads;
        TextureAtlasSprite from = originalModel.getParticleIcon();
        TextureAtlasSprite to = caution();
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(quad.getSprite() == from ? remap(quad, from, to) : quad);
        }
        return out;
    }

    private static BakedQuad remap(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to) {
        int[] vertices = quad.getVertices().clone();
        float fromU = from.getU1() - from.getU0();
        float fromV = from.getV1() - from.getV0();
        float toU = to.getU1() - to.getU0();
        float toV = to.getV1() - to.getV0();
        for (int i = 0; i < 4; i++) {
            int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float u = Float.intBitsToFloat(vertices[offset]);
            float v = Float.intBitsToFloat(vertices[offset + 1]);
            vertices[offset] = Float.floatToRawIntBits(to.getU0() + (u - from.getU0()) / fromU * toU);
            vertices[offset + 1] = Float.floatToRawIntBits(to.getV0() + (v - from.getV0()) / fromV * toV);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), to, quad.isShade(),
                quad.hasAmbientOcclusion());
    }
}
