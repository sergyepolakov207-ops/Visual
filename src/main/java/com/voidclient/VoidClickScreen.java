package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int PANEL_W = 520, PANEL_H = 448;
    private static final int ROW_H = 42, COL_GAP = 10, CARD_W = 234;
    private int panelX, panelY, scroll;
    private VoidModules.Category category = VoidModules.Category.PVP;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() { super(Component.literal("SWIFT VISUALS")); }

    @Override protected void init() {
        panelX = Math.max(6, (width - PANEL_W) / 2);
        panelY = Math.max(6, (height - PANEL_H) / 2);
        rebuild();
    }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream()
            .filter(n -> VoidModules.categoryOf(n) == category)
            .forEach(visible::add);
        scroll = Math.min(scroll, Math.max(0, (visible.size() + 1) / 2 - 6));
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0xB9000000);
        g.fill(panelX + 4, panelY + 5, panelX + PANEL_W + 4, panelY + PANEL_H + 5, 0x65000000);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF00E101A);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, 0xFFAA78FF);
        g.fill(panelX, panelY + 3, panelX + 3, panelY + PANEL_H, 0xFFAA78FF);

        // Header
        g.fill(panelX + 14, panelY + 14, panelX + 42, panelY + 42, 0xFF7546C8);
        g.drawCenteredString(font, "S", panelX + 28, panelY + 23, 0xFFFFFFFF);
        g.drawString(font, "SWIFT", panelX + 52, panelY + 15, 0xFFB994FF, true);
        g.drawString(font, "VISUALS", panelX + 91, panelY + 15, 0xFFFFFFFF, true);
        g.drawString(font, "CLIENT  /  1.21.11  •  FABRIC", panelX + 52, panelY + 30, 0xFF858BA2, false);
        g.drawString(font, "RIGHT SHIFT  •  CLOSE", panelX + PANEL_W - 151, panelY + 24, 0xFF858BA2, false);
        g.fill(panelX + 14, panelY + 53, panelX + PANEL_W - 14, panelY + 54, 0xFF292D3D);

        // Tabs
        String[] tabs = {"VISUALS", "HUD", "PVP"};
        int tabY = panelY + 66;
        for (int i = 0; i < tabs.length; i++) {
            int x = panelX + 14 + i * 164;
            boolean active = category.ordinal() == i;
            g.fill(x, tabY, x + 156, tabY + 31, active ? 0xFF6840B4 : 0xFF191D2A);
            if (active) g.fill(x, tabY + 28, x + 156, tabY + 31, 0xFFB58AFF);
            g.drawCenteredString(font, tabs[i], x + 78, tabY + 11, active ? 0xFFFFFFFF : 0xFF9BA1B5);
        }

        g.drawString(font, category.name() + "  /  MODULES", panelX + 17, panelY + 111, 0xFFB99BEB, true);
        g.drawString(font, enabledCount() + " ENABLED", panelX + PANEL_W - 91, panelY + 111, 0xFF8D92A5, false);

        int firstRowY = panelY + 132;
        int rows = 6;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 2; col++) {
                int index = (scroll + row) * 2 + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = panelX + 14 + col * (CARD_W + COL_GAP);
                int y = firstRowY + row * ROW_H;
                boolean hover = mx >= x && mx <= x + CARD_W && my >= y && my <= y + 36;
                g.fill(x, y, x + CARD_W, y + 36, hover ? 0xFF282D40 : 0xFF191D2A);
                g.fill(x, y, x + 3, y + 36, enabled ? 0xFFAA78FF : 0xFF34394A);
                g.drawString(font, name, x + 12, y + 7, 0xFFE9EAF2, true);
                g.drawString(font, enabled ? "ACTIVE" : "DISABLED", x + 12, y + 21,
                    enabled ? 0xFF9E83D8 : 0xFF70778D, false);
                int tx = x + CARD_W - 43;
                g.fill(tx, y + 10, tx + 31, y + 26, enabled ? 0xFF7044BD : 0xFF3A4052);
                g.fill(enabled ? tx + 17 : tx + 3, y + 12, enabled ? tx + 29 : tx + 15, y + 24, 0xFFFFFFFF);
            }
        }

        int totalRows = (visible.size() + 1) / 2;
        if (totalRows > rows) {
            int trackX = panelX + PANEL_W - 7;
            g.fill(trackX, firstRowY, trackX + 2, firstRowY + rows * ROW_H - 6, 0xFF292D3D);
            int thumbH = Math.max(18, (rows * ROW_H - 6) * rows / totalRows);
            int thumbY = firstRowY + (firstRowY + rows * ROW_H - 6 - thumbH) * scroll / (totalRows - rows);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFFAA78FF);
        }

        int footerY = panelY + PANEL_H - 28;
        g.fill(panelX + 14, footerY - 9, panelX + PANEL_W - 14, footerY - 8, 0xFF292D3D);
        g.drawString(font, "SWIFT VISUALS", panelX + 17, footerY, 0xFF8E94A9, false);
        g.drawString(font, "SCROLL  •  CLICK TO TOGGLE  •  FAIR PLAY", panelX + PANEL_W - 243, footerY,
            0xFF777F94, false);
        super.render(g, mx, my, partialTick);
    }

    private int enabledCount() {
        int count = 0;
        for (String name : visible) if (VoidModules.isEnabled(name)) count++;
        return count;
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        int tabY = panelY + 66;
        if (my >= tabY && my <= tabY + 31 && mx >= panelX + 14 && mx < panelX + 506) {
            int idx = Math.min(2, (int)((mx - panelX - 14) / 164));
            category = VoidModules.Category.values()[idx];
            scroll = 0;
            rebuild();
            return true;
        }
        int firstRowY = panelY + 132;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 2; col++) {
                int index = (scroll + row) * 2 + col;
                if (index >= visible.size()) continue;
                int x = panelX + 14 + col * (CARD_W + COL_GAP);
                int y = firstRowY + row * ROW_H;
                if (mx >= x && mx <= x + CARD_W && my >= y && my <= y + 36) {
                    VoidModules.toggle(visible.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int max = Math.max(0, (visible.size() + 1) / 2 - 6);
        scroll = Math.max(0, Math.min(max, scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
