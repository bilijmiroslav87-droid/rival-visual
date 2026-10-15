package com.collapse.util;

/** Цветовые темы клиента: акцент + второй цвет для градиентов. */
public final class Theme {
    public static final String[] NAMES = {"Aurora", "Violet", "Crimson", "Mint", "Candy", "Sunset", "Ice", "Mono", "Neon", "Ocean", "Gold", "Sakura"};
    private static final int[] ACCENT = {0xFF5B8CFF, 0xFFA66BFF, 0xFFFF4D6D, 0xFF2FE3A0, 0xFFFF6BC1, 0xFFFF9A3D, 0xFF6FE7FF, 0xFFE8E8F0, 0xFF39FF88, 0xFF1AA6FF, 0xFFFFC53D, 0xFFFF8FB8};
    private static final int[] SECOND = {0xFFB06BFF, 0xFFFF5CA8, 0xFFFF9A3D, 0xFF3DB8FF, 0xFFA05CFF, 0xFFFF4D6D, 0xFF5B8CFF, 0xFF8A8A9A, 0xFF00E5FF, 0xFF6C5CFF, 0xFFFF7A1A, 0xFFB98CFF};
    private static int index = 0;
    /** Если true, акцент плавно переливается между двумя цветами темы. */
    public static boolean animated = true;

    private Theme() {}

    public static int accent() {
        if (!animated) return ACCENT[index];
        double t = (System.currentTimeMillis() % 6000L) / 6000.0;
        float k = (float) (0.5 - 0.5 * Math.cos(t * Math.PI * 2)) * 0.45f;
        return mix(ACCENT[index], SECOND[index], k);
    }
    public static int accent2() { return SECOND[index]; }
    public static int base() { return ACCENT[index]; }
    public static String name() { return NAMES[index]; }
    public static void next() { index = (index + 1) % ACCENT.length; }
    public static void prev() { index = (index + ACCENT.length - 1) % ACCENT.length; }
    public static int index() { return index; }
    public static int count() { return ACCENT.length; }
    public static int accentOf(int i) { return ACCENT[i]; }
    public static int secondOf(int i) { return SECOND[i]; }
    public static void setIndex(int i) { if (i >= 0 && i < ACCENT.length) index = i; }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    public static int rainbow(double offset) {
        double t = (System.currentTimeMillis() % 4000L) / 4000.0 + offset;
        int rgb = java.awt.Color.HSBtoRGB((float) (t % 1.0), 0.55f, 1.0f);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (a >>> 24) & 255, ba = (b >>> 24) & 255;
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int al = (int) (aa + (ba - aa) * t);
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (Math.max(al, 0) << 24) | (r << 16) | (g << 8) | bl;
    }
}
