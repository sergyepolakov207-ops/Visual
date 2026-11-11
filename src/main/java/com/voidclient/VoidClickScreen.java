package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int ROW_H = 42;
    private int panelX, panelY, panelW, panelH, scroll;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();
    private final double[] particleX = new double[28];
    private final double[] particleY = new double[28];
    private final double[] particleSpeed = new double[28];
    private int animationTick;

    public VoidClickScreen() { super(Component.literal("SWIFT VISUALS")); }

    @Override protected void init() {
        panelW = Math.min(760, Math.max(280, width - 28));
        panelH = Math.min(390, Math.max(220, height - 48));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        for (int i = 0; i < particleX.length; i++) {
            particleX[i] = (i * 73 + 19) % Math.max(1, panelW - 20);
            particleY[i] = (i * 47 + 31) % Math.max(1, panelH - 20);
            particleSpeed[i] = 0.25 + (i % 4) * 0.12;
        }
        rebuild();
    }

    private int sidePad() { return panelW < 420 ? 12 : 20; }
    private int gap() { return panelW < 420 ? 8 : 12; }
    private int columns() { return panelW < 340 ? 1 : 2; }
    private int cardW() { return columns() == 1 ? panelW - sidePad() * 2 : (panelW - sidePad() * 2 - gap()) / 2; }
    private int rowY() { return panelY + 82; }
    private int visibleRows() { return Math.max(1, (panelH - 100) / ROW_H); }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream().filter(n -> VoidModules.categoryOf(n) == category).forEach(visible::add);
        scroll = Math.min(scroll, Math.max(0, (visible.size() + columns() - 1) / columns() - visibleRows()));
    }

    @Override public void tick() {
        super.tick();
        animationTick++;
        if (!VoidModules.isEnabled("Menu Particles")) return;
        int count = VoidModules.isEnabled("Reduced Effects") ? 7 : particleX.length;
        for (int i = 0; i < count; i++) {
            particleY[i] -= particleSpeed[i];
            particleX[i] += Math.sin((animationTick + i * 17) * 0.025) * 0.12;
            if (particleY[i] < 4) {
                particleY[i] = panelH - 8;
                particleX[i] = (i * 73 + animationTick * 3) % Math.max(1, panelW - 20);
            }
        }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0xFF08070D);
        g.fill(panelX + 3, panelY + 4, panelX + panelW + 3, panelY + panelH + 4, 0x50000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20D0C14);
        g.fill(panelX, panelY, panelX + panelW, panelY + 3, 0xFFB17CFF);
        g.fill(panelX, panelY + 3, panelX + 1, panelY + panelH - 1, 0xFF51366F);
        g.fill(panelX + panelW - 1, panelY + 3, panelX + panelW, panelY + panelH - 1, 0xFF51366F);
        g.drawString(font, "SWIFT", panelX + sidePad(), panelY + 10, 0xFFF1E9FF, true);
        g.drawString(font, "VISUAL CLIENT", panelX + sidePad() + 43, panelY + 10, 0xFF9E91B6, false);

        if (VoidModules.isEnabled("Menu Particles")) {
            int count = VoidModules.isEnabled("Reduced Effects") ? 7 : particleX.length;
            for (int i = 0; i < count; i++) {
                int px = panelX + 5 + (int) particleX[i];
                int py = panelY + 5 + (int) particleY[i];
                g.fill(px, py, px + 2, py + 2, (i % 3 == 0) ? 0x557F5BCE : 0x337F5BCE);
            }
        }

        String[] tabs = {"VISUALS", "HUD", "PVP"};
        int tabY = panelY + 35;
        int tabW = Math.min(104, (panelW - sidePad() * 2) / 3);
        for (int i = 0; i < tabs.length; i++) {
            int x = panelX + sidePad() + i * (tabW + 10);
            boolean active = category.ordinal() == i;
            int tabWidth = Math.min(tabW, font.width(tabs[i]) + 22);
            g.fill(x - 6, tabY, x + tabWidth, tabY + 23, active ? 0xFF38264F : 0xFF191620);
            if (active) {
                g.fill(x - 6, tabY + 21, x + tabWidth, tabY + 23, 0xFFB17CFF);
                g.fill(x - 6, tabY, x - 4, tabY + 23, 0xFFB17CFF);
            }
            g.drawString(font, tabs[i], x + 4, tabY + 8, active ? 0xFFF1E8FF : 0xFF8D879C, active);
        }

        int searchW = Math.min(210, Math.max(76, panelW / 3));
        int searchX = panelX + panelW - sidePad() - searchW - 25;
        int searchY = panelY + 12;
        g.fill(searchX, searchY, searchX + searchW, searchY + 30, 0xFF171520);
        g.drawString(font, "⌕  Search", searchX + 10, searchY + 11, 0xFF777184);
        g.drawString(font, "✎", panelX + panelW - sidePad() - 14, searchY + 9, 0xFF9384B4);
        g.fill(panelX + sidePad(), panelY + 52, panelX + panelW - sidePad(), panelY + 53, 0xFF24212D);

        int cols = columns(), cw = cardW(), firstY = rowY(), rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = panelX + sidePad() + col * (cw + gap());
                int y = firstY + row * ROW_H, h = ROW_H - 5;
                boolean hover = mx >= x && mx <= x + cw && my >= y && my <= y + h;
                g.fill(x, y, x + cw, y + h, hover ? 0xFF241B32 : 0xFF17151F);
                g.fill(x, y, x + cw, y + 1, hover ? 0xFF986BD2 : 0xFF352B42);
                g.fill(x, y + h - 1, x + cw, y + h, 0xFF292332);
                g.fill(x, y + 5, x + 2, y + h - 5, enabled ? 0xFFB17CFF : 0xFF514A5B);
                g.drawString(font, name, x + 13, y + (h - 8) / 2, 0xFFE0DDE8, false);

                int sw = 34, sh = 18, sx = x + cw - sw - 13, sy = y + (h - sh) / 2;
                g.fill(sx, sy, sx + sw, sy + sh, enabled ? 0xFF8955D8 : 0xFF34313D);
                g.fill(enabled ? sx + 18 : sx + 3, sy + 3, enabled ? sx + 31 : sx + 16, sy + 15,
                    enabled ? 0xFFFFFFFF : 0xFF898493);
            }
        }

        int totalRows = (visible.size() + cols - 1) / cols;
        if (totalRows > rows) {
            int trackX = panelX + panelW - 4, trackH = rows * ROW_H - 5;
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
        int tabY = panelY + 35, tabW = Math.min(104, (panelW - sidePad() * 2) / 3);
        for (int i = 0; i < 3; i++) {
            int x = panelX + sidePad() + i * (tabW + 10);
            if (mx >= x && mx <= x + tabW && my >= tabY && my <= tabY + 28) {
                category = VoidModules.Category.values()[i]; scroll = 0; rebuild(); return true;
            }
        }
        int cols = columns(), cw = cardW(), rows = visibleRows(), firstY = rowY();
        for (int row = 0; row < rows; row++) for (int col = 0; col < cols; col++) {
            int index = (scroll + row) * cols + col;
            if (index >= visible.size()) continue;
            int x = panelX + sidePad() + col * (cw + gap()), y = firstY + row * ROW_H;
            if (mx >= x && mx <= x + cw && my >= y && my <= y + ROW_H - 5) {
                VoidModules.toggle(visible.get(index)); return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int cols = columns();
        int max = Math.max(0, (visible.size() + cols - 1) / cols - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
