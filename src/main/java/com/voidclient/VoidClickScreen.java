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
        rail = width < 420 ? 34 : 48;
        cx = px + rail + 10;
        cw = px + pw - cx - 12;
        gap = 7;
        cols = cw >= 510 ? 3 : cw >= 300 ? 2 : 1;
        cardW = (cw - gap * (cols - 1)) / cols;
        cardH = width < 420 ? 33 : 39;
        searchX = cx;
        searchY = py + 93;
        searchW = cw;
        gridY = searchY + 38;
        gridBottom = py + ph - 37;
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
            try {
                renderBlurredBackground(g);
            } catch (RuntimeException | LinkageError e) {
                blurAvailable = false;
            }
        }
        g.fill(0, 0, width, height, DIM);
        rounded(g, px, py, pw, ph, 10, PANEL);
        // Refined frame and layered header preserve the familiar layout.
        g.fill(px + 1, py + 1, px + pw - 1, py + 40, HEADER);
        g.fill(px + 1, py + 1, px + pw - 1, py + 3, ACCENT);
        g.fill(px + 1, py + 40, px + pw - 1, py + 41, 0x553F3454);
        g.fill(px, py + 8, px + 1, py + ph - 8, 0x664D3B69);
        g.fill(px + pw - 1, py + 8, px + pw, py + ph - 8, 0x664D3B69);

        // Sidebar
        g.fill(px + 1, py + 40, px + rail, py + ph - 1, RAIL);
        g.fill(px + rail, py + 40, px + rail + 1, py + ph - 1, 0x553C354F);
        g.drawCenteredString(font, "S", px + rail / 2, py + 13, ACCENT);
        String[] icons = {"✦", "▦", "⚔"};
        for (int i = 0; i < 3; i++) {
            int y = py + 56 + i * 42;
            if (category.ordinal() == i) rounded(g, px + 5, y - 5, rail - 10, 29, 6, 0xFF30263F);
            g.drawCenteredString(font, icons[i], px + rail / 2, y + 4,
                category.ordinal() == i ? ACCENT : MUTED);
        }

        // Header branding and small status
        rounded(g, cx, py + 8, 25, 25, 7, 0xFF302440);
        g.drawCenteredString(font, "S", cx + 12, py + 16, ACCENT);
        g.drawString(font, "SWIFT VISUALS", cx + 33, py + 9, TEXT, true);
        g.drawString(font, "CLIENT MENU  /  1.21.11", cx + 33, py + 23, MUTED, false);
        rounded(g, px + pw - 31, py + 8, 21, 22, 6, 0xFF292331);
        g.drawCenteredString(font, "×", px + pw - 20, py + 14, TEXT);

        // Category tabs
        int tabY = py + 49;
        int tabGap = 7;
        int tabW = (cw - tabGap * 2) / 3;
        String[] tabs = {"VISUALS", "HUD", "PVP"};
        for (int i = 0; i < 3; i++) {
            int x = cx + i * (tabW + tabGap);
            boolean active = category.ordinal() == i;
            rounded(g, x, tabY, tabW, 27, 6, active ? 0xFF352943 : 0x66202430);
            if (active) {
                g.fill(x + 10, tabY + 25, x + tabW - 10, tabY + 27, ACCENT);
                g.fill(x + 1, tabY + 5, x + 2, tabY + 22, ACCENT_DARK);
            }
            g.drawCenteredString(font, tabs[i], x + tabW / 2, tabY + 9, active ? ACCENT : MUTED);
        }

        // Search bar
        rounded(g, searchX, searchY, searchW, 28, 6, searchFocused ? 0xEE211B2C : 0xC51A1B27);
        if (searchFocused) g.fill(searchX + 7, searchY + 5, searchX + 8, searchY + 23, ACCENT);
        g.drawString(font, "⌕", searchX + 10, searchY + 9, ACCENT, false);
        String label = search.isEmpty() ? "Search " + categoryName().toLowerCase() + " modules..." : search;
        g.enableScissor(searchX + 28, searchY, searchX + searchW - 8, searchY + 28);
        g.drawString(font, label + (searchFocused ? "_" : ""), searchX + 29, searchY + 9,
            search.isEmpty() ? MUTED : TEXT, false);
        g.disableScissor();

        // Module cards
        for (int row = 0; row < visibleRows(); row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= modules.size()) continue;
                String name = modules.get(index);
                int x = cx + col * (cardW + gap);
                int y = gridY + row * (cardH + gap);
                boolean enabled = VoidModules.isEnabled(name);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + cardH;
                rounded(g, x, y, cardW, cardH, 5, hover ? CARD_HOVER : CARD);
                g.fill(x + 1, y + 1, x + cardW - 1, y + 2, 0x332F3042);
                if (enabled) rounded(g, x + 1, y + 7, 2, cardH - 14, 1, ACCENT);
                else if (hover) g.fill(x + 1, y + 7, x + 2, y + cardH - 7, 0x886D6680);
                int sw = 26, sh = 14, sx = x + cardW - sw - 9, sy = y + (cardH - sh) / 2;
                g.enableScissor(x + 9, y, sx - 5, y + cardH);
                g.drawString(font, name, x + 10, y + 7, TEXT, false);
                g.disableScissor();
                rounded(g, sx, sy, sw, sh, 7, enabled ? ACCENT_DARK : OFF);
                rounded(g, enabled ? sx + 14 : sx + 2, sy + 2, 10, 10, 5,
                    enabled ? 0xFFFFFFFF : 0xFFB7B5C2);
            }
        }
        if (modules.isEmpty()) g.drawCenteredString(font, "No modules found", cx + cw / 2, gridY + 12, MUTED);

        int totalRows = (modules.size() + cols - 1) / cols;
        if (totalRows > visibleRows()) {
            int trackH = gridBottom - gridY;
            int thumbH = Math.max(16, trackH * visibleRows() / totalRows);
            int thumbY = gridY + (trackH - thumbH) * scroll / (totalRows - visibleRows());
            rounded(g, px + pw - 5, thumbY, 3, thumbH, 2, ACCENT_DARK);
        }

        int footer = py + ph - 24;
        g.fill(cx, footer - 5, px + pw - 10, footer - 4, 0x554A405A);
        g.drawString(font, "SWIFT  /  " + categoryName(), cx + 2, footer + 2, ACCENT, true);
        int enabled = 0;
        for (String n : modules) if (VoidModules.isEnabled(n)) enabled++;
        String status = enabled + " ACTIVE  •  " + modules.size() + " MODULES";
        g.drawString(font, status, px + pw - 14 - font.width(status), footer + 2, MUTED, false);
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
        for (int i = 0; i < 3; i++) {
            int y = py + 51 + i * 42;
            if (mx >= px + 2 && mx <= px + rail - 2 && my >= y && my <= y + 28) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                rebuild();
                return true;
            }
        }
        int tabY = py + 49, tabGap = 7, tabW = (cw - tabGap * 2) / 3;
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
