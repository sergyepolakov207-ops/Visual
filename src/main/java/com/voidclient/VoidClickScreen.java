package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int TEXT = 0xFFF1F0F7;
    private static final int MUTED = 0xFF9294A8;
    private static final int ACCENT = 0xFFB47CFF;
    private static final int ACCENT_DARK = 0xFF62458C;

    private int px, py, pw, ph, rail, cx, cw, gridY, gridBottom, cardW, cardH, gap, cols;
    private int scroll, searchX, searchY, searchW;
    private boolean searchFocused, blurAvailable = true;
    private String search = "";
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> modules = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        pw = width >= 1000 ? Math.min(900, Math.max(620, (int)(width * 0.56)))
                           : Math.max(280, (int)(width * 0.94));
        ph = Math.max(220, Math.min(height - 24, (int)(height * 0.84)));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        rail = width < 520 ? 40 : 54;
        cx = px + rail + 12;
        cw = pw - rail - 24;
        gap = 8;
        cols = cw >= 570 ? 3 : cw >= 360 ? 2 : 1;
        cardW = (cw - gap * (cols - 1)) / cols;
        cardH = width < 520 ? 37 : 43;
        searchX = cx;
        searchY = py + 104;
        searchW = cw;
        gridY = searchY + 36;
        gridBottom = py + ph - 18;
        rebuild();
    }

    private int visibleRows() {
        return Math.max(1, (gridBottom - gridY + gap) / (cardH + gap));
    }

    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String name : VoidModules.all().keySet()) {
            if (category == VoidModules.Category.HUD && !name.equals("HUD")) continue;
            if (VoidModules.categoryOf(name) == category
                && (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q))) {
                modules.add(name);
            }
        }
        int rows = (modules.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows - visibleRows())));
    }

    private void rounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private String categoryName() {
        return switch (category) {
            case VISUALS -> "VISUALS";
            case HUD -> "HUD";
            case PVP -> "PVP";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (blurAvailable) {
            try { renderBlurredBackground(g); }
            catch (RuntimeException | LinkageError e) { blurAvailable = false; }
        }
        g.fill(0, 0, width, height, 0x76000000);

        // Reference-inspired translucent glass panel and narrow icon rail.
        rounded(g, px, py, pw, ph, 13, 0xA9151411);
        g.fill(px + 2, py + 2, px + pw - 2, py + 3, 0x66FFFFFF);
        g.fill(px + 1, py + 1, px + rail, py + ph - 1, 0x881A1915);
        g.fill(px + rail, py + 1, px + rail + 1, py + ph - 1, 0x553D3930);
        g.fill(px + rail, py + 53, px + pw - 1, py + 54, 0x553D3930);
        g.fill(px + rail + 1, py + 1, px + pw - 1, py + 53, 0x70151510);

        // Left icon navigation
        g.drawCenteredString(font, "F", px + rail / 2, py + 18, ACCENT);
        String[] railIcons = {"◉", "▣", "☷"};
        for (int i = 0; i < 3; i++) {
            int iy = py + 66 + i * 48;
            boolean active = category.ordinal() == i;
            if (active) rounded(g, px + 8, iy, rail - 16, 30, 6, 0xFF302B40);
            g.drawCenteredString(font, railIcons[i], px + rail / 2, iy + 10,
                active ? ACCENT : 0xFF85827A);
        }

        // Header row: selected category and its purpose.
        rounded(g, cx + 2, py + 12, 94, 29, 7, 0x552C2923);
        g.drawString(font, "●  " + categoryName(), cx + 11, py + 22, TEXT, true);
        String description = switch (category) {
            case VISUALS -> "Dedicated to aesthetics, visual storytelling, and creative content";
            case HUD -> "Useful information and clean on-screen indicators";
            case PVP -> "Legitimate combat information and movement helpers";
        };
        int descX = cx + 104;
        int descW = Math.max(0, px + pw - 12 - descX);
        if (descW > 80) {
            rounded(g, descX, py + 12, descW, 29, 7, 0x442C2923);
            g.enableScissor(descX + 10, py + 12, descX + descW - 8, py + 41);
            g.drawString(font, "☷  " + description, descX + 10, py + 22, 0xFFD2CEC4, false);
            g.disableScissor();
        }

        // Search remains available but visually quiet, integrated into the list header.
        searchX = cx + 2;
        searchY = py + 64;
        searchW = cw - 4;
        rounded(g, searchX, searchY, searchW, 25, 6,
            searchFocused ? 0x995D4B70 : 0x664A463D);
        g.drawString(font, "⌕", searchX + 8, searchY + 8, ACCENT, false);
        String searchLabel = search.isEmpty() ? "Search modules..." : search;
        g.enableScissor(searchX + 25, searchY, searchX + searchW - 8, searchY + 25);
        g.drawString(font, searchLabel + (searchFocused ? "_" : ""), searchX + 26,
            searchY + 8, search.isEmpty() ? 0xFFAAA69D : TEXT, false);
        g.disableScissor();

        gridY = searchY + 33;
        gridBottom = py + ph - 15;
        int rowCount = visibleRows();
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= modules.size()) continue;
                String name = modules.get(index);
                int x = cx + col * (cardW + gap);
                int y = gridY + row * (cardH + gap);
                boolean enabled = VoidModules.isEnabled(name);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + cardH;
                rounded(g, x, y, cardW, cardH, 8,
                    hover ? 0xA86B5C80 : 0x805D6378);
                g.fill(x + 1, y + 1, x + cardW - 1, y + 2, 0x22FFFFFF);
                g.drawString(font, "♟", x + 10, y + 15,
                    enabled ? ACCENT : 0xFFAAA69D, false);
                int switchW = 29, switchH = 15;
                int sx = x + cardW - switchW - 10;
                int sy = y + (cardH - switchH) / 2;
                int tagW = 0;
                g.enableScissor(x + 27, y, sx - 7, y + cardH);
                g.drawString(font, name, x + 29, y + (cardH - 8) / 2,
                    enabled ? TEXT : 0xFFB8B4AA, false);
                g.disableScissor();
                rounded(g, sx, sy, switchW, switchH, 8,
                    enabled ? 0xFF7562A0 : 0xFF383831);
                rounded(g, enabled ? sx + 15 : sx + 2, sy + 2, 11, 11, 6,
                    enabled ? 0xFFF7F3FF : 0xFF77766E);
            }
        }
        if (modules.isEmpty()) {
            g.drawCenteredString(font, "No modules in this category", cx + cw / 2,
                gridY + 14, MUTED);
        }

        int rows = (modules.size() + cols - 1) / cols;
        if (rows > rowCount) {
            int track = gridBottom - gridY;
            int thumb = Math.max(18, track * rowCount / rows);
            int ty = gridY + (track - thumb) * scroll / (rows - rowCount);
            rounded(g, px + pw - 5, ty, 3, thumb, 2, ACCENT_DARK);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + 25) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        for (int i = 0; i < 3; i++) {
            int iy = py + 66 + i * 48;
            if (mx >= px + 4 && mx <= px + rail - 4 && my >= iy - 2 && my <= iy + 32) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                rebuild();
                return true;
            }
        }
        for (int row = 0; row < visibleRows(); row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= modules.size()) continue;
                int x = cx + col * (cardW + gap), y = gridY + row * (cardH + gap);
                if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) {
                    VoidModules.toggle(modules.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(e, doubled);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (searchFocused) {
            char ch = (char)e.codepoint();
            if (ch >= 32 && ch != 127 && search.length() < 48) {
                search += ch;
                scroll = 0;
                rebuild();
            }
            return true;
        }
        return super.charTyped(e);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (searchFocused) {
            if (e.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                rebuild();
                return true;
            }
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int rows = (modules.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(Math.max(0, rows - visibleRows()),
            scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
