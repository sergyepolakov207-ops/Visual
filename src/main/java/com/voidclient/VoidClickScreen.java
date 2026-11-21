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
    // palette: near-black panel, soft purple glow, purple switches
    private static final int BG = 0xFA08080D, ROW = 0xFF0E0E15, LINE = 0xFF191925, LINE_HOVER = 0xFF3A2F7A;
    private static final int WHITE = 0xFFF1F1F8, MUTED = 0xFF5D5D72, SEP = 0xFF2A2A3A;
    private static final int PURPLE = 0xFF6B4DF2, TOG_OFF = 0xFF1A1A25, KNOB = 0xFFE4E4F3, KNOB_OFF = 0xFF5A5A70;
    private static final int ROW_H = 30, GAP = 8, FOOT = 34;

    private int x, y, w, h, cols, colW, gridX, gridY, gridBottom, scroll;
    private int searchX, searchY, searchW, searchH;
    private final int[] tabL = new int[5], tabR = new int[5];
    private String search = "";
    private boolean searchFocused;
    private boolean draggingHudScale;
    private VoidModules.Category category = VoidModules.Category.RENDER;
    private final List<String> modules = new ArrayList<>();
    private long last = System.nanoTime();
    private float intro;

    public VoidClickScreen() { super(Component.literal("SWIFT VISUALS")); }

    private Component c(String s) { return Component.literal(s); }
    private void txt(GuiGraphics g, String s, int px, int py, int color) { g.drawString(font, c(s), px, py, color, false); }
    private int tw(String s) { return font.width(c(s)); }
    private void box(GuiGraphics g, int l, int t, int r, int b, int color) { if (r > l && b > t) g.fill(l, t, r, b, color); }

    /** Filled rounded rectangle (scanline corners). */
    private void rrect(GuiGraphics g, int l, int t, int r, int b, int rad, int color) {
        int hh = b - t;
        if (hh <= 0 || r <= l) return;
        rad = Math.max(0, Math.min(rad, Math.min(hh, r - l) / 2));
        for (int yy = 0; yy < hh; yy++) {
            int d = Math.min(yy, hh - 1 - yy), inset = 0;
            if (d < rad) {
                double dy = rad - d - 0.5;
                inset = (int) Math.ceil(rad - Math.sqrt(Math.max(0, rad * rad - dy * dy)));
            }
            g.fill(l + inset, t + yy, r - inset, t + yy + 1, color);
        }
    }

    /** Rounded card with a 1px border. */
    private void card(GuiGraphics g, int l, int t, int r, int b, int rad, int fill, int line) {
        rrect(g, l, t, r, b, rad, line);
        rrect(g, l + 1, t + 1, r - 1, b - 1, Math.max(0, rad - 1), fill);
    }

    private void pill(GuiGraphics g, int l, int t, int ww, int hh, int color) { rrect(g, l, t, l + ww, t + hh, hh / 2, color); }

    private void toggle(GuiGraphics g, int sx, int sy, boolean on) {
        pill(g, sx, sy, 34, 18, on ? PURPLE : TOG_OFF);
        int cx = sx + (on ? 25 : 9), col = on ? KNOB : KNOB_OFF;
        for (int dy = -5; dy <= 5; dy++) {
            int half = (int) Math.sqrt(25 - dy * dy);
            g.fill(cx - half, sy + 9 + dy, cx + half + 1, sy + 10 + dy, col);
        }
    }

    private String label(VoidModules.Category cat) {
        return switch (cat) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Visuals";
            case PLAYER -> "Player";
            case MISC -> "Utility";
        };
    }

    private void tabs() {
        VoidModules.Category[] cats = VoidModules.Category.values();
        int tx = gridX;
        for (int i = 0; i < cats.length && i < tabL.length; i++) {
            tabL[i] = tx;
            tabR[i] = tx + tw(label(cats[i]));
            tx = tabR[i] + tw("/") + 14;
        }
    }

    private int visibleRows() { return Math.max(1, (gridBottom - gridY + GAP) / (ROW_H + GAP)); }
    private int totalRows() { return (modules.size() + cols - 1) / cols; }

    @Override protected void init() {
        w = Math.min(width - 12, 460);
        h = Math.min(height - 12, 280);
        x = (width - w) / 2;
        y = (height - h) / 2;
        gridX = x + 14;
        gridY = y + 48;
        gridBottom = y + h - FOOT;
        cols = w >= 330 ? 2 : 1;
        colW = (w - 28 - (cols - 1) * GAP) / cols;
        searchW = Math.max(70, Math.min(110, w - 320));
        searchH = 18;
        searchX = x + w - 14 - searchW;
        searchY = y + 9;
        intro = 0;
        draggingHudScale = false;
        rebuild();
    }

    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String n : VoidModules.all().keySet()) {
            if (VoidModules.categoryOf(n) == category
                    && (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q)
                    || VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q))) modules.add(n);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, totalRows() - visibleRows())));
    }

    private int sliderX() { return gridX + tw("HUD scale") + 12; }
    private int sliderW() { return Math.max(20, (x + w - 14) - tw("1.50x") - 12 - sliderX()); }

    private void applyHudScaleFromMouse(double mx) {
        int bx = sliderX(), bw = sliderW();
        float pct = (float) Math.max(0.0, Math.min(1.0, (mx - bx) / bw));
        float value = 0.75f + pct * 0.75f;
        VoidModules.setHudScale(Math.round(value * 20f) / 20f);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - last) / 1e9f));
        last = now;
        intro = Math.min(1, intro + dt * 2.8f);
        int lift = Math.round((1 - intro) * 10), py = y + lift;

        g.fill(0, 0, width, height, 0xB0000000);
        card(g, x, py, x + w, py + h, 10, BG, LINE);
        g.fillGradient(x + 8, py + 1, x + w - 8, py + 70, 0x55583EC8, 0x00583EC8);

        // header: category tabs + search
        tabs();
        VoidModules.Category[] cats = VoidModules.Category.values();
        for (int i = 0; i < cats.length && i < tabL.length; i++) {
            boolean active = cats[i] == category;
            boolean hover = mx >= tabL[i] && mx <= tabR[i] && my >= py + 10 && my <= py + 26;
            txt(g, label(cats[i]), tabL[i], py + 15, active ? WHITE : hover ? 0xFF9A9AB5 : MUTED);
            if (i < cats.length - 1) txt(g, "/", tabR[i] + 6, py + 15, SEP);
        }
        card(g, searchX, py + 9, searchX + searchW, py + 9 + searchH, 6, ROW, searchFocused ? LINE_HOVER : LINE);
        txt(g, search.isEmpty() && !searchFocused ? "Поиск" : searchFocused ? search + "_" : search,
                searchX + 8, py + 14, search.isEmpty() ? MUTED : WHITE);
        box(g, gridX, py + 36, x + w - 14, py + 37, LINE);

        // module rows
        int vr = visibleRows();
        for (int r = 0; r < vr; r++) {
            for (int cc = 0; cc < cols; cc++) {
                int idx = (scroll + r) * cols + cc;
                if (idx >= modules.size()) break;
                String n = modules.get(idx);
                int rx = gridX + cc * (colW + GAP), ry = gridY + r * (ROW_H + GAP) + lift;
                boolean on = VoidModules.isEnabled(n);
                boolean hover = mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H;
                card(g, rx, ry, rx + colW, ry + ROW_H, 8, ROW, hover ? LINE_HOVER : LINE);
                txt(g, n, rx + 14, ry + (ROW_H - 8) / 2, on ? WHITE : MUTED);
                toggle(g, rx + colW - 14 - 34, ry + (ROW_H - 18) / 2, on);
            }
        }
        if (modules.isEmpty()) txt(g, "Ничего не найдено", gridX, gridY + 4, MUTED);
        // fade out at the bottom when more rows are hidden below
        if (scroll + vr < totalRows())
            g.fillGradient(x + 8, gridBottom - 34 + lift, x + w - 8, gridBottom + lift, 0x0008080D, BG);

        // footer: HUD scale
        box(g, gridX, py + h - 30, x + w - 14, py + h - 29, LINE);
        txt(g, "HUD scale", gridX, py + h - 21, MUTED);
        String scale = String.format(Locale.ROOT, "%.2fx", VoidModules.hudScale());
        txt(g, scale, x + w - 14 - tw(scale), py + h - 21, WHITE);
        int bx = sliderX(), bw = sliderW(), by = py + h - 18;
        box(g, bx, by, bx + bw, by + 3, TOG_OFF);
        int knob = bx + Math.round((VoidModules.hudScale() - .75f) / .75f * bw);
        box(g, bx, by, knob, by + 3, PURPLE);
        pill(g, knob - 5, by - 4, 10, 11, KNOB);

        super.render(g, mx, my, partial);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        tabs();
        VoidModules.Category[] cats = VoidModules.Category.values();
        for (int i = 0; i < cats.length && i < tabL.length; i++) {
            if (mx >= tabL[i] && mx <= tabR[i] && my >= y + 10 && my <= y + 26) {
                category = cats[i]; scroll = 0; searchFocused = false; rebuild(); return true;
            }
        }
        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= y + 9 && my <= y + 9 + searchH;
        if (searchFocused) return true;
        int vr = visibleRows();
        for (int r = 0; r < vr; r++) {
            for (int cc = 0; cc < cols; cc++) {
                int idx = (scroll + r) * cols + cc;
                if (idx >= modules.size()) break;
                int rx = gridX + cc * (colW + GAP), ry = gridY + r * (ROW_H + GAP);
                if (mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H) {
                    VoidModules.toggle(modules.get(idx));
                    return true;
                }
            }
        }
        int bx = sliderX(), bw = sliderW();
        if (mx >= bx - 5 && mx <= bx + bw + 5 && my >= y + h - 27 && my <= y + h - 8) {
            draggingHudScale = true;
            applyHudScaleFromMouse(mx);
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (draggingHudScale && e.button() == 0) {
            applyHudScaleFromMouse(e.x());
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (draggingHudScale && e.button() == 0) {
            draggingHudScale = false;
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int max = Math.max(0, totalRows() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override public boolean charTyped(CharacterEvent e) {
        if (!searchFocused) return super.charTyped(e);
        char ch = (char) e.codepoint();
        if (ch >= 32 && ch != 127 && search.length() < 48) { search += ch; scroll = 0; rebuild(); }
        return true;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if (searchFocused) {
            if (e.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); rebuild(); return true; }
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
        }
        return super.keyPressed(e);
    }

    @Override public boolean isPauseScreen() { return false; }
}