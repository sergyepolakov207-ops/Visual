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
    private static final int PURPLE = 0xFFD7A5FF;
    private static final int PURPLE_DARK = 0xFF9B4DFF;

    private int x, y, w, h, rail, left, right, top, bottom;
    private int cardW, cardH = 82, gap = 12, columns, scroll;
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
        w = Math.min(width - 24, Math.max(360, (int)(width * 0.92)));
        h = Math.min(height - 24, Math.max(250, (int)(height * 0.88)));
        x = (width - w) / 2;
        y = (height - h) / 2;
        rail = Math.max(78, Math.min(104, width / 13));
        left = x + rail + 22;
        right = x + w - 24;
        top = y + 126;
        bottom = y + h - 96;
        columns = (right - left) >= 500 ? 2 : 1;
        cardW = (right - left - gap * (columns - 1)) / columns;
        cardH = 82;
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
        if (!modules.contains(selected)) selected = modules.isEmpty() ? "" : modules.get(0);
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
        rounded(g, px, py, pw, ph, active ? 0xE8322049 : hover ? 0xD52C263A : 0xB91B1925, 13);
        g.fill(px + 13, py + 1, px + pw - 13, py + 2, active ? 0xBBD7A5FF : 0x44FFFFFF);
        g.fill(px + 1, py + 8, px + 2, py + ph - 8, 0x20FFFFFF);
        g.fill(px + pw - 2, py + 8, px + pw - 1, py + ph - 8, 0x187F73A6);
        if (active) { g.fill(px + 1, py + 12, px + 3, py + ph - 12, PURPLE); g.fill(px + 4, py + 14, px + 5, py + ph - 14, 0x557D43C7); }
    }

    private String categoryName() {
        return switch (category) {
            case RENDER -> "RENDER";
            case HUD -> "HUD";
            case PLAYER -> "PLAYER";
            case MOVEMENT -> "MOVE";
            case MISC -> "MISC";
        };
    }

    private String categoryInfo() {
        return switch (category) {
            case RENDER -> "Visual appearance and camera options";
            case HUD -> "Your interface, your layout";
            case PLAYER -> "Player status and useful information";
            case MOVEMENT -> "Movement indicators and key display";
            case MISC -> "Performance, world and session information";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        try { renderBlurredBackground(g); } catch (RuntimeException | LinkageError ignored) { }
        g.fill(0, 0, width, height, 0x72070412);
        g.fill(0, 0, width, height / 3, 0x120F0620);
        g.fill(0, height * 2 / 3, width, height, 0x180A0615);

        // Large translucent glass body with a reflected top edge.
        rounded(g, x, y, w, h, 0xE20B0A14, 19);
        rounded(g, x + 2, y + 2, w - 4, h - 4, 0x4A39264F, 17);
        g.fill(x + 18, y + 2, x + w - 18, y + 4, 0x66FFFFFF);
        g.fill(x + 30, y + 5, x + w - 30, y + 6, 0x35D7B8FF);
        g.fill(x + 5, y + h - 5, x + w - 5, y + h - 4, 0x337D43C7);
        g.fill(x + 3, y + 18, x + 4, y + h - 18, 0x44FFFFFF);
        g.fill(x + w - 4, y + 20, x + w - 3, y + h - 20, 0x338D78C5);
        g.fill(x + rail, y + 14, x + rail + 1, y + h - 14, 0x443E354F);

        // Sidebar.
        rounded(g, x + 12, y + 14, rail - 24, 42, 0xFF352344, 12);
        g.fill(x + 19, y + 15, x + rail - 19, y + 16, 0x88E4C8FF);
        g.drawCenteredString(font, "S", x + rail / 2, y + 29, PURPLE);
        g.drawCenteredString(font, "SWIFT", x + rail / 2, y + 56, MUTED);
        String[] icons = {"✧", "▦", "♙", "⌁", "⚙"};
        String[] names = {"RENDER", "HUD", "PLAYER", "MOVE", "MISC"};
        for (int i = 0; i < names.length; i++) {
            int iy = y + 70 + i * 58;
            boolean active = category.ordinal() == i;
            if (active) { rounded(g, x + 7, iy, rail - 14, 50, 0xFF39254C, 13); g.fill(x + 7, iy + 10, x + 9, iy + 40, PURPLE); }
            g.drawCenteredString(font, icons[i], x + rail / 2, iy + 8, active ? PURPLE : MUTED);
            g.drawCenteredString(font, names[i], x + rail / 2, iy + 27, active ? TEXT : MUTED);
        }

        // Brand and category heading.
        g.drawString(font, "SWIFT", left, y + 17, TEXT, true);
        g.drawString(font, "VISUALS", left + font.width("SWIFT ") + 2, y + 17, PURPLE, true);
        rounded(g, left, y + 38, 6, 6, PURPLE, 3);
        g.drawString(font, categoryInfo(), left + 13, y + 37, MUTED, false);
        g.drawString(font, "FABRIC 1.21.11", right - font.width("FABRIC 1.21.11"), y + 18, MUTED, false);
        String activeCount = VoidModules.all().entrySet().stream().filter(e -> VoidModules.categoryOf(e.getKey()) == category && e.getValue()).count() + " ACTIVE";
        rounded(g, right - font.width(activeCount) - 22, y + 37, font.width(activeCount) + 18, 18, 0xFF292333, 7);
        g.drawString(font, activeCount, right - font.width(activeCount) - 13, y + 42, PURPLE, true);
        g.fill(left, y + 68, right, y + 69, 0x443E354F);

        // Search field.
        rounded(g, left, y + 79, right - left, 32, searchFocused ? 0xFF38284A : 0xC423202E, 11);
        g.fill(left + 12, y + 80, right - 12, y + 81, searchFocused ? 0x99D7A5FF : 0x33FFFFFF);
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
                g.drawString(font, name, cx + 31, cy + 9, TEXT, true);
                String description = VoidModules.descriptionOf(name);
                int maxDescription = Math.max(30, cardW - 100);
                while (font.width(description) > maxDescription && description.length() > 8) description = description.substring(0, description.length() - 2);
                if (!description.equals(VoidModules.descriptionOf(name))) description += "...";
                g.drawString(font, description, cx + 31, cy + 29, MUTED, false);
                String state = enabled ? "ON" : "OFF";
                g.drawString(font, state, cx + 31, cy + 52, enabled ? PURPLE : MUTED, true);
                int sx = cx + cardW - 48;
                int sy = cy + 30;
                rounded(g, sx, sy, 37, 20, enabled ? 0xFF9A45E8 : 0xFF484253, 10);
                rounded(g, enabled ? sx + 19 : sx + 2, sy + 3, 14, 14,
                    enabled ? 0xFFF8F0FF : 0xFFB6B0C1, 7);
            }
        }

        if (modules.isEmpty()) g.drawCenteredString(font, "No modules found", (left + right) / 2, top + 18, MUTED);

        // Selected module's details/settings card.
        if (!selected.isEmpty()) {
            int dy = y + h - 82;
            rounded(g, left, dy, right - left, 72, 0xE51A1525, 14);
            g.fill(left + 12, dy + 1, right - 12, dy + 2, 0x44FFFFFF);
            g.fill(left + 10, dy + 1, right - 10, dy + 2, 0x99D7A5FF);
            g.drawString(font, selected, left + 14, dy + 10, TEXT, true);
            g.drawString(font, VoidModules.descriptionOf(selected), left + 14, dy + 30, MUTED, false);
            g.drawString(font, "MODULE DETAILS", left + 14, dy + 49, 0xFF8D829F, true);
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
        searchFocused = mx >= left && mx <= right && my >= y + 79 && my <= y + 109;
        for (int i = 0; i < VoidModules.Category.values().length; i++) {
            int iy = y + 70 + i * 58;
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
