package com.voidclient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VoidModules {
    public enum Category { VISUALS, HUD, PVP }
    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();
    static {
        MODULES.put("Fullbright", false);
        MODULES.put("No View Bobbing", false);
        MODULES.put("FPS Counter", true);
        MODULES.put("Coordinates", true);
        MODULES.put("Clock", false);
        MODULES.put("Keystrokes", false);
        MODULES.put("Sprint Status", false);
        MODULES.put("Direction HUD", false);
        MODULES.put("Health HUD", false);
        MODULES.put("Hunger HUD", false);
        MODULES.put("Armor HUD", false);
        MODULES.put("Ping HUD", false);
        MODULES.put("Biome HUD", false);
        MODULES.put("Auto Sprint", false);
        MODULES.put("Attack Cooldown", false);
    }
    private VoidModules() {}
    public static Map<String, Boolean> all() { return MODULES; }
    public static boolean isEnabled(String name) { return MODULES.getOrDefault(name, false); }
    public static void toggle(String name) {
        if (MODULES.containsKey(name)) MODULES.put(name, !MODULES.get(name));
    }
    public static Category categoryOf(String name) {
        return switch (name) {
            case "FPS Counter", "Coordinates", "Clock", "Keystrokes", "Sprint Status",
                 "Direction HUD", "Health HUD", "Hunger HUD", "Armor HUD", "Ping HUD", "Biome HUD" -> Category.HUD;
            case "Auto Sprint", "Attack Cooldown" -> Category.PVP;
            default -> Category.VISUALS;
        };
    }
}
