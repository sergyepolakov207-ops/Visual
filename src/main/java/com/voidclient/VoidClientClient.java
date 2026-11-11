package com.voidclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class VoidClientClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Double previousGamma = null;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.swiftvisuals.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new VoidClickScreen());
                else if (client.screen instanceof VoidClickScreen) client.setScreen(null);
            }

            if (client.options != null) {
                if (VoidModules.isEnabled("Fullbright")) {
                    if (previousGamma == null) previousGamma = client.options.gamma().get();
                    client.options.gamma().set(16.0);
                } else if (previousGamma != null) {
                    client.options.gamma().set(previousGamma);
                    previousGamma = null;
                }
            }

            if (client.player != null && VoidModules.isEnabled("Auto Sprint")) {
                boolean movingForward = client.options.keyUp.isDown();
                if (movingForward && !client.player.isCrouching() && client.player.getFoodData().getFoodLevel() > 6) {
                    client.player.setSprinting(true);
                }
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("swiftvisuals", "info_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            int y = 8;
            if (VoidModules.isEnabled("FPS Counter")) {
                graphics.drawString(client.font, "FPS: " + client.getFps(), 8, y, 0xFFB89AFF, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Coordinates")) {
                var pos = client.player.blockPosition();
                graphics.drawString(client.font, "XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                    8, y, 0xFFFFFFFF, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Clock")) {
                graphics.drawString(client.font, LocalTime.now().format(CLOCK), 8, y, 0xFFFFFFFF, true);
            }
        });
    }
}
