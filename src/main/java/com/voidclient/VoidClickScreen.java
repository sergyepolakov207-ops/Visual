package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoidClickScreen extends Screen {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 220;
    private int panelX;
    private int panelY;
    private final List<String> moduleNames = new ArrayList<>(VoidModules.all().keySet());

    public VoidClickScreen() {
        super(Component.literal("SWIFT VISUALS"));
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Draw a plain overlay instead of Minecraft's blurred background.
        // This avoids invoking the optional blur/post-processing pipeline.
        g.fill(0, 0, width, height, 0x99000000);

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF0121420);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, 0xFF8B5CF6);
        g.fill(panelX, panelY + 34, panelX + PANEL_W, panelY + 35, 0xFF303344);

        g.drawString(font, "SWIFT VISUALS", panelX + 14, panelY + 13, 0xFFFFFFFF, false);
        g.drawString(font, "VISUALS  /  MODULES", panelX + 14, panelY + 44, 0xFFAAAFC2, false);
        g.drawString(font, "RIGHT SHIFT  -  CLOSE", panelX + 164, panelY + 13, 0xFF777D91, false);

        int y = panelY + 66;
        for (String name : moduleNames) {
            boolean enabled = VoidModules.isEnabled(name);
            boolean hovered = mouseX >= panelX + 12 && mouseX <= panelX + PANEL_W - 12
                && mouseY >= y - 5 && mouseY <= y + 15;

            g.fill(panelX + 12, y - 5, panelX + PANEL_W - 12, y + 17,
                hovered ? 0xFF292D40 : 0xFF1C2030);
            g.drawString(font, name, panelX + 22, y + 2, 0xFFE6E8F0, false);

            int toggleColor = enabled ? 0xFF8B5CF6 : 0xFF454A5C;
            g.fill(panelX + PANEL_W - 48, y, panelX + PANEL_W - 25, y + 10, toggleColor);
            g.fill(panelX + PANEL_W - (enabled ? 31 : 46), y + 2,
                panelX + PANEL_W - (enabled ? 27 : 42), y + 8, 0xFFFFFFFF);
            y += 24;
        }

        g.drawString(font, "SWIFT VISUALS  -  FABRIC 1.21.11",
            panelX + 14, panelY + PANEL_H - 16, 0xFF777D91, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0) {
            int y = panelY + 66;
            for (String name : moduleNames) {
                if (mouseX >= panelX + 12 && mouseX <= panelX + PANEL_W - 12
                    && mouseY >= y - 5 && mouseY <= y + 17) {
                    VoidModules.toggle(name);
                    return true;
                }
                y += 24;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
