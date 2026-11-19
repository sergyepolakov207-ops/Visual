package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int WHITE = 0xFFF5F2FC;
    private static final int MUTED = 0xFF9B96AA;
    private static final int PURPLE = 0xFFC69AFF;
    private static final int PANEL = 0xE9161420;
    private static final int CARD = 0xD51F1C2B;
    private static final int CARD_HOVER = 0xE52B2539;
    private static final int ACCENT = 0xFF9A62E8;

    private int x, y, w, h, rail, contentX, contentRight, listTop, listBottom;
    private int cardW, cardH = 76, gap = 12, columns, scroll;
    private boolean searchFocused;
    private String search = "";
    private String selected = "";
    private VoidModules.Category category = VoidModules.Category.RENDER;
    private final List<String> modules = new ArrayList<>();

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        w = Math.min(width - 20, Math.max(300, (int)(width * 0.94)));
        h = Math.min(height - 20, Math.max(230, (int)(height * 0.90)));
        x = (width - w) / 2;
        y = (height - h) / 2;
        rail = Math.max(68, Math.min(92, width / 14));
        contentX = x + rail + 18;
        contentRight = x + w - 20;
        listTop = y + 118;
        listBottom = y + h - 88;
        columns = contentRight - contentX >= 590 ? 2 : 1;
        cardW = Math.max(90, (contentRight - contentX - gap * (columns - 1)) / columns);
        cardH = Math.max(68, Math.min(78, h / 8));
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
        int rows = (modules.size() + columns - 1) / columns;
        int visible = Math.max(1, (listBottom - listTop + gap) / (cardH + gap));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows - visible)));
        if (!modules.contains(selected)) selected = modules.isEmpty() ? "" : modules.get(0);
    }

    private void round(GuiGraphics g, int px, int py, int pw, int ph, int color, int radius) {
        if (pw <= 0 || ph <= 0) return;
        int r = Math.min(radius, Math.min(pw / 2, ph / 2));
        for (int row = 0; row < ph; row++) {
            int inset = 0;
            if (row < r) inset = r - (int)Math.sqrt(r * r - (r - row) * (r - row));
            else if (row >= ph - r) {
                int d = row - (ph - r - 1);
                inset = r - (int)Math.sqrt(r * r - d * d);
            }
            g.fill(px + inset, py + row, px + pw - inset, py + row + 1, color);
        }
    }

    private String categoryLabel() {
        return switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Visuals";
            case PLAYER -> "Player";
            case MISC -> "Utilities";
        };
    }

    private String categoryHint() {
        return switch (category) {
            case COMBAT -> "Useful combat information";
            case MOVEMENT -> "Movement and input indicators";
            case RENDER -> "Visual effects and camera options";
            case PLAYER -> "Player and inventory information";
            case MISC -> "HUD widgets and client tools";
        };
    }

    private int enabledCount() {
        return (int) modules.stream().filter(VoidModules::isEnabled).count();
    }

    private String[] wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (font.width(next) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        if (lines.size() > 2) {
            String second = lines.get(1);
            while (!second.isEmpty() && font.width(second + "…") > maxWidth) {
                second = second.substring(0, second.length() - 1);
            }
            lines.set(1, second + "…");
        }
        return lines.toArray(new String[0]);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        try { renderBlurredBackground(g); } catch (RuntimeException | LinkageError ignored) { }
        g.fill(0, 0, width, height, 0x85070610);
        g.fill(0, 0, width, height / 2, 0x100F0920);

        // Main glass shell and reflective frame.
        round(g, x, y, w, h, PANEL, 18);
        round(g, x + 2, y + 2, w - 4, h - 4, 0x321F1A30, 16);
        g.fill(x + 18, y + 2, x + w - 18, y + 3, 0x70FFFFFF);
        g.fill(x + 28, y + 4, x + w - 28, y + 5, 0x45C99CFF);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0x559A62E8);
        g.fill(x + rail, y + 12, x + rail + 1, y + h - 12, 0x332C2940);

        // Brand rail.
        round(g, x + 13, y + 14, rail - 26, 40, 0xFF30233F, 12);
        g.drawCenteredString(font, "S", x + rail / 2, y + 27, PURPLE);
        g.drawCenteredString(font, "SWIFT", x + rail / 2, y + 57, MUTED);
        String[] icons = {"✧", "⌁", "◈", "♙", "⚙"};
        String[] labels = {"FIGHT", "MOVE", "VISUAL", "PLAYER", "TOOLS"};
        for (int i = 0; i < VoidModules.Category.values().length; i++) {
            int iy = y + 78 + i * 58;
            boolean active = category.ordinal() == i;
            if (active) {
                round(g, x + 7, iy, rail - 14, 49, 0xFF382849, 12);
                g.fill(x + 7, iy + 9, x + 9, iy + 40, PURPLE);
            }
            g.drawCenteredString(font, icons[i], x + rail / 2, iy + 7, active ? PURPLE : MUTED);
            g.drawCenteredString(font, labels[i], x + rail / 2, iy + 27, active ? WHITE : MUTED);
        }

        // Top bar.
        g.drawString(font, "SWIFT", contentX, y + 17, WHITE, true);
        g.drawString(font, "VISUALS", contentX + font.width("SWIFT ") + 2, y + 17, PURPLE, true);
        round(g, contentX, y + 42, font.width(categoryLabel()) + 22, 20, 0xFF342542, 9);
        g.drawString(font, categoryLabel().toUpperCase(Locale.ROOT), contentX + 11, y + 48, PURPLE, true);
        g.drawString(font, categoryHint(), contentX, y + 70, MUTED, false);
        String version = "FABRIC 1.21.11";
        g.drawString(font, version, contentRight - font.width(version), y + 20, MUTED, false);
        g.fill(contentX, y + 91, contentRight, y + 92, 0x443E354F);

        // Search and result summary.
        round(g, contentX, y + 99, contentRight - contentX, 30,
            searchFocused ? 0xFF34283F : 0xB923202D, 10);
        g.drawString(font, "⌕", contentX + 12, y + 108, PURPLE, false);
        String query = search.isEmpty() ? "Search modules and descriptions..." : search;
        g.drawString(font, query + (searchFocused ? "  |" : ""), contentX + 31, y + 108,
            search.isEmpty() ? MUTED : WHITE, false);
        String count = modules.size() + " MODULES  •  " + enabledCount() + " ENABLED";
        g.drawString(font, count, contentRight - font.width(count) - 12, y + 108, MUTED, false);

        // Module cards.
        int visibleRows = Math.max(1, (listBottom - listTop + gap) / (cardH + gap));
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= modules.size()) continue;
                String name = modules.get(index);
                int cx = contentX + col * (cardW + gap);
                int cy = listTop + row * (cardH + gap);
                boolean enabled = VoidModules.isEnabled(name);
                boolean chosen = selected.equals(name);
                boolean hover = mx >= cx && mx <= cx + cardW && my >= cy && my <= cy + cardH;
                round(g, cx, cy, cardW, cardH, chosen ? 0xE52D233B : hover ? CARD_HOVER : CARD, 12);
                g.fill(cx + 12, cy + 1, cx + cardW - 12, cy + 2, chosen ? 0x99D5B0FF : 0x28FFFFFF);
                g.fill(cx + 1, cy + 12, cx + 2, cy + cardH - 12, enabled ? ACCENT : 0x333F3A4B);
                g.drawString(font, enabled ? "✦" : "◇", cx + 13, cy + 10, enabled ? PURPLE : MUTED, false);
                g.drawString(font, name, cx + 31, cy + 9, WHITE, true);
                String[] desc = wrap(VoidModules.descriptionOf(name), Math.max(50, cardW - 90));
                g.drawString(font, desc[0], cx + 31, cy + 29, MUTED, false);
                if (desc.length > 1) g.drawString(font, desc[1], cx + 31, cy + 41, MUTED, false);
                String status = enabled ? "ACTIVE" : "OFF";
                g.drawString(font, status, cx + 31, cy + cardH - 17, enabled ? PURPLE : 0xFF777182, true);
                int sx = cx + cardW - 48;
                int sy = cy + (cardH - 18) / 2;
                round(g, sx, sy, 36, 18, enabled ? 0xFF8150B5 : 0xFF45414F, 9);
                round(g, enabled ? sx + 19 : sx + 2, sy + 2, 14, 14,
                    enabled ? 0xFFFFF9FF : 0xFFBDB8C8, 7);
            }
        }
        if (modules.isEmpty()) {
            g.drawCenteredString(font, "Nothing found here", (contentX + contentRight) / 2, listTop + 20, MUTED);
        }

        // Bottom inspector: selection is now a real second step, separate from toggling.
        int panelY = y + h - 76;
        round(g, contentX, panelY, contentRight - contentX, 58, 0xE51D1928, 12);
        g.fill(contentX + 14, panelY + 1, contentRight - 14, panelY + 2, 0x55FFFFFF);
        if (!selected.isEmpty()) {
            g.drawString(font, selected, contentX + 13, panelY + 9, WHITE, true);
            String[] info = wrap(VoidModules.descriptionOf(selected), Math.max(80, contentRight - contentX - 190));
            g.drawString(font, info[0], contentX + 13, panelY + 29, MUTED, false);
            if (selected.equals("HUD")) {
                String scale = Math.round(VoidModules.hudScale() * 100) + "%";
                g.drawString(font, "HUD SCALE", contentRight - 142, panelY + 10, MUTED, true);
                round(g, contentRight - 88, panelY + 27, 25, 22, 0xFF393044, 7);
                round(g, contentRight - 57, panelY + 27, 25, 22, 0xFF393044, 7);
                g.drawCenteredString(font, "−", contentRight - 76, panelY + 34, WHITE);
                g.drawCenteredString(font, "+", contentRight - 45, panelY + 34, WHITE);
                g.drawString(font, scale, contentRight - 132, panelY + 31, PURPLE, true);
            } else {
                String status = VoidModules.isEnabled(selected) ? "ENABLED" : "DISABLED";
                g.drawString(font, status, contentRight - font.width(status) - 14, panelY + 20,
                    VoidModules.isEnabled(selected) ? PURPLE : MUTED, true);
            }
        } else {
            g.drawString(font, "MODULE INSPECTOR", contentX + 13, panelY + 10, PURPLE, true);
            g.drawString(font, "Select a card to view its information and options", contentX + 13, panelY + 30, MUTED, false);
        }

        int totalRows = (modules.size() + columns - 1) / columns;
        if (totalRows > visibleRows) {
            int track = Math.max(1, listBottom - listTop);
            int thumb = Math.max(18, track * visibleRows / totalRows);
            int ty = listTop + (track - thumb) * scroll / Math.max(1, totalRows - visibleRows);
            round(g, contentRight + 6, ty, 3, thumb, ACCENT, 2);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() != 0) return super.mouseClicked(e, doubled);
        double mx = e.x(), my = e.y();
        searchFocused = mx >= contentX && mx <= contentRight && my >= y + 99 && my <= y + 129;
        for (int i = 0; i < VoidModules.Category.values().length; i++) {
            int iy = y + 78 + i * 58;
            if (mx >= x + 5 && mx <= x + rail - 5 && my >= iy && my <= iy + 49) {
                category = VoidModules.Category.values()[i];
                scroll = 0;
                selected = "";
                rebuild();
                return true;
            }
        }
        int panelY = y + h - 76;
        if (selected.equals("HUD") && my >= panelY + 25 && my <= panelY + 55) {
            if (mx >= contentRight - 90 && mx <= contentRight - 62)
                VoidModules.setHudScale(VoidModules.hudScale() - 0.05F);
            else if (mx >= contentRight - 59 && mx <= contentRight - 31)
                VoidModules.setHudScale(VoidModules.hudScale() + 0.05F);
            return true;
        }
        int visibleRows = Math.max(1, (listBottom - listTop + gap) / (cardH + gap));
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index >= modules.size()) continue;
                int cx = contentX + col * (cardW + gap);
                int cy = listTop + row * (cardH + gap);
                if (mx >= cx && mx <= cx + cardW && my >= cy && my <= cy + cardH) {
                    String name = modules.get(index);
                    if (mx >= cx + cardW - 54) VoidModules.toggle(name);
                    else selected = selected.equals(name) ? "" : name;
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!searchFocused) return super.charTyped(e);
        char ch = (char)e.codepoint();
        if (ch >= 32 && ch != 127 && search.length() < 48) {
            search += ch;
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
        int rows = (modules.size() + columns - 1) / columns;
        int visible = Math.max(1, (listBottom - listTop + gap) / (cardH + gap));
        scroll = Math.max(0, Math.min(Math.max(0, rows - visible), scroll - (int)Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
