package com.collapse;

import com.collapse.config.Config;
import com.collapse.gui.ClickMenuScreen;
import com.collapse.hud.CollapseHud;
import com.collapse.render.Fonts;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CollapseClient implements ClientModInitializer {
    public static final String MOD_ID = "collapse-client";
    public static final String NAME = "Collapse Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;
    private final Map<Integer, Boolean> held = new HashMap<>();

    @Override
    public void onInitializeClient() {
        try { Fonts.init(); } catch (Throwable t) { LOGGER.error("Fonts init failed", t); }
        ModuleManager.init();
        try { Config.load(); } catch (Throwable t) { LOGGER.error("Config load failed", t); }
        // Водянка и уведомления включены по умолчанию
        if (!java.nio.file.Files.exists(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("collapse-client.properties"))) {
            for (String n : new String[]{"Watermark", "Notifications"}) {
                Module m = ModuleManager.byName(n);
                if (m != null) m.setEnabled(true);
            }
        }

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
                // бинды модулей
                if (client.currentScreen == null && client.getWindow() != null) {
                    long handle = client.getWindow().getHandle();
                    for (Module m : ModuleManager.MODULES) {
                        int k = m.getBind();
                        if (k <= 0) continue;
                        boolean down = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
                        boolean was = held.getOrDefault(k, false);
                        if (down && !was) m.toggle();
                        held.put(k, down);
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Key handler error", t);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
        CollapseHud.register();
        LOGGER.info("=== {} v0.5.0 loaded ===", NAME);
        LOGGER.info("modules: {}", ModuleManager.MODULES.size());
        LOGGER.info("fonts ok: medium={}, bold={}", Fonts.MEDIUM != null, Fonts.BOLD != null);
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            int t = 0;
            @Override public void onEndTick(net.minecraft.client.MinecraftClient c) {
                if (++t == 200 && c.player != null) {
                    int on = 0; for (Module m : ModuleManager.MODULES) if (m.isEnabled()) on++;
                    LOGGER.info("selfcheck: in-world, enabled modules={}, menuKey={}", on, menuKey.getBoundKeyTranslationKey());
                }
            }
        });
    }
}
