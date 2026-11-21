package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SV Visual Client menu: black window, sidebar with categories, module cards
 * with description, key bind button and purple switch.
 * Open/close key: Right Shift (closing is handled here, opening in VoidClientClient).
 */
public final class VoidClickScreen extends Screen {
    // palette: black panels, purple switches
    private static final int OUTER = 0xFF050506, PANEL = 0xFF0E0E10, CARD = 0xFF17171A, CARD_HOVER = 0xFF202024;
    private static final int PILL = 0xFF222226, LINE = 0xFF1C1C20, LINE_HOVER = 0xFF3A2F7A;
    private static final int WHITE = 0xFFF2F2F4, MUTED = 0xFF8B8B92, DIM = 0xFF76767C;
    private static final int PURPLE = 0xFF8B5CF6, TOG_OFF = 0xFF2A2A2E, RING = 0xFF3A3A40;
    private static final int KNOB_ON = 0xFFFFFFFF, KNOB_OFF = 0xFFE6E6EA;
    private static final int GREEN = 0xFF6BE34A, PINK = 0xFFE45BD0, BLUE = 0xFF3D9BE0;
    private static final int CARD_H = 40, GAP = 8, PAD = 5;

    /** Only visual categories are shown. Add Category.COMBAT / MOVEMENT here to show more tabs. */
    private static final VoidModules.Category[] CATS = {
            VoidModules.Category.RENDER, VoidModules.Category.PLAYER, VoidModules.Category.MISC
    };

    /** module name -> GLFW key. Call VoidClickScreen.onKey(key) from your key handler so binds work in game. */
    public static final Map<String, Integer> BINDS = new HashMap<>();

    private int x, y, w, h, sbW, mainL, mainR, mainT, mainB, cols, colW, gridX, gridY, gridBottom, scroll;
    private int searchX, searchW, searchH = 16;
    private boolean wide;
    private String search = "";
    private boolean searchFocused;
    private String bindListen;
    private VoidModules.Category category = VoidModules.Category.RENDER;
    private final List<String> modules = new ArrayList<>();
    private long last = System.nanoTime();
    private float intro;

    public VoidClickScreen() { super(Component.literal("SV VISUAL CLIENT")); }

    /** Toggle every module whose bind matches the pressed key. */
    public static void onKey(int key) {
        for (Map.Entry<String, Integer> en : BINDS.entrySet()) {
            if (en.getValue().intValue() == key) VoidModules.toggle(en.getKey());
        }
    }

    private Component c(String s) { return Component.literal(s); }
    private void txt(GuiGraphics g, String s, int px, int py, int color) { g.drawString(font, c(s), px, py, color, false); }
    private int tw(String s) { return font.width(c(s)); }

    private String fit(String s, int maxW) {
        if (tw(s) <= maxW) return s;
        int n = s.length();
        while (n > 0 && tw(s.substring(0, n)) + tw("...") > maxW) n--;
        return s.substring(0, n) + "...";
    }

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

    private void circle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private void toggle(GuiGraphics g, int sx, int sy, boolean on) {
        pill(g, sx, sy, 30, 16, on ? PURPLE : RING);
        if (!on) pill(g, sx + 1, sy + 1, 28, 14, TOG_OFF);
        circle(g, sx + (on ? 22 : 8), sy + 8, 5, on ? KNOB_ON : KNOB_OFF);
    }

    /** Small keyboard icon (18x12). */
    private void keyboard(GuiGraphics g, int bx, int by, int color) {
        card(g, bx, by, bx + 18, by + 12, 3, CARD, color);
        g.fill(bx + 4, by + 3, bx + 6, by + 4, color);
        g.fill(bx + 8, by + 3, bx + 10, by + 4, color);
        g.fill(bx + 12, by + 3, bx + 14, by + 4, color);
        g.fill(bx + 5, by + 7, bx + 13, by + 8, color);
    }

    private void icon(GuiGraphics g, VoidModules.Category cat, int ix, int iy, int color) {
        switch (cat) {
            case RENDER -> {
                g.fill(ix + 2, iy, ix + 7, iy + 2, color);
                g.fill(ix, iy + 3, ix + 9, iy + 5, color);
                g.fill(ix, iy + 6, ix + 9, iy + 8, color);
            }
            case PLAYER -> {
                g.fill(ix + 3, iy, ix + 6, iy + 3, color);
                g.fill(ix + 1, iy + 4, ix + 8, iy + 8, color);
            }
            case MISC -> {
                g.fill(ix + 4, iy, ix + 5, iy + 9, color);
                g.fill(ix, iy + 4, ix + 9, iy + 5, color);
                g.fill(ix + 2, iy + 2, ix + 7, iy + 7, color);
            }
            default -> g.fill(ix + 1, iy + 1, ix + 8, iy + 8, color);
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

    private static String keyLabel(int k) {
        return switch (k) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            default -> {
                String s = GLFW.glfwGetKeyName(k, 0);
                if (s != null) yield s.toUpperCase(Locale.ROOT);
                if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) yield "F" + (k - GLFW.GLFW_KEY_F1 + 1);
                yield "K" + k;
            }
        };
    }

    // ---- layout helpers ----
    private int catY(int i) { return y + PAD + 34 + i * 23; }
    private int toggleX(int rx) { return rx + colW - 10 - 30; }

    private int bindW(String n) {
        if (n.equals(bindListen)) return tw("...") + 10;
        Integer k = BINDS.get(n);
        return k != null ? tw(keyLabel(k)) + 10 : 18;
    }

    private int bindX(String n, int rx) { return toggleX(rx) - 6 - bindW(n); }

    private int visibleRows() { return Math.max(1, (gridBottom - gridY + GAP) / (CARD_H + GAP)); }
    private int totalRows() { return (modules.size() + cols - 1) / cols; }
    private int sliderX() { return gridX + tw("HUD scale") + 12; }
    private int sliderW() { return Math.max(20, (mainR - 10) - tw("1.50x") - 12 - sliderX()); }

    @Override protected void init() {
        w = Math.min(width - 12, 460);
        h = Math.min(height - 12, 280);
        x = (width - w) / 2;
        y = (height - h) / 2;
        wide = w >= 330;
        sbW = wide ? 80 : 26;
        mainL = x + PAD + sbW + PAD;
        mainR = x + w - PAD;
        mainT = y + PAD;
        mainB = y + h - PAD;
        gridX = mainL + 10;
        gridY = mainT + 30;
        gridBottom = mainB - 26;
        int gw = mainR - 10 - gridX;
        cols = gw >= 280 ? 2 : 1;
        colW = (gw - (cols - 1) * GAP) / cols;
        searchW = Math.max(60, Math.min(100, gw / 3));
        searchX = mainR - 10 - searchW;
        intro = 0;
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

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - last) / 1e9f));
        last = now;
        intro = Math.min(1, intro + dt * 2.8f);
        int oy = Math.round((1 - intro) * 10);

        g.fill(0, 0, width, height, 0xB0000000);
        card(g, x, y + oy, x + w, y + h + oy, 10, OUTER, LINE);

        // ---- sidebar ----
        int sl = x + PAD, sr = x + PAD + sbW;
        rrect(g, sl, y + PAD + oy, sr, y + h - PAD + oy, 8, PANEL);
        int lx = wide ? sl + 10 : sl + (sbW - tw("SV")) / 2;
        txt(g, "S", lx, y + PAD + 9 + oy, GREEN);
        txt(g, "V", lx + tw("S"), y + PAD + 9 + oy, PINK);
        if (wide) txt(g, "visual client", lx, y + PAD + 20 + oy, BLUE);

        for (int i = 0; i < CATS.length; i++) {
            int ry = catY(i) + oy, rl = sl + 3, rr = sr - 3;
            boolean act = CATS[i] == category;
            boolean hov = mx >= rl && mx <= rr && my >= ry && my <= ry + 20;
            int col = act ? WHITE : hov ? 0xFFB8B8C2 : MUTED;
            if (act) rrect(g, rl, ry, rr, ry + 20, 7, PILL);
            icon(g, CATS[i], wide ? rl + 8 : rl + (rr - rl - 9) / 2, ry + 6, col);
            if (wide) txt(g, label(CATS[i]), rl + 22, ry + 6, col);
        }

        // avatar (SV badge)
        int ax = wide ? sl + 18 : sl + sbW / 2, ay = y + h - PAD - 16 + oy;
        circle(g, ax, ay, 10, 0xFF3A3A40);
        circle(g, ax, ay, 9, 0xFF000000);
        txt(g, "S", ax - 5, ay - 4, GREEN);
        txt(g, "V", ax, ay - 4, PINK);
        if (wide) {
            txt(g, "player", sl + 34, ay - 8, WHITE);
            txt(g, "Member", sl + 34, ay + 1, DIM);
        }

        // ---- main panel ----
        rrect(g, mainL, mainT + oy, mainR, mainB + oy, 8, PANEL);

        // top bar: Right Shift hint + search
        int avail = searchX - 8 - gridX - 16;
        String hint = "Right Shift · открытие меню";
        if (tw(hint) > avail) hint = "Right Shift · меню";
        hint = fit(hint, avail);
        rrect(g, gridX, mainT + 8 + oy, gridX + tw(hint) + 16, mainT + 24 + oy, 6, PILL);
        txt(g, hint, gridX + 8, mainT + 12 + oy, MUTED);

        card(g, searchX, mainT + 8 + oy, searchX + searchW, mainT + 8 + searchH + oy, 6, PILL, searchFocused ? LINE_HOVER : LINE);
        String st = search.isEmpty() && !searchFocused ? "Поиск" : searchFocused ? search + "_" : search;
        txt(g, fit(st, searchW - 12), searchX + 6, mainT + 12 + oy, search.isEmpty() ? MUTED : WHITE);

        // ---- module cards ----
        int vr = visibleRows();
        for (int r = 0; r < vr; r++) {
            for (int cc = 0; cc < cols; cc++) {
                int idx = (scroll + r) * cols + cc;
                if (idx >= modules.size()) break;
                String n = modules.get(idx);
                int rx = gridX + cc * (colW + GAP), ry = gridY + r * (CARD_H + GAP) + oy;
                boolean on = VoidModules.isEnabled(n);
                boolean hover = mx >= rx && mx <= rx + colW && my >= ry && my <= ry + CARD_H;
                card(g, rx, ry, rx + colW, ry + CARD_H, 8, hover ? CARD_HOVER : CARD, hover ? LINE_HOVER : LINE);

                int bw = bindW(n), bx = bindX(n, rx), by = ry + 7;
                txt(g, fit(n, bx - 6 - (rx + 10)), rx + 10, ry + 11, WHITE);
                txt(g, fit(VoidModules.descriptionOf(n), colW - 20), rx + 10, ry + 25, DIM);

                boolean bHover = mx >= bx && mx <= bx + bw && my >= by && my <= by + 12;
                if (n.equals(bindListen)) {
                    rrect(g, bx, by, bx + bw, by + 12, 4, PURPLE);
                    txt(g, "...", bx + 5, by + 2, WHITE);
                } else if (BINDS.containsKey(n)) {
                    rrect(g, bx, by, bx + bw, by + 12, 4, bHover ? RING : PILL);
                    txt(g, keyLabel(BINDS.get(n)), bx + 5, by + 2, WHITE);
                } else {
                    keyboard(g, bx, by, bHover ? WHITE : MUTED);
                }
                toggle(g, toggleX(rx), ry + 7, on);
            }
        }
        if (modules.isEmpty()) txt(g, "Ничего не найдено", gridX, gridY + 4 + oy, MUTED);
        // fade out at the bottom when more rows are hidden below
        if (scroll + vr < totalRows())
            g.fillGradient(mainL + 4, gridBottom - 30 + oy, mainR - 4, gridBottom + oy, 0x000E0E10, PANEL);

        // footer: HUD scale
        g.fill(gridX, mainB - 24 + oy, mainR - 10, mainB - 23 + oy, LINE);
        txt(g, "HUD scale", gridX, mainB - 16 + oy, MUTED);
        String scale = String.format(Locale.ROOT, "%.2fx", VoidModules.hudScale());
        txt(g, scale, mainR - 10 - tw(scale), mainB - 16 + oy, WHITE);
        int sx = sliderX(), sw = sliderW(), sy = mainB - 13 + oy;
        g.fill(sx, sy, sx + sw, sy + 3, TOG_OFF);
        int knob = sx + Math.round((VoidModules.hudScale() - .75f) / .75f * sw);
        g.fill(sx, sy, knob, sy + 3, PURPLE);
        pill(g, knob - 5, sy - 4, 10, 11, KNOB_ON);

        super.render(g, mx, my, partial);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        bindListen = null;

        // sidebar categories
        int sl = x + PAD + 3, sr = x + PAD + sbW - 3;
        for (int i = 0; i < CATS.length; i++) {
            int ry = catY(i);
            if (mx >= sl && mx <= sr && my >= ry && my <= ry + 20) {
                category = CATS[i]; scroll = 0; searchFocused = false; rebuild(); return true;
            }
        }
        // search
        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= mainT + 8 && my <= mainT + 8 + searchH;
        if (searchFocused) return true;

        // cards
        int vr = visibleRows();
        for (int r = 0; r < vr; r++) {
            for (int cc = 0; cc < cols; cc++) {
                int idx = (scroll + r) * cols + cc;
                if (idx >= modules.size()) break;
                String n = modules.get(idx);
                int rx = gridX + cc * (colW + GAP), ry = gridY + r * (CARD_H + GAP);
                if (mx >= rx && mx <= rx + colW && my >= ry && my <= ry + CARD_H) {
                    int bx = bindX(n, rx), bw = bindW(n), by = ry + 7;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + 12) {
                        bindListen = n; // next key press becomes the bind
                    } else {
                        VoidModules.toggle(n);
                    }
                    return true;
                }
            }
        }
        // HUD scale slider
        int sx = sliderX(), sw = sliderW();
        if (mx >= sx - 5 && mx <= sx + sw + 5 && my >= mainB - 20 && my <= mainB - 6) {
            VoidModules.setHudScale(.75f + (float) Math.max(0, Math.min(1, (mx - sx) / sw)) * .75f);
            return true;
        }
        return true;
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
        int key = e.key();
        if (bindListen != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) BINDS.remove(bindListen);
            else BINDS.put(bindListen, key);
            bindListen = null;
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1); rebuild(); return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
        } else if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override public boolean isPauseScreen() { return false; }
}