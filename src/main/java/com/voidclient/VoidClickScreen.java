package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int OVERLAY = 0x78050916;
    private static final int WINDOW = 0xE9161724;
    private static final int WINDOW_EDGE = 0xAA343A59;
    private static final int ROW = 0xB91B1D2B;
    private static final int ROW_HOVER = 0xE1262A3D;
    private static final int TEXT = 0xFFF2F3FA;
    private static final int MUTED = 0xFF858BA5;
    private static final int BLUE = 0xFF3978FF;
    private static final int SWITCH_OFF = 0xFF373B4D;

    private int panelX, panelY, panelW, panelH;
    private int columns, cardW, rowH, gap, listY, listBottom, scroll;
    private int tabY, tabStartX, tabStep, searchX, searchY, searchW;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(1200, Math.max(280, width - 40));
        panelH = Math.min(520, Math.max(240, height - 32));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        columns = panelW >= 520 ? 2 : 1;
        gap = 10;
        rowH = Math.max(34, Math.min(44, panelH / 11));
        cardW = (panelW - 44 - (columns - 1) * gap) / columns;
        tabY = panelY + 57;
        tabStartX = panelX + 20;
        tabStep = 82;
        searchW = Math.min(190, Math.max(90, panelW / 4));
        searchX = panelX + panelW - searchW - 54;
        searchY = panelY + 48;
        listY = panelY + 105;
        listBottom = panelY + panelH - 57;
        rebuild();
    }

    private int rowsVisible() {
        return Math.max(1, (listBottom - listY + gap) / (rowH + gap));
    }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream()
            .filter(name -> VoidModules.categoryOf(name) == category)
            .forEach(visible::add);
        int total = (visible.size() + columns - 1) / columns;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - rowsVisible())));
    }

    private void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
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
        return new String[] {"VISUALS", "HUD", "PVP"};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        // Keep the world visible behind the translucent client panel.
        g.fill(0, 0, width, height, OVERLAY);
        rounded(g, panelX, panelY, panelW, panelH, 14, WINDOW);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 2, WINDOW_EDGE);

        // Centered client branding, without the Pulse logo.
        g.drawCenteredString(font, "SWIFT VISUALS", panelX + panelW / 2, panelY + 17, TEXT);
        g.drawString(font, "X", panelX + panelW - 25, panelY + 18, MUTED, true);

        // Category navigation.
        String[] tabs = tabLabels();
        for (int i = 0; i < tabs.length; i++) {
            int x = tabStartX + i * tabStep;
            boolean active = category.ordinal() == i;
            g.drawString(font, tabs[i], x, tabY, active ? TEXT : MUTED, active);
            if (active) rounded(g, x, tabY + 17, font.width(tabs[i]) + 5, 3, 2, BLUE);
            if (i < tabs.length - 1) g.drawString(font, "/", x + 63, tabY, 0xFF555B72, false);
        }

        // Search-shaped field and settings glyph.
        rounded(g, searchX, searchY, searchW, 27, 13, 0xA910111B);
        g.drawString(font, "⌕  Поиск", searchX + 10, searchY + 9, MUTED, false);
        g.drawString(font, "⚙", panelX + panelW - 31, searchY + 8, BLUE, false);
        g.fill(panelX + 16, panelY + 87, panelX + panelW - 16, panelY + 88, 0x553F4665);

        // Two-column module list. These toggles call the real module registry.
        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = panelX + 20 + col * (cardW + gap);
                int y = listY + row * (rowH + gap);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + rowH;
                rounded(g, x, y, cardW, rowH, 12, hover ? ROW_HOVER : ROW);
                g.drawString(font, name, x + 14, y + (rowH - 8) / 2, TEXT, false);

                int sw = 36, sh = 19;
                int sx = x + cardW - sw - 13;
                int sy = y + (rowH - sh) / 2;
                rounded(g, sx, sy, sw, sh, 10, enabled ? BLUE : SWITCH_OFF);
                rounded(g, enabled ? sx + 19 : sx + 3, sy + 3, 13, 13, 7, 0xFFF4F5FC);
            }
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (totalRows > rowsVisible()) {
            int trackX = panelX + panelW - 7;
            int trackH = listBottom - listY;
            g.fill(trackX, listY, trackX + 2, listBottom, 0x553C4158);
            int thumbH = Math.max(18, trackH * rowsVisible() / totalRows);
            int thumbY = listY + (trackH - thumbH) * scroll / (totalRows - rowsVisible());
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, BLUE);
        }

        // Bottom icon rail, kept subtle like the reference.
        int navW = 174;
        int navX = panelX + (panelW - navW) / 2;
        int navY = panelY + panelH - 43;
        rounded(g, navX, navY, navW, 34, 12, 0xB910111C);
        String[] icons = {"G", "◇", "♟", "▣", "≡"};
        for (int i = 0; i < icons.length; i++) {
            int ix = navX + 7 + i * 32;
            if (i == 0) rounded(g, ix, navY + 4, 27, 26, 8, BLUE);
            g.drawCenteredString(font, icons[i], ix + 13, navY + 12, i == 0 ? TEXT : MUTED);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();

        // The X closes the screen.
        if (mx >= panelX + panelW - 38 && mx <= panelX + panelW - 10
                && my >= panelY + 8 && my <= panelY + 38) {
            onClose();
            return true;
        }

        String[] tabs = tabLabels();
        for (int i = 0; i < tabs.length; i++) {
            int x = tabStartX + i * tabStep;
            int w = font.width(tabs[i]) + 8;
            if (mx >= x - 3 && mx <= x + w && my >= tabY - 5 && my <= tabY + 22) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                rebuild();
                return true;
            }
        }

        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                int x = panelX + 20 + col * (cardW + gap);
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
