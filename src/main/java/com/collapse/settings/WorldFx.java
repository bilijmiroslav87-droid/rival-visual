package com.collapse.settings;

/** Общие значения «мировых» визуалов: читаются миксинами рендера (туман, небо). */
public final class WorldFx {
    private WorldFx() {}

    // туман
    public static boolean fogOn = false;
    public static float fogStart = 20f;     // блоков
    public static float fogEnd = 120f;      // блоков
    public static int fogColor = 0xF65C52;  // RGB
    public static boolean fogRainbow = false;

    // небо / облака
    public static boolean skyOn = false;
    public static int skyColor = 0x7A5CFF;

    public static int fogRgb() {
        if (!fogRainbow) return fogColor;
        float h = (System.currentTimeMillis() % 6000L) / 6000f;
        return java.awt.Color.HSBtoRGB(h, 0.55f, 1f) & 0xFFFFFF;
    }
}
