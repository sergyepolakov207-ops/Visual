package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 390;
    private int panelX, panelY, scroll;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() { super(Component.literal("SWIFT VISUALS")); }

    @Override protected void init() {
        panelX = Math.max(4, (width - PANEL_W) / 2);
        panelY = Math.max(4, (height - PANEL_H) / 2);
        scroll = 0;
        rebuild();
    }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream()
            .filter(n -> VoidModules.categoryOf(n) == category)
            .forEach(visible::add);
        scroll = Math.min(scroll, Math.max(0, visible.size() - 1));
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF0121420);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, 0xFF8B5CF6);
        g.drawString(font, "SWIFT VISUALS", panelX + 14, panelY + 13, 0xFFFFFFFF, false);
        g.drawString(font, "RIGHT SHIFT  -  CLOSE", panelX + 190, panelY + 14, 0xFF85899D, false);
        g.fill(panelX + 10, panelY + 34, panelX + PANEL_W - 10, panelY + 35, 0xFF303344);

        String[] tabs = {"VISUALS", "HUD", "PVP"};
        int tabY = panelY + 46;
        for (int i = 0; i < tabs.length; i++) {
            int x = panelX + 12 + i * 103;
            boolean selected = category.ordinal() == i;
            g.fill(x, tabY, x + 98, tabY + 25, selected ? 0xFF5835B5 : 0xFF202333);
            g.drawCenteredString(font, tabs[i], x + 49, tabY + 8, selected ? 0xFFFFFFFF : 0xFFAAAFC2);
        }

        int y = panelY + 82;
        int shown = 0;
        for (int i = scroll; i < visible.size() && shown < 10; i++, shown++) {
            String name = visible.get(i);
            boolean enabled = VoidModules.isEnabled(name);
            boolean hovered = mouseX >= panelX + 12 && mouseX <= panelX + PANEL_W - 12
                && mouseY >= y && mouseY < y + 27;
            g.fill(panelX + 12, y, panelX + PANEL_W - 12, y + 27, hovered ? 0xFF292D40 : 0xFF1C2030);
            g.drawString(font, name, panelX + 22, y + 9, 0xFFE6E8F0, false);
            int tx = panelX + PANEL_W - 55;
            g.fill(tx, y + 7, tx + 31, y + 20, enabled ? 0xFF7547E8 : 0xFF454A5C);
            g.fill(enabled ? tx + 18 : tx + 2, y + 9, enabled ? tx + 29 : tx + 13, y + 18, 0xFFFFFFFF);
            y += 32;
        }
        if (visible.isEmpty()) g.drawString(font, "No modules in this category", panelX + 20, y + 8, 0xFFAAAFC2, false);
        g.drawString(font, "Scroll to see more  •  Settings save in this session",
            panelX + 14, panelY + PANEL_H - 22, 0xFF777D91, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        int tabY = panelY + 46;
        if (my >= tabY && my <= tabY + 25 && mx >= panelX + 12 && mx < panelX + 321) {
            int index = Math.min(2, (int) ((mx - (panelX + 12)) / 103));
            category = VoidModules.Category.values()[index];
            scroll = 0;
            rebuild();
            return true;
        }
        int y = panelY + 82;
        for (int i = scroll; i < visible.size() && i < scroll + 10; i++, y += 32) {
            if (mx >= panelX + 12 && mx <= panelX + PANEL_W - 12 && my >= y && my < y + 27) {
                VoidModules.toggle(visible.get(i));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(Math.max(0, visible.size() - 10), scroll - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
