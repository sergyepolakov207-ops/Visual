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
    private static final int TEXT = 0xFFF5F0FF;
    private static final int MUTED = 0xFFAAA4B8;
    private static final int PURPLE = 0xFFC49AFF;
    private static final int PURPLE_DARK = 0xFF7650A8;

    private int x, y, w, h, rail, left, right, top, bottom;
    private int cardW, cardH, gap = 10, columns, scroll;
    private boolean searchFocused;
    private String search = "";
    private String selected = "";
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> modules = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        w = Math.min(width - 32, Math.max(360, (int)(width * 0.78)));
        h = Math.min(height - 28, Math.max(250, (int)(height * 0.82)));
        x = (width - w) / 2;
        y = (height - h) / 2;
        rail = Math.max(58, Math.min(70, width / 18));
        left = x + rail + 22;
        right = x + w - 24;
        top = y + 112;
        bottom = y + h - (selected.isEmpty() ? 26 : 104);
        columns = (right - left) >= 650 ? 3 : (right - left) >= 390 ? 2 : 1;
        cardW = (right - left - gap * (columns - 1)) / columns;
        cardH = 54;
        rebuild();
    }

    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String name : VoidModules.all().keySet()) {
            if (VoidModules.categoryOf(name) == category
                && (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q))) modules.add(name);
        }
        int rows = (modules.size() + columns - 1) / columns;
        int visible = Math.max(1, (bottom - top + gap) / (cardH + gap));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows - visible)));
        if (!modules.contains(selected)) selected = "";
    }

    private void rounded(GuiGraphics g, int px, int py, int pw, int ph, int color, int radius) {
        if (pw <= 0 || ph <= 0) return;
        int r = Math.min(radius, Math.min(pw / 2, ph / 2));
        for (int row = 0; row < ph; row++) {
            int inset = 0;
            if (row < r) inset = r - (int)Math.sqrt(r * r - (r - row) * (r - row));
            else if (row >= ph - r) {
                int d = row - (ph - r - 1);
                inset = r - (int)Math.sqrt(r * r - d * d);
            }
            g.fill(px + inset, py + row, px + pw - inset, py + row + 1, color);
        }
    }

    private void glassCard(GuiGraphics g, int px, int py, int pw, int ph, boolean active, boolean hover) {
        rounded(g, px, py, pw, ph, active ? 0xE52C223A : hover ? 0xD3262330 : 0xB91A1922, 9);
        g.fill(px + 9, py + 1, px + pw - 9, py + 2, active ? 0x66D9B8FF : 0x22FFFFFF);
        g.fill(px + 1, py + 8, px + 2, py + ph - 8, 0x20FFFFFF);
        g.fill(px + pw - 2, py + 8, px + pw - 1, py + ph - 8, 0x187F73A6);
        if (active) g.fill(px + 1, py + 12, px + 3, py + ph - 12, PURPLE);
    }

    private String categoryName() {
        return switch (category) {
            case VISUALS -> "VISUALS";
            case HUD -> "HUD";
            case PVP -> "PVP";
        };
    }

    private String categoryInfo() {
        return switch (category) {
            case VISUALS -> "Visual effects and quality of life";
            case HUD -> "Your interface, your layout";
            case PVP -> "Combat information and movement";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        try { renderBlurredBackground(g); } catch (RuntimeException | LinkageError ignored) { }
        g.fill(0, 0, width, height, 0x62090712);

        // Large translucent glass body with a reflected top edge.
        rounded(g, x, y, w, h, 0xD914121B, 15);
        rounded(g, x + 2, y + 2, w - 4, h - 4, 0x321F1A2B, 14);
        g.fill(x + 18, y + 2, x + w - 18, y + 4, 0x66FFFFFF);
        g.fill(x + 3, y + 18, x + 4, y + h - 18, 0x44FFFFFF);
        g.fill(x + w - 4, y + 20, x + w - 3, y + h - 20, 0x338D78C5);
        g.fill(x + rail, y + 14, x + rail + 1, y + h - 14, 0x443E354F);

        // Sidebar.
        rounded(g, x + 12, y + 14, rail - 24, 38, 0xFF30263E, 10);
        g.drawCenteredString(font, "S", x + rail / 2, y + 27, PURPLE);
        String[] icons = {"✧", "▦", "⚔"};
        String[] names = {"VISUALS", "HUD", "PVP"};
        for (int i = 0; i < 3; i++) {
            int iy = y + 70 + i * 62;
            boolean active = category.ordinal() == i;
            if (active) rounded(g, x + 8, iy, rail - 16, 48, 0xFF342943, 11);
            g.drawCenteredString(font, icons[i], x + rail / 2, iy + 8, active ? PURPLE : MUTED);
            g.drawCenteredString(font, names[i], x + rail / 2, iy + 27, active ? TEXT : MUTED);
        }

        // Brand and category heading.
        g.drawString(font, "SWIFT VISUALS", left, y + 20, TEXT, true);
        rounded(g, left + 126, y + 12, 82, 25, 0xFF30243E, 8);
        g.drawCenteredString(font, categoryName(), left + 167, y + 20, PURPLE);
        g.drawString(font, categoryInfo(), left, y + 48, MUTED, false);
        g.drawString(font, "FABRIC 1.21.11", right - font.width("FABRIC 1.21.11"), y + 22, MUTED, false);
        g.fill(left, y + 68, right, y + 69, 0x443E354F);

        // Search field.
        rounded(g, left, y + 80, right - left, 27, searchFocused ? 0xFF30263E : 0xB923202C, 8);
        g.drawString(font, "⌕", left + 11, y + 89, PURPLE, false);
        String shownSearch = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, shownSearch + (searchFocused ? "  |" : ""), left + 31, y + 89,
            search.isEmpty() ? MUTED : TEXT, false);
        g.drawString(font, modules.size() + " MODULES", right - font.width(modules.size() + " MODULES") - 12,
            y + 89, MUTED, false);

        int visibleRows = Math.max(1, (bottom - top + gap) / (cardH + gap));
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= modules.size()) continue;
                String name = modules.get(index);
                int cx = left + col * (cardW + gap);
                int cy = top + row * (cardH + gap);
                boolean enabled = VoidModules.isEnabled(name);
                boolean active = name.equals(selected);
                boolean hover = mx >= cx && mx <= cx + cardW && my >= cy && my <= cy + cardH;
                glassCard(g, cx, cy, cardW, cardH, active, hover);
                g.drawString(font, enabled ? "✦" : "◇", cx + 13, cy + 13, enabled ? PURPLE : MUTED);
                g.drawString(font, name, cx + 31, cy + 12, TEXT, true);
                String state = enabled ? "ON" : "OFF";
                g.drawString(font, state, cx + 31, cy + 31, enabled ? PURPLE : MUTED, false);
                int sx = cx + cardW - 48;
                int sy = cy + 18;
                rounded(g, sx, sy, 35, 18, enabled ? 0xFF7955AA : 0xFF46414F, 9);
                rounded(g, enabled ? sx + 18 : sx + 2, sy + 2, 14, 14,
                    enabled ? 0xFFF8F0FF : 0xFFB6B0C1, 7);
            }
        }

        if (modules.isEmpty()) g.drawCenteredString(font, "No modules found", (left + right) / 2, top + 18, MUTED);

        // Selected module's details/settings card.
        if (!selected.isEmpty()) {
            int dy = y + h - 88;
            rounded(g, left, dy, right - left, 70, 0xE51D1928, 10);
            g.fill(left + 10, dy + 1, right - 10, dy + 2, 0x66C49AFF);
            g.drawString(font, selected, left + 14, dy + 10, TEXT, true);
            g.drawString(font, VoidModules.descriptionOf(selected), left + 14, dy + 29, MUTED, false);
            if (selected.equals("HUD")) {
                String scale = "HUD SCALE  " + Math.round(VoidModules.hudScale() * 100) + "%";
                g.drawString(font, scale, left + 14, dy + 48, PURPLE, true);
                rounded(g, right - 82, dy + 39, 28, 24, 0xFF393044, 7);
                rounded(g, right - 47, dy + 39, 28, 24, 0xFF393044, 7);
                g.drawCenteredString(font, "−", right - 68, dy + 47, TEXT);
                g.drawCenteredString(font, "+", right - 33, dy + 47, TEXT);
            } else {
                String state = VoidModules.isEnabled(selected) ? "ENABLED" : "DISABLED";
                g.drawString(font, state, right - font.width(state) - 14, dy + 13,
                    VoidModules.isEnabled(selected) ? PURPLE : MUTED, true);
                g.drawString(font, "Click the switch on the card to toggle", right - 220, dy + 42, MUTED, false);
            }
        }

        int totalRows = (modules.size() + columns - 1) / columns;
        if (totalRows > visibleRows) {
            int track = Math.max(1, bottom - top);
            int thumb = Math.max(20, track * visibleRows / totalRows);
            int ty = top + (track - thumb) * scroll / (totalRows - visibleRows);
            rounded(g, right + 8, ty, 3, thumb, PURPLE_DARK, 2);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        searchFocused = mx >= left && mx <= right && my >= y + 80 && my <= y + 107;
        for (int i = 0; i < 3; i++) {
            int iy = y + 70 + i * 62;
            if (mx >= x + 5 && mx <= x + rail - 5 && my >= iy && my <= iy + 48) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                selected = "";
                rebuild();
                return true;
            }
        }
        if (selected.equals("HUD")) {
            int dy = y + h - 88;
            if (my >= dy + 39 && my <= dy + 63) {
                if (mx >= right - 84 && mx <= right - 52) VoidModules.setHudScale(VoidModules.hudScale() - 0.05F);
                else if (mx >= right - 49 && mx <= right - 17) VoidModules.setHudScale(VoidModules.hudScale() + 0.05F);
                return true;
            }
        }
        int visibleRows = Math.max(1, (bottom - top + gap) / (cardH + gap));
        for (int row = 0; row < visibleRows; row++) for (int col = 0; col < columns; col++) {
            int index = (scroll + row) * columns + col;
            if (index >= modules.size()) continue;
            int cx = left + col * (cardW + gap), cy = top + row * (cardH + gap);
            if (mx >= cx && mx <= cx + cardW && my >= cy && my <= cy + cardH) {
                String name = modules.get(index);
                if (mx >= cx + cardW - 54) VoidModules.toggle(name);
                else selected = selected.equals(name) ? "" : name;
                init();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!searchFocused) return super.charTyped(e);
        char ch = (char)e.codepoint();
        if (ch >= 32 && ch != 127 && search.length() < 48) {
            search += ch;
            scroll = 0;
            rebuild();
        }
        return true;
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
        int rows = (modules.size() + columns - 1) / columns;
        int visible = Math.max(1, (bottom - top + gap) / (cardH + gap));
        scroll = Math.max(0, Math.min(Math.max(0, rows - visible), scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
