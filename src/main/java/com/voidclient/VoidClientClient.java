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
    private static Boolean previousBobView = null;

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
                if (VoidModules.isEnabled("No View Bobbing")) {
                    if (previousBobView == null) previousBobView = client.options.bobView().get();
                    client.options.bobView().set(false);
                } else if (previousBobView != null) {
                    client.options.bobView().set(previousBobView);
                    previousBobView = null;
                }
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
                y += 12;
            }
            if (VoidModules.isEnabled("Keystrokes")) {
                String keys = (client.options.keyUp.isDown() ? "[W]" : " W ")
                    + (client.options.keyLeft.isDown() ? "[A]" : " A ")
                    + (client.options.keyDown.isDown() ? "[S]" : " S ")
                    + (client.options.keyRight.isDown() ? "[D]" : " D ")
                    + (client.options.keyJump.isDown() ? "[SPACE]" : " SPACE ");
                graphics.drawString(client.font, keys, 8, y, 0xFFB89AFF, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Sprint Status")) {
                boolean sprinting = client.player.isSprinting();
                graphics.drawString(client.font, "SPRINT: " + (sprinting ? "ON" : "OFF"), 8, y,
                    sprinting ? 0xFF72E0A1 : 0xFFFFFFFF, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Direction HUD")) {
                String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
                int index = Math.floorMod(Math.round(client.player.getYRot() / 45.0F), 8);
                graphics.drawString(client.font, "FACING: " + directions[index], 8, y, 0xFFFFFFFF, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Health HUD")) {
                graphics.drawString(client.font, "HEALTH: " + Math.round(client.player.getHealth()) + "/"
                    + Math.round(client.player.getMaxHealth()), 8, y, 0xFFFF7777, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Hunger HUD")) {
                graphics.drawString(client.font, "FOOD: " + client.player.getFoodData().getFoodLevel() + "/20",
                    8, y, 0xFFFFC66D, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Armor HUD")) {
                graphics.drawString(client.font, "ARMOR: " + client.player.getArmorValue() + "/20",
                    8, y, 0xFF9BCBFF, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Ping HUD") && client.getConnection() != null) {
                var info = client.getConnection().getPlayerInfo(client.player.getUUID());
                if (info != null) {
                    graphics.drawString(client.font, "PING: " + info.getLatency() + " ms", 8, y, 0xFFB89AFF, true);
                    y += 12;
                }
            }
            if (VoidModules.isEnabled("Biome HUD") && client.level != null) {
                String biome = client.level.getBiome(client.player.blockPosition()).unwrapKey()
                    .map(key -> key.location().getPath()).orElse("unknown");
                graphics.drawString(client.font, "BIOME: " + biome, 8, y, 0xFF9FE0B0, true);
                y += 12;
            }
            if (VoidModules.isEnabled("Attack Cooldown")) {
                int percent = Math.round(client.player.getAttackStrengthScale(0.0F) * 100.0F);
                graphics.drawString(client.font, "ATTACK: " + percent + "%", 8, y,
                    percent >= 100 ? 0xFF72E0A1 : 0xFFFFC66D, true);
            }
        });
    }
}
