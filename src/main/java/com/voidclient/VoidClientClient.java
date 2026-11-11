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
import java.util.Locale;

public final class VoidClientClient implements ClientModInitializer {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Double previousGamma = null;
    private static Boolean previousBobView = null;

    @Override
    public void onInitializeClient() {
        KeyMapping openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.swiftvisuals.open_menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC
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
                if (movingForward && !client.player.isCrouching()
                    && client.player.getFoodData().getFoodLevel() > 6) {
                    client.player.setSprinting(true);
                }
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("swiftvisuals", "info_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            int x = 8, y = 8;
            int accent = 0xFFAA78FF;
            if (VoidModules.isEnabled("FPS Counter"))
                y = drawCard(graphics, client, "FPS", Integer.toString(client.getFps()), x, y, accent);

            if (VoidModules.isEnabled("Coordinates")) {
                var pos = client.player.blockPosition();
                y = drawCard(graphics, client, "XYZ", pos.getX() + "  " + pos.getY() + "  " + pos.getZ(), x, y, 0xFFFFFFFF);
            }
            if (VoidModules.isEnabled("Clock"))
                y = drawCard(graphics, client, "LOCAL TIME", LocalTime.now().format(CLOCK), x, y, 0xFFFFFFFF);

            if (VoidModules.isEnabled("Keystrokes")) {
                String keys = (client.options.keyUp.isDown() ? "[W]" : " W ")
                    + (client.options.keyLeft.isDown() ? "[A]" : " A ")
                    + (client.options.keyDown.isDown() ? "[S]" : " S ")
                    + (client.options.keyRight.isDown() ? "[D]" : " D ")
                    + (client.options.keyJump.isDown() ? "[SPACE]" : " SPACE ");
                y = drawCard(graphics, client, "KEYS", keys, x, y, accent);
            }
            if (VoidModules.isEnabled("Sprint Status"))
                y = drawCard(graphics, client, "SPRINT", client.player.isSprinting() ? "ON" : "OFF", x, y, 0xFF72E0A1);

            if (VoidModules.isEnabled("Direction HUD")) {
                String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
                int index = Math.floorMod(Math.round(client.player.getYRot() / 45.0F), 8);
                y = drawCard(graphics, client, "FACING", directions[index], x, y, 0xFFFFFFFF);
            }
            if (VoidModules.isEnabled("Health HUD")) {
                int hp = Math.round(client.player.getHealth());
                y = drawCard(graphics, client, "HEALTH", hp + "/" + Math.round(client.player.getMaxHealth()), x, y,
                    hp <= 6 ? 0xFFFF6666 : 0xFF75D99A);
            }
            if (VoidModules.isEnabled("Low Health Alert") && client.player.getHealth() <= 6.0F) {
                graphics.fill(0, 0, client.gui.getGuiWidth(), 3, 0xFFFF4545);
                graphics.drawCenteredString(client.font, "LOW HEALTH", client.gui.getGuiWidth() / 2, 12, 0xFFFF7777);
            }
            if (VoidModules.isEnabled("Hunger HUD"))
                y = drawCard(graphics, client, "FOOD", client.player.getFoodData().getFoodLevel() + "/20", x, y, 0xFFFFC66D);
            if (VoidModules.isEnabled("Armor HUD"))
                y = drawCard(graphics, client, "ARMOR", client.player.getArmorValue() + "/20", x, y, 0xFF9BCBFF);

            if (VoidModules.isEnabled("Ping HUD") && client.getConnection() != null) {
                var info = client.getConnection().getPlayerInfo(client.player.getUUID());
                if (info != null) y = drawCard(graphics, client, "PING", info.getLatency() + " ms", x, y, accent);
            }
            if (VoidModules.isEnabled("Biome HUD") && client.level != null) {
                String biome = client.level.getBiome(client.player.blockPosition()).unwrapKey()
                    .map(Object::toString).orElse("unknown");
                y = drawCard(graphics, client, "BIOME", biome, x, y, 0xFF9FE0B0);
            }
            if (VoidModules.isEnabled("Attack Cooldown")) {
                int percent = Math.round(client.player.getAttackStrengthScale(0.0F) * 100.0F);
                y = drawBarCard(graphics, client, "ATTACK READY", percent, x, y, accent);
            }
            if (VoidModules.isEnabled("Item Durability HUD")) {
                var item = client.player.getMainHandItem();
                String value = item.isDamageableItem()
                    ? (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage() : "—";
                y = drawCard(graphics, client, "DURABILITY", value, x, y,
                    item.isDamageableItem() && item.getMaxDamage() - item.getDamageValue() < 20 ? 0xFFFF6666 : 0xFFFFFFFF);
            }
            if (VoidModules.isEnabled("Selected Item HUD"))
                y = drawCard(graphics, client, "HELD ITEM", client.player.getMainHandItem().getHoverName().getString(), x, y, 0xFFFFFFFF);
            if (VoidModules.isEnabled("Offhand Item HUD"))
                y = drawCard(graphics, client, "OFFHAND", client.player.getOffhandItem().getHoverName().getString(), x, y, 0xFFB89AFF);

            if (VoidModules.isEnabled("Movement Speed HUD")) {
                var velocity = client.player.getDeltaMovement();
                double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
                y = drawCard(graphics, client, "SPEED", String.format(Locale.ROOT, "%.2f m/s", speed), x, y, accent);
            }
            if (VoidModules.isEnabled("World Time HUD") && client.level != null) {
                long time = Math.floorMod(client.level.getDayTime(), 24000L);
                long hours = (time / 1000 + 6) % 24;
                long minutes = (time % 1000) * 60 / 1000;
                drawCard(graphics, client, "WORLD TIME", String.format(Locale.ROOT, "%02d:%02d", hours, minutes), x, y, 0xFFFFFFFF);
            }

            if (VoidModules.isEnabled("Crosshair Dot")) {
                int cx = client.gui.getGuiWidth() / 2;
                int cy = client.gui.getGuiHeight() / 2;
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFB98AFF);
            }
        });
    }

    private static int drawCard(net.minecraft.client.gui.GuiGraphics g, Minecraft client,
                                String label, String value, int x, int y, int color) {
        int maxWidth = Math.max(client.font.width(label), client.font.width(value));
        int width = Math.min(210, maxWidth + 18);
        g.fill(x, y, x + width, y + 27, 0xB910121E);
        g.fill(x, y, x + 2, y + 27, color);
        g.drawString(client.font, label, x + 8, y + 4, 0xFF9298AD, false);
        g.drawString(client.font, value, x + 8, y + 15, color, true);
        return y + 31;
    }

    private static int drawBarCard(net.minecraft.client.gui.GuiGraphics g, Minecraft client,
                                   String label, int percent, int x, int y, int color) {
        g.fill(x, y, x + 210, y + 29, 0xB910121E);
        g.fill(x, y, x + 2, y + 29, color);
        g.drawString(client.font, label + "  " + percent + "%", x + 8, y + 4, 0xFFE7E8F0, true);
        g.fill(x + 8, y + 19, x + 202, y + 24, 0xFF34384A);
        int fill = Math.max(0, Math.min(194, percent * 194 / 100));
        if (fill > 0) g.fill(x + 8, y + 19, x + 8 + fill, y + 24, color);
        return y + 33;
    }
}
