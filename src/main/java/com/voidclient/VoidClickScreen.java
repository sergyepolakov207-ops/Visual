package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int SHADE = 0x70000000;
    private static final int PANEL = 0xE8171512;
    private static final int PANEL_TOP = 0xFF211D18;
    private static final int BORDER = 0x553F382D;
    private static final int CARD = 0xA827241F;
    private static final int CARD_HOVER = 0xD6373028;
    private static final int TEXT = 0xFFF0ECE4;
    private static final int MUTED = 0xFF938B7E;
    private static final int ACCENT = 0xFF36A7FF;
    private static final int SWITCH_OFF = 0xFF49443D;

    private int panelX, panelY, panelW, panelH;
    private int columns, cardW, rowH, gap, listX, listY, listBottom, scroll;
    private int sidebarW, gridW;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(1200, Math.max(280, width - 16));
        panelH = Math.min(520, Math.max(220, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sidebarW = 34;
        gap = 6;
        columns = panelW >= 340 ? 3 : panelW >= 270 ? 2 : 1;
        gridW = panelW - sidebarW - 26;
        cardW = (gridW - (columns - 1) * gap) / columns;
        rowH = Math.max(25, Math.min(34, panelH / 15));
        listX = panelX + sidebarW + 8;
        listY = panelY + 70;
        listBottom = panelY + panelH - 13;
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
        // Use Minecraft 1.21.11 native post-process blur for the world behind this screen.
        renderBlurredBackground(g);
        g.fill(0, 0, width, height, SHADE);
        rounded(g, panelX, panelY, panelW, panelH, 12, PANEL);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 2, BORDER);
        g.fill(panelX + sidebarW, panelY + 1, panelX + sidebarW + 1, panelY + panelH - 1, 0x443D352B);

        // Header and compact category tabs.
        g.drawString(font, "SWIFT VISUALS", listX + 2, panelY + 13, TEXT, true);
        g.drawString(font, "CLIENT  /  MODULES", listX + 2, panelY + 28, MUTED, false);
        String[] tabs = tabLabels();
        int tabX = listX + 2;
        int tabY = panelY + 48;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabX + i * 62;
            boolean active = category.ordinal() == i;
            if (active) rounded(g, x - 5, tabY - 5, 54, 19, 6, 0x5536A7FF);
            g.drawString(font, tabs[i], x, tabY, active ? ACCENT : MUTED, active);
        }

        // Left category rail.
        String[] rail = {"S", "H", "P", "⋯"};
        int railY = panelY + 18;
        for (int i = 0; i < rail.length; i++) {
            int y = railY + i * 34;
            boolean active = i < 3 && category.ordinal() == i;
            if (active) rounded(g, panelX + 7, y - 5, 21, 23, 6, 0x5536A7FF);
            g.drawCenteredString(font, rail[i], panelX + 17, y + 2, active ? ACCENT : MUTED);
        }

        // Module cards in a three-column grid, with real module-state switches.
        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = listX + col * (cardW + gap);
                int y = listY + row * (rowH + gap);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + rowH;
                rounded(g, x, y, cardW, rowH, 6, hover ? CARD_HOVER : CARD);
                int switchW = 23, switchH = 13;
                int sx = x + cardW - switchW - 5;
                int sy = y + (rowH - switchH) / 2;
                int textRight = sx - 5;
                g.enableScissor(x + 7, y, textRight, y + rowH);
                g.drawString(font, name, x + 7, y + (rowH - 8) / 2, TEXT, false);
                g.disableScissor();
                rounded(g, sx, sy, switchW, switchH, 7, enabled ? ACCENT : SWITCH_OFF);
                rounded(g, enabled ? sx + 12 : sx + 2, sy + 2, 9, 9, 5, 0xFFF7F7F5);
            }
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (totalRows > rowsVisible()) {
            int trackX = panelX + panelW - 5;
            int trackH = listBottom - listY;
            int thumbH = Math.max(16, trackH * rowsVisible() / totalRows);
            int thumbY = listY + (trackH - thumbH) * scroll / (totalRows - rowsVisible());
            g.fill(trackX, listY, trackX + 2, listBottom, 0x443F382D);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, ACCENT);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();

        String[] tabs = tabLabels();
        int tabX = listX + 2;
        int tabY = panelY + 48;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabX + i * 62;
            if (mx >= x - 5 && mx <= x + 50 && my >= tabY - 6 && my <= tabY + 16) {
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
                int x = listX + col * (cardW + gap);
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
