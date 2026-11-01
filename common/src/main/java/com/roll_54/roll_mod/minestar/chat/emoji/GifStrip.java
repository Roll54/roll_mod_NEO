package com.roll_54.roll_mod.minestar.chat.emoji;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>An animated emoji as the client draws it: every GIF frame fully composed (GIFs store partial
 * frames with disposal rules) and stacked top to bottom in one PNG, plus each frame's delay.
 * {@link #frames} can also serve a GIF viewer.
 */
public record GifStrip(byte[] png, List<Integer> delays) {

    /** Enough for any emoji; longer GIFs are cut (memory and payload stay small). */
    private static final int MAX_FRAMES = 64;
    /** Browsers (and Discord) play delays of 10 ms or less as 100 ms. */
    private static final int FAST_DELAY = 100;

    /** A GIF's frames, each fully composed (disposal rules applied), with their delays in ms. */
    public record Frames(List<BufferedImage> images, List<Integer> delays) {
    }

    static GifStrip decode(byte[] gif) throws IOException {
        Frames frames = frames(gif, MAX_FRAMES, Integer.MAX_VALUE);
        int width = frames.images().get(0).getWidth();
        int height = frames.images().get(0).getHeight();
        BufferedImage strip = new BufferedImage(width, height * frames.images().size(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D s = strip.createGraphics();
        for (int i = 0; i < frames.images().size(); i++) {
            s.drawImage(frames.images().get(i), 0, i * height, null);
        }
        s.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(strip, "png", out);
        return new GifStrip(out.toByteArray(), frames.delays());
    }

    /**
     * Every frame of a GIF (up to {@code maxFrames}), composed as a browser shows it; frames larger
     * than {@code maxSide} are shrunk to fit as they're read (only the small copies are kept).
     */
    public static Frames frames(byte[] gif, int maxFrames, int maxSide) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("no GIF reader");
        }
        ImageReader reader = readers.next();
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(gif))) {
            reader.setInput(input, false);
            int count = Math.min(maxFrames, reader.getNumImages(true));
            if (count < 1) {
                throw new IOException("no frames");
            }
            BufferedImage first = reader.read(0);
            int width = first.getWidth();
            int height = first.getHeight();
            // The logical screen can be larger than the first frame.
            Node screen = child(reader.getStreamMetadata() == null ? null
                    : reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
            if (screen != null) {
                width = Math.max(width, intAttr(screen, "logicalScreenWidth", width));
                height = Math.max(height, intAttr(screen, "logicalScreenHeight", height));
            }
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            double scale = Math.min(1.0, (double) maxSide / Math.max(width, height));
            int outWidth = Math.max(1, (int) Math.round(width * scale));
            int outHeight = Math.max(1, (int) Math.round(height * scale));
            List<BufferedImage> images = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                BufferedImage frame = i == 0 ? first : reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);
                Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
                Node descriptor = child(root, "ImageDescriptor");
                Node control = child(root, "GraphicControlExtension");
                int left = descriptor == null ? 0 : intAttr(descriptor, "imageLeftPosition", 0);
                int top = descriptor == null ? 0 : intAttr(descriptor, "imageTopPosition", 0);
                String disposal = control == null ? "none" : attr(control, "disposalMethod", "none");
                int delay = control == null ? 0 : intAttr(control, "delayTime", 0) * 10;
                delays.add(delay <= 10 ? FAST_DELAY : delay);

                BufferedImage before = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
                Graphics2D g = canvas.createGraphics();
                g.drawImage(frame, left, top, null);
                g.dispose();
                images.add(scale < 1.0 ? shrink(canvas, outWidth, outHeight) : copy(canvas));
                if ("restoreToBackgroundColor".equals(disposal)) {
                    Graphics2D clear = canvas.createGraphics();
                    clear.setComposite(AlphaComposite.Clear);
                    clear.fillRect(left, top, frame.getWidth(), frame.getHeight());
                    clear.dispose();
                } else if (before != null) {
                    canvas = before;
                }
            }
            return new Frames(images, List.copyOf(delays));
        } finally {
            reader.dispose();
        }
    }

    private static BufferedImage shrink(BufferedImage image, int width, int height) {
        BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return small;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static Node child(Node parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (name.equals(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }

    private static String attr(Node node, String name, String fallback) {
        NamedNodeMap attributes = node.getAttributes();
        Node value = attributes == null ? null : attributes.getNamedItem(name);
        return value == null ? fallback : value.getNodeValue();
    }

    private static int intAttr(Node node, String name, int fallback) {
        try {
            return Integer.parseInt(attr(node, name, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
