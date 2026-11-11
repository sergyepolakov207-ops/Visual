package com.voidclient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VoidModules {
    public enum Category { VISUALS, HUD, PVP }
    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();
    static {
        MODULES.put("Fullbright", false);
        MODULES.put("No View Bobbing", false);
        MODULES.put("Crosshair Dot", false);

        MODULES.put("FPS Counter", true);
        MODULES.put("Coordinates", true);
        MODULES.put("Health HUD", false);
        MODULES.put("Armor HUD", false);
        MODULES.put("Hunger HUD", false);
        MODULES.put("Ping HUD", false);
        MODULES.put("Inventory HUD", false);
        MODULES.put("Keystrokes", false);
        MODULES.put("Sprint Status", false);
        MODULES.put("Direction HUD", false);
        MODULES.put("Biome HUD", false);
        MODULES.put("Item Durability HUD", false);
        MODULES.put("Attack Cooldown", false);

        MODULES.put("Auto Sprint", false);
        MODULES.put("Low Health Alert", false);
    }

    private VoidModules() {}
    public static Map<String, Boolean> all() { return MODULES; }
    public static boolean isEnabled(String name) { return MODULES.getOrDefault(name, false); }
    public static void toggle(String name) {
        if (MODULES.containsKey(name)) MODULES.put(name, !MODULES.get(name));
    }
    public static Category categoryOf(String name) {
        return switch (name) {
            case "FPS Counter", "Coordinates", "Health HUD", "Armor HUD", "Hunger HUD",
                 "Ping HUD", "Inventory HUD", "Keystrokes", "Sprint Status", "Direction HUD",
                 "Biome HUD", "Item Durability HUD", "Attack Cooldown" -> Category.HUD;
            case "Auto Sprint", "Low Health Alert" -> Category.PVP;
            default -> Category.VISUALS;
        };
    }
}
