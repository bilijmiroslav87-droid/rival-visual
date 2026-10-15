package com.collapse.util;

public final class Theme {
    public static final String[] NAMES = {"Ocean", "Violet", "Blood", "Toxic", "Candy", "Sunset"};
    private static final int[] ACCENT = {0xFF4A9EFF, 0xFF9B6BFF, 0xFFFF4D5E, 0xFF3DDC84, 0xFFFF6BC1, 0xFFFF9A3D};
    private static final int[] SECOND = {0xFF7A5CFF, 0xFFFF5CA8, 0xFFFF9A3D, 0xFF3DDCD0, 0xFFA05CFF, 0xFFFF4D5E};
    private static int index = 0;

    private Theme() {}

    public static int accent() { return ACCENT[index]; }
    public static String name() { return NAMES[index]; }
    public static void next() { index = (index + 1) % ACCENT.length; }
    public static int index() { return index; }
    public static void setIndex(int i) { if (i >= 0 && i < ACCENT.length) index = i; }
    public static int accent2() { return SECOND[index]; }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    public static int rainbow(double offset) {
        double t = (System.currentTimeMillis() % 4000L) / 4000.0 + offset;
        int rgb = java.awt.Color.HSBtoRGB((float) (t % 1.0), 0.55f, 1.0f);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
