package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int BG = 0xF707080A;
    private static final int PANEL = 0xF70D0F12;
    private static final int CARD = 0xF7111317;
    private static final int CARD_HOVER = 0xF7191C21;
    private static final int LINE = 0xFF24272D;
    private static final int WHITE = 0xFFF1F2F4;
    private static final int MUTED = 0xFF777D87;
    private static final int SOFT = 0xFFB3B7BF;
    private static final int ACCENT = 0xFFE5E7EB;

    private int x, y, w, h, sideW, contentX, contentW, topY, listY, listBottom;
    private final int rowH = 46;
    private int scroll;
    private String search = "", selected = "";
    private boolean searchFocused;
    private VoidModules.Category category = VoidModules.Category.RENDER;
    private final List<String> modules = new ArrayList<>();
    private final Map<String, Float> toggleProgress = new HashMap<>();
    private final Map<String, Float> hoverProgress = new HashMap<>();
    private long lastRenderNanos = System.nanoTime();
    private float intro = 0F;

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    private Component text(String value) {
        return Component.literal(value);
    }

    private void label(GuiGraphics g, String value, int px, int py, int color) {
        g.drawString(font, text(value), px, py, color, false);
    }

    private void centered(GuiGraphics g, String value, int px, int py, int color) {
        g.drawCenteredString(font, text(value), px, py, color);
    }

    private int textWidth(String value) {
        return font.width(text(value));
    }

    @Override
    protected void init() {
        w = Math.min(width - 16, Math.max(340, (int)(width * 0.94)));
        h = Math.min(height - 16, Math.max(250, (int)(height * 0.92)));
        x = (width - w) / 2;
        y = (height - h) / 2;
        sideW = Math.max(82, Math.min(118, w / 4));
        contentX = x + sideW + 18;
        contentW = Math.max(100, x + w - 20 - contentX);
        topY = y + 65;
        listY = topY + 58;
        listBottom = y + h - 35;
        intro = 0F;
        rebuild();
    }

    private void rebuild() {
        modules.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String name : VoidModules.all().keySet()) {
            if (VoidModules.categoryOf(name) == category &&
                (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q) ||
                 VoidModules.descriptionOf(name).toLowerCase(Locale.ROOT).contains(q))) {
                modules.add(name);
            }
        }
        int visible = Math.max(1, (listBottom - listY) / (rowH + 6));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, modules.size() - visible)));
        if (!modules.contains(selected)) selected = modules.isEmpty() ? "" : modules.get(0);
    }

    private void rect(GuiGraphics g, int l, int t, int r, int b, int color) {
        if (r > l && b > t) g.fill(l, t, r, b, color);
    }

    private void rounded(GuiGraphics g, int l, int t, int width, int height, int color) {
        int radius = height / 2;
        int center = t + radius;
        for (int yy = t; yy < t + height; yy++) {
            int dy = Math.abs(yy - center);
            int inset = dy >= radius ? radius :
                (int)Math.ceil(radius - Math.sqrt(radius * radius - dy * dy));
            g.fill(l + inset, yy, l + width - inset, yy + 1, color);
        }
    }

    private void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int)Math.floor(Math.sqrt(radius * radius - dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private String categoryName() {
        return switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Visuals";
            case PLAYER -> "Player";
            case MISC -> "Utility";
        };
    }

    private int enabledCount() {
        return (int)VoidModules.all().keySet().stream().filter(VoidModules::isEnabled).count();
    }

    private void drawToggle(GuiGraphics g, int sx, int sy, float progress) {
        rounded(g, sx, sy, 34, 18, progress > 0.5F ? 0xFFE6E8EC : 0xFF30343B);
        int knob = sx + 9 + Math.round(progress * 16F);
        circle(g, knob, sy + 9, 6, progress > 0.5F ? 0xFF101216 : 0xFFE5E7EB);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        long now = System.nanoTime();
        float delta = Math.min(0.05F, Math.max(0.001F, (now - lastRenderNanos) / 1_000_000_000F));
        lastRenderNanos = now;
        intro = Math.min(1F, intro + delta * 2.8F);

        // Restrained monochrome backdrop and a soft entrance animation.
        g.fill(0, 0, width, height, 0xA9000000);
        int lift = Math.round((1F - intro) * 9F);
        int py = y + lift;
        rect(g, x - 2, py + 5, x + w + 2, py + h + 5, 0x65000000);
        rect(g, x, py, x + w, py + h, BG);
        rect(g, x + 1, py + 1, x + w - 1, py + 2, 0x20FFFFFF);
        rect(g, x, py, x + w, py + 1, 0xFFE1E4E8);
        rect(g, x + 1, py + 1, x + w - 1, py + h - 1, BG);

        // Navigation rail.
        rect(g, x + 1, py + 1, x + sideW, py + h - 1, PANEL);
        rect(g, x + sideW, py + 1, x + sideW + 1, py + h - 1, LINE);
        rect(g, x + 20, py + 20, x + 48, py + 48, 0xFFE8EAED);
        centered(g, "S", x + 34, py + 28, 0xFF101216);
        label(g, "SWIFT", x + 56, py + 21, WHITE);
        label(g, "VISUALS", x + 56, py + 35, MUTED);
        rect(g, x + 18, py + 61, x + sideW - 12, py + 62, LINE);
        label(g, "WORKSPACE", x + 20, py + 76, MUTED);

        VoidModules.Category[] categories = VoidModules.Category.values();
        String[] names = {"Combat", "Movement", "Visuals", "Player", "Utility"};
        String[] symbols = {"C", "M", "V", "P", "U"};
        for (int i = 0; i < categories.length; i++) {
            int cy = py + 96 + i * 37;
            boolean active = category == categories[i];
            boolean hover = mx >= x + 10 && mx <= x + sideW - 8 && my >= cy && my <= cy + 30;
            if (active) {
                rect(g, x + 10, cy, x + sideW - 8, cy + 30, 0xFFE5E7EB);
                rect(g, x + 10, cy + 5, x + 12, cy + 25, 0xFF111318);
            } else if (hover) {
                rect(g, x + 10, cy, x + sideW - 8, cy + 30, 0xFF202329);
            }
            int fg = active ? 0xFF101216 : hover ? WHITE : MUTED;
            label(g, symbols[i], x + 19, cy + 9, fg);
            label(g, names[i], x + 37, cy + 9, fg);
        }
        rect(g, x + 18, py + h - 52, x + sideW - 12, py + h - 51, LINE);
        circle(g, x + 25, py + h - 32, 3, 0xFFBFC3CA);
        label(g, "FABRIC 1.21.11", x + 35, py + h - 37, MUTED);

        // Header.
        label(g, "SWIFT VISUALS", contentX, py + 21, MUTED);
        label(g, "/", contentX + textWidth("SWIFT VISUALS") + 8, py + 21, 0xFF555B65);
        label(g, categoryName().toUpperCase(Locale.ROOT),
            contentX + textWidth("SWIFT VISUALS /") + 14, py + 21, WHITE);
        String enabled = enabledCount() + " ACTIVE";
        rounded(g, x + w - 20 - textWidth(enabled) - 20, py + 16,
            textWidth(enabled) + 20, 22, 0xFF1D2127);
        circle(g, x + w - 20 - textWidth(enabled) - 9, py + 27, 3, 0xFFE6E8EC);
        label(g, enabled, x + w - 20 - textWidth(enabled), py + 23, SOFT);

        label(g, categoryName(), contentX, topY - 4 + lift, WHITE);
        String sub = "Manage your " + categoryName().toLowerCase(Locale.ROOT) + " modules";
        label(g, sub, contentX, topY + 15 + lift, MUTED);
        String count = modules.size() + " MODULES";
        label(g, count, contentX + contentW - textWidth(count), topY + 1 + lift, MUTED);

        int searchY = topY + 39 + lift;
        rect(g, contentX, searchY, contentX + contentW, searchY + 31, searchFocused ? 0xFF20242B : 0xFF15181D);
        rect(g, contentX, searchY, contentX + contentW, searchY + 1, searchFocused ? 0xFFE5E7EB : LINE);
        circle(g, contentX + 13, searchY + 14, 4, MUTED);
        rect(g, contentX + 16, searchY + 17, contentX + 18, searchY + 20, MUTED);
        String query = search.isEmpty() ? "Search modules" : search;
        label(g, query + (searchFocused ? "_" : ""), contentX + 26, searchY + 11,
            search.isEmpty() ? MUTED : WHITE);
        label(g, " / ", contentX + contentW - 28, searchY + 11, MUTED);

        // Feature list with subtle animated hover and toggle states.
        int visible = Math.max(1, (listBottom - listY) / (rowH + 6));
        for (int i = 0; i < visible; i++) {
            int index = scroll + i;
            if (index >= modules.size()) break;
            String name = modules.get(index);
            int ry = listY + i * (rowH + 6) + lift;
            boolean on = VoidModules.isEnabled(name);
            boolean chosen = name.equals(selected);
            boolean hover = mx >= contentX && mx <= contentX + contentW && my >= ry && my <= ry + rowH;
            float targetHover = hover ? 1F : 0F;
            float hp = hoverProgress.getOrDefault(name, 0F);
            hp += (targetHover - hp) * Math.min(1F, delta * 13F);
            hoverProgress.put(name, hp);
            float target = on ? 1F : 0F;
            float progress = toggleProgress.getOrDefault(name, target);
            progress += (target - progress) * Math.min(1F, delta * 14F);
            if (Math.abs(target - progress) < 0.004F) progress = target;
            toggleProgress.put(name, progress);

            int base = chosen ? 0xFF202329 : CARD;
            if (hp > 0.01F) base = blend(base, CARD_HOVER, hp);
            rect(g, contentX, ry, contentX + contentW, ry + rowH, base);
            rect(g, contentX, ry, contentX + contentW, ry + 1, chosen ? 0xFF858B95 : LINE);
            rect(g, contentX + 10, ry + 10, contentX + 12, ry + rowH - 10,
                progress > 0.5F ? 0xFFE5E7EB : 0xFF41464F);
            label(g, name, contentX + 21, ry + 8, WHITE);
            String desc = VoidModules.descriptionOf(name);
            int max = Math.max(20, contentW - 92);
            while (textWidth(desc) > max && desc.length() > 5) desc = desc.substring(0, desc.length() - 2);
            if (!desc.equals(VoidModules.descriptionOf(name))) desc += "...";
            label(g, desc, contentX + 21, ry + 26, MUTED);
            drawToggle(g, contentX + contentW - 46, ry + 14, progress);
        }

        if (modules.isEmpty()) {
            rect(g, contentX, listY + lift, contentX + contentW, listY + 74 + lift, CARD);
            centered(g, "NO MODULES FOUND", contentX + contentW / 2, listY + 22 + lift, SOFT);
            centered(g, "Try another search", contentX + contentW / 2, listY + 41 + lift, MUTED);
        }

        rect(g, contentX, y + h - 25 + lift, contentX + contentW, y + h - 24 + lift, LINE);
        label(g, "SWIFT VISUALS  •  CLEAN BY DESIGN", contentX, y + h - 16 + lift, MUTED);
        label(g, "ESC  CLOSE", contentX + contentW - textWidth("ESC  CLOSE"), y + h - 16 + lift, MUTED);
        super.render(g, mx, my, partialTick);
    }

    private int blend(int from, int to, float amount) {
        int a = (int)(((from >> 16) & 255) * (1F - amount) + ((to >> 16) & 255) * amount);
        int b = (int)(((from >> 8) & 255) * (1F - amount) + ((to >> 8) & 255) * amount);
        int c = (int)((from & 255) * (1F - amount) + (to & 255) * amount);
        return 0xFF000000 | (a << 16) | (b << 8) | c;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        searchFocused = mx >= contentX && mx <= contentX + contentW &&
            my >= topY + 39 && my <= topY + 70;
        VoidModules.Category[] categories = VoidModules.Category.values();
        for (int i = 0; i < categories.length; i++) {
            int cy = y + 96 + i * 37;
            if (mx >= x + 10 && mx <= x + sideW - 8 && my >= cy && my <= cy + 30) {
                category = categories[i];
                scroll = 0;
                selected = "";
                rebuild();
                return true;
            }
        }
        int visible = Math.max(1, (listBottom - listY) / (rowH + 6));
        for (int i = 0; i < visible; i++) {
            int index = scroll + i;
            if (index >= modules.size()) break;
            int ry = listY + i * (rowH + 6);
            if (mx >= contentX && mx <= contentX + contentW && my >= ry && my <= ry + rowH) {
                String name = modules.get(index);
                selected = name;
                if (mx >= contentX + contentW - 54) VoidModules.toggle(name);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!searchFocused) return super.charTyped(e);
        char c = (char)e.codepoint();
        if (c >= 32 && c != 127 && search.length() < 48) {
            search += c;
            scroll = 0;
            rebuild();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (searchFocused) {
            if (e.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                rebuild();
                return true;
            }
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int visible = Math.max(1, (listBottom - listY) / (rowH + 6));
        scroll = Math.max(0, Math.min(Math.max(0, modules.size() - visible),
            scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
