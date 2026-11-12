package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int BACKDROP = 0x99000000;
    private static final int WINDOW = 0xF0181722;
    private static final int ROW = 0xE91A1A27;
    private static final int ROW_HOVER = 0xF1262638;
    private static final int TEXT = 0xFFF1F1F1;
    private static final int MUTED = 0xFF85839B;
    private static final int BLUE = 0xFF3978FF;
    private static final int SWITCH_OFF = 0xFF39394B;

    private int panelX, panelY, panelW, panelH;
    private int columns, cardW, rowH, gap, listY, listBottom, scroll;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(760, Math.max(290, width - 32));
        panelH = Math.min(470, Math.max(230, height - 28));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        columns = panelW >= 500 ? 2 : 1;
        gap = 9;
        rowH = 36;
        cardW = (panelW - 40 - (columns - 1) * gap) / columns;
        listY = panelY + 104;
        listBottom = panelY + panelH - 54;
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
        return new String[] {"VISUALS", "HUD", "UTILITIES"};
    }

    private int tabStart() {
        int tabW = panelW >= 500 ? 78 : 68;
        return panelX + panelW - 14 - tabW * 3 - 7 * 2;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, BACKDROP);
        rounded(g, panelX, panelY, panelW, panelH, 9, WINDOW);

        String brand = "SWIFT VISUALS";
        g.drawCenteredString(font, brand, panelX + panelW / 2, panelY + 15, TEXT);
        g.drawString(font, "X", panelX + panelW - 25, panelY + 15, MUTED, true);

        String[] tabs = tabLabels();
        int tabX = panelX + 18;
        int tabY = panelY + 49;
        int tabW = 57;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabX + i * (tabW + 12);
            boolean active = category.ordinal() == i;
            g.drawString(font, tabs[i], x, tabY, active ? TEXT : MUTED, active);
            if (active) g.fill(x, tabY + 17, x + font.width(tabs[i]), tabY + 19, BLUE);
            if (i < 2) g.drawString(font, "/", x + tabW + 2, tabY, 0xFF555367, false);
        }
        int searchW = Math.min(150, panelW / 3);
        int searchX = panelX + panelW - searchW - 48;
        rounded(g, searchX, panelY + 42, searchW, 25, 12, 0xCC11111B);
        g.drawString(font, "⌕  Поиск", searchX + 10, panelY + 50, MUTED, false);
        g.drawString(font, "⚙", panelX + panelW - 29, panelY + 49, MUTED, false);
        g.fill(panelX + 14, panelY + 78, panelX + panelW - 14, panelY + 79, 0x553F3D55);

        int cw = cardW;
        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = panelX + 20 + col * (cw + gap);
                int y = listY + row * (rowH + gap);
                boolean hover = mx >= x && mx <= x + cw && my >= y && my <= y + rowH;
                rounded(g, x, y, cw, rowH, 6, hover ? ROW_HOVER : ROW);
                g.drawString(font, name, x + 11, y + 11, TEXT, false);

                int sw = 27, sh = 15;
                int sx = x + cw - sw - 10;
                int sy = y + (rowH - sh) / 2;
                rounded(g, sx, sy, sw, sh, 8, enabled ? BLUE : SWITCH_OFF);
                rounded(g, enabled ? sx + 14 : sx + 2, sy + 2, 11, 11, 6, 0xFFF7F7F7);
            }
        }

        int navW = 156;
        int navX = panelX + (panelW - navW) / 2;
        int navY = panelY + panelH - 39;
        rounded(g, navX, navY, navW, 30, 9, 0xCC11111B);
        String[] navIcons = {"⬡", "◇", "♟", "▣", "▤"};
        for (int i = 0; i < navIcons.length; i++) {
            int nx = navX + 5 + i * 30;
            if (i == 0) rounded(g, nx, navY + 3, 25, 24, 6, BLUE);
            g.drawCenteredString(font, navIcons[i], nx + 12, navY + 10, i == 0 ? TEXT : MUTED);
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (totalRows > rowsVisible()) {
            int trackX = panelX + panelW - 6;
            int trackH = listBottom - listY;
            g.fill(trackX, listY, trackX + 2, listBottom, 0xFF383838);
            int thumbH = Math.max(16, trackH * rowsVisible() / totalRows);
            int thumbY = listY + (trackH - thumbH) * scroll / (totalRows - rowsVisible());
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, BLUE);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        String[] tabs = tabLabels();
        int tabW = panelW >= 500 ? 78 : 68;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabStart() + i * (tabW + 7);
            if (mx >= x && mx <= x + tabW && my >= panelY + 9 && my <= panelY + 36) {
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
