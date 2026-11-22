package com.voidclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;


public final class VoidClientClient implements ClientModInitializer {
    private static Double previousGamma = null;
    private static Boolean previousBobView = null;

    @Override
    public void onInitializeClient() {
        KeyMapping openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.swiftvisuals.open_menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            VoidModules.handleKeyBinds(client);
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

        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("swiftvisuals", "info_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || !VoidModules.isEnabled("HUD")) return;

            graphics.pose().pushMatrix();
            graphics.pose().scale(VoidModules.hudScale(), VoidModules.hudScale());
            int x = 8, y = 8;
            int accent = 0xFFAA78FF;
            if (VoidModules.isEnabled("FPS Counter"))
                y = drawCard(graphics, client, "FPS", Integer.toString(client.getFps()), x, y, accent);

            if (VoidModules.isEnabled("Coordinates")) {
                var pos = client.player.blockPosition();
                y = drawCard(graphics, client, "XYZ", pos.getX() + "  " + pos.getY() + "  " + pos.getZ(), x, y, 0xFFFFFFFF);
            }

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
                graphics.fill(0, 0, client.getWindow().getGuiScaledWidth(), 3, 0xFFFF4545);
                graphics.drawCenteredString(client.font, "LOW HEALTH", client.getWindow().getGuiScaledWidth() / 2, 12, 0xFFFF7777);
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

            if (VoidModules.isEnabled("Target HUD") || VoidModules.isEnabled("Target Range")) {
                if (client.hitResult instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity target && target.isAlive()) {
                    if (VoidModules.isEnabled("Target HUD")) {
                        String targetName = target.getName().getString();
                        String hp = Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth());
                        y = drawCard(graphics, client, "TARGET " + targetName, hp, x, y,
                            target.getHealth() <= 6.0F ? 0xFFFF6868 : 0xFFB88CFF);
                    }
                    if (VoidModules.isEnabled("Target Range")) {
                        String distance = String.format(java.util.Locale.ROOT, "%.1f blocks",
                            client.player.distanceTo(target));
                        y = drawCard(graphics, client, "TARGET RANGE", distance, x, y, accent);
                    }
                }
            }

            if (VoidModules.isEnabled("Totem Counter")) {
                int totems = 0;
                for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                    if (client.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) totems +=
                        client.player.getInventory().getItem(i).getCount();
                }
                y = drawCard(graphics, client, "TOTEMS", Integer.toString(totems), x, y, 0xFFFFD36A);
            }

            if (VoidModules.isEnabled("Armor Durability")) {
                StringBuilder armor = new StringBuilder();
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET};
                for (EquipmentSlot slot : slots) {
                    var stack = client.player.getItemBySlot(slot);
                    if (stack.isEmpty()) armor.append("— ");
                    else if (stack.isDamageableItem()) {
                        int remaining = stack.getMaxDamage() - stack.getDamageValue();
                        armor.append(remaining).append(" ");
                    } else armor.append("• ");
                }
                y = drawCard(graphics, client, "ARMOR DURABILITY", armor.toString().trim(), x, y, 0xFF9BCBFF);
            }

            if (VoidModules.isEnabled("Pearl Cooldown")) {
                boolean cooling = client.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL));
                y = drawCard(graphics, client, "PEARL", cooling ? "COOLDOWN" : "READY", x, y,
                    cooling ? 0xFFFF9B70 : 0xFF75D99A);
            }

            if (VoidModules.isEnabled("Shield Status")) {
                y = drawCard(graphics, client, "SHIELD", client.player.isBlocking() ? "BLOCKING" : "READY",
                    x, y, client.player.isBlocking() ? 0xFF75D99A : 0xFF9BCBFF);
            }
            if (VoidModules.isEnabled("Item Durability HUD")) {
                var item = client.player.getMainHandItem();
                String value = item.isDamageableItem()
                    ? (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage() : "—";
                y = drawCard(graphics, client, "DURABILITY", value, x, y,
                    item.isDamageableItem() && item.getMaxDamage() - item.getDamageValue() < 20 ? 0xFFFF6666 : 0xFFFFFFFF);
            }


            if (VoidModules.isEnabled("Inventory HUD")) drawInventoryHud(graphics, client);

            drawPhantomHud(graphics, client);

            if (VoidModules.isEnabled("Crosshair Dot")) {
                int cx = client.getWindow().getGuiScaledWidth() / 2;
                int cy = client.getWindow().getGuiScaledHeight() / 2;
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFB98AFF);
            }
            graphics.pose().popMatrix();
        });
    }



    private static void drawPhantomHud(net.minecraft.client.gui.GuiGraphics g, Minecraft client) {
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        int purple = 0xFFAA83FF;
        int white = 0xFFF0EDF7;
        int muted = 0xFFAAA5B8;

        if (VoidModules.isEnabled("Status Bar")) {
            String ping = "—";
            if (client.getConnection() != null) {
                var info = client.getConnection().getPlayerInfo(client.player.getUUID());
                if (info != null) ping = info.getLatency() + " ms";
            }
            String line = "✦  " + client.player.getName().getString()
                + "   ◈ " + ping + "   ▣ " + client.getFps() + " FPS"
                + "   ⌖ " + client.player.blockPosition().getX() + " "
                + client.player.blockPosition().getY() + " "
                + client.player.blockPosition().getZ();
            drawGlassPanel(g, 8, 8, Math.min(sw - 16, client.font.width(line) + 22), 22);
            g.drawString(client.font, line, 18, 15, white, false);
        }

        if (VoidModules.isEnabled("Hotkeys HUD")) {
            int x = 8, y = 38, w = 112, h = 91;
            drawGlassPanel(g, x, y, w, h);
            g.drawString(client.font, "HOTKEYS", x + 9, y + 7, purple, true);
            String[] keys = {
                (client.options.keyUp.isDown() ? "W" : "·") + "  Forward",
                (client.options.keyLeft.isDown() ? "A" : "·") + "  Left",
                (client.options.keyDown.isDown() ? "S" : "·") + "  Back",
                (client.options.keyRight.isDown() ? "D" : "·") + "  Right",
                (client.options.keyJump.isDown() ? "SPACE" : "·") + "  Jump"
            };
            for (int i = 0; i < keys.length; i++)
                g.drawString(client.font, keys[i], x + 9, y + 22 + i * 12,
                    keys[i].startsWith("·") ? muted : white, false);
        }

        if (VoidModules.isEnabled("Potions HUD") && !client.player.getActiveEffects().isEmpty()) {
            int x = Math.max(8, sw - 178), y = 8;
            int rows = Math.min(6, client.player.getActiveEffects().size());
            drawGlassPanel(g, x, y, 170, 24 + rows * 15);
            g.drawString(client.font, "POTIONS", x + 9, y + 7, purple, true);
            int i = 0;
            for (var effect : client.player.getActiveEffects()) {
                if (i >= 6) break;
                String name = effect.getEffect().value().getDisplayName().getString();
                if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
                int ticks = effect.getDuration();
                String time = ticks == -1 ? "∞" : String.format(java.util.Locale.ROOT, "%d:%02d",
                    ticks / 1200, (ticks / 20) % 60);
                g.drawString(client.font, name, x + 9, y + 22 + i * 15, white, false);
                g.drawString(client.font, time, x + 164 - client.font.width(time), y + 22 + i * 15, purple, false);
                i++;
            }
        }

        if (VoidModules.isEnabled("Player Card")) {
            String name = client.player.getName().getString();
            String hp = "♥ " + Math.round(client.player.getHealth()) + "  "
                + "◆ " + client.player.getArmorValue();
            int w = Math.max(142, client.font.width(name) + 28);
            int x = sw - w - 8, y = sh - 42;
            drawGlassPanel(g, x, y, w, 34);
            g.drawString(client.font, name, x + 9, y + 6, white, true);
            g.drawString(client.font, hp, x + 9, y + 19, purple, false);
        }

        if (VoidModules.isEnabled("Online Players HUD") && client.getConnection() != null) {
            var players = client.getConnection().getOnlinePlayers();
            int count = Math.min(5, players.size());
            int w = 158, h = 23 + count * 13;
            int x = (sw - w) / 2, y = sh - h - 10;
            drawGlassPanel(g, x, y, w, h);
            g.drawString(client.font, "ONLINE  " + players.size(), x + 9, y + 6, purple, true);
            int i = 0;
            for (var playerInfo : players) {
                if (i >= 5) break;
                String playerName = playerInfo.getProfile().name();
                g.drawString(client.font, playerName, x + 9, y + 19 + i * 13, white, false);
                i++;
            }
        }

        if (VoidModules.isEnabled("Movement HUD")) {
            double speed = Math.sqrt(client.player.getDeltaMovement().x * client.player.getDeltaMovement().x
                + client.player.getDeltaMovement().z * client.player.getDeltaMovement().z) * 20.0;
            String value = String.format(java.util.Locale.ROOT, "%.2f m/s", speed);
            int x = 8, y = sh - 31, w = client.font.width(value) + 68;
            drawGlassPanel(g, x, y, w, 22);
            g.drawString(client.font, "BPS", x + 9, y + 7, purple, true);
            g.drawString(client.font, value, x + 43, y + 7, white, false);
        }
    }

    private static void drawGlassPanel(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h) {
        int radius = Math.min(7, Math.min(w / 2, h / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < radius) inset = radius - (int)Math.sqrt(radius * radius - (radius - row) * (radius - row));
            else if (row >= h - radius) {
                int d = row - (h - radius - 1);
                inset = radius - (int)Math.sqrt(radius * radius - d * d);
            }
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, 0xD90D0C16);
        }
        g.fill(x + 8, y + 1, x + w - 8, y + 2, 0x55FFFFFF);
        g.fill(x + 1, y + 7, x + 2, y + h - 7, 0x35FFFFFF);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0x16BFA0FF);
    }

    private static void drawInventoryHud(net.minecraft.client.gui.GuiGraphics g, Minecraft client) {
        int slot = 20;
        int gap = 3;
        int count = 9;
        int totalW = count * slot + (count - 1) * gap + 12;
        int x = (client.getWindow().getGuiScaledWidth() - totalW) / 2;
        int y = client.getWindow().getGuiScaledHeight() - 58;
        g.fill(x, y, x + totalW, y + slot + 12, 0xD910121E);
        g.fill(x, y, x + totalW, y + 1, 0xFFAA78FF);
        for (int i = 0; i < 9; i++) {
            int sx = x + 6 + i * (slot + gap);
            int sy = y + 6;
            g.fill(sx, sy, sx + slot, sy + slot, 0xFF292536);
            var stack = client.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 2, sy + 2);
                g.renderItemDecorations(client.font, stack, sx + 2, sy + 2);
            }
        }
    }

    private static int drawCard(net.minecraft.client.gui.GuiGraphics g, Minecraft client,
                                String label, String value, int x, int y, int color) {
        boolean compact = VoidModules.isEnabled("Compact HUD");
        int maxWidth = Math.max(client.font.width(label), client.font.width(value));
        int width = Math.min(210, maxWidth + 18);
        int height = compact ? 19 : 27;
        g.fill(x, y, x + width, y + height, 0xB910121E);
        g.fill(x, y, x + 2, y + height, color);
        if (compact) {
            g.drawString(client.font, label + "  " + value, x + 8, y + 5, color, false);
            return y + 22;
        }
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
