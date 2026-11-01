package com.roll_54.roll_mod.minestar.chat.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.minestar.chat.emoji.GifStrip;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>Fetches and decodes a chat GIF for {@link GifViewer}: a Tenor/Giphy page link is resolved to its
 * .gif through the page's og:image; image files are fetched as they are. Blocking — call off the
 * render thread.
 */
@OnlyIn(Dist.CLIENT)
final class MediaLoader {

    private static final long MAX_BYTES = 12L * 1024 * 1024;
    private static final Pattern OG_IMAGE = Pattern.compile(
            "<meta[^>]+(?:property|name)=\"(?:og:image|twitter:image)\"[^>]+content=\"([^\"]+)\"");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();

    private MediaLoader() {}

    /** The link's frames (up to {@code maxFrames}, shrunk to {@code maxSide}); a still image is one frame. */
    static GifStrip.Frames load(String link, int maxFrames, int maxSide) throws IOException, InterruptedException {
        byte[] bytes = download(resolve(link));
        if (bytes.length > 3 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return GifStrip.frames(bytes, maxFrames, maxSide);
        }
        BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (image == null) throw new IOException("not an image this viewer can show");
        double scale = Math.min(1.0, (double) maxSide / Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage fitted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = fitted.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return new GifStrip.Frames(List.of(fitted), List.of(1000));
    }

    /** A page link (Tenor, Giphy, …) → its GIF, through the page's og:image; image files stay as they are. */
    private static String resolve(String link) throws IOException, InterruptedException {
        if (link.toLowerCase(Locale.ROOT).matches("https?://\\S+\\.gif(?:[?#]\\S*)?")) return link;
        HttpResponse<String> page = HTTP.send(request(link), HttpResponse.BodyHandlers.ofString());
        if (page.statusCode() != 200) throw new IOException("page answered " + page.statusCode());
        Matcher matcher = OG_IMAGE.matcher(page.body());
        while (matcher.find()) {
            String image = matcher.group(1).replace("&amp;", "&");
            if (image.toLowerCase(Locale.ROOT).contains(".gif")) return image;
        }
        throw new IOException("no GIF on the page");
    }

    private static byte[] download(String gif) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = HTTP.send(request(gif), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IOException("GIF answered " + response.statusCode());
        if (response.body().length > MAX_BYTES) throw new IOException("GIF too large");
        return response.body();
    }

    private static HttpRequest request(String link) {
        return HttpRequest.newBuilder(URI.create(link)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 (Minecraft; roll_mod GIF preview)").GET().build();
    }

    /** A decoded frame as a texture image (render thread). */
    static NativeImage toNative(BufferedImage image) {
        NativeImage out = new NativeImage(image.getWidth(), image.getHeight(), true);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                // NativeImage stores ABGR.
                out.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16));
            }
        }
        return out;
    }

    /** The frame showing {@code elapsed} ms into a loop of these delays. */
    static int frameAt(List<Integer> delays, int loop, long elapsed) {
        long t = elapsed % Math.max(1, loop);
        for (int i = 0; i < delays.size(); i++) {
            t -= delays.get(i);
            if (t < 0) return i;
        }
        return 0;
    }
}
