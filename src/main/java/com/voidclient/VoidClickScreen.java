package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int ACCENT = 0xFFB58CFF;
    private static final int TEXT = 0xFFF2EEFA;
    private static final int MUTED = 0xFF938BA5;
    private int panelX, panelY, panelW, panelH, scroll, tick;
    private int sideW, contentX, contentW, listY, cardH, rowGap, columns;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(940, Math.max(280, width - 20));
        panelH = Math.min(590, Math.max(230, height - 20));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sideW = panelW >= 560 ? 142 : 0;
        contentX = panelX + sideW;
        contentW = panelW - sideW;
        columns = contentW >= 570 ? 2 : 1;
        cardH = panelW < 400 ? 48 : 54;
        rowGap = 10;
        listY = panelY + (sideW == 0 ? 126 : 145);
        rebuild();
    }

    private int pad() { return panelW < 400 ? 12 : 22; }
    private int cardWidth() { return (contentW - pad() * 2 - (columns - 1) * 12) / columns; }
    private int visibleRows() { return Math.max(1, (panelY + panelH - 58 - listY) / (cardH + rowGap)); }

    private void rebuild() {
        visible.clear();
        VoidModules.all().keySet().stream()
            .filter(name -> VoidModules.categoryOf(name) == category)
            .forEach(visible::add);
        int total = (visible.size() + columns - 1) / columns;
        scroll = Math.max(0, Math.min(scroll, total - visibleRows()));
    }

    @Override
    public void tick() {
        super.tick();
        tick++;
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private String categoryTitle() {
        return switch (category) {
            case VISUALS -> "VISUAL MODULES";
            case HUD -> "HUD ELEMENTS";
            case PVP -> "PVP UTILITIES";
        };
    }

    private String categoryHint() {
        return switch (category) {
            case VISUALS -> "Customize the look and feel of your game";
            case HUD -> "Choose the information displayed on screen";
            case PVP -> "Lightweight tools for everyday gameplay";
        };
    }

    private String description(String name) {
        return switch (name) {
            case "Fullbright" -> "Brighten dark areas";
            case "No View Bobbing" -> "Keep the camera steady";
            case "Crosshair Dot" -> "Add a precise center marker";
            case "Menu Particles" -> "Ambient particles in this menu";
            case "FPS Counter" -> "Show your current frame rate";
            case "Coordinates" -> "Display your world position";
            case "Clock" -> "Show your local time";
            case "Keystrokes" -> "Display movement key presses";
            case "Sprint Status" -> "Show sprint state";
            case "Direction HUD" -> "Display the direction you face";
            case "Health HUD" -> "Show current health";
            case "Hunger HUD" -> "Show food level";
            case "Armor HUD" -> "Show armor points";
            case "Ping HUD" -> "Display server latency";
            case "Biome HUD" -> "Show the current biome";
            case "Item Durability HUD" -> "Track held item durability";
            case "Selected Item HUD" -> "Show your held item";
            case "Offhand Item HUD" -> "Show your offhand item";
            case "Movement Speed HUD" -> "Display movement speed";
            case "World Time HUD" -> "Show the in-game time";
            case "Compact HUD" -> "Use smaller HUD cards";
            case "Auto Sprint" -> "Sprint while moving forward";
            case "Attack Cooldown" -> "Show attack readiness";
            case "Low Health Alert" -> "Warn when health is low";
            case "Reduced Effects" -> "Reduce menu visual effects";
            default -> "Client-side option";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        // Fully opaque backdrop: no other mod screens can show through.
        panel(g, 0, 0, width, height, 0xFF080910);
        panel(g, 0, 0, width, height / 2, 0xFF0B0B15);
        panel(g, panelX + 5, panelY + 7, panelW, panelH, 0x90000000);
        panel(g, panelX, panelY, panelW, panelH, 0xFF10111B);
        panel(g, panelX, panelY, panelW, 3, ACCENT);
        panel(g, panelX + 1, panelY + 3, panelW - 2, 1, 0xFF38304B);

        // Brand header
        panel(g, panelX + 17, panelY + 17, 34, 34, 0xFF29203C);
        panel(g, panelX + 17, panelY + 17, 3, 34, ACCENT);
        g.drawCenteredString(font, "S", panelX + 34, panelY + 27, TEXT);
        g.drawString(font, "SWIFT", panelX + 60, panelY + 18, TEXT, true);
        g.drawString(font, "VISUAL CLIENT", panelX + 60, panelY + 31, MUTED, false);
        g.drawString(font, "NEON GLASS  /  1.21.11", panelX + panelW - pad() - font.width("NEON GLASS  /  1.21.11"), panelY + 28, 0xFF777187, false);

        // Sidebar navigation on wide screens; compact navigation on phones.
        if (sideW > 0) {
            panel(g, panelX, panelY + 62, sideW, panelH - 62, 0xFF0C0D15);
            panel(g, panelX + sideW - 1, panelY + 62, 1, panelH - 62, 0xFF292536);
            g.drawString(font, "WORKSPACE", panelX + 17, panelY + 82, 0xFF716A80, true);
            String[] names = {"VISUALS", "HUD", "PVP"};
            for (int i = 0; i < names.length; i++) {
                int y = panelY + 105 + i * 43;
                boolean active = category.ordinal() == i;
                if (active) {
                    panel(g, panelX + 10, y, sideW - 20, 34, 0xFF29213A);
                    panel(g, panelX + 10, y, 3, 34, ACCENT);
                }
                g.drawString(font, names[i], panelX + 25, y + 13, active ? TEXT : MUTED, active);
                g.drawString(font, String.format("%02d", countFor(i)), panelX + sideW - 34, y + 13, active ? ACCENT : 0xFF625C70, false);
            }
            panel(g, panelX + 16, panelY + panelH - 68, sideW - 32, 1, 0xFF292536);
            g.drawString(font, "RIGHT SHIFT", panelX + 17, panelY + panelH - 53, 0xFFB7AEC8, true);
            g.drawString(font, "TOGGLE MENU", panelX + 17, panelY + panelH - 39, 0xFF716A80, false);
        } else {
            String[] names = {"VISUALS", "HUD", "PVP"};
            int tabW = (contentW - pad() * 2 - 12) / 3;
            for (int i = 0; i < names.length; i++) {
                int x = panelX + pad() + i * (tabW + 6);
                boolean active = category.ordinal() == i;
                panel(g, x, panelY + 67, tabW, 30, active ? 0xFF302441 : 0xFF191A26);
                if (active) panel(g, x, panelY + 94, tabW, 2, ACCENT);
                g.drawCenteredString(font, names[i], x + tabW / 2, panelY + 78, active ? TEXT : MUTED);
            }
        }

        // Main content heading
        int left = contentX + pad();
        int headingY = sideW == 0 ? panelY + 111 : panelY + 82;
        g.drawString(font, categoryTitle(), left, headingY, TEXT, true);
        g.drawString(font, categoryHint(), left, headingY + 14, MUTED, false);
        String count = visible.size() + " OPTIONS";
        g.drawString(font, count, contentX + contentW - pad() - font.width(count), headingY + 3, ACCENT, true);

        // Module cards
        int cw = cardWidth();
        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                String name = visible.get(index);
                boolean enabled = VoidModules.isEnabled(name);
                int x = contentX + pad() + col * (cw + 12);
                int y = listY + row * (cardH + rowGap);
                boolean hover = mx >= x && mx <= x + cw && my >= y && my <= y + cardH;
                panel(g, x, y, cw, cardH, hover ? 0xFF242337 : 0xFF191A27);
                panel(g, x, y, cw, 1, enabled ? 0xFF7753A8 : 0xFF343143);
                panel(g, x, y + 1, 3, cardH - 2, enabled ? ACCENT : 0xFF444052);
                g.drawString(font, name, x + 13, y + 12, TEXT, enabled);
                g.drawString(font, description(name), x + 13, y + 28, 0xFF8D879E, false);

                int sw = 35, sh = 19;
                int sx = x + cw - sw - 12, sy = y + (cardH - sh) / 2;
                panel(g, sx, sy, sw, sh, enabled ? 0xFF8051C8 : 0xFF353543);
                int knobX = enabled ? sx + 19 : sx + 3;
                panel(g, knobX, sy + 3, 13, 13, enabled ? 0xFFFFFFFF : 0xFF898696);
                if (enabled) panel(g, sx + 2, sy + 2, 2, sh - 4, 0xFFB995F2);
            }
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (totalRows > rows) {
            int trackX = contentX + contentW - 5;
            int trackY = listY;
            int trackH = rows * (cardH + rowGap) - rowGap;
            panel(g, trackX, trackY, 2, trackH, 0xFF292737);
            int thumbH = Math.max(20, trackH * rows / totalRows);
            int thumbY = trackY + (trackH - thumbH) * scroll / (totalRows - rows);
            panel(g, trackX, thumbY, 2, thumbH, ACCENT);
        }

        // Footer
        panel(g, contentX + pad(), panelY + panelH - 37, contentW - pad() * 2, 1, 0xFF292536);
        g.drawString(font, "SWIFT VISUALS", contentX + pad(), panelY + panelH - 24, 0xFF777187, false);
        String status = enabledCount() + " ENABLED";
        g.drawString(font, status, contentX + contentW - pad() - font.width(status), panelY + panelH - 24, 0xFFB99AEF, true);
        super.render(g, mx, my, partialTick);
    }

    private int countFor(int categoryIndex) {
        int count = 0;
        for (String name : VoidModules.all().keySet())
            if (VoidModules.categoryOf(name).ordinal() == categoryIndex) count++;
        return count;
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

        if (sideW > 0) {
            for (int i = 0; i < 3; i++) {
                int y = panelY + 105 + i * 43;
                if (mx >= panelX + 10 && mx <= panelX + sideW - 10 && my >= y && my <= y + 34) {
                    category = VoidModules.Category.values()[i];
                    scroll = 0;
                    rebuild();
                    return true;
                }
            }
        } else {
            int tabW = (contentW - pad() * 2 - 12) / 3;
            for (int i = 0; i < 3; i++) {
                int x = panelX + pad() + i * (tabW + 6);
                if (mx >= x && mx <= x + tabW && my >= panelY + 67 && my <= panelY + 97) {
                    category = VoidModules.Category.values()[i];
                    scroll = 0;
                    rebuild();
                    return true;
                }
            }
        }

        int cw = cardWidth();
        for (int row = 0; row < visibleRows(); row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= visible.size()) continue;
                int x = contentX + pad() + col * (cw + 12);
                int y = listY + row * (cardH + rowGap);
                if (mx >= x && mx <= x + cw && my >= y && my <= y + cardH) {
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
        int max = Math.max(0, totalRows - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
