package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int ROW_H = 52;
    private int panelX, panelY, panelW, panelH, scroll;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() { super(Component.literal("SWIFT VISUALS")); }

    @Override protected void init() {
        panelW = Math.min(720, Math.max(280, width - 24));
        panelH = Math.min(520, Math.max(260, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        rebuild();
    }

    private int sidePad() { return panelW < 420 ? 12 : 20; }
    private int gap() { return panelW < 420 ? 8 : 12; }
    private int cardW() { return (panelW - sidePad() * 2 - gap()) / 2; }
    private int columns() { return panelW < 340 ? 1 : 2; }
    private int rowY() { return panelY + 104; }
    private int visibleRows() { return Math.max(1, (panelH - 140) / ROW_H); }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream()
            .filter(n -> VoidModules.categoryOf(n) == category)
            .forEach(visible::add);
        scroll = Math.min(scroll, Math.max(0, (visible.size() + columns() - 1) / columns() - visibleRows()));
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0xC9000000);
        g.fill(panelX + 3, panelY + 4, panelX + panelW + 3, panelY + panelH + 4, 0x50000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20D0C14);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x180E0B20);

        // Thin purple ambient top edge, matching the reference's restrained dark look.
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF62409B);

        // Category navigation.
        String[] tabs = {"Visuals", "HUD", "Utilities"};
        int tabY = panelY + 18;
        int tabW = Math.min(104, (panelW - sidePad() * 2) / 3);
        for (int i = 0; i < tabs.length; i++) {
            int x = panelX + sidePad() + i * (tabW + 10);
            boolean active = category.ordinal() == i;
            g.drawString(font, tabs[i], x, tabY + 5, active ? 0xFFE7D9FF : 0xFF777487, active);
            if (active) g.fill(x, tabY + 24, x + font.width(tabs[i]), tabY + 26, 0xFF9C70F5);
        }

        // Search field and small edit glyph.
        int searchW = Math.min(210, Math.max(76, panelW / 3));
        int searchX = panelX + panelW - sidePad() - searchW - 25;
        int searchY = panelY + 12;
        g.fill(searchX, searchY, searchX + searchW, searchY + 30, 0xFF171520);
        g.fill(searchX, searchY, searchX + searchW, searchY + 1, 0xFF292536);
        g.drawString(font, "⌕  Search", searchX + 10, searchY + 11, 0xFF666274);
        g.drawString(font, "✎", panelX + panelW - sidePad() - 14, searchY + 9, 0xFF9384B4);
        g.fill(panelX + sidePad(), panelY + 52, panelX + panelW - sidePad(), panelY + 53, 0xFF24212D);

        int cols = columns();
        int cw = cardW();
        int firstY = rowY();
        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = panelX + sidePad() + col * (cw + gap());
                int y = firstY + row * ROW_H;
                int h = ROW_H - 6;
                boolean hover = mx >= x && mx <= x + cw && my >= y && my <= y + h;

                // Subtle outlined module tile; no extra status text, like the reference.
                g.fill(x, y, x + cw, y + h, hover ? 0xFF191722 : 0xFF13121A);
                g.fill(x, y, x + cw, y + 1, 0xFF252230);
                g.fill(x, y + h - 1, x + cw, y + h, 0xFF201E29);
                g.fill(x, y, x + 1, y + h, 0xFF211F2A);
                g.drawString(font, name, x + 13, y + (h - 8) / 2, 0xFFE0DDE8, false);

                int sw = 34, sh = 18;
                int sx = x + cw - sw - 13;
                int sy = y + (h - sh) / 2;
                g.fill(sx, sy, sx + sw, sy + sh, enabled ? 0xFF7950D5 : 0xFF30303D);
                g.fill(enabled ? sx + 18 : sx + 3, sy + 3, enabled ? sx + 31 : sx + 16, sy + 15,
                    enabled ? 0xFFF6F1FF : 0xFF777584);
            }
        }

        int totalRows = (visible.size() + cols - 1) / cols;
        if (totalRows > rows) {
            int trackX = panelX + panelW - 4;
            int trackH = rows * ROW_H - 6;
            g.fill(trackX, firstY, trackX + 2, firstY + trackH, 0xFF24212D);
            int thumbH = Math.max(18, trackH * rows / totalRows);
            int thumbY = firstY + (trackH - thumbH) * scroll / (totalRows - rows);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFF8960DE);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        int tabY = panelY + 18;
        int tabW = Math.min(104, (panelW - sidePad() * 2) / 3);
        for (int i = 0; i < 3; i++) {
            int x = panelX + sidePad() + i * (tabW + 10);
            if (mx >= x && mx <= x + tabW && my >= tabY && my <= tabY + 28) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                rebuild();
                return true;
            }
        }
        int cols = columns(), cw = cardW(), rows = visibleRows(), firstY = rowY();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= visible.size()) continue;
                int x = panelX + sidePad() + col * (cw + gap());
                int y = firstY + row * ROW_H;
                if (mx >= x && mx <= x + cw && my >= y && my <= y + ROW_H - 6) {
                    VoidModules.toggle(visible.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int cols = columns();
        int max = Math.max(0, (visible.size() + cols - 1) / cols - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
