package com.roll_54.roll_mod.items.electricItems.orescanner.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.items.electricItems.orescanner.OreScanResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The ore scanner's chunk grid: one cell per scanned chunk, north up, the player's chunk in the middle and outlined.
 * Each cell is the chunk's 16 x 16 block columns, each drawn in the colour of the material with the most ore blocks
 * in that column at any height — 200 hematite outweigh 90 bauxite in the same column. With a search, the column shows
 * its most abundant matching material instead, so a searched ore shows up under a more common one. Hovering lists
 * the chunk's counts and the hovered column's.
 */
public class OreChunkMap extends UIElement {

    private static final int GAP = 2;
    /** Empty cells; {@link MapColors} keeps ore colours readable against it. */
    static final int EMPTY = 0xFF1E1E1E;
    private static final int OUTLINE = 0xFFFFFFFF;
    /** The hovered block column: its box on the map and its line in the tooltip. */
    private static final int COLUMN = 0xFF22D6CA;
    /** Where the player stood when scanning, and F3's axis colours for the lines pointing from it. */
    private static final int PLAYER = 0xFF30FF30;
    private static final int AXIS_X = 0xFFFF3030;
    private static final int AXIS_Z = 0xFF3030FF;

    private final Supplier<OreScanResult> result;
    private final Predicate<String> matches;

    /** The cell under the mouse as of the last frame, or -1; drawing finds it, the tooltip reads it. */
    private int hoveredCol = -1;
    private int hoveredRow = -1;
    /** The block column under the mouse within the hovered chunk, 0..15 each. */
    private int hoveredX = -1;
    private int hoveredZ = -1;

    public OreChunkMap(Supplier<OreScanResult> result, Predicate<String> matches) {
        this.result = result;
        this.matches = matches;
        addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            OreScanResult scan = result.get();
            if (scan == null || hoveredCol < 0) return;
            e.hoverTooltips = new HoverTooltips(tooltip(scan, hoveredCol, hoveredRow, hoveredX, hoveredZ),
                    null, null, ItemStack.EMPTY);
        });
    }

    private static List<Component> tooltip(OreScanResult scan, int col, int row, int x, int z) {
        List<Component> lines = new ArrayList<>();
        int chunkX = scan.centerX() - scan.radius() + col;
        int chunkZ = scan.centerZ() - scan.radius() + row;
        // World block coordinates throughout: the chunk as the blocks it spans, the column as its own X and Z.
        lines.add(Component.translatable("gui.roll_mod.ore_scanner.chunk",
                        chunkX * 16, chunkX * 16 + 15, chunkZ * 16, chunkZ * 16 + 15)
                .withStyle(ChatFormatting.WHITE));
        OreScanResult.Chunk chunk = scan.at(col, row);
        if (chunk.totals().isEmpty()) {
            lines.add(Component.translatable("gui.roll_mod.ore_scanner.no_ores").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        chunk.totals().forEach((material, count) -> lines.add(line(material, count)));
        int[] column = chunk.column(x, z);
        if (column.length > 0) {
            lines.add(Component.translatable("gui.roll_mod.ore_scanner.column", chunkX * 16 + x, chunkZ * 16 + z)
                    .withStyle(style -> style.withColor(COLUMN & 0xFFFFFF)));
            for (int i = 0; i < column.length; i += 2) lines.add(line(scan.palette().get(column[i]), column[i + 1]));
        }
        return lines;
    }

    private static Component line(String material, int count) {
        MutableComponent line = OreScannerUI.materialName(material)
                .withStyle(style -> style.withColor(OreScannerUI.color(material) & 0xFFFFFF));
        return line.append(Component.literal(": " + count).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        OreScanResult scan = result.get();
        if (scan == null) {
            hoveredCol = hoveredRow = -1;
            return;
        }
        int n = scan.size();
        float x0 = getContentX();
        float y0 = getContentY();
        float cell = (Math.min(getContentWidth(), getContentHeight()) - GAP * (n - 1)) / n;

        // Which colour each palette entry draws in, and whether the search keeps it; once per frame, not per pixel.
        int[] colors = new int[scan.palette().size()];
        boolean[] shown = new boolean[colors.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = MapColors.forMap(OreScannerUI.color(scan.palette().get(i)));
            shown[i] = matches.test(scan.palette().get(i));
        }

        hoveredCol = hoveredRow = hoveredX = hoveredZ = -1;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                float cellLeft = x0 + col * (cell + GAP);
                float cellTop = y0 + row * (cell + GAP);
                int left = Math.round(cellLeft);
                int top = Math.round(cellTop);
                int right = Math.round(cellLeft + cell);
                int bottom = Math.round(cellTop + cell);
                drawCell(context, scan.at(col, row), cellLeft, cellTop, cell, colors, shown);
                if (context.mouseX >= left && context.mouseX < right
                        && context.mouseY >= top && context.mouseY < bottom) {
                    hoveredCol = col;
                    hoveredRow = row;
                    hoveredX = Math.min(15, (int) ((context.mouseX - cellLeft) / cell * 16));
                    hoveredZ = Math.min(15, (int) ((context.mouseY - cellTop) / cell * 16));
                }
                if (col == scan.radius() && row == scan.radius()) {
                    outline(context, left - 1, top - 1, right + 1, bottom + 1, OUTLINE);
                }
            }
        }
        if (hoveredCol >= 0) {
            float cellLeft = x0 + hoveredCol * (cell + GAP);
            float cellTop = y0 + hoveredRow * (cell + GAP);
            outline(context, Math.round(cellLeft), Math.round(cellTop),
                    Math.round(cellLeft + cell), Math.round(cellTop + cell), 0x80FFFFFF);
            // One pixel outside the column, so the box stays visible around a 2-3 px column on a 9x9 map.
            outline(context, px(cellLeft, cell, hoveredX) - 1, px(cellTop, cell, hoveredZ) - 1,
                    px(cellLeft, cell, hoveredX + 1) + 1, px(cellTop, cell, hoveredZ + 1) + 1, COLUMN);
        }
        drawPlayer(context, scan, x0, y0, cell);
    }

    /**
     * A green dot on the block column the player scanned from, at least 3 px across, with F3's axis lines out of it:
     * red towards +X (east, right on the map), blue towards +Z (south, down).
     */
    private static void drawPlayer(GUIContext context, OreScanResult scan, float x0, float y0, float cell) {
        float cellLeft = x0 + scan.radius() * (cell + GAP);
        float cellTop = y0 + scan.radius() * (cell + GAP);
        int cx = Math.round(cellLeft + cell * (Math.floorMod(scan.playerX(), 16) + 0.5f) / 16f);
        int cy = Math.round(cellTop + cell * (Math.floorMod(scan.playerZ(), 16) + 0.5f) / 16f);
        int half = Math.max(1, Math.round(cell / 32f));
        int length = Math.round(Math.max(12f, Math.min(cell * 0.6f, 30f)));
        context.graphics.fill(cx, cy - 1, cx + length, cy + 1, AXIS_X);
        context.graphics.fill(cx - 1, cy, cx + 1, cy + length, AXIS_Z);
        context.graphics.fill(cx - half - 1, cy - half - 1, cx + half + 1, cy + half + 1, 0xFF000000);
        context.graphics.fill(cx - half, cy - half, cx + half, cy + half, PLAYER);
    }

    /** Screen pixel where block column {@code i} (0..16) of a cell starting at {@code start} begins. */
    private static int px(float start, float cell, int i) {
        return Math.round(start + cell * i / 16f);
    }

    /** Column by column, merging runs of the same colour along a row into one fill. */
    private static void drawCell(GUIContext context, OreScanResult.Chunk chunk, float left, float top, float cell,
                                 int[] colors, boolean[] shown) {
        context.graphics.fill(px(left, cell, 0), px(top, cell, 0), px(left, cell, 16), px(top, cell, 16), EMPTY);
        if (chunk.totals().isEmpty()) return;
        for (int z = 0; z < 16; z++) {
            int y1 = px(top, cell, z);
            int y2 = px(top, cell, z + 1);
            int runStart = 0;
            int runColor = columnColor(chunk.column(0, z), colors, shown);
            for (int x = 1; x <= 16; x++) {
                int color = x < 16 ? columnColor(chunk.column(x, z), colors, shown) : 0;
                if (x < 16 && color == runColor) continue;
                if (runColor != 0) context.graphics.fill(px(left, cell, runStart), y1, px(left, cell, x), y2, runColor);
                runStart = x;
                runColor = color;
            }
        }
    }

    /** The colour of the column's most abundant shown material (columns are sorted most first), or 0 for none. */
    private static int columnColor(int[] column, int[] colors, boolean[] shown) {
        for (int i = 0; i < column.length; i += 2) {
            if (shown[column[i]]) return colors[column[i]];
        }
        return 0;
    }

    private static void outline(GUIContext context, int left, int top, int right, int bottom, int color) {
        context.graphics.fill(left, top, right, top + 1, color);
        context.graphics.fill(left, bottom - 1, right, bottom, color);
        context.graphics.fill(left, top, left + 1, bottom, color);
        context.graphics.fill(right - 1, top, right, bottom, color);
    }
}
