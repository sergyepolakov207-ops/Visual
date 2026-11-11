package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int ACCENT = 0xFFFF8733;
    private static final int TEXT = 0xFFF4F1ED;
    private static final int MUTED = 0xFF8D8986;
    private int panelX, panelY, panelW, panelH, scroll, tick;
    private int sideW, contentX, contentW, listY, cardH, rowGap, columns;
    private VoidModules.Category category = VoidModules.Category.VISUALS;
    private final List<String> visible = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(720, Math.max(270, width - 28));
        panelH = Math.min(500, Math.max(250, height - 28));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sideW = 0;
        contentX = panelX + sideW;
        contentW = panelW - sideW;
        columns = contentW >= 570 ? 2 : 1;
        cardH = panelW < 400 ? 58 : 64;
        rowGap = 7;
        listY = panelY + 145;
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

    private void drawParticles(GuiGraphics g) {
        // Subtle ambient particles behind the window; deterministic and allocation-free.
        for (int i = 0; i < 30; i++) {
            double phase = i * 2.399963229728653;
            double speed = 0.22 + (i % 5) * 0.055;
            int x = Math.floorMod((int) (i * 97 + Math.sin(tick * speed * 0.035 + phase) * 24), Math.max(1, width));
            int y = Math.floorMod((int) (i * 53 - tick * speed + Math.cos(phase) * 18), Math.max(1, height));
            if (x > panelX - 3 && x < panelX + panelW + 3 && y > panelY - 3 && y < panelY + panelH + 3) continue;
            int color = (i % 4 == 0) ? 0x887F54B8 : 0x557D68A8;
            int size = (i % 7 == 0) ? 3 : 2;
            g.fill(x, y, x + size, y + size, color);
        }
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
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
            
            case "FPS Counter" -> "Show your current frame rate";
            case "Coordinates" -> "Display your world position";
            case "Inventory HUD" -> "Show your hotbar items on screen";
            case "Keystrokes" -> "Display movement key presses";
            case "Sprint Status" -> "Show sprint state";
            case "Direction HUD" -> "Display the direction you face";
            case "Health HUD" -> "Show current health";
            case "Hunger HUD" -> "Show food level";
            case "Armor HUD" -> "Show armor points";
            case "Ping HUD" -> "Display server latency";
            case "Biome HUD" -> "Show the current biome";
            case "Item Durability HUD" -> "Track held item durability";
            
            
            
            
            
            case "Auto Sprint" -> "Sprint while moving forward";
            case "Attack Cooldown" -> "Show attack readiness";
            case "Low Health Alert" -> "Warn when health is low";
            
            default -> "Client-side option";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        // Fully opaque backdrop: no other mod screens can show through.
        panel(g, 0, 0, width, height, 0xFF080910);
        panel(g, 0, 0, width, height / 2, 0xFF101010);
        panel(g, panelX + 5, panelY + 7, panelW, panelH, 0x90000000);
        panel(g, panelX, panelY, panelW, panelH, 0xFF111111);
        panel(g, panelX, panelY, panelW, 3, ACCENT);
        panel(g, panelX + 1, panelY + 3, panelW - 2, 1, 0xFF3B2A20);

        // Brand header
        panel(g, panelX + 17, panelY + 17, 34, 34, 0xFF33251C);
        panel(g, panelX + 17, panelY + 17, 3, 34, ACCENT);
        g.drawCenteredString(font, "S", panelX + 34, panelY + 27, TEXT);
        g.drawString(font, "SWIFT", panelX + 60, panelY + 18, TEXT, false);
        g.drawString(font, "VISUAL CLIENT", panelX + 60, panelY + 31, MUTED, false);
        String live = "● ONLINE";
        int liveX = panelX + panelW - pad() - font.width("SWIFT CLIENT  /  1.21.11") - font.width(live) - 18;
        g.drawString(font, live, liveX, panelY + 28, 0xFFFFA45F, false);
        g.drawString(font, "NEON GLASS  /  1.21.11", panelX + panelW - pad() - font.width("NEON GLASS  /  1.21.11"), panelY + 28, 0xFF777187, false);

        // Horizontal navigation inspired by the reference menu.
        String[] names = {"VISUALS", "HUD", "PVP"};
        int tabGap = 8;
        int tabW = (contentW - pad() * 2 - tabGap * 2) / 3;
        for (int i = 0; i < names.length; i++) {
            int x = panelX + pad() + i * (tabW + tabGap);
            int y = panelY + 68;
            boolean active = category.ordinal() == i;
            rounded(g, x, y, tabW, 31, 6, active ? 0xFF3A281D : 0xFF1B1B1B);
            if (active) panel(g, x + 9, y + 29, tabW - 18, 2, ACCENT);
            g.drawCenteredString(font, names[i], x + tabW / 2, y + 11, active ? TEXT : MUTED);
            String amount = String.format("%02d", countFor(i));
            g.drawString(font, amount, x + tabW - font.width(amount) - 8, y + 11, active ? ACCENT : 0xFF66615D, false);
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
                rounded(g, x, y, cw, cardH, 7, hover ? 0xFF29231F : 0xFF1B1B1B);
                panel(g, x, y, cw, 1, enabled ? ACCENT : 0xFF37312C);
                panel(g, x, y + 1, 3, cardH - 2, enabled ? ACCENT : 0xFF4B4038);
                g.drawString(font, name, x + 13, y + 11, TEXT, true);
                g.drawString(font, description(name), x + 13, y + 27, MUTED, false);
                String state = enabled ? "ON" : "OFF";
                g.drawString(font, state, x + cw - 59 - font.width(state), y + 27, enabled ? ACCENT : MUTED, true);
                int sw = 38, sh = 20;
                int sx = x + cw - sw - 12, sy = y + (cardH - sh) / 2;
                rounded(g, sx, sy, sw, sh, 10, enabled ? ACCENT : 0xFF414141);
                int knobX = enabled ? sx + 21 : sx + 3;
                rounded(g, knobX, sy + 3, 14, 14, 7, 0xFFF5F2EF);
            }
        }

        int totalRows = (visible.size() + columns - 1) / columns;
        if (category == VoidModules.Category.VISUALS && totalRows <= 2) {
            drawOverview(g, listY + totalRows * (cardH + rowGap) + 10);
        }
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

    private void drawOverview(GuiGraphics g, int y) {
        int left = contentX + pad();
        int gap = 10;
        int w = (contentW - pad() * 2 - gap) / 2;
        int bottom = panelY + panelH - 48;
        if (y + 100 > bottom) return;

        g.drawString(font, "LIVE OVERVIEW", left, y, 0xFFFFA45F, true);
        g.drawString(font, "Your client at a glance", left, y + 13, MUTED, false);
        int top = y + 31;
        panel(g, left, top, w, 48, 0xFF191A27);
        panel(g, left, top, 2, 48, ACCENT);
        g.drawString(font, "ACTIVE MODULES", left + 11, top + 8, MUTED, true);
        g.drawString(font, Integer.toString(enabledCount()), left + 11, top + 22, TEXT, true);
        g.drawString(font, "of " + VoidModules.all().size() + " available", left + 34, top + 25, MUTED, false);

        int right = left + w + gap;
        panel(g, right, top, w, 48, 0xFF191A27);
        panel(g, right, top, 2, 48, 0xFF7A69A8);
        g.drawString(font, "CURRENT SECTION", right + 11, top + 8, MUTED, true);
        g.drawString(font, category.name(), right + 11, top + 23, TEXT, true);

        int guideY = top + 56;
        panel(g, left, guideY, contentW - pad() * 2, 39, 0xFF151620);
        panel(g, left, guideY, 2, 39, 0xFF66518A);
        g.drawString(font, "QUICK CONTROLS", left + 10, guideY + 6, 0xFFBDA2ED, true);
        g.drawString(font, "Tap a card to toggle  •  Swipe to scroll  •  RIGHT SHIFT to close",
            left + 10, guideY + 21, MUTED, false);
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

        int tabGap = 8;
        int tabW = (contentW - pad() * 2 - tabGap * 2) / 3;
        for (int i = 0; i < 3; i++) {
            int x = panelX + pad() + i * (tabW + tabGap);
            if (mx >= x && mx <= x + tabW && my >= panelY + 68 && my <= panelY + 99) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                rebuild();
                return true;
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
