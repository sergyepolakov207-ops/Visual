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
    private static final int WHITE = 0xFFF4F1FA;
    private static final int MUTED = 0xFF9693A8;
    private static final int PURPLE = 0xFFB88AFF;
    private static final int PURPLE_DARK = 0xFF7654B5;

    private int px, py, pw, ph, rail, contentX, contentW;
    private int searchX, searchY, searchW, gridY, gridBottom;
    private int cardW, cardH, gap, columns, scroll;
    private boolean searchFocused, blurAvailable = true;
    private String search = "";
    private String selectedModule = "";
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> modules = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        pw = Math.min(width - 28, Math.max(300, (int)(width * 0.54)));
        ph = Math.min(height - 24, Math.max(230, (int)(height * 0.83)));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        rail = width < 520 ? 46 : 54;
        contentX = px + rail + 8;
        contentW = pw - rail - 24;
        gap = 8;
        columns = contentW >= 500 ? 3 : contentW >= 300 ? 2 : 1;
        cardW = (contentW - gap * (columns - 1)) / columns;
        cardH = width < 520 ? 38 : 42;
        searchX = contentX;
        searchY = py + 58;
        searchW = contentW;
        gridY = searchY + 38;
        gridBottom = py + ph - 22;
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
        int rows = (modules.size() + columns - 1) / columns;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows - visibleRows())));
        if (!modules.contains(selectedModule)) selectedModule = "";
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w < 4 || h < 4) return;
        int radius = Math.min(7, Math.min(w / 2, h / 2));
        for (int dy = 0; dy < h; dy++) {
            int edge = dy < radius ? radius - (int)Math.sqrt(radius * radius - (radius - dy) * (radius - dy))
                : dy >= h - radius ? radius - (int)Math.sqrt(radius * radius - (dy - (h - radius - 1)) * (dy - (h - radius - 1)))
                : 0;
            g.fill(x + edge, y + dy, x + w - edge, y + dy + 1, color);
        }
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 6, y, x + w - 6, y + 1, color);
        g.fill(x + 6, y + h - 1, x + w - 6, y + h, color);
        g.fill(x, y + 6, x + 1, y + h - 6, color);
        g.fill(x + w - 1, y + 6, x + w, y + h - 6, color);
    }

    private String categoryName() {
        return switch (category) {
            case VISUALS -> "VISUALS";
            case HUD -> "HUD";
            case PVP -> "PVP";
        };
    }

    private String categoryDescription() {
        return switch (category) {
            case VISUALS -> "Visual effects and quality of life";
            case HUD -> "Your interface, your layout";
            case PVP -> "Combat information and movement";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (blurAvailable) {
            try { renderBlurredBackground(g); }
            catch (RuntimeException | LinkageError e) { blurAvailable = false; }
        }
        g.fill(0, 0, width, height, 0x5A08060D);

        // New glass shell: cool tinted glass, fine rim and reflected light.
        panel(g, px, py, pw, ph, 0xD9161319);
        panel(g, px + 2, py + 2, pw - 4, ph - 4, 0x241F1926);
        outline(g, px, py, pw, ph, 0x448D70C4);
        g.fill(px + 12, py + 2, px + pw - 12, py + 3, 0x44FFFFFF);
        g.fill(px + 2, py + 18, px + 3, py + ph - 18, 0x55FFFFFF);
        g.fill(px + pw - 3, py + 20, px + pw - 2, py + ph - 20, 0x338C9BFF);

        // Left navigation rail.
        g.fill(px + 1, py + 1, px + rail, py + ph - 1, 0xDD17131D);
        g.fill(px + rail, py + 12, px + rail + 1, py + ph - 12, 0x443F4660);
        panel(g, px + 13, py + 15, rail - 26, 34, 0xFF29253A);
        g.drawCenteredString(font, "S", px + rail / 2, py + 27, PURPLE);

        String[] icons = {"✧", "▦", "⚔"};
        String[] labels = {"VIS", "HUD", "PVP"};
        for (int i = 0; i < 3; i++) {
            int iy = py + 62 + i * 54;
            boolean active = category.ordinal() == i;
            if (active) {
                panel(g, px + 7, iy, rail - 14, 40, 0xFF30263B);
                g.fill(px + 7, iy + 8, px + 9, iy + 32, PURPLE);
            }
            g.drawCenteredString(font, icons[i], px + rail / 2, iy + 7,
                active ? PURPLE : 0xFF85869A);
            g.drawCenteredString(font, labels[i], px + rail / 2, iy + 25,
                active ? WHITE : MUTED);
        }

        // Compact Phantom-style header: category chip and short description.
        panel(g, contentX + 2, py + 10, 88, 30, 0xFF211D25);
        g.drawCenteredString(font, categoryName(), contentX + 46, py + 21, WHITE);
        String subtitle = categoryDescription();
        panel(g, contentX + 98, py + 10, Math.max(120, Math.min(contentW - 220, font.width(subtitle) + 22)), 30, 0xFF211D25);
        g.drawString(font, "☷", contentX + 108, py + 21, MUTED, false);
        g.drawString(font, subtitle, contentX + 124, py + 21, WHITE, false);
        g.fill(contentX, py + 49, px + pw - 12, py + 50, 0x332E2937);

        // Module grid begins directly under the compact header (no oversized search bar).
        searchX = contentX;
        searchY = py + 54;
        searchW = contentW;
        gridY = searchY + 8;
        gridBottom = py + ph - 20;

        gridBottom = py + ph - 20;
        int rowsVisible = visibleRows();
        for (int row = 0; row < rowsVisible; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= modules.size()) continue;
                String name = modules.get(index);
                int x = contentX + col * (cardW + gap);
                int y = gridY + row * (cardH + gap);
                boolean enabled = VoidModules.isEnabled(name);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + cardH;
                boolean selected = name.equals(selectedModule);

                panel(g, x, y, cardW, cardH,
                    selected ? 0xE13B2D50 : hover ? 0xC532293D : 0xB51B171D);
                outline(g, x, y, cardW, cardH,
                    selected ? 0xCCB88AFF : hover ? 0x887F6A91 : 0x443D3548);
                if (enabled) g.fill(x + 1, y + 10, x + 3, y + cardH - 10, PURPLE);
                g.drawString(font, enabled ? "✦" : "◇", x + 12, y + (cardH - 8) / 2,
                    enabled ? PURPLE : MUTED, false);

                int sx = x + cardW - 42;
                int sy = y + (cardH - 17) / 2;
                g.enableScissor(x + 34, y, sx - 8, y + cardH);
                g.drawString(font, name, x + 35, y + (cardH - 8) / 2,
                    enabled ? WHITE : 0xFFB5B4C1, true);
                g.disableScissor();

                panel(g, sx, sy, 31, 17, enabled ? 0xFF795CB0 : 0xFF39343F);
                panel(g, enabled ? sx + 16 : sx + 2, sy + 2, 13, 13,
                    enabled ? 0xFFF7F0FF : 0xFF9A9BA8);
                if (selected) g.drawString(font, "i", x + cardW - 58, y + 5, PURPLE, true);
            }
        }

        if (modules.isEmpty()) {
            g.drawCenteredString(font, "No modules found", contentX + contentW / 2,
                gridY + 18, MUTED);
        }

        // Settings drawer at the bottom of the content area.
        if (!selectedModule.isEmpty()) {
            int drawerH = category == VoidModules.Category.HUD && selectedModule.equals("HUD") ? 62 : 50;
            int drawerY = py + ph - drawerH - 12;
            panel(g, contentX, drawerY, contentW, drawerH, 0xF01B2030);
            outline(g, contentX, drawerY, contentW, drawerH, 0x88B88AFF);
            g.drawString(font, selectedModule, contentX + 12, drawerY + 9, WHITE, true);
            g.drawString(font, VoidModules.descriptionOf(selectedModule),
                contentX + 12, drawerY + 25, MUTED, false);
            if (selectedModule.equals("HUD")) {
                String scale = "SCALE  " + Math.round(VoidModules.hudScale() * 100) + "%";
                g.drawString(font, scale, contentX + 12, drawerY + 43, PURPLE, true);
                panel(g, contentX + contentW - 76, drawerY + 34, 26, 22, 0xFF39334B);
                panel(g, contentX + contentW - 43, drawerY + 34, 26, 22, 0xFF39334B);
                g.drawCenteredString(font, "−", contentX + contentW - 63, drawerY + 41, WHITE);
                g.drawCenteredString(font, "+", contentX + contentW - 30, drawerY + 41, WHITE);
            } else {
                g.drawString(font, VoidModules.isEnabled(selectedModule) ? "ENABLED" : "DISABLED",
                    contentX + contentW - 78, drawerY + 18, enabledColor(selectedModule), true);
            }
        }

        int totalRows = (modules.size() + columns - 1) / columns;
        if (totalRows > rowsVisible) {
            int track = Math.max(1, gridBottom - gridY);
            int thumb = Math.max(18, track * rowsVisible / totalRows);
            int ty = gridY + (track - thumb) * scroll / (totalRows - rowsVisible);
            panel(g, px + pw - 9, ty, 3, thumb, PURPLE_DARK);
        }
        super.render(g, mx, my, partialTick);
    }

    private int enabledColor(String name) {
        return VoidModules.isEnabled(name) ? 0xFFB88AFF : MUTED;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();

        if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + 32) {
            if (!search.isEmpty() && mx >= searchX + searchW - 30) {
                search = "";
                scroll = 0;
                rebuild();
                searchFocused = false;
            } else {
                searchFocused = true;
            }
            return true;
        }
        searchFocused = false;

        for (int i = 0; i < 3; i++) {
            int iy = py + 62 + i * 54;
            if (mx >= px + 5 && mx <= px + rail - 5 && my >= iy && my <= iy + 42) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                selectedModule = "";
                rebuild();
                return true;
            }
        }

        if ("HUD".equals(selectedModule)) {
            int drawerY = py + ph - 74;
            if (my >= drawerY + 34 && my <= drawerY + 58) {
                if (mx >= contentX + contentW - 78 && mx <= contentX + contentW - 48)
                    VoidModules.setHudScale(VoidModules.hudScale() - 0.05F);
                else if (mx >= contentX + contentW - 45 && mx <= contentX + contentW - 15)
                    VoidModules.setHudScale(VoidModules.hudScale() + 0.05F);
                return true;
            }
        }

        for (int row = 0; row < visibleRows(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= modules.size()) continue;
                int x = contentX + col * (cardW + gap);
                int y = gridY + row * (cardH + gap);
                if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) {
                    String name = modules.get(index);
                    if (mx >= x + cardW - 44) VoidModules.toggle(name);
                    else selectedModule = selectedModule.equals(name) ? "" : name;
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
        int rows = (modules.size() + columns - 1) / columns;
        scroll = Math.max(0, Math.min(Math.max(0, rows - visibleRows()),
            scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
