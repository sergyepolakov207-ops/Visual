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
    // palette: charcoal & slate panel, soft purple accent, green S / purple V brand mark
    private static final int BG = 0xFA0B0C0E, ROW = 0xFF1C1E24, LINE = 0xFF262932, LINE_HOVER = 0xFF4A4E5E;
    private static final int WHITE = 0xFFF1F5F9, MUTED = 0xFF8B92A0, SEP = 0xFF333844;
    private static final int PURPLE = 0xFF6B4DF2, TOG_OFF = 0xFF22252C, KNOB = 0xFFFFFFFF, KNOB_OFF = 0xFF94A3B8;
    private static final int GREEN_S = 0xFF4ADE80, VIOLET_V = 0xFFC084FC, BADGE_BG = 0xFF181A20;
    private static final int ROW_H = 28, GAP = 8, FOOT = 52;

    private int x, y, w, h, cols, colW, gridX, gridY, gridBottom, scroll;
    private int searchX, searchY, searchW, searchH;
    private int clearBtnX, clearBtnY, clearBtnW, clearBtnH;
    private final int[] tabL = new int[5], tabR = new int[5];
    private String search = "";
    private boolean searchFocused;
    private boolean draggingHudScale;
    private VoidModules.Category category = VoidModules.Category.RENDER;
    private final List<String> modules = new ArrayList<>();
    private long last = System.nanoTime();
    private float intro;

    public VoidClickScreen() { super(Component.literal("SV VISUALS")); }

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
        pill(g, sx, sy, 32, 16, on ? PURPLE : TOG_OFF);
        int cx = sx + (on ? 24 : 8), col = on ? KNOB : KNOB_OFF;
        for (int dy = -5; dy <= 5; dy++) {
            int half = (int) Math.sqrt(25 - dy * dy);
            g.fill(cx - half, sy + 8 + dy, cx + half + 1, sy + 9 + dy, col);
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
            tx = tabR[i] + tw("/") + 12;
        }
    }

    private int visibleRows() { return Math.max(1, (gridBottom - gridY + GAP) / (ROW_H + GAP)); }
    private int totalRows() { return (modules.size() + cols - 1) / cols; }

    @Override protected void init() {
        w = Math.min(width - 16, 760);
        h = Math.min(height - 16, 320);
        x = (width - w) / 2;
        y = (height - h) / 2;
        gridX = x + 14;
        gridY = y + 48;
        gridBottom = y + h - FOOT;
        if (w >= 540) cols = 4;
        else if (w >= 340) cols = 2;
        else cols = 1;
        colW = (w - 28 - (cols - 1) * GAP) / cols;

        clearBtnW = tw("Выкл все") + 14;
        clearBtnH = 18;
        clearBtnX = x + w - 14 - clearBtnW;
        clearBtnY = y + 9;

        searchW = Math.max(64, Math.min(115, clearBtnX - (gridX + 265)));
        searchH = 18;
        searchX = clearBtnX - 8 - searchW;
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

    private void disableAllModules() {
        for (String n : VoidModules.all().keySet()) {
            if (VoidModules.isEnabled(n)) {
                VoidModules.toggle(n);
            }
        }
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
        card(g, x, py, x + w, py + h, 12, BG, LINE);
        g.fillGradient(x + 8, py + 1, x + w - 8, py + 70, 0x55583EC8, 0x00583EC8);

        // header: category tabs + search + clear button + SV mark (S green, V purple)
        tabs();
        VoidModules.Category[] cats = VoidModules.Category.values();
        for (int i = 0; i < cats.length && i < tabL.length; i++) {
            boolean active = cats[i] == category;
            boolean hover = mx >= tabL[i] && mx <= tabR[i] && my >= py + 10 && my <= py + 26;
            txt(g, label(cats[i]), tabL[i], py + 14, active ? WHITE : hover ? 0xFF9A9AB5 : MUTED);
            if (i < cats.length - 1) txt(g, "/", tabR[i] + 5, py + 14, SEP);
        }

        if (searchX > tabR[Math.min(cats.length, tabR.length) - 1] + 16) {
            card(g, searchX, py + 9, searchX + searchW, py + 9 + searchH, 6, ROW, searchFocused ? LINE_HOVER : LINE);
            txt(g, search.isEmpty() && !searchFocused ? "Поиск" : searchFocused ? search + "_" : search,
                    searchX + 7, py + 14, search.isEmpty() ? MUTED : WHITE);
        }

        boolean clearHover = mx >= clearBtnX && mx <= clearBtnX + clearBtnW && my >= py + 9 && my <= py + 9 + clearBtnH;
        card(g, clearBtnX, py + 9, clearBtnX + clearBtnW, py + 9 + clearBtnH, 6, ROW, clearHover ? LINE_HOVER : LINE);
        txt(g, "Выкл все", clearBtnX + 7, py + 14, clearHover ? WHITE : MUTED);

        box(g, gridX, py + 36, x + w - 14, py + 37, LINE);

        // module cards
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
                box(g, rx + 9, ry + (ROW_H - 4) / 2, rx + 13, ry + (ROW_H + 4) / 2, on ? WHITE : MUTED);
                txt(g, n, rx + 18, ry + (ROW_H - 8) / 2, on ? WHITE : MUTED);
                toggle(g, rx + colW - 10 - 32, ry + (ROW_H - 16) / 2, on);
            }
        }
        if (modules.isEmpty()) txt(g, "Ничего не найдено", gridX, gridY + 4, MUTED);
        if (scroll + vr < totalRows())
            g.fillGradient(x + 8, gridBottom - 34 + lift, x + w - 8, gridBottom + lift, 0x000B0C0E, BG);

        // footer row 1: HUD scale
        int scaleRowY = py + h - 46;
        box(g, gridX, scaleRowY - 3, x + w - 14, scaleRowY - 2, LINE);
        txt(g, "HUD scale", gridX, scaleRowY + 2, MUTED);
        String scale = String.format(Locale.ROOT, "%.2fx", VoidModules.hudScale());
        txt(g, scale, x + w - 14 - tw(scale), scaleRowY + 2, WHITE);
        int bx = sliderX(), bw = sliderW(), by = scaleRowY + 5;
        box(g, bx, by, bx + bw, by + 3, TOG_OFF);
        int knob = bx + Math.round((VoidModules.hudScale() - .75f) / .75f * bw);
        box(g, bx, by, knob, by + 3, PURPLE);
        pill(g, knob - 5, by - 4, 10, 11, KNOB);

        // footer row 2: neutral profile badge + SV brand mark (S green, V purple)
        int profY = py + h - 24;
        card(g, gridX, profY, gridX + 18, profY + 18, 5, BADGE_BG, LINE);
        txt(g, "V", gridX + 6, profY + 5, WHITE);
        txt(g, "vbiver", gridX + 24, profY + 2, WHITE);
        txt(g, "Member", gridX + 24, profY + 11, MUTED);

        int svX = x + w - 14 - tw("SV");
        txt(g, "S", svX, profY + 5, GREEN_S);
        txt(g, "V", svX + tw("S"), profY + 5, VIOLET_V);

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
        if (mx >= clearBtnX && mx <= clearBtnX + clearBtnW && my >= clearBtnY && my <= clearBtnY + clearBtnH) {
            searchFocused = false;
            disableAllModules();
            return true;
        }
        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH;
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
        int bx = sliderX(), bw = sliderW(), scaleRowY = y + h - 46;
        if (mx >= bx - 5 && mx <= bx + bw + 5 && my >= scaleRowY - 4 && my <= scaleRowY + 14) {
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