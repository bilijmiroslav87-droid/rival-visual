package com.collapse.render;

import net.minecraft.client.gui.DrawContext;

/** Скруглённые фигуры без шейдеров: строим из горизонтальных строк. */
public final class Draw {
    private Draw() {}

    public static void roundRect(DrawContext ctx, float fx, float fy, float fw, float fh, float radius, int color) {
        int x = Math.round(fx), y = Math.round(fy), w = Math.round(fw), h = Math.round(fh);
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min((int) radius, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, color); return; }

        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    public static void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        roundRect(ctx, x - 1, y - 1, w + 2, h + 2, r + 1, color);
    }

    public static void gradientV(DrawContext ctx, int x, int y, int w, int h, int top, int bottom) {
        ctx.fillGradient(x, y, x + w, y + h, top, bottom);
    }

    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, int layers) {
        for (int i = layers; i >= 1; i--) {
            int a = (int) (22 * (1 - i / (float) (layers + 1)));
            roundRect(ctx, x - i, y - i + 2, w + i * 2, h + i * 2, r + i, a << 24);
        }
    }

    public static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }
}
