package com.roll_54.roll_mod.data.datagen;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

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

    /** Voltage prefix for the side/bottom textures, one per consecutive group of 3 tiers. */
    private static final String[] VOLTAGE_GROUPS = {"lv", "mv", "hv", "ev", "iv"};

    /** Tier of a panel, parsed from its registry id {@code solar_panel_<tier>} (tiers aren't consecutive). */
    static int tier(DeferredBlock<Block> panel) {
        String path = panel.getId().getPath();
        return Integer.parseInt(path.substring(path.lastIndexOf('_') + 1));
    }

    /** Voltage prefix of a panel {@code tier}: tiers 1–3 are lv, 4–6 mv, … 13–15 iv. */
    static String voltage(int tier) {
        return VOLTAGE_GROUPS[(tier - 1) / 3];
    }

    @Override
    protected void registerStatesAndModels() {
        for (DeferredBlock<Block> panel : BlockRegistry.SOLAR_PANELS) {
            int tier = tier(panel);
            // Per-tier top texture; sides and bottom are shared per voltage group of 3 tiers.
            solarPanel(panel.get(), voltage(tier), modLoc("block/solar_panel_top_" + tier));
        }
    }

    /**
     * Builds a cube whose 4 sides and bottom use the voltage group's {@code block/<voltage>_solar_panel_side}
     * and {@code block/<voltage>_solar_panel_bottom} textures, and whose up face uses the supplied
     * {@code topTexture}. Also generates the item model.
     */
    private void solarPanel(Block block, String voltage, ResourceLocation topTexture) {
        ResourceLocation side = modLoc("block/" + voltage + "_solar_panel_side");
        ResourceLocation bottom = modLoc("block/" + voltage + "_solar_panel_bottom");
        trackTexture(side);
        trackTexture(bottom);
        trackTexture(topTexture);

        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile model = models().cubeBottomTop(name, side, bottom, topTexture);
        simpleBlockWithItem(block, model);

        // Fusion connected variant, swapped in by the model modifier from SolarPanelConnectedTextureProvider.
        ResourceLocation sideCt = connected(side);
        ResourceLocation bottomCt = connected(bottom);
        ResourceLocation topCt = connected(topTexture);
        trackTexture(sideCt);
        trackTexture(bottomCt);
        trackTexture(topCt);
        models().cubeBottomTop(name + "_connected", sideCt, bottomCt, topCt)
                .texture("particle", side)
                .customLoader(FusionConnectingBuilder::new);
    }

    /** The {@code _ct} sheet {@link SolarPanelConnectedTextureProvider} cuts from {@code texture}. */
    private static ResourceLocation connected(ResourceLocation texture) {
        return texture.withSuffix("_ct");
    }

    /** {@code "loader": "fusion:model"} connecting model, panels joining panels of the same tier only. */
    private static final class FusionConnectingBuilder extends CustomLoaderBuilder<BlockModelBuilder> {
        private FusionConnectingBuilder(BlockModelBuilder parent, ExistingFileHelper existingFileHelper) {
            super(ResourceLocation.fromNamespaceAndPath("fusion", "model"), parent, existingFileHelper, false);
        }

        @Override
        public JsonObject toJson(JsonObject json) {
            json = super.toJson(json);
            json.addProperty("type", "connecting");
            json.add("connections", SolarPanelConnectedTextureProvider.sameBlockConnection());
            return json;
        }
    }

    /** Register the texture as generated so the model builder doesn't fail when the PNG isn't present yet. */
    private void trackTexture(ResourceLocation texture) {
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
    }
}
