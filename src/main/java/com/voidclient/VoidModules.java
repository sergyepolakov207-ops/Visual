package com.voidclient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import java.util.Properties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class VoidModules {
    public enum Category { COMBAT, MOVEMENT, RENDER, PLAYER, MISC }

    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();
    private static float hudScale = 1.0F;
    private static final Map<String, Integer> KEY_BINDS = new LinkedHashMap<>();
    private static final Map<String, Float> MODULE_VALUES = new LinkedHashMap<>();
    private static final Set<Integer> KEYS_DOWN = new HashSet<>();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("swiftvisuals.properties");

    public static float hudScale() { return hudScale; }
    public static void setHudScale(float value) {
        hudScale = Math.max(0.75F, Math.min(1.50F, value));
        saveSettings();
    }

    public static void loadSettings() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(CONFIG_FILE)) {
            properties.load(reader);
            hudScale = Math.max(0.75F, Math.min(1.50F,
                Float.parseFloat(properties.getProperty("hud.scale", "1.0"))));
            KEY_BINDS.clear();
            MODULE_VALUES.clear();
            for (String name : MODULES.keySet()) {
                String suffix = name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
                try {
                    int key = Integer.parseInt(properties.getProperty("bind." + suffix, "-1"));
                    if (key >= 0) KEY_BINDS.put(name, key);
                } catch (NumberFormatException ignored) { }
                try {
                    float value = Float.parseFloat(properties.getProperty("value." + suffix, "0.75"));
                    MODULE_VALUES.put(name, Math.max(0.0F, Math.min(1.0F, value)));
                } catch (NumberFormatException ignored) { }
            }
            for (String name : MODULES.keySet()) {
                String key = "module." + name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
                MODULES.put(name, Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(MODULES.get(name)))));
            }
        } catch (IOException | NumberFormatException ignored) {
            // Keep safe defaults if the config is missing or malformed.
        }
    }

    public static void saveSettings() {
        Properties properties = new Properties();
        properties.setProperty("hud.scale", Float.toString(hudScale));
        for (Map.Entry<String, Boolean> entry : MODULES.entrySet()) {
            String suffix = entry.getKey().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
            properties.setProperty("module." + suffix, Boolean.toString(entry.getValue()));
            if (KEY_BINDS.containsKey(entry.getKey())) properties.setProperty("bind." + suffix, Integer.toString(KEY_BINDS.get(entry.getKey())));
            if (MODULE_VALUES.containsKey(entry.getKey())) properties.setProperty("value." + suffix, Float.toString(MODULE_VALUES.get(entry.getKey())));
        }
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (var writer = Files.newBufferedWriter(CONFIG_FILE)) {
                properties.store(writer, "SWIFT VISUALS settings");
            }
        } catch (IOException ignored) {
            // A read-only config folder must not crash the client.
        }
    }

    static {
        // Visual quality of life
        MODULES.put("Fullbright", false);
        MODULES.put("No View Bobbing", false);
        MODULES.put("Crosshair Dot", false);

        // Master switch for the complete HUD overlay
        MODULES.put("HUD", true);

        // Optional HUD elements (individual settings remain available)
        MODULES.put("FPS Counter", true);
        MODULES.put("Coordinates", true);
        MODULES.put("Health HUD", false);
        MODULES.put("Armor HUD", false);
        MODULES.put("Hunger HUD", false);
        MODULES.put("Keystrokes", false);
        MODULES.put("Sprint Status", false);
        MODULES.put("Direction HUD", false);
        MODULES.put("Biome HUD", false);
        MODULES.put("Item Durability HUD", false);
        MODULES.put("Inventory HUD", false);
        MODULES.put("Compact HUD", false);
        MODULES.put("Status Bar", true);
        MODULES.put("Hotkeys HUD", true);
        MODULES.put("Potions HUD", true);
        MODULES.put("Player Card", true);
        MODULES.put("Online Players HUD", false);
        MODULES.put("Movement HUD", true);

        // Legitimate PvP information and movement helpers
        MODULES.put("Low Health Alert", false);
        MODULES.put("Ping HUD", false);
        MODULES.put("Attack Cooldown", false);
        MODULES.put("Target HUD", false);
        MODULES.put("Target Range", false);
        MODULES.put("Totem Counter", false);
        MODULES.put("Armor Durability", false);
        MODULES.put("Pearl Cooldown", false);
        MODULES.put("Shield Status", false);
        loadSettings();
    }

    private VoidModules() {}

    public static Map<String, Boolean> all() {
        return MODULES;
    }

    public static boolean isEnabled(String name) {
        return MODULES.getOrDefault(name, false);
    }

    public static void toggle(String name) {
        if (MODULES.containsKey(name)) {
            MODULES.put(name, !MODULES.get(name));
            saveSettings();
        }
    }

    public static Map<String, Integer> keyBinds() { return KEY_BINDS; }
    public static void setKeyBind(String name, Integer key) {
        if (!MODULES.containsKey(name)) return;
        if (key == null || key < 0) KEY_BINDS.remove(name); else KEY_BINDS.put(name, key);
        saveSettings();
    }
    public static float moduleValue(String name) { return MODULE_VALUES.getOrDefault(name, 0.75F); }
    public static void setModuleValue(String name, float value) {
        if (!MODULES.containsKey(name)) return;
        MODULE_VALUES.put(name, Math.max(0.0F, Math.min(1.0F, value)));
        saveSettings();
    }
    public static void handleKeyBinds(Minecraft client) {
        if (client == null || client.screen != null || client.getWindow() == null) { KEYS_DOWN.clear(); return; }
        long window = client.getWindow().handle();
        Set<Integer> nowDown = new HashSet<>();
        for (Map.Entry<String, Integer> entry : KEY_BINDS.entrySet()) {
            int key = entry.getValue();
            if (key >= 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                nowDown.add(key);
                if (!KEYS_DOWN.contains(key)) toggle(entry.getKey());
            }
        }
        KEYS_DOWN.clear();
        KEYS_DOWN.addAll(nowDown);
    }

    public static Category categoryOf(String name) {
        return switch (name) {
            case "Fullbright", "No View Bobbing", "Crosshair Dot" -> Category.RENDER;
            case "Keystrokes", "Sprint Status", "Movement HUD" -> Category.MOVEMENT;
            case "Low Health Alert", "Ping HUD", "Attack Cooldown", "Shield Status" -> Category.COMBAT;
            case "Health HUD", "Armor HUD", "Hunger HUD", "Item Durability HUD",
                 "Player Card", "Totem Counter", "Armor Durability", "Pearl Cooldown" -> Category.PLAYER;
            case "HUD", "Inventory HUD", "Compact HUD", "Hotkeys HUD", "Potions HUD",
                 "Direction HUD", "FPS Counter", "Coordinates", "Biome HUD",
                 "Online Players HUD", "Status Bar" -> Category.MISC;
            default -> Category.MISC;
        };
    }

    public static String descriptionOf(String name) {
        return switch (name) {
            case "Fullbright" -> "Brightens dark areas";
            case "No View Bobbing" -> "Keeps the camera steady";
            case "Crosshair Dot" -> "Adds a precise center dot";
            case "HUD" -> "Master switch for all on-screen information";
            case "FPS Counter" -> "Shows current frame rate";
            case "Coordinates" -> "Displays your XYZ position";
            case "Health HUD" -> "Shows your current health";
            case "Armor HUD" -> "Displays equipped armor points";
            case "Hunger HUD" -> "Shows your food level";
            case "Keystrokes" -> "Displays movement key states";
            case "Sprint Status" -> "Shows sprint state";
            case "Direction HUD" -> "Displays your facing direction";
            case "Biome HUD" -> "Shows the current biome";
            case "Item Durability HUD" -> "Main-hand item durability";
            case "Inventory HUD" -> "Shows your hotbar contents";
            case "Compact HUD" -> "Uses a smaller information layout";
            case "Status Bar" -> "Shows FPS, ping, coordinates and session info";
            case "Hotkeys HUD" -> "Displays movement and action key states";
            case "Potions HUD" -> "Lists active potion effects and durations";
            case "Player Card" -> "Shows your name, health and armor";
            case "Online Players HUD" -> "Shows players currently in the tab list";
            case "Movement HUD" -> "Displays horizontal movement speed";
            case "Low Health Alert" -> "Warns when health is critical";
            case "Ping HUD" -> "Shows your server latency";
            case "Attack Cooldown" -> "Shows attack recharge progress";
            case "Target HUD" -> "Shows the player under your crosshair";
            case "Target Range" -> "Displays distance to your target";
            case "Totem Counter" -> "Counts totems in your inventory";
            case "Armor Durability" -> "Shows durability of worn armor";
            case "Pearl Cooldown" -> "Shows ender pearl cooldown state";
            case "Shield Status" -> "Shows whether your shield is active";
            default -> "SWIFT VISUALS module";
        };
    }
}
