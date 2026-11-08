package com.voidclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public final class VoidClientClient implements ClientModInitializer {
    private boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = GLFW.glfwGetKey(
                client.getWindow().handle(),
                GLFW.GLFW_KEY_RIGHT_SHIFT
            ) == GLFW.GLFW_PRESS;

            if (down && !rightShiftWasDown && client.screen == null) {
                client.setScreen(new VoidClickScreen());
            }
            rightShiftWasDown = down;
        });
    }
}
