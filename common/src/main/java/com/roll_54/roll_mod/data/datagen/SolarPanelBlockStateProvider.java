package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class SolarPanelBlockStateProvider extends BlockStateProvider {
    public SolarPanelBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RollMod.MODID, exFileHelper);
    }

    @Override
    public String getName() {
        // Distinct from OreBlockStateProvider; DataGenerator keys providers by name and
        // BlockStateProvider's default ("Block States: <modid>") would otherwise collide.
        return "Solar Panel Block States: " + RollMod.MODID;
    }

    @Override
    protected void registerStatesAndModels() {
        for (int i = 0; i < BlockRegistry.SOLAR_PANELS.size(); i++) {
            int tier = i + 1;
            Block block = BlockRegistry.SOLAR_PANELS.get(i).get();
            // Per-tier top texture; all other faces share the static side texture.
            solarPanel(block, modLoc("block/solar_panel_top_" + tier));
        }
    }

    /**
     * Builds a cube whose 4 sides and bottom use the shared static {@code block/solar_panel_side}
     * texture, and whose up face uses the supplied {@code topTexture}. Also generates the item model.
     */
    private void solarPanel(Block block, ResourceLocation topTexture) {
        ResourceLocation side = modLoc("block/solar_panel_side");
        ResourceLocation bottom = modLoc("block/solar_panel_bottom");
        trackTexture(side);
        trackTexture(topTexture);

        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile model = models().cubeBottomTop(name, side, bottom, topTexture);
        simpleBlockWithItem(block, model);
    }

    /** Register the texture as generated so the model builder doesn't fail when the PNG isn't present yet. */
    private void trackTexture(ResourceLocation texture) {
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
    }
}
