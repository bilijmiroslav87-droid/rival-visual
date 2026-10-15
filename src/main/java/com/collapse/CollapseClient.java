package com.collapse;

import com.collapse.gui.ClickMenuScreen;
import com.collapse.hud.CollapseHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollapseClient implements ClientModInitializer {
    public static final String MOD_ID = "collapse-client";
    public static final String NAME = "Collapse Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.collapse.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.collapse.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                while (menuKey.wasPressed()) {
                    if (client.currentScreen == null) client.setScreen(ClickMenuScreen.create());
                }
                Module zoom = ModuleManager.byName("Zoom");
                if (zoom != null && client.currentScreen == null) {
                    if (zoomKey.isPressed()) { if (!zoom.isEnabled()) zoom.setEnabled(true); }
                    else if (zoom.isEnabled()) zoom.setEnabled(false);
                }
            } catch (Throwable t) {
                LOGGER.error("Key handler error", t);
            }
        });

        CollapseHud.register();
        LOGGER.info("{} loaded, modules: {}", NAME, ModuleManager.MODULES.size());
    }
}
