package com.voidclient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VoidModules {
    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();

    static {
        MODULES.put("FPS Boost", false);
        MODULES.put("Fullbright", false);
        MODULES.put("Particle Reducer", false);
        MODULES.put("Coordinates HUD", false);
        MODULES.put("Custom Crosshair", false);
        MODULES.put("Entity ESP", false);
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
        }
    }
}
