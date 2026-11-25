package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    // Exact compact dark-grid clone matching the screenshot layout
    private static final int SCRIM = 0xAA0A0A0C;
    private static final int WINDOW_BG = 0xF5101012;
    private static final int SIDEBAR_BG = 0xFA0C0C0E;
    private static final int CARD_BG = 0xF5151518;
    private static final int CARD_HOVER = 0xFA1C1C20;
    private static final int CARD_ON = 0xFA181A24;

    private static final int LINE_WINDOW = 0x22FFFFFF;
    private static final int LINE_CARD = 0x16FFFFFF;
    private static final int LINE_CARD_ON = 0x446378D8;

    private static final int TXT_NAME = 0xFFD4D4D8;
    private static final int TXT_BIND = 0xFF9CA3AF;
    private static final int TXT_BIND_ON = 0xFF93C5FD;
    private static final int ICON_BLUE = 0xFF6B84D6;
    private static final int ICON_DIM = 0xFF52525B;

    private static final int SW_TRACK_ON = 0xFF4F67B8;
    private static final int SW_TRACK_OFF = 0xFF232327;
    private static final int SW_KNOB_ON = 0xFFF4F4F5;
    private static final int SW_KNOB_OFF = 0xFF71717A;

    private static final int TAB_COUNT = 4;
    private static final int COLS = 3;
    private static final int ROWS = 8;
    private static final int SIDEBAR_W = 34;
    private static final int ROW_H = 21;
    private static final int GAP_X = 7;
    private static final int GAP_Y = 5;

    private int x, y, w, h;
    private int gridL, gridR, gridTop, gridBottom, colW, scroll;
    private int searchX, searchY, searchW, searchH;

    private final int[] tabT = new int[TAB_COUNT];
    private final int[] tabB = new int[TAB_COUNT];

    private String search = "";
    private String listeningModule;
    private String selectedParamModule;
    private boolean searchFocused;
    private boolean draggingParam;
    private int activeTabIndex;

    private final List<String> modules = new ArrayList<>();
    private long last = System.nanoTime();
    private float pulsePhase;

    public VoidClickScreen() {
        super(Component.literal("Menu"));
    }

    private Component c(String s) {
        return Component.literal(s);
    }

    private int tw(String s) {
        return font.width(c(s));
    }

    private void txt(GuiGraphics g, String s, int px, int py, int color) {
        g.drawString(font, c(s), px, py, color, false);
    }

    private void fill(GuiGraphics g, int l, int t, int r, int b, int color) {
        if (r > l && b > t) {
            g.fill(l, t, r, b, color);
        }
    }

    private void rr(GuiGraphics g, int l, int t, int r, int b, int rad, int color) {
        int hh = b - t;
        if (hh <= 0 || r <= l) return;
        rad = Math.max(0, Math.min(rad, Math.min(hh, r - l) / 2));
        for (int yy = 0; yy < hh; yy++) {
            int d = Math.min(yy, hh - 1 - yy);
            int in = 0;
            if (d < rad) {
                double dy = rad - d - 0.5;
                in = (int) Math.ceil(rad - Math.sqrt(Math.max(0, rad * rad - dy * dy)));
            }
            g.fill(l + in, t + yy, r - in, t + yy + 1, color);
        }
    }

    private void card(GuiGraphics g, int l, int t, int r, int b, int rad, int bg, int border) {
        rr(g, l, t, r, b, rad, border);
        rr(g, l + 1, t + 1, r - 1, b - 1, Math.max(0, rad - 1), bg);
    }

    private void pill(GuiGraphics g, int l, int t, int ww, int hh, int color) {
        rr(g, l, t, l + ww, t + hh, hh / 2, color);
    }

    private void toggle(GuiGraphics g, int sx, int sy, boolean on) {
        int sw = 22;
        int sh = 11;
        pill(g, sx, sy, sw, sh, on ? SW_TRACK_ON : SW_TRACK_OFF);
        int rad = 4;
        int cx = sx + (on ? sw - 6 : 6);
        int cy = sy + sh / 2;
        int col = on ? SW_KNOB_ON : SW_KNOB_OFF;
        for (int dy = -rad; dy <= rad; dy++) {
            int half = (int) Math.sqrt(rad * rad - dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, col);
        }
    }

    private void drawSidebarRowGlyph(GuiGraphics g, int idx, int cx, int cy, boolean active) {
        int barCol = active ? ICON_BLUE : ICON_DIM;
        int icoCol = active ? ICON_BLUE : ICON_DIM;
        // Left vertical bar
        fill(g, cx - 7, cy - 4, cx - 6, cy + 4, barCol);
        // Right glyph matching the screenshot
        switch (idx) {
            case 0 -> {
                fill(g, cx + 1, cy - 3, cx + 6, cy - 2, icoCol);
                fill(g, cx + 2, cy, cx + 5, cy + 1, icoCol);
                fill(g, cx + 1, cy + 2, cx + 6, cy + 3, icoCol);
            }
            case 1 -> {
                fill(g, cx + 1, cy - 3, cx + 2, cy + 3, icoCol);
                fill(g, cx + 3, cy - 2, cx + 6, cy - 1, icoCol);
                fill(g, cx + 3, cy + 1, cx + 6, cy + 2, icoCol);
            }
            case 2 -> {
                fill(g, cx + 1, cy - 3, cx + 6, cy - 2, icoCol);
                fill(g, cx + 3, cy - 2, cx + 4, cy + 3, icoCol);
            }
            default -> {
                fill(g, cx + 1, cy - 3, cx + 2, cy + 3, icoCol);
                fill(g, cx + 5, cy - 3, cx + 6, cy + 3, icoCol);
                fill(g, cx + 2, cy - 1, cx + 5, cy + 1, icoCol);
            }
        }
    }

    private boolean matchesTab(String name, int idx) {
        VoidModules.Category cat = VoidModules.categoryOf(name);
        return switch (idx) {
            case 0 -> cat == VoidModules.Category.RENDER;
            case 1 -> cat == VoidModules.Category.PLAYER;
            case 2 -> cat == VoidModules.Category.COMBAT || cat == VoidModules.Category.MOVEMENT;
            default -> cat == VoidModules.Category.MISC;
        };
    }

    private String fit(String s, int max) {
        if (max <= 0) return "";
        if (tw(s) <= max) return s;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            String candidate = out + String.valueOf(s.charAt(i)) + "…";
            if (tw(candidate) > max) break;
            out.append(s.charAt(i));
        }
        return out + "…";
    }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) return "SPC";
        if (key == GLFW.GLFW_KEY_ESCAPE) return "ESC";
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "K" + key : n.toUpperCase(Locale.ROOT);
    }

    private void layoutGrid() {
        int curY = y + 10;
        for (int i = 0; i < TAB_COUNT; i++) {
            tabT[i] = curY;
            tabB[i] = curY + 21;
            curY += 25;
        }

        // Bottom-left small search/filter box
        searchH = 14;
        searchW = SIDEBAR_W - 10;
        searchX = x + 5;
        searchY = y + h - searchH - 8;

        gridL = x + SIDEBAR_W + 9;
        gridR = x + w - 9;
        gridTop = y + 9;
        int paramReserve = selectedParamModule != null ? 22 : 0;
        gridBottom = y + h - 9 - paramReserve;
        colW = (gridR - gridL - GAP_X * (COLS - 1)) / COLS;
    }

    private int visibleRows() {
        return Math.max(1, Math.min(ROWS, (gridBottom - gridTop + GAP_Y) / (ROW_H + GAP_Y)));
    }

    private int totalRows() {
        return (modules.size() + COLS - 1) / COLS;
    }

    private int paramX() {
        String label = (selectedParamModule == null ? "" : selectedParamModule) + " · Value";
        return gridL + tw(label) + 12;
    }

    private int paramW() {
        return Math.max(28, gridR - tw("100%") - 12 - paramX());
    }

    private void applyParam(double mx) {
        if (selectedParamModule != null) {
            float p = (float) Math.max(0.0, Math.min(1.0, (mx - paramX()) / paramW()));
            VoidModules.setModuleValue(selectedParamModule, p);
        }
    }

    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String n : VoidModules.all().keySet()) {
            if (matchesTab(n, activeTabIndex) && (q.isEmpty()
                    || n.toLowerCase(Locale.ROOT).contains(q)
                    || VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q))) {
                modules.add(n);
            }
        }
        int maxScroll = Math.max(0, totalRows() - visibleRows());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    @Override
    protected void init() {
        w = Math.min(width - 20, 640);
        h = Math.min(height - 24, 224);
        x = (width - w) / 2;
        y = Math.max(8, (height - h) / 2);

        layoutGrid();
        draggingParam = false;
        listeningModule = null;
        rebuild();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0.001f, (now - last) / 1e9f));
        last = now;
        pulsePhase += dt * 2.2f;

        layoutGrid();
        fill(g, 0, 0, width, height, SCRIM);
        card(g, x, y, x + w, y + h, 10, WINDOW_BG, LINE_WINDOW);

        // Narrow left sidebar
        fill(g, x + 1, y + 1, x + SIDEBAR_W, y + h - 1, SIDEBAR_BG);

        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = i == activeTabIndex;
            int ty = tabT[i];
            int tb = tabB[i];
            int cx = x + SIDEBAR_W / 2;
            int cy = (ty + tb) / 2;
            drawSidebarRowGlyph(g, i, cx, cy, active || (mx >= x && mx <= x + SIDEBAR_W && my >= ty && my <= tb));
        }

        // Bottom-left small search/filter trigger
        card(g, searchX, searchY, searchX + searchW, searchY + searchH, 4, WINDOW_BG,
                searchFocused ? LINE_CARD_ON : LINE_CARD);
        if (searchFocused || !search.isEmpty()) {
            String qText = searchFocused ? search + (((int) pulsePhase) % 2 == 0 ? "_" : "") : search;
            txt(g, fit(qText, searchW - 4), searchX + 2, searchY + 3, TXT_BIND);
        } else {
            fill(g, searchX + 5, searchY + 4, searchX + 9, searchY + 8, ICON_DIM);
            fill(g, searchX + 9, searchY + 8, searchX + 11, searchY + 10, ICON_DIM);
        }

        // 3-column grid of compact cards
        int vr = visibleRows();
        for (int r = 0; r < vr; r++) {
            for (int cIdx = 0; cIdx < COLS; cIdx++) {
                int idx = (scroll + r) * COLS + cIdx;
                if (idx >= modules.size()) break;

                String name = modules.get(idx);
                int rx = gridL + cIdx * (colW + GAP_X);
                int ry = gridTop + r * (ROW_H + GAP_Y);
                int rb = ry + ROW_H;

                boolean on = VoidModules.isEnabled(name);
                boolean hover = mx >= rx && mx <= rx + colW && my >= ry && my <= rb;
                boolean selected = name.equals(selectedParamModule);

                int bgCol = on ? CARD_ON : (hover ? CARD_HOVER : CARD_BG);
                int brdCol = selected || on ? LINE_CARD_ON : LINE_CARD;
                card(g, rx, ry, rx + colW, rb, 5, bgCol, brdCol);

                int toggleX = rx + colW - 27;
                int bindW = 24;
                int bindX = toggleX - bindW - 6;

                String bindLabel = name.equals(listeningModule)
                        ? "..."
                        : VoidModules.keyBinds().containsKey(name) ? keyName(VoidModules.keyBinds().get(name)) : "N/A";
                boolean hasBind = VoidModules.keyBinds().containsKey(name) || name.equals(listeningModule);

                int nameMaxW = Math.max(12, bindX - rx - 10);
                txt(g, fit(name, nameMaxW), rx + 7, ry + 6, TXT_NAME);

                String fittedBind = fit(bindLabel, bindW);
                int bindTextX = bindX + Math.max(0, (bindW - tw(fittedBind)) / 2);
                txt(g, fittedBind, bindTextX, ry + 6, hasBind ? TXT_BIND_ON : TXT_BIND);

                toggle(g, toggleX, ry + 5, on);
            }
        }

        // Parameter slider bar when right-clicked
        if (selectedParamModule != null) {
            int pBarY = y + h - 22;
            card(g, gridL, pBarY, gridR, pBarY + 16, 5, CARD_BG, LINE_CARD_ON);
            txt(g, selectedParamModule + " · Value", gridL + 8, pBarY + 4, TXT_NAME);

            int sx = paramX();
            int sw = paramW();
            int sy = pBarY + 7;
            float val = VoidModules.moduleValue(selectedParamModule);

            fill(g, sx, sy, sx + sw, sy + 2, SW_TRACK_OFF);
            int knob = sx + Math.round(val * sw);
            fill(g, sx, sy, knob, sy + 2, SW_TRACK_ON);
            pill(g, knob - 4, sy - 3, 8, 8, SW_KNOB_ON);

            String pct = Math.round(val * 100) + "%";
            txt(g, pct, gridR - 6 - tw(pct), pBarY + 4, TXT_BIND_ON);
        }

        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        double mx = e.x();
        double my = e.y();
        layoutGrid();

        if (e.button() == 1) {
            int vr = visibleRows();
            for (int r = 0; r < vr; r++) {
                for (int cIdx = 0; cIdx < COLS; cIdx++) {
                    int idx = (scroll + r) * COLS + cIdx;
                    if (idx >= modules.size()) break;
                    int rx = gridL + cIdx * (colW + GAP_X);
                    int ry = gridTop + r * (ROW_H + GAP_Y);
                    if (mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H) {
                        String n = modules.get(idx);
                        selectedParamModule = n.equals(selectedParamModule) ? null : n;
                        init();
                        return true;
                    }
                }
            }
            return super.mouseClicked(e, doubled);
        }

        if (e.button() != 0) {
            return super.mouseClicked(e, doubled);
        }

        for (int i = 0; i < TAB_COUNT; i++) {
            if (mx >= x && mx <= x + SIDEBAR_W && my >= tabT[i] && my <= tabB[i]) {
                activeTabIndex = i;
                scroll = 0;
                searchFocused = false;
                listeningModule = null;
                selectedParamModule = null;
                init();
                return true;
            }
        }

        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH;
        if (searchFocused) {
            listeningModule = null;
            return true;
        }

        if (selectedParamModule != null) {
            int pBarY = y + h - 22;
            if (mx >= paramX() - 4 && mx <= paramX() + paramW() + 4 && my >= pBarY && my <= pBarY + 16) {
                draggingParam = true;
                applyParam(mx);
                return true;
            }
        }

        int vr = visibleRows();
        for (int r = 0; r < vr; r++) {
            for (int cIdx = 0; cIdx < COLS; cIdx++) {
                int idx = (scroll + r) * COLS + cIdx;
                if (idx >= modules.size()) break;
                String n = modules.get(idx);
                int rx = gridL + cIdx * (colW + GAP_X);
                int ry = gridTop + r * (ROW_H + GAP_Y);
                int toggleX = rx + colW - 27;
                int bindW = 24;
                int bindX = toggleX - bindW - 6;

                if (mx >= bindX && mx <= bindX + bindW && my >= ry + 3 && my <= ry + ROW_H - 3) {
                    listeningModule = n.equals(listeningModule) ? null : n;
                    return true;
                }
                if (mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H) {
                    listeningModule = null;
                    VoidModules.toggle(n);
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (e.button() == 0 && draggingParam) {
            applyParam(e.x());
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() == 0 && draggingParam) {
            draggingParam = false;
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int max = Math.max(0, totalRows() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!searchFocused) return super.charTyped(e);
        int cp = e.codepoint();
        if (Character.isValidCodePoint(cp) && !Character.isISOControl(cp) && search.length() < 48) {
            search += new String(Character.toChars(cp));
            scroll = 0;
            rebuild();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int k = e.key();
        if (listeningModule != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
                VoidModules.setKeyBind(listeningModule, null);
            } else if (k != GLFW.GLFW_KEY_UNKNOWN) {
                VoidModules.setKeyBind(listeningModule, k);
            }
            listeningModule = null;
            return true;
        }
        if (searchFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                rebuild();
                return true;
            }
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        } else if (k == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}