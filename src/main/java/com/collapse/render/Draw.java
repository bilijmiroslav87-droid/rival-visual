package com.collapse.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Рисование антиалиасных скруглённых фигур через 9-slice текстуры (LINEAR),
 * поэтому углы гладкие, а не "лесенкой". Текстуры белые, цвет задаётся тинтом.
 */
public final class Draw {
    private Draw() {}

    private static final Identifier RR = Identifier.of("collapse-client", "ui/rr.png");
    private static final Identifier RING = Identifier.of("collapse-client", "ui/rr_ring.png");
    private static final Identifier CIRCLE = Identifier.of("collapse-client", "ui/circle.png");
    private static final Identifier SHADOW = Identifier.of("collapse-client", "ui/shadow.png");
    private static final Identifier GLOW = Identifier.of("collapse-client", "ui/glow.png");

    /** Заливка скруглённого прямоугольника. */
    public static void roundRect(DrawContext ctx, float fx, float fy, float fw, float fh, float radius, int color) {
        if (((color >>> 24) & 0xFF) == 0) return;
        nine(ctx, RR, 64, 24, fx, fy, fw, fh, radius, color);
    }

    /** Контур (тонкая обводка) скруглённого прямоугольника. */
    public static void ring(DrawContext ctx, float fx, float fy, float fw, float fh, float radius, int color) {
        if (((color >>> 24) & 0xFF) == 0) return;
        nine(ctx, RING, 64, 24, fx, fy, fw, fh, radius, color);
    }

    public static void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        ring(ctx, x, y, w, h, r, color);
    }

    public static void circle(DrawContext ctx, float cx, float cy, float d, int color) {
        if (((color >>> 24) & 0xFF) == 0) return;
        int x = Math.round(cx), y = Math.round(cy), s = Math.max(1, Math.round(d));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, CIRCLE, x, y, 0, 0, s, s, 64, 64, 64, 64, color);
    }

    /** Мягкая тень вокруг прямоугольника. */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, int spread) {
        shadowColor(ctx, x, y + 3, w, h, spread, 0x99000000);
    }

    public static void shadowColor(DrawContext ctx, float x, float y, float w, float h, int spread, int color) {
        nineBlur(ctx, SHADOW, x - spread, y - spread, w + spread * 2, h + spread * 2, color);
    }

    /** Цветное свечение (для акцента). */
    public static void glow(DrawContext ctx, float x, float y, float w, float h, int spread, int color) {
        nineBlur(ctx, GLOW, x - spread, y - spread, w + spread * 2, h + spread * 2, color);
    }

    private static void nineBlur(DrawContext ctx, Identifier tex, float fx, float fy, float fw, float fh, int color) {
        if (((color >>> 24) & 0xFF) == 0) return;
        nine(ctx, tex, 128, 52, fx, fy, fw, fh, 52, color);
    }

    /** Градиент сверху вниз внутри скруглённого прямоугольника (верх и низ скруглены, середина градиентом). */
    public static void roundGradientV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        roundRect(ctx, x, y, w, h, r, top);
        float half = h * 0.5f;
        roundRect(ctx, x, y + half, w, half, r, bottom);
        int rr = Math.round(r);
        int y0 = Math.round(y + Math.min(half, rr)), y1 = Math.round(y + h - Math.min(half, rr));
        if (y1 > y0) ctx.fillGradient(Math.round(x), y0, Math.round(x + w), y1, top, bottom);
    }

    public static void gradientV(DrawContext ctx, int x, int y, int w, int h, int top, int bottom) {
        ctx.fillGradient(x, y, x + w, y + h, top, bottom);
    }

    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        int segs = Math.max(2, Math.min(32, (int) (w / 4)));
        float sw = w / segs;
        for (int i = 0; i < segs; i++) {
            int c = com.collapse.util.Theme.mix(left, right, (i + 0.5f) / segs);
            int a = (int) (((left >>> 24) & 0xFF) + (((right >>> 24) & 0xFF) - ((left >>> 24) & 0xFF)) * ((i + 0.5f) / segs));
            c = (a << 24) | (c & 0xFFFFFF);
            ctx.fill(Math.round(x + i * sw), Math.round(y), Math.round(x + (i + 1) * sw), Math.round(y + h), c);
        }
    }

    public static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int rgb(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    // ---- 9-slice ----
    private static void nine(DrawContext ctx, Identifier tex, int ts, int tc, float fx, float fy, float fw, float fh, float radius, int color) {
        int x = Math.round(fx), y = Math.round(fy), w = Math.round(fw), h = Math.round(fh);
        if (w <= 0 || h <= 0) return;
        int c = Math.max(1, Math.min(Math.round(radius), Math.min(w, h) / 2));
        int mid = ts - tc * 2; // середина текстуры
        // углы
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, c, c, tc, tc, ts, ts, color);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + w - c, y, ts - tc, 0, c, c, tc, tc, ts, ts, color);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y + h - c, 0, ts - tc, c, c, tc, tc, ts, ts, color);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + w - c, y + h - c, ts - tc, ts - tc, c, c, tc, tc, ts, ts, color);
        int iw = w - c * 2, ih = h - c * 2;
        // края
        if (iw > 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + c, y, tc, 0, iw, c, mid, tc, ts, ts, color);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + c, y + h - c, tc, ts - tc, iw, c, mid, tc, ts, ts, color);
        }
        if (ih > 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y + c, 0, tc, c, ih, tc, mid, ts, ts, color);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + w - c, y + c, ts - tc, tc, c, ih, tc, mid, ts, ts, color);
        }
        // центр
        if (iw > 0 && ih > 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + c, y + c, tc, tc, iw, ih, mid, mid, ts, ts, color);
        }
    }
}
