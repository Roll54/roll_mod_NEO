package com.roll_54.roll_mod.data.datagen;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Fusion connected textures for the solar panels, cut from the hand-made 16x16 textures. Every face
 * texture {@code <name>} gets a 4x4 Fusion "simple" sheet {@code <name>_ct} whose 16 tiles are the
 * texture with its frame stripped on the connected sides, plus an mcmeta; animated textures (tier 15's
 * top) get one sheet per frame, stacked like the source. Each panel also gets a Fusion model modifier
 * swapping its model for {@code block/solar_panel_<tier>_connected} (built in
 * {@link SolarPanelBlockStateProvider}), so without Fusion the panels keep their plain models.
 */
public class SolarPanelConnectedTextureProvider implements DataProvider {

    /**
     * Frame thickness on every edge: the tops' frame is a dark outer line, a light bevel and a dark line
     * around the cells (3px); sides and bottoms have only the outer line and bevel (2px).
     */
    private static int frame(String texture) {
        return texture.startsWith("solar_panel_top_") ? 3 : 2;
    }

    /**
     * Fusion's "simple" layout ({@code SimpleLayoutHandler.getTilePos}, Fusion 1.3.16): the {@code {column, row}}
     * of the tile for each set of connected sides, indexed by top | right << 1 | bottom << 2 | left << 3.
     */
    private static final int[][] SIMPLE_TILES = new int[16][];

    static {
        tile(false, false, false, false, 0, 0);
        tile(false, false, false, true, 3, 0);
        tile(true, false, false, false, 3, 1);
        tile(false, true, false, false, 2, 1);
        tile(false, false, true, false, 2, 0);
        tile(false, true, false, true, 0, 1);
        tile(true, false, true, false, 1, 1);
        tile(true, false, false, true, 3, 3);
        tile(true, true, false, false, 2, 3);
        tile(false, true, true, false, 2, 2);
        tile(false, false, true, true, 3, 2);
        tile(true, true, true, false, 0, 2);
        tile(false, true, true, true, 1, 2);
        tile(true, false, true, true, 1, 3);
        tile(true, true, false, true, 0, 3);
        tile(true, true, true, true, 1, 0);
    }

    private static void tile(boolean top, boolean right, boolean bottom, boolean left, int column, int row) {
        SIMPLE_TILES[(top ? 1 : 0) | (right ? 2 : 0) | (bottom ? 4 : 0) | (left ? 8 : 0)] = new int[]{column, row};
    }

    private final PackOutput output;
    private final Path sourceDir;

    public SolarPanelConnectedTextureProvider(PackOutput output) {
        this.output = output;
        this.sourceDir = DatagenSourcePaths.locateAssets(Path.of("textures", "block"));
    }

    /** Every texture a solar panel face uses, without the {@code block/} prefix. */
    static Set<String> faceTextures() {
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < BlockRegistry.SOLAR_PANELS.size(); i++) {
            String voltage = SolarPanelBlockStateProvider.voltage(i);
            names.add("solar_panel_top_" + (i + 1));
            names.add(voltage + "_solar_panel_side");
            names.add(voltage + "_solar_panel_bottom");
        }
        return names;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(RollMod.MODID);
                Path textureOut = assets.resolve(Path.of("textures", "block"));
                for (String name : faceTextures()) {
                    connectedTexture(cache, name, textureOut);
                }
                Path modifierOut = assets.resolve(Path.of("fusion", "model_modifiers", "blocks"));
                for (int tier = 1; tier <= BlockRegistry.SOLAR_PANELS.size(); tier++) {
                    String panel = "solar_panel_" + tier;
                    saveJson(cache, modelModifier(panel), modifierOut.resolve(panel + ".json"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor());
    }

    private void connectedTexture(CachedOutput cache, String name, Path textureOut) throws IOException {
        BufferedImage source = readPng(sourceDir.resolve(name + ".png"));
        int frame = frame(name);
        Path sourceMeta = sourceDir.resolve(name + ".png.mcmeta");
        JsonObject meta = Files.exists(sourceMeta)
                ? JsonParser.parseString(Files.readString(sourceMeta)).getAsJsonObject()
                : new JsonObject();

        // Square frames stacked vertically, as vanilla animations without frame sizes expect.
        int size = source.getWidth();
        int frames = source.getHeight() / size;
        BufferedImage sheet = new BufferedImage(size * 4, size * 4 * frames, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < frames; f++) {
            BufferedImage tile = source.getSubimage(0, f * size, size, size);
            for (int index = 0; index < 16; index++) {
                int[] pos = SIMPLE_TILES[index];
                writeTile(sheet, tile, frame, (index & 1) != 0, (index & 2) != 0, (index & 4) != 0, (index & 8) != 0,
                        pos[0] * size, f * size * 4 + pos[1] * size);
            }
        }
        savePng(cache, sheet, textureOut.resolve(name + "_ct.png"));

        // Fusion slices each animation frame as one whole sheet, so the source animation carries over as is.
        meta.add("fusion", fusionTextureMeta());
        saveJson(cache, meta, textureOut.resolve(name + "_ct.png.mcmeta"));
    }

    /**
     * Copies {@code tile} into {@code sheet} at {@code (x, y)} with the {@code frame}-thick frame stripped on
     * the connected sides. A stripped line is filled with the inner pattern continued past the edge (it wraps
     * around the area inside the frame), so the cells of neighbouring panels read as one field and checker
     * patterns keep their parity; the frame on the unconnected sides, corners included, stays.
     */
    private static void writeTile(BufferedImage sheet, BufferedImage tile, int frame, boolean top, boolean right, boolean bottom, boolean left, int x, int y) {
        int size = tile.getWidth();
        int inner = size - 2 * frame;
        for (int ty = 0; ty < size; ty++) {
            int sy = ty;
            if (top && sy < frame) sy += inner;
            if (bottom && sy >= size - frame) sy -= inner;
            for (int tx = 0; tx < size; tx++) {
                int sx = tx;
                if (left && sx < frame) sx += inner;
                if (right && sx >= size - frame) sx -= inner;
                sheet.setRGB(x + tx, y + ty, tile.getRGB(sx, sy));
            }
        }
    }

    /** Panels connect to panels of the same tier only. */
    static JsonObject sameBlockConnection() {
        JsonObject connection = new JsonObject();
        connection.addProperty("type", "is_same_block");
        return connection;
    }

    private static JsonObject fusionTextureMeta() {
        JsonObject fusion = new JsonObject();
        fusion.addProperty("type", "connecting");
        fusion.addProperty("layout", "simple");
        fusion.add("connections", sameBlockConnection());
        return fusion;
    }

    private static JsonObject modelModifier(String panel) {
        JsonObject json = new JsonObject();
        json.add("targets", stringArray(List.of(RollMod.MODID + ":" + panel)));
        json.add("default_model_overrides", stringArray(List.of(RollMod.MODID + ":block/" + panel + "_connected")));
        return json;
    }

    private static JsonElement stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static BufferedImage readPng(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Missing solar panel texture: " + path);
        }
        BufferedImage img = ImageIO.read(path.toFile());
        if (img == null) {
            throw new IOException("Failed to read image: " + path);
        }
        BufferedImage converted = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        converted.getGraphics().drawImage(img, 0, 0, null);
        return converted;
    }

    private static void savePng(CachedOutput cache, BufferedImage image, Path out) throws IOException {
        Files.createDirectories(out.getParent());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", bos);
        byte[] data = bos.toByteArray();
        cache.writeIfNeeded(out, data, Hashing.sha1().hashBytes(data));
    }

    private static void saveJson(CachedOutput cache, JsonObject json, Path out) {
        DataProvider.saveStable(cache, json, out).join();
    }

    @Override
    public String getName() {
        return "Solar Panel Connected Textures: " + RollMod.MODID;
    }
}
