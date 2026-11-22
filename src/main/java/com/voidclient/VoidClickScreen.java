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
    private static final int SCRIM = 0xB805030B, GLASS_BG = 0xDF0B0714;
    private static final int CARD = 0xD8140D24, CARD_HOVER = 0xEC1B1130, CARD_ON = 0xE6180F2E;
    private static final int BORDER = 0x557C3AED, BORDER_ACTIVE = 0xCCA855F7, DOCK_BG = 0xE80E091A;
    private static final int WHITE = 0xFFF8FAFC, MUTED = 0xFF948BB0, VIOLET = 0xFFC084FC;
    private static final int PURPLE = 0xFF7C3AED, BRIGHT = 0xFF9333EA, SWITCH_OFF = 0xFF2A213D, KNOB_OFF = 0xFF8C82A6;
    private static final int TAB_COUNT = 4, COLS = 2, ROW_H = 26, GAP = 7;

    private int x, y, w, h, bannerH, tabsY, gridX, gridY, gridBottom, dockY, colW, scroll;
    private int searchX, searchY, searchW, searchH;
    private final int[] tabL = new int[TAB_COUNT], tabR = new int[TAB_COUNT];
    private final int[] dockL = new int[TAB_COUNT], dockR = new int[TAB_COUNT];
    private String search = "", listeningModule, selectedParamModule;
    private boolean searchFocused, draggingParam;
    private int activeTabIndex;
    private final List<String> modules = new ArrayList<>();
    private long last = System.nanoTime();
    private float intro, pulsePhase;

    public VoidClickScreen() { super(Component.literal("PulseVisuals")); }
    private Component c(String s) { return Component.literal(s); }
    private int tw(String s) { return font.width(c(s)); }
    private void txt(GuiGraphics g, String s, int px, int py, int color) { g.drawString(font, c(s), px, py, color, false); }
    private void fill(GuiGraphics g, int l, int t, int r, int b, int color) { if (r > l && b > t) g.fill(l, t, r, b, color); }

    private void rr(GuiGraphics g, int l, int t, int r, int b, int rad, int color) {
        int hh = b - t;
        if (hh <= 0 || r <= l) return;
        rad = Math.max(0, Math.min(rad, Math.min(hh, r - l) / 2));
        for (int yy = 0; yy < hh; yy++) {
            int d = Math.min(yy, hh - 1 - yy), in = 0;
            if (d < rad) {
                double dy = rad - d - .5;
                in = (int) Math.ceil(rad - Math.sqrt(Math.max(0, rad * rad - dy * dy)));
            }
            g.fill(l + in, t + yy, r - in, t + yy + 1, color);
        }
    }
    private void card(GuiGraphics g, int l, int t, int r, int b, int rad, int bg, int border) {
        rr(g, l, t, r, b, rad, border);
        rr(g, l + 1, t + 1, r - 1, b - 1, Math.max(0, rad - 1), bg);
    }
    private void pill(GuiGraphics g, int l, int t, int ww, int hh, int color) { rr(g, l, t, l + ww, t + hh, hh / 2, color); }
    private void toggle(GuiGraphics g, int sx, int sy, boolean on) {
        pill(g, sx, sy, 28, 14, on ? BRIGHT : SWITCH_OFF);
        int cx = sx + (on ? 21 : 7), col = on ? WHITE : KNOB_OFF;
        for (int dy = -4; dy <= 4; dy++) {
            int half = (int) Math.sqrt(16 - dy * dy);
            g.fill(cx - half, sy + 7 + dy, cx + half + 1, sy + 8 + dy, col);
        }
    }
    private void drawMiniKeyboard(GuiGraphics g, int kx, int ky) {
        rr(g, kx, ky, kx + 18, ky + 9, 2, 0xFF8D79B8);
        int key = 0xFFD8C8F2;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int px = kx + 2 + col * 3, py = ky + 2 + row * 3;
                fill(g, px, py, px + 2, py + 2, key);
            }
        }
        fill(g, kx + 6, ky + 7, kx + 13, ky + 8, key);
    }
    private String tabTitle(int idx) {
        return switch (idx) { case 0 -> "Visuals"; case 1 -> "HUD"; case 2 -> "Utils"; default -> "Misc"; };
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
    private void layoutTabsAndSearch() {
        int pad = 12, curX = x + pad;
        for (int i = 0; i < TAB_COUNT; i++) {
            int bw = tw(tabTitle(i)) + 12;
            tabL[i] = curX; tabR[i] = curX + bw; curX += bw + 6;
        }
        searchH = 18;
        searchW = Math.max(96, Math.min(210, x + w - pad - (curX + 10)));
        searchX = x + w - pad - searchW; searchY = tabsY;
        int dockW = 148, dockH = 26, dx = (width - dockW) / 2, itemW = 24;
        int gap = (dockW - TAB_COUNT * itemW) / (TAB_COUNT + 1);
        for (int i = 0; i < TAB_COUNT; i++) { dockL[i] = dx + gap + i * (itemW + gap); dockR[i] = dockL[i] + itemW; }
        dockY = Math.min(height - dockH - 14, y + h + 12);
    }
    private int visibleRows() { return Math.max(1, (gridBottom - gridY + GAP) / (ROW_H + GAP)); }
    private int totalRows() { return (modules.size() + COLS - 1) / COLS; }
    private int paramX() { return gridX + tw((selectedParamModule == null ? "" : selectedParamModule) + " · Value") + 12; }
    private int paramW() { return Math.max(24, x + w - 12 - tw("100%") - 10 - paramX()); }
    private void applyParam(double mx) {
        if (selectedParamModule != null) {
            float p = (float) Math.max(0, Math.min(1, (mx - paramX()) / paramW()));
            VoidModules.setModuleValue(selectedParamModule, p);
        }
    }
    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String n : VoidModules.all().keySet()) {
            if (matchesTab(n, activeTabIndex) && (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q)
                    || VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q))) modules.add(n);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, totalRows() - visibleRows())));
    }
    @Override protected void init() {
        w = Math.min(width - 32, 740); h = Math.min(height - 48, 344);
        x = (width - w) / 2; y = Math.max(12, (height - h - 38) / 2);
        bannerH = 66; tabsY = y + bannerH + 8; gridX = x + 12; gridY = tabsY + 26;
        int paramReserve = selectedParamModule != null ? 24 : 0;
        gridBottom = y + h - 10 - paramReserve; colW = (w - 24 - GAP) / COLS;
        layoutTabsAndSearch(); intro = 0f; draggingParam = false; listeningModule = null; rebuild();
    }
    private void renderMirrorGlassBackground(GuiGraphics g, int py) {
        fill(g, 0, 0, width, height, SCRIM);
        card(g, x, py, x + w, py + h, 18, GLASS_BG, BORDER);
        int bTop = py + 1, bBot = py + bannerH;
        g.fillGradient(x + 1, bTop, x + w - 1, bBot, 0xCC1E1038, 0x990D071B);
        fill(g, x + 2, bTop + 1, x + w - 2, bTop + 2, 0x44C084FC);
        int sceneL = x + 10, sceneR = x + w - 10, sceneB = bBot - 4;
        fill(g, sceneL + 12, bTop + 22, sceneL + 74, sceneB, 0x332A184A);
        fill(g, sceneL + 84, bTop + 14, sceneL + 148, sceneB, 0x38381E63);
        fill(g, sceneL + 160, bTop + 26, sceneR - 95, sceneB, 0x2E241442);
        fill(g, sceneR - 82, bTop + 18, sceneR - 18, sceneB, 0x36321B59);
        int refTop = py + h - 38;
        g.fillGradient(x + 2, refTop, x + w - 2, py + h - 1, 0x00A855F7, 0x28A855F7);

    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - last) / 1e9f)); last = now;
        intro = Math.min(1f, intro + dt * 3.2f); pulsePhase += dt * 2.2f;
        int lift = Math.round((1f - intro) * 8f), py = y + lift;
        renderMirrorGlassBackground(g, py); layoutTabsAndSearch();
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = i == activeTabIndex;
            int ty = py + (tabsY - y);
            boolean hover = mx >= tabL[i] && mx <= tabR[i] && my >= ty && my <= ty + 18;
            card(g, tabL[i], ty, tabR[i], ty + 18, 5, active ? 0xE62E1954 : hover ? CARD_HOVER : CARD,
                    active ? BORDER_ACTIVE : BORDER);
            String title = tabTitle(i);
            txt(g, title, tabL[i] + (tabR[i] - tabL[i] - tw(title)) / 2, ty + 5, active ? VIOLET : MUTED);
        }
        int syTop = py + (searchY - y);
        card(g, searchX, syTop, searchX + searchW, syTop + searchH, 5, CARD, searchFocused ? BORDER_ACTIVE : BORDER);
        String queryText = search.isEmpty() && !searchFocused ? "Поиск"
                : searchFocused ? search + (((int) pulsePhase) % 2 == 0 ? "_" : "") : search;
        txt(g, fit(queryText, searchW - 12), searchX + 6, syTop + 5, search.isEmpty() ? MUTED : WHITE);
        int curGridY = py + (gridY - y), vr = visibleRows();
        for (int r = 0; r < vr; r++) for (int cc = 0; cc < COLS; cc++) {
            int idx = (scroll + r) * COLS + cc;
            if (idx >= modules.size()) break;
            String n = modules.get(idx); int rx = gridX + cc * (colW + GAP), ry = curGridY + r * (ROW_H + GAP);
            boolean on = VoidModules.isEnabled(n), hover = mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H;
            boolean selected = n.equals(selectedParamModule);
            card(g, rx, ry, rx + colW, ry + ROW_H, 6, selected ? 0xF028154A : on ? CARD_ON : hover ? CARD_HOVER : CARD,
                    selected ? BORDER_ACTIVE : on ? 0x889333EA : hover ? 0x77A855F7 : BORDER);
            int toggleX = rx + colW - 34, bindX = toggleX - 38;
            String bindLabel = n.equals(listeningModule) ? "..." : VoidModules.keyBinds().containsKey(n)
                    ? keyName(VoidModules.keyBinds().get(n)) : "·";
            boolean hasBind = VoidModules.keyBinds().containsKey(n) || n.equals(listeningModule);
            card(g, bindX, ry + 5, bindX + 34, ry + 21, 5, GLASS_BG, hasBind ? BORDER_ACTIVE : BORDER);
            if (hasBind) {
                txt(g, fit(bindLabel, 28), bindX + 3, ry + 9, VIOLET);
            } else {
                drawMiniKeyboard(g, bindX + 8, ry + 9);
            }
            txt(g, fit(n, Math.max(0, bindX - rx - 16)), rx + 9, ry + 9, on ? WHITE : MUTED);
            toggle(g, toggleX, ry + 6, on);
        }
        if (modules.isEmpty()) txt(g, "Ничего не найдено", gridX + 2, curGridY + 4, MUTED);
        if (selectedParamModule != null) {
            int pBarY = py + h - 26;
            card(g, gridX, pBarY, x + w - 12, pBarY + 18, 5, CARD, BORDER_ACTIVE);
            txt(g, selectedParamModule + " · Value", gridX + 8, pBarY + 5, WHITE);
            int sx = paramX(), sw = paramW(), sy = pBarY + 8;
            float v = VoidModules.moduleValue(selectedParamModule);
            fill(g, sx, sy, sx + sw, sy + 2, SWITCH_OFF);
            int knob = sx + Math.round(v * sw);
            fill(g, sx, sy, knob, sy + 2, BRIGHT); pill(g, knob - 4, sy - 3, 8, 8, WHITE);
            String pct = Math.round(v * 100) + "%";
            txt(g, pct, x + w - 18 - tw(pct), pBarY + 5, VIOLET);
        }

        super.render(g, mx, my, partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        double mx = e.x(), my = e.y(); layoutTabsAndSearch();
        if (e.button() == 1) {
            for (int r = 0; r < visibleRows(); r++) for (int cc = 0; cc < COLS; cc++) {
                int idx = (scroll + r) * COLS + cc; if (idx >= modules.size()) break;
                int rx = gridX + cc * (colW + GAP), ry = gridY + r * (ROW_H + GAP);
                if (mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H) {
                    String n = modules.get(idx); selectedParamModule = n.equals(selectedParamModule) ? null : n;
                    init(); return true;
                }
            }
            return super.mouseClicked(e, doubled);
        }
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        for (int i = 0; i < TAB_COUNT; i++) if (mx >= tabL[i] && mx <= tabR[i] && my >= tabsY && my <= tabsY + 18) {
            activeTabIndex = i; scroll = 0; searchFocused = false; listeningModule = null; selectedParamModule = null; init(); return true;
        }

        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH;
        if (searchFocused) { listeningModule = null; return true; }
        if (selectedParamModule != null) {
            int pBarY = y + h - 26;
            if (mx >= paramX() - 4 && mx <= paramX() + paramW() + 4 && my >= pBarY && my <= pBarY + 18) {
                draggingParam = true; applyParam(mx); return true;
            }
        }
        for (int r = 0; r < visibleRows(); r++) for (int cc = 0; cc < COLS; cc++) {
            int idx = (scroll + r) * COLS + cc; if (idx >= modules.size()) break;
            int rx = gridX + cc * (colW + GAP), ry = gridY + r * (ROW_H + GAP);
            int tx = rx + colW - 34, bx = tx - 38;
            if (mx >= bx && mx <= bx + 34 && my >= ry + 5 && my <= ry + 21) {
                String n = modules.get(idx); listeningModule = n.equals(listeningModule) ? null : n; return true;
            }
            if (mx >= rx && mx <= rx + colW && my >= ry && my <= ry + ROW_H) {
                listeningModule = null; VoidModules.toggle(modules.get(idx)); return true;
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (e.button() == 0 && draggingParam) { applyParam(e.x()); return true; }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() == 0 && draggingParam) { draggingParam = false; return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int max = Math.max(0, totalRows() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }
    @Override public boolean charTyped(CharacterEvent e) {
        if (!searchFocused) return super.charTyped(e);
        int cp = e.codepoint();
        if (Character.isValidCodePoint(cp) && !Character.isISOControl(cp) && search.length() < 48) {
            search += new String(Character.toChars(cp)); scroll = 0; rebuild();
        }
        return true;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        int k = e.key();
        if (listeningModule != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE)
                VoidModules.setKeyBind(listeningModule, null);
            else if (k != GLFW.GLFW_KEY_UNKNOWN) VoidModules.setKeyBind(listeningModule, k);
            listeningModule = null; return true;
        }
        if (searchFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1); rebuild(); return true;
            }
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
        } else if (k == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(e);
    }
    @Override public boolean isPauseScreen() { return false; }
}
