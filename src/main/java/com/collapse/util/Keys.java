package com.collapse.util;

import net.minecraft.client.MinecraftClient;

/** Ввод движения напрямую из клавиш (надёжнее, чем playerInput, который обновляется позже тика). */
public final class Keys {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Keys() {}

    public static boolean forward() { return mc.options.forwardKey.isPressed(); }
    public static boolean back() { return mc.options.backKey.isPressed(); }
    public static boolean left() { return mc.options.leftKey.isPressed(); }
    public static boolean right() { return mc.options.rightKey.isPressed(); }
    public static boolean jump() { return mc.options.jumpKey.isPressed(); }
    public static boolean sneak() { return mc.options.sneakKey.isPressed(); }
    public static boolean moving() { return forward() || back() || left() || right(); }
}
