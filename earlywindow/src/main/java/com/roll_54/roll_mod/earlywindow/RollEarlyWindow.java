package com.roll_54.roll_mod.earlywindow;

import net.neoforged.fml.earlydisplay.ColourScheme;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.EarlyFramebuffer;
import net.neoforged.fml.earlydisplay.RenderElement;
import net.neoforged.fml.earlydisplay.SimpleBufferBuilder;
import net.neoforged.fml.earlydisplay.SimpleFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL32C.*;

/**
 * roll_mod's loading screen, selected in {@code config/fml.toml} with {@code earlyWindowProvider = "roll_mod"}.
 *
 * <p>It is NeoForge's own early window — the window, its render thread, the hand-over to the game
 * window and the loading overlay that keeps drawing it there are all unchanged — with what it draws
 * replaced by a {@link LoadingScene}. NeoForge keeps those drawing pieces package-private, so they are
 * swapped in by reflection once its renderer is up; if that fails on some other NeoForge version, the
 * stock loading screen is left as it was and the game loads normally.
 */
public class RollEarlyWindow extends DisplayWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger("roll_mod-earlywindow");
    private static final String PACKAGE = "net.neoforged.fml.earlydisplay.";

    @Override
    public String name() {
        return "roll_mod";
    }

    @Override
    public Runnable initialize(String[] arguments) {
        Runnable tick = super.initialize(arguments);
        try {
            install();
        } catch (Throwable t) {
            LOGGER.error("Could not set up the roll_mod loading screen; NeoForge's stays", t);
        }
        return tick;
    }

    /**
     * NeoForge's version makes {@code getClass().getModule()} read NeoForge — for this subclass that is
     * this jar's module, which only code in this module may change. Same steps, from here: find the
     * loading overlay that keeps drawing this screen once the game window takes over.
     */
    @Override
    public void updateModuleReads(ModuleLayer layer) {
        Module neoforge = layer.findModule("neoforge").orElseThrow();
        RollEarlyWindow.class.getModule().addReads(neoforge);
        try {
            Class<?> overlay = Class.forName(neoforge, "net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay");
            Method newInstance = null;
            for (Method m : overlay.getMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getName().equals("newInstance")) newInstance = m;
            }
            if (newInstance == null) throw new NoSuchMethodException("NeoForgeLoadingOverlay.newInstance");
            field("loadingOverlay").set(this, newInstance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("roll_mod early window: NeoForge's loading overlay not found", e);
        }
    }

    /** No Mojang logo over the bases when the game window takes over. */
    @Override
    public void addMojangTexture(int textureId) {
    }

    private void install() throws Exception {
        // The renderer is set up on NeoForge's render thread just after the window opens.
        Future<?> init = (Future<?>) field("initializationFuture").get(this);
        init.get(30, TimeUnit.SECONDS);

        Semaphore renderLock = (Semaphore) field("renderLock").get(this);
        renderLock.acquire();
        try {
            // Black wherever nothing is drawn yet, not NeoForge's red.
            field("colourScheme").set(this, ColourScheme.BLACK);
            RenderElement.DisplayContext old = context();
            field("context").set(this, new RenderElement.DisplayContext(old.width(), old.height(), old.scale(),
                    old.elementShader(), ColourScheme.BLACK, old.performance()));

            SimpleFont font = (SimpleFont) field("font").get(this);
            int fontTexture = (int) method(SimpleFont.class, "textureNumber").invoke(font);
            int lineSpacing = (int) method(SimpleFont.class, "lineSpacing").invoke(font);
            LoadingScene scene = new LoadingScene(font, fontTexture, lineSpacing);

            @SuppressWarnings("unchecked")
            List<RenderElement> elements = (List<RenderElement>) field("elements").get(this);
            elements.clear();
            elements.add(element(scene));
        } finally {
            renderLock.release();
        }
        LOGGER.info("roll_mod loading screen installed");
    }

    /**
     * A {@link RenderElement} that draws the scene. Its constructor takes NeoForge's package-private
     * {@code Initializer}, which hands out an equally private {@code Renderer}; both are implemented
     * here as proxies.
     */
    private RenderElement element(LoadingScene scene) throws Exception {
        ClassLoader loader = RenderElement.class.getClassLoader();
        Class<?> rendererType = Class.forName(PACKAGE + "RenderElement$Renderer", false, loader);
        Class<?> initializerType = Class.forName(PACKAGE + "RenderElement$Initializer", false, loader);

        Object renderer = Proxy.newProxyInstance(loader, new Class<?>[] {rendererType}, handler("accept", args -> {
            scene.render((SimpleBufferBuilder) args[0], fitCanvas((RenderElement.DisplayContext) args[1]));
            return null;
        }));
        Object initializer = Proxy.newProxyInstance(loader, new Class<?>[] {initializerType}, handler("get", args -> renderer));

        Constructor<RenderElement> constructor = RenderElement.class.getDeclaredConstructor(initializerType);
        constructor.setAccessible(true);
        return constructor.newInstance(initializer);
    }

    /* ======================================================== canvas ======================================================== */

    /** Largest canvas side, in pixels — far beyond any screen, but a texture has to stop somewhere. */
    private static final int MAX_CANVAS = 8192;

    /**
     * Keeps the canvas the window's shape and about its resolution: NeoForge draws into a fixed
     * 854x480 (times the FML scale) and fits that to the window, leaving bars on any window that is
     * not 16:9. Here the canvas follows the window: its size in pixels divided by the scale, so the
     * picture covers the whole window at roughly one canvas pixel per screen pixel, and the panels
     * keep the size the scale gives them. NeoForge's own code maps a canvas of the window's shape
     * onto the window edge to edge, both in this window and in the game window afterwards.
     *
     * <p>Called while a frame is being drawn: when the size changed, the new canvas is bound, cleared
     * and handed back, and the scene draws this frame into it.
     */
    private RenderElement.DisplayContext fitCanvas(RenderElement.DisplayContext ctx) {
        try {
            int[] size = windowSize();
            if (size == null) return ctx;
            int scale = Math.max(1, ctx.scale());
            int width = Math.clamp(Math.round(size[0] / (float) scale), 1, MAX_CANVAS / scale);
            int height = Math.clamp(Math.round(size[1] / (float) scale), 1, MAX_CANVAS / scale);
            if (width == ctx.width() && height == ctx.height()) return ctx;
            LOGGER.debug("Loading screen canvas {}x{} (x{}) for a {}x{} window", width, height, scale, size[0], size[1]);

            RenderElement.DisplayContext fitted = new RenderElement.DisplayContext(width, height, scale,
                    ctx.elementShader(), ctx.colourScheme(), ctx.performance());
            Constructor<EarlyFramebuffer> create = EarlyFramebuffer.class.getDeclaredConstructor(RenderElement.DisplayContext.class);
            create.setAccessible(true);
            EarlyFramebuffer framebuffer = create.newInstance(fitted);
            EarlyFramebuffer old = (EarlyFramebuffer) field("framebuffer").get(this);
            field("framebuffer").set(this, framebuffer);
            field("context").set(this, fitted);
            if (old != null) old.close();

            // Continue this frame in the new canvas.
            method(EarlyFramebuffer.class, "activate").invoke(framebuffer);
            glViewport(0, 0, fitted.scaledWidth(), fitted.scaledHeight());
            fitted.elementShader().activate();
            fitted.elementShader().updateScreenSizeUniform(fitted.scaledWidth(), fitted.scaledHeight());
            glClearColor(0f, 0f, 0f, 1f);
            glClear(GL_COLOR_BUFFER_BIT);
            return fitted;
        } catch (Throwable t) {
            if (!canvasFailed) LOGGER.warn("Could not fit the loading screen to the window; it stays 16:9", t);
            canvasFailed = true;
            return ctx;
        }
    }

    private boolean canvasFailed;

    private static final boolean MAC = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");

    /**
     * The window's real framebuffer size.
     *
     * <p>NeoForge keeps it in fields its resize callback updates, but callbacks only run when the main
     * thread polls window events — which it barely does while mods load. A window the window manager
     * resizes or tiles right after it opens then keeps its first size there: NeoForge's own blit and
     * this canvas filled only part of the window, the rest black. So the size is asked from GLFW
     * directly, and NeoForge's fields are corrected with it so its blit to the window uses it too.
     *
     * <p>GLFW only promises that call on the main thread. On Windows and X11 it is a plain window
     * query, safe from this one; macOS (Cocoa) is strict about it, so there NeoForge's fields are used
     * as they are. Written back to NeoForge in both phases: before the hand-over its callback-fed fields
     * lag, after it nothing updates them at all.
     */
    private int[] windowSize() throws ReflectiveOperationException {
        boolean mainThread = "Render thread".equals(Thread.currentThread().getName());
        if (mainThread || !MAC) {
            long window = (long) field("window").get(this);
            int[] w = new int[1], h = new int[1];
            glfwGetFramebufferSize(window, w, h);
            if (w[0] <= 0 || h[0] <= 0) return null;
            // Also once the game has the window: NeoForge removes its resize callbacks at the hand-over
            // but keeps drawing with these fields until the loading overlay takes over.
            field("fbWidth").set(this, w[0]);
            field("fbHeight").set(this, h[0]);
            return new int[] {w[0], h[0]};
        }
        int w = (int) field("fbWidth").get(this);
        int h = (int) field("fbHeight").get(this);
        return w > 0 && h > 0 ? new int[] {w, h} : null;
    }

    private interface Body {
        Object call(Object[] args) throws Throwable;
    }

    /** Implements the one abstract method {@code name}; defaults and Object's methods behave normally. */
    private static InvocationHandler handler(String name, Body body) {
        return (proxy, method, args) -> {
            if (method.getName().equals(name) && !method.isDefault()) return body.call(args);
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            return switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "roll_mod loading scene";
                default -> throw new UnsupportedOperationException(method.toString());
            };
        };
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = DisplayWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method method(Class<?> owner, String name) throws NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }
}
