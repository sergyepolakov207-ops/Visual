package com.voidclient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VoidModules {
    public enum Category { VISUALS, HUD, PVP }

    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();
    private static float hudScale = 1.0F;

    public static float hudScale() { return hudScale; }
    public static void setHudScale(float value) { hudScale = Math.max(0.75F, Math.min(1.50F, value)); }

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

        // Legitimate PvP information and movement helpers
        MODULES.put("Auto Sprint", false);
        MODULES.put("Low Health Alert", false);
        MODULES.put("Ping HUD", false);
        MODULES.put("Attack Cooldown", false);
        MODULES.put("Target HUD", false);
        MODULES.put("Target Range", false);
        MODULES.put("Totem Counter", false);
        MODULES.put("Armor Durability", false);
        MODULES.put("Pearl Cooldown", false);
        MODULES.put("Shield Status", false);
    }

    private VoidModules() {}

    public static Map<String, Boolean> all() {
        return MODULES;
    }

    public static boolean isEnabled(String name) {
        return MODULES.getOrDefault(name, false);
    }

    public static void toggle(String name) {
        if (MODULES.containsKey(name)) MODULES.put(name, !MODULES.get(name));
    }

    public static Category categoryOf(String name) {
        return switch (name) {
            case "Fullbright", "No View Bobbing", "Crosshair Dot" -> Category.VISUALS;
            case "HUD", "FPS Counter", "Coordinates", "Health HUD", "Armor HUD",
                 "Hunger HUD", "Keystrokes", "Sprint Status", "Direction HUD",
                 "Biome HUD", "Item Durability HUD", "Inventory HUD", "Compact HUD" -> Category.HUD;
            case "Auto Sprint", "Low Health Alert", "Ping HUD", "Attack Cooldown",
                 "Target HUD", "Target Range", "Totem Counter", "Armor Durability",
                 "Pearl Cooldown", "Shield Status" -> Category.PVP;
            default -> Category.HUD;
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
            case "Auto Sprint" -> "Sprints while moving forward";
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
