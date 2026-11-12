package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int BG = 0xFF171717;
    private static final int PANEL = 0xFF242424;
    private static final int CARD = 0xFF303030;
    private static final int CARD_HOVER = 0xFF383838;
    private static final int TEXT = 0xFFF0F0F0;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int BLUE = 0xFF579BFF;

    private int panelX, panelY, panelW, panelH;
    private int scroll;
    private int columns, cardW, cardH, gap, listY, listBottom;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(760, Math.max(290, width - 24));
        panelH = Math.min(500, Math.max(250, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        columns = panelW >= 520 ? 2 : 1;
        gap = 10;
        cardH = 54;
        cardW = (panelW - 36 - (columns - 1) * gap) / columns;
        listY = panelY + 112;
        listBottom = panelY + panelH - 38;
        rebuild();
    }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream()
            .filter(name -> VoidModules.categoryOf(name) == category)
            .forEach(visible::add);
        int rows = Math.max(1, (listBottom - listY + gap) / (cardH + gap));
        int total = (visible.size() + columns - 1) / columns;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - rows)));
    }

    private int rowsVisible() {
        return Math.max(1, (listBottom - listY + gap) / (cardH + gap));
    }

    private String tabName(int index) {
        return switch (index) {
            case 0 -> "VISUALS";
            case 1 -> "HUD";
            default -> "UTILITIES";
        };
    }

    private String description(String name) {
        return switch (name) {
            case "Fullbright" -> "Brighten dark areas";
            case "No View Bobbing" -> "Disable camera movement";
            case "Crosshair Dot" -> "Add a small center dot";
            case "FPS Counter" -> "Display current frame rate";
            case "Coordinates" -> "Show your position";
            case "Inventory HUD" -> "Display inventory items";
            case "Keystrokes" -> "Show pressed movement keys";
            case "Sprint Status" -> "Display sprint state";
            case "Direction HUD" -> "Show facing direction";
            case "Health HUD" -> "Display your health";
            case "Hunger HUD" -> "Display food level";
            case "Armor HUD" -> "Display armor points";
            case "Ping HUD" -> "Show server latency";
            case "Biome HUD" -> "Display current biome";
            case "Item Durability HUD" -> "Track held item durability";
            case "Attack Cooldown" -> "Show attack readiness";
            case "Auto Sprint" -> "Sprint while moving forward";
            case "Low Health Alert" -> "Warn at low health";
            default -> "Client-side option";
        };
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

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0x99000000);
        rounded(g, panelX, panelY, panelW, panelH, 9, BG);

        // Compact Pulse-inspired header
        rounded(g, panelX + 14, panelY + 13, 28, 28, 7, 0xFF343434);
        g.drawCenteredString(font, "S", panelX + 28, panelY + 23, BLUE);
        g.drawString(font, "SWIFT VISUALS", panelX + 51, panelY + 14, TEXT, true);
        g.drawString(font, "Client settings", panelX + 51, panelY + 28, MUTED, false);

        // Top category tabs
        String[] tabs = {"VISUALS", "HUD", "UTILITIES"};
        int tabX = panelX + panelW - (panelW >= 520 ? 282 : 246);
        int tabY = panelY + 13;
        int tabW = panelW >= 520 ? 86 : 74;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabX + i * (tabW + 5);
            boolean active = category.ordinal() == i;
            rounded(g, x, tabY, tabW, 28, 6, active ? 0xFF414141 : 0xFF252525);
            g.drawCenteredString(font, tabs[i], x + tabW / 2, tabY + 10, active ? TEXT : MUTED);
        }

        // Section title and divider
        String section = tabName(category.ordinal());
        g.drawString(font, section, panelX + 18, panelY + 73, TEXT, true);
        String count = visible.size() + " MODULES";
        g.drawString(font, count, panelX + panelW - 18 - font.width(count), panelY + 74, MUTED, false);
        g.fill(panelX + 18, panelY + 91, panelX + panelW - 18, panelY + 92, 0xFF3A3A3A);

        // Two-column module rows
        for (int row = 0; row < rowsVisible(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = panelX + 18 + col * (cardW + gap);
                int y = listY + row * (cardH + gap);
                boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + cardH;
                rounded(g, x, y, cardW, cardH, 7, hover ? CARD_HOVER : CARD);
                g.drawString(font, name, x + 13, y + 11, TEXT, true);
                g.drawString(font, description(name), x + 13, y + 29, MUTED, false);

                int sw = 42, sh = 22;
                int sx = x + cardW - sw - 12;
                int sy = y + (cardH - sh) / 2;
                rounded(g, sx, sy, sw, sh, 11, enabled ? BLUE : 0xFF555555);
                rounded(g, enabled ? sx + 22 : sx + 3, sy + 3, 16, 16, 8, 0xFFF4F4F4);
            }
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (totalRows > rowsVisible()) {
            int trackX = panelX + panelW - 7;
            int trackH = listBottom - listY;
            g.fill(trackX, listY, trackX + 2, listBottom, 0xFF3A3A3A);
            int thumbH = Math.max(18, trackH * rowsVisible() / totalRows);
            int thumbY = listY + (trackH - thumbH) * scroll / (totalRows - rowsVisible());
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, BLUE);
        }

        g.fill(panelX + 18, panelY + panelH - 27, panelX + panelW - 18, panelY + panelH - 26, 0xFF3A3A3A);
        g.drawString(font, "SWIFT VISUALS", panelX + 18, panelY + panelH - 17, MUTED, false);
        String active = enabledCount() + " ENABLED";
        g.drawString(font, active, panelX + panelW - 18 - font.width(active), panelY + panelH - 17, BLUE, true);
        super.render(g, mx, my, partialTick);
    }

    private int enabledCount() {
        int count = 0;
        for (boolean enabled : VoidModules.all().values()) if (enabled) count++;
        return count;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();

        String[] tabs = {"VISUALS", "HUD", "UTILITIES"};
        int tabX = panelX + panelW - (panelW >= 520 ? 282 : 246);
        int tabW = panelW >= 520 ? 86 : 74;
        for (int i = 0; i < tabs.length; i++) {
            int x = tabX + i * (tabW + 5);
            if (mx >= x && mx <= x + tabW && my >= panelY + 13 && my <= panelY + 41) {
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
                int x = panelX + 18 + col * (cardW + gap);
                int y = listY + row * (cardH + gap);
                if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) {
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
