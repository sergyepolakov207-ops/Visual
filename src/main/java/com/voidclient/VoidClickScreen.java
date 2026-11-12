package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int BACKDROP = 0x99000000;
    private static final int WINDOW = 0xFF202020;
    private static final int ROW = 0xFF303030;
    private static final int ROW_HOVER = 0xFF383838;
    private static final int TEXT = 0xFFF1F1F1;
    private static final int MUTED = 0xFFB7B7B7;
    private static final int BLUE = 0xFF579BFF;
    private static final int SWITCH_OFF = 0xFF555555;

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
        rowH = 31;
        cardW = (panelW - 40 - (columns - 1) * gap) / columns;
        listY = panelY + 68;
        listBottom = panelY + panelH - 18;
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

        // Pulse-inspired compact top bar: close indicator and three category tabs.
        rounded(g, panelX + 15, panelY + 14, 10, 10, 5, 0xFFE65F5F);
        String[] tabs = tabLabels();
        int tabW = panelW >= 500 ? 78 : 68;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabStart() + i * (tabW + 7);
            boolean active = category.ordinal() == i;
            rounded(g, x, panelY + 9, tabW, 27, 6, active ? 0xFF454545 : 0xFF2A2A2A);
            g.drawCenteredString(font, tabs[i], x + tabW / 2, panelY + 18, active ? TEXT : MUTED);
        }

        String heading = tabs[category.ordinal()];
        g.drawString(font, heading, panelX + 20, panelY + 49, TEXT, true);
        String count = visible.size() + " MODULES";
        g.drawString(font, count, panelX + panelW - 20 - font.width(count), panelY + 49, MUTED, false);

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
