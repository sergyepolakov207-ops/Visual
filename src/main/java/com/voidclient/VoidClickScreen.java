package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int SHADE = 0x9905000D;
    private static final int PANEL = 0xF00D0A13;
    private static final int PANEL_EDGE = 0xFF382047;
    private static final int SURFACE = 0xFF15101D;
    private static final int SURFACE_HOVER = 0xFF21152D;
    private static final int SURFACE_ACTIVE = 0xFF2B1740;
    private static final int TEXT = 0xFFF4ECFF;
    private static final int MUTED = 0xFF9B8BAA;
    private static final int ACCENT = 0xFFB34CFF;
    private static final int ACCENT_BRIGHT = 0xFFD18BFF;
    private static final int SWITCH_OFF = 0xFF403747;

    private int panelX, panelY, panelW, panelH;
    private int railW, contentX, contentW;
    private int listY, listBottom, cardW, rowH, gap, columns, scroll;
    private int searchX, searchY, searchW;
    private String searchText = "";
    private boolean blurAvailable = true;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(660, Math.max(260, width - 20));
        panelH = Math.min(430, Math.max(220, height - 28));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        railW = panelW >= 430 ? 42 : 30;
        contentX = panelX + railW + 12;
        contentW = panelW - railW - 24;
        gap = 7;
        columns = contentW >= 390 ? 2 : 1;
        cardW = (contentW - (columns - 1) * gap) / columns;
        rowH = panelH < 280 ? 24 : 28;

        searchX = contentX;
        searchY = panelY + 91;
        searchW = contentW;
        listY = searchY + 35;
        listBottom = panelY + panelH - 38;
        rebuild();
    }

    private int rowsVisible() {
        return Math.max(1, (listBottom - listY + gap) / (rowH + gap));
    }

    private void rebuild() {
        visible.clear();
        String query = searchText.toLowerCase(Locale.ROOT).trim();
        VoidModules.all().keySet().stream()
            .filter(name -> VoidModules.categoryOf(name) == category)
            .filter(name -> query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query))
            .forEach(visible::add);

        int totalRows = (visible.size() + columns - 1) / columns;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, totalRows - rowsVisible())));
    }

    private void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) {
                double dy = r - row - 0.5;
                inset = Math.max(0, r - (int) Math.sqrt(r * r - dy * dy));
            } else if (row >= h - r) {
                double dy = row - (h - r) + 0.5;
                inset = Math.max(0, r - (int) Math.sqrt(r * r - dy * dy));
            }
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private String[] tabLabels() {
        return new String[] {"VISUALS", "HUD", "MISC"};
    }

    private int tabWidth() {
        return Math.max(58, contentW / 3);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (blurAvailable) {
            try {
                renderBlurredBackground(g);
            } catch (RuntimeException | LinkageError blurError) {
                blurAvailable = false;
            }
        }
        g.fill(0, 0, width, height, SHADE);

        // Main glass panel and violet edge.
        rounded(g, panelX, panelY, panelW, panelH, 11, PANEL);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 2, PANEL_EDGE);
        g.fill(panelX + 1, panelY + panelH - 2, panelX + panelW - 1, panelY + panelH - 1, 0xAA6D28A0);

        // Header banner.
        g.fillGradient(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 43,
            0xFF32134C, 0xFF130D20);
        g.fill(panelX + 1, panelY + 42, panelX + panelW - 1, panelY + 44, ACCENT);
        g.drawCenteredString(font, "SWIFT VISUALS", panelX + panelW / 2, panelY + 10, TEXT);
        g.drawCenteredString(font, "CUSTOM CLIENT  /  FABRIC", panelX + panelW / 2, panelY + 26, ACCENT_BRIGHT);
        g.drawString(font, "×", panelX + panelW - 19, panelY + 9, ACCENT_BRIGHT, true);

        // Left icon rail.
        g.fill(panelX + railW, panelY + 44, panelX + railW + 1, panelY + panelH - 2, 0x553E2850);
        String[] railIcons = {"◇", "▤", "⚙"};
        for (int i = 0; i < railIcons.length; i++) {
            int y = panelY + 58 + i * 35;
            boolean active = category.ordinal() == i;
            if (active) rounded(g, panelX + 6, y - 5, railW - 12, 25, 6, SURFACE_ACTIVE);
            g.drawCenteredString(font, railIcons[i], panelX + railW / 2, y + 2,
                active ? ACCENT_BRIGHT : MUTED);
        }

        // Category tabs.
        String[] tabs = tabLabels();
        int tw = tabWidth();
        int tabY = panelY + 53;
        for (int i = 0; i < tabs.length; i++) {
            int x = contentX + i * tw;
            boolean active = category.ordinal() == i;
            if (active) {
                rounded(g, x, tabY, tw - 4, 25, 6, SURFACE_ACTIVE);
                g.fill(x + 9, tabY + 23, x + tw - 13, tabY + 25, ACCENT);
            }
            g.drawCenteredString(font, tabs[i], x + (tw - 4) / 2, tabY + 8,
                active ? ACCENT_BRIGHT : MUTED);
        }

        // Functional search field.
        rounded(g, searchX, searchY, searchW, 27, 7,
            SURFACE);
        g.fill(searchX + 1, searchY + 1, searchX + searchW - 1, searchY + 2,
            0xFF352A40);
        g.drawString(font, "⌕", searchX + 9, searchY + 8, ACCENT_BRIGHT, false);
        g.drawString(font, "Search modules", searchX + 26, searchY + 9, MUTED, false);

        // Two-column module cards; every switch reads and changes the real module state.
        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = contentX + col * (cardW + gap);
                int y = listY + row * (rowH + gap);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + rowH;
                rounded(g, x, y, cardW, rowH, 6, hover ? SURFACE_HOVER : SURFACE);
                g.fill(x + 1, y + 1, x + cardW - 1, y + 2, 0x443E2C4D);

                int switchW = 24, switchH = 13;
                int sx = x + cardW - switchW - 7;
                int sy = y + (rowH - switchH) / 2;
                g.enableScissor(x + 8, y, sx - 7, y + rowH);
                g.drawString(font, name, x + 9, y + (rowH - 8) / 2, TEXT, false);
                g.disableScissor();

                rounded(g, sx, sy, switchW, switchH, 7, enabled ? ACCENT : SWITCH_OFF);
                rounded(g, enabled ? sx + 13 : sx + 2, sy + 2, 9, 9, 5, 0xFFF8F2FF);
            }
        }

        if (visible.isEmpty()) {
            g.drawCenteredString(font, "No modules found", contentX + contentW / 2,
                listY + 8, MUTED);
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (totalRows > rowsVisible()) {
            int trackX = panelX + panelW - 5;
            int trackH = listBottom - listY;
            int thumbH = Math.max(15, trackH * rowsVisible() / totalRows);
            int thumbY = listY + (trackH - thumbH) * scroll / (totalRows - rowsVisible());
            g.fill(trackX, listY, trackX + 2, listBottom, 0x553D2B4B);
            rounded(g, trackX - 1, thumbY, 4, thumbH, 2, ACCENT);
        }

        // Compact footer/status strip.
        int footerY = panelY + panelH - 27;
        g.fill(contentX, footerY - 5, panelX + panelW - 10, footerY - 4, 0x553E2850);
        g.drawString(font, "SWIFT", contentX + 2, footerY + 3, ACCENT_BRIGHT, true);
        g.drawString(font, "•", contentX + 38, footerY + 3, ACCENT);
        g.drawString(font, "1.21.11", contentX + 47, footerY + 3, MUTED, false);
        String count = visible.size() + " MODULES";
        g.drawString(font, count, panelX + panelW - 14 - font.width(count),
            footerY + 3, MUTED, false);

        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();

        // Close button.
        if (mx >= panelX + panelW - 27 && mx <= panelX + panelW - 7
            && my >= panelY + 3 && my <= panelY + 29) {
            onClose();
            return true;
        }

        // Search focus.

        // Tabs.
        String[] tabs = tabLabels();
        int tw = tabWidth();
        int tabY = panelY + 53;
        for (int i = 0; i < tabs.length; i++) {
            int x = contentX + i * tw;
            if (mx >= x && mx <= x + tw - 4 && my >= tabY && my <= tabY + 25) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                rebuild();
                return true;
            }
        }

        // Module switches.
        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                int x = contentX + col * (cardW + gap);
                int y = listY + row * (rowH + gap);
                if (mx >= x && mx <= x + cardW && my >= y && my <= y + rowH) {
                    VoidModules.toggle(visible.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int totalRows = (visible.size() + columns - 1) / columns;
        int max = Math.max(0, totalRows - rowsVisible());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
