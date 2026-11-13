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
    private static final int DIM = 0x72050910;
    private static final int PANEL = 0xE90C0D14;
    private static final int HEADER = 0xF0141520;
    private static final int CARD = 0xE51A1D29;
    private static final int CARD_HOVER = 0xF02A2D3C;
    private static final int RAIL = 0xF0090A10;
    private static final int TEXT = 0xFFF1F0F7;
    private static final int MUTED = 0xFF9294A8;
    private static final int ACCENT = 0xFFB47CFF;
    private static final int ACCENT_DARK = 0xFF62458C;
    private static final int OFF = 0xFF3C3E4C;

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
        pw = Math.max(260, Math.min(width - 12, (int)(width * 0.94)));
        ph = Math.max(210, Math.min(height - 12, (int)(height * 0.86)));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        rail = 0;
        cx = px + 18;
        cw = pw - 36;
        gap = 7;
        cols = cw >= 510 ? 3 : cw >= 300 ? 2 : 1;
        cardW = (cw - gap * (cols - 1)) / cols;
        cardH = width < 420 ? 36 : 43;
        searchX = cx;
        searchY = py + 126;
        searchW = cw;
        gridY = searchY + 39;
        gridBottom = py + ph - 35;
        rebuild();
    }

    private int visibleRows() {
        return Math.max(1, (gridBottom - gridY + gap) / (cardH + gap));
    }

    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String name : VoidModules.all().keySet()) {
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
        g.fill(0, 0, width, height, 0xA9000610);

        // New floating dashboard: wide canvas, compact top navigation, clean module grid.
        rounded(g, px, py, pw, ph, 12, 0xF20A0D16);
        g.fill(px + 12, py + 1, px + pw - 12, py + 3, ACCENT);
        g.fill(px + 1, py + 1, px + pw - 1, py + 48, 0xFF111622);
        g.fill(px + 1, py + 48, px + pw - 1, py + 49, 0xFF252333);

        // Brand block
        rounded(g, px + 17, py + 11, 27, 27, 8, 0xFF332447);
        g.drawCenteredString(font, "S", px + 30, py + 20, ACCENT);
        g.drawString(font, "SWIFT", px + 53, py + 10, TEXT, true);
        g.drawString(font, "VISUALS  /  CLIENT", px + 53, py + 25, MUTED, false);
        rounded(g, px + pw - 39, py + 12, 25, 24, 8, 0xFF252B39);
        g.drawCenteredString(font, "×", px + pw - 27, py + 19, TEXT);

        // Category navigation as compact chips
        int tabY = py + 59;
        int tabGap = 8;
        int tabW = Math.max(48, (cw - tabGap * 2) / 3);
        String[] tabs = {"VISUALS", "HUD", "PVP"};
        for (int i = 0; i < 3; i++) {
            int x = cx + i * (tabW + tabGap);
            boolean active = category.ordinal() == i;
            rounded(g, x, tabY, tabW, 27, 8, active ? 0xFF39284E : 0xFF171C28);
            if (active) g.fill(x + 9, tabY + 24, x + tabW - 9, tabY + 26, ACCENT);
            g.drawCenteredString(font, tabs[i], x + tabW / 2, tabY + 9,
                active ? 0xFFF2E8FF : MUTED);
        }

        // Page title and count
        int headingY = py + 96;
        g.drawString(font, categoryName(), cx + 1, headingY, TEXT, true);
        String sub = "Choose modules and customize your client";
        g.drawString(font, sub, cx + 1, headingY + 13, MUTED, false);

        // Search control
        searchX = cx;
        searchY = py + 126;
        searchW = cw;
        rounded(g, searchX, searchY, searchW, 29, 8,
            searchFocused ? 0xFF292136 : 0xFF171C28);
        if (searchFocused) g.fill(searchX + 1, searchY + 5, searchX + 2, searchY + 24, ACCENT);
        g.drawString(font, "⌕", searchX + 11, searchY + 10, ACCENT, false);
        String label = search.isEmpty() ? "Find a module..." : search;
        g.enableScissor(searchX + 29, searchY, searchX + searchW - 8, searchY + 29);
        g.drawString(font, label + (searchFocused ? "_" : ""), searchX + 30,
            searchY + 10, search.isEmpty() ? MUTED : TEXT, false);
        g.disableScissor();

        gridY = searchY + 39;
        gridBottom = py + ph - 35;
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
                    hover ? 0xFF252B3B : 0xFF171D29);
                if (enabled) {
                    rounded(g, x + 8, y + 8, 23, 23, 7, 0xFF39294D);
                    g.drawCenteredString(font, "✓", x + 19, y + 15, ACCENT);
                } else {
                    rounded(g, x + 8, y + 8, 23, 23, 7, 0xFF242B39);
                    g.drawCenteredString(font, "•", x + 19, y + 15, MUTED);
                }
                int sx = x + cardW - 34, sy = y + (cardH - 16) / 2;
                g.enableScissor(x + 39, y, sx - 5, y + cardH);
                g.drawString(font, name, x + 39, y + (cardH - 8) / 2,
                    enabled ? 0xFFF4E9FF : TEXT, false);
                g.disableScissor();
                rounded(g, sx, sy, 27, 16, 8, enabled ? 0xFF8058AD : 0xFF343B49);
                rounded(g, enabled ? sx + 13 : sx + 2, sy + 2, 12, 12, 6,
                    0xFFF8F4FF);
            }
        }
        if (modules.isEmpty()) {
            g.drawCenteredString(font, "No modules found", cx + cw / 2, gridY + 15, MUTED);
        }

        int rows = (modules.size() + cols - 1) / cols;
        if (rows > rowCount) {
            int track = gridBottom - gridY;
            int thumb = Math.max(18, track * rowCount / rows);
            int ty = gridY + (track - thumb) * scroll / (rows - rowCount);
            rounded(g, px + pw - 7, ty, 3, thumb, 2, ACCENT_DARK);
        }

        int footerY = py + ph - 23;
        g.fill(cx, footerY - 7, px + pw - 13, footerY - 6, 0xFF252333);
        int activeCount = 0;
        for (String n : modules) if (VoidModules.isEnabled(n)) activeCount++;
        g.drawString(font, "SWIFT VISUALS", cx + 1, footerY, MUTED, false);
        String count = activeCount + " ENABLED  /  " + modules.size() + " TOTAL";
        g.drawString(font, count, px + pw - 14 - font.width(count), footerY, ACCENT, false);
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        if (mx >= px + pw - 27 && my >= py + 3 && my <= py + 32) {
            onClose();
            return true;
        }
        if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + 28) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        int tabY = py + 59, tabGap = 8, tabW = Math.max(48, (cw - tabGap * 2) / 3);
        for (int i = 0; i < 3; i++) {
            int x = cx + i * (tabW + tabGap);
            if (mx >= x && mx <= x + tabW && my >= tabY && my <= tabY + 27) {
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
