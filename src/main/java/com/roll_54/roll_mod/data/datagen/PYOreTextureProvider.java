package com.roll_54.roll_mod.data.datagen;

import com.google.common.hash.Hashing;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.BlockOverlay;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.ItemBase;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.ItemLayer;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Java rewrite of the Python ore texture generator. It composes template layers from
 * {@code src/main/resources/assets/roll_mod/py_datagen} into final ore/item textures
 * under generated resources.
 */
public class PYOreTextureProvider implements DataProvider {
    private final PackOutput output;

    private final Path templatesRoot;
    private final Path blockSubLayersDir;
    private final Path blockOverlaysDir;
    private final Path itemBasesDir;
    private final Path itemLayersDir;

    public PYOreTextureProvider(PackOutput output) {
        this.output = output;
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        if (projectRoot.getFileName().toString().equals("run")) {
            projectRoot = projectRoot.getParent();
        }
        this.templatesRoot = projectRoot.resolve(Path.of("src", "main", "resources", "assets", RollMod.MODID, "py_datagen"));
        this.blockSubLayersDir = templatesRoot.resolve(Path.of("blocks", "sub_layers"));
        this.blockOverlaysDir = templatesRoot.resolve(Path.of("blocks", "overlays"));
        this.itemBasesDir = templatesRoot.resolve(Path.of("items", "bases"));
        this.itemLayersDir = templatesRoot.resolve(Path.of("items", "layers"));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path resourceRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
                Path blockTexOut = resourceRoot.resolve(Path.of(RollMod.MODID, "textures", "block"));
                Path itemTexOut = resourceRoot.resolve(Path.of( RollMod.MODID, "textures", "item"));
                Files.createDirectories(blockTexOut);
                Files.createDirectories(itemTexOut);

                for (OreDefinition def : OreDefinitions.ALL) {
                    generateItems(cache, def, itemTexOut);
                    generateBlocks(cache, def, blockTexOut);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor());
    }

    private void generateItems(CachedOutput cache, OreDefinition def, Path itemTexOut) throws IOException {
        String ore = def.oreName();
        createItemTexture(cache, "raw_" + ore, def.itemBase(), def.hexColor(), itemTexOut);
        createItemTexture(cache, ore + "_dust", ItemBase.DUST, def.hexColor(), itemTexOut);
        createCrushed(cache, def, itemTexOut);
    }

    private void generateBlocks(CachedOutput cache, OreDefinition def, Path blockTexOut) throws IOException {
        BlockOverlay overlay = def.overlay();
        for (BlockSubLayer base : def.bases()) {
            String blockId = base.id() + "_" + def.oreName();
            createBlockTexture(cache, base, overlay, def.oreName(), def.hexColor(), blockTexOut, blockId);
        }
    }

    private void createCrushed(CachedOutput cache, OreDefinition def, Path itemTexOut) throws IOException {
        String ore = def.oreName();
        String tint = def.hexColor();

        createItemMultilayerTexture(cache, "crushed_" + ore + "_ore",
                List.of(ItemLayer.CRUSHED.resolve(itemLayersDir), ItemLayer.CRUSHED_OVERLAY.resolve(itemLayersDir)), tint, itemTexOut);

        createItemMultilayerTexture(cache, "refined_" + ore + "_ore",
                List.of(ItemLayer.CRUSHED_REFINED.resolve(itemLayersDir), ItemLayer.CRUSHED_REFINED_OVERLAY.resolve(itemLayersDir)), tint, itemTexOut);

        createItemMultilayerTexture(cache, "purified_" + ore + "_ore",
                List.of(ItemLayer.CRUSHED_PURIFIED.resolve(itemLayersDir)), tint, itemTexOut);

        createItemMultilayerTexture(cache, "pure_" + ore + "_dust",
                List.of(ItemLayer.DUST_PURE.resolve(itemLayersDir), ItemLayer.DUST_PURE_OVERLAY.resolve(itemLayersDir)), tint, itemTexOut);

        createItemMultilayerTexture(cache, "impure_" + ore + "_dust",
                List.of(ItemLayer.DUST_IMPURE.resolve(itemLayersDir), ItemLayer.DUST_IMPURE_OVERLAY.resolve(itemLayersDir)), tint, itemTexOut);
    }

    private void createBlockTexture(CachedOutput cache,
                                    BlockSubLayer base,
                                    BlockOverlay overlay,
                                    String oreName,
                                    String tint,
                                    Path blockTexOut,
                                    String blockId) throws IOException {
        BufferedImage sub = readPng(base.resolve(blockSubLayersDir));
        BufferedImage ovl = readPng(overlay.resolve(blockOverlaysDir));
        BufferedImage tintedOverlay = applyColorMultiply(ovl, tint);

        BufferedImage result = alphaComposite(sub, tintedOverlay);
        Path out = blockTexOut.resolve(blockId + ".png");
        savePng(cache, result, out);
    }

    private void createItemTexture(CachedOutput cache, String itemName, ItemBase itemBase, String tint, Path itemTexOut) throws IOException {
        BufferedImage baseImg = readPng(itemBase.resolve(itemBasesDir));
        BufferedImage result = applyColorMultiply(baseImg, tint);
        Path out = itemTexOut.resolve(itemName + ".png");
        savePng(cache, result, out);
    }

    private void createItemMultilayerTexture(CachedOutput cache, String outName, List<Path> layers, String tint, Path itemTexOut) throws IOException {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("No layers provided for " + outName);
        }
        BufferedImage base = readPng(layers.get(0));
        base = applyColorMultiply(base, tint);

        BufferedImage result = base;
        for (int i = 1; i < layers.size(); i++) {
            BufferedImage overlay = readPng(layers.get(i));
            result = alphaComposite(result, overlay);
        }

        Path out = itemTexOut.resolve(outName + ".png");
        savePng(cache, result, out);
    }

    private BufferedImage readPng(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Missing template: " + path);
        }
        BufferedImage img = ImageIO.read(path.toFile());
        if (img == null) {
            throw new IOException("Failed to read image: " + path);
        }
        if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
            BufferedImage converted = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            converted.getGraphics().drawImage(img, 0, 0, null);
            return converted;
        }
        return img;
    }

    private BufferedImage applyColorMultiply(BufferedImage image, String hexColor) {
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        int rgbVal = Integer.parseInt(hex, 16);
        float r = ((rgbVal >> 16) & 0xFF) / 255f;
        float g = ((rgbVal >> 8) & 0xFF) / 255f;
        float b = (rgbVal & 0xFF) / 255f;

        // Convert tint to HSB
        float[] hsbTint = java.awt.Color.RGBtoHSB((int)(r*255), (int)(g*255), (int)(b*255), null);
        float tintHue = hsbTint[0];
        float tintSat = hsbTint[1];
        float tintBri = hsbTint[2];

        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) {
                    out.setRGB(x, y, 0);
                    continue;
                }

                int pr = (argb >> 16) & 0xFF;
                int pg = (argb >> 8) & 0xFF;
                int pb = argb & 0xFF;

                // Calculate luminance of the pixel (assuming grayscale or close to it)
                // Standard weighting: 0.299 R + 0.587 G + 0.114 B
                float lum = (pr * 0.299f + pg * 0.587f + pb * 0.114f) / 255f;

                // Adjust luminance based on the tint's brightness.
                // We want to preserve the shading (lum) but scale it by the tint's brightness.
                float newBri = lum * tintBri;

                // Reconstruct color using Tint Hue/Sat and Adjusted Brightness
                int newRgb = java.awt.Color.HSBtoRGB(tintHue, tintSat, newBri);

                // Combine with original alpha
                int resultArgb = (a << 24) | (newRgb & 0x00FFFFFF);
                out.setRGB(x, y, resultArgb);
            }
        }
        return out;
    }

    private BufferedImage alphaComposite(BufferedImage base, BufferedImage overlay) {
        BufferedImage out = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int bg = base.getRGB(x, y);
                int fg = overlay.getRGB(x, y);

                int aFg = (fg >>> 24) & 0xFF;
                if (aFg == 0) {
                    out.setRGB(x, y, bg);
                    continue;
                }
                int aBg = (bg >>> 24) & 0xFF;

                int rBg = (bg >> 16) & 0xFF;
                int gBg = (bg >> 8) & 0xFF;
                int bBg = bg & 0xFF;

                int rFg = (fg >> 16) & 0xFF;
                int gFg = (fg >> 8) & 0xFF;
                int bFg = fg & 0xFF;

                int aOut = aFg + (aBg * (255 - aFg) + 127) / 255;
                int rOut = (rFg * aFg + rBg * (255 - aFg)) / 255;
                int gOut = (gFg * aFg + gBg * (255 - aFg)) / 255;
                int bOut = (bFg * aFg + bBg * (255 - aFg)) / 255;

                int argb = (aOut << 24) | (rOut << 16) | (gOut << 8) | bOut;
                out.setRGB(x, y, argb);
            }
        }
        return out;
    }


    private void savePng(CachedOutput cache, BufferedImage image, Path out) throws IOException {
        Files.createDirectories(out.getParent());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", bos);
        byte[] data = bos.toByteArray();
        cache.writeIfNeeded(out, data, Hashing.sha1().hashBytes(data));
    }

    @Override
    public String getName() {
        return "PY Ore Texture Generator";
    }
}
