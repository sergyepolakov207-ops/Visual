package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int PANEL_W = 440, PANEL_H = 430;
    private int panelX, panelY, scroll;
    private VoidModules.Category category = VoidModules.Category.PVP;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() { super(Component.literal("SWIFT VISUALS")); }
    @Override protected void init() {
        panelX = Math.max(4, (width - PANEL_W) / 2);
        panelY = Math.max(4, (height - PANEL_H) / 2);
        rebuild();
    }
    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream().filter(n -> VoidModules.categoryOf(n) == category).forEach(visible::add);
        scroll = Math.min(scroll, Math.max(0, visible.size() - 9));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0xB0000000);
        g.fill(panelX + 3, panelY + 3, panelX + PANEL_W + 3, panelY + PANEL_H + 3, 0x70000000);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF010121D);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 4, 0xFF9B65FF);
        g.fill(panelX, panelY + 4, panelX + 5, panelY + PANEL_H, 0xFF9B65FF);
        g.drawString(font, "SWIFT", panelX + 20, panelY + 17, 0xFFB994FF, false);
        g.drawString(font, "VISUALS", panelX + 65, panelY + 17, 0xFFFFFFFF, false);
        g.drawString(font, "CLIENT  •  1.21.11", panelX + 20, panelY + 34, 0xFF777F98, false);
        g.drawString(font, "RIGHT SHIFT  /  CLOSE", panelX + PANEL_W - 150, panelY + 21, 0xFF85899D, false);
        g.fill(panelX + 16, panelY + 57, panelX + PANEL_W - 16, panelY + 58, 0xFF303344);

        String[] tabs = {"VISUALS", "HUD", "PVP"};
        int tabY = panelY + 72;
        for (int i = 0; i < tabs.length; i++) {
            int x = panelX + 18 + i * 136;
            boolean active = category.ordinal() == i;
            g.fill(x, tabY, x + 128, tabY + 32, active ? 0xFF7042C5 : 0xFF202434);
            g.drawCenteredString(font, tabs[i], x + 64, tabY + 12, active ? 0xFFFFFFFF : 0xFF9BA1B5);
        }

        g.drawString(font, category.name() + " MODULES", panelX + 20, panelY + 119, 0xFFB9A1E8, false);
        int y = panelY + 140;
        int shown = 0;
        for (int i = scroll; i < visible.size() && shown < 9; i++, shown++) {
            String name = visible.get(i);
            boolean enabled = VoidModules.isEnabled(name);
            boolean hover = mx >= panelX + 17 && mx <= panelX + PANEL_W - 17 && my >= y && my < y + 32;
            int left = panelX + 17, right = panelX + PANEL_W - 17;
            g.fill(left, y, right, y + 32, hover ? 0xFF292D40 : 0xFF1B1F2D);
            g.fill(left, y, left + 3, y + 32, enabled ? 0xFF9B65FF : 0xFF34394A);
            g.drawString(font, name, left + 14, y + 11, 0xFFE8E9F2, false);
            int tx = right - 48;
            g.fill(tx, y + 8, tx + 34, y + 24, enabled ? 0xFF7547E8 : 0xFF42485A);
            g.fill(enabled ? tx + 19 : tx + 3, y + 10, enabled ? tx + 31 : tx + 15, y + 22, 0xFFFFFFFF);
            y += 36;
        }
        if (visible.isEmpty()) g.drawString(font, "No modules here yet", panelX + 22, y + 12, 0xFF9BA1B5, false);
        g.fill(panelX + 16, panelY + PANEL_H - 43, panelX + PANEL_W - 16, panelY + PANEL_H - 42, 0xFF303344);
        g.drawString(font, "SWIFT VISUALS", panelX + 20, panelY + PANEL_H - 28, 0xFF8D92A5, false);
        g.drawString(font, "SCROLL  •  TOGGLE  •  PLAY FAIR", panelX + PANEL_W - 190, panelY + PANEL_H - 28, 0xFF777D91, false);
        super.render(g, mx, my, partialTick);
    }
    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        int tabY = panelY + 72;
        if (my >= tabY && my <= tabY + 32 && mx >= panelX + 18 && mx < panelX + 426) {
            int idx = Math.min(2, (int)((mx - panelX - 18) / 136));
            category = VoidModules.Category.values()[idx]; scroll = 0; rebuild(); return true;
        }
        int y = panelY + 140;
        for (int i = scroll; i < visible.size() && i < scroll + 9; i++, y += 36) {
            if (mx >= panelX + 17 && mx <= panelX + PANEL_W - 17 && my >= y && my < y + 32) {
                VoidModules.toggle(visible.get(i)); return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(Math.max(0, visible.size() - 9), scroll - (int)Math.signum(vertical)));
        return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
