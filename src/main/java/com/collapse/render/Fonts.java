package com.collapse.render;

import com.collapse.CollapseClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Шрифты: Manrope (текст, Medium/Bold) и Unbounded (заголовки).
 * Каждая гарнитура это один атлас 64px, суперсэмплированный и сглаженный (LINEAR),
 * масштабируется под нужный размер. Поддерживает кириллицу.
 */
public final class Fonts {
    public static Fonts MEDIUM;
    public static Fonts BOLD;
    public static Fonts HEAD;

    private static final class Glyph { int x, y, w, h, ox; float adv; }

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final Identifier tex;
    private int aw = 1024, ah = 1024, base = 64;
    private float capTop = 22f, cellPad = 3f;
    private boolean ok = false;
    private final Map<String, Float> widthCache = new HashMap<>();

    private Fonts(String name) {
        tex = Identifier.of("collapse-client", "font/" + name + ".png");
        try (InputStream in = Fonts.class.getResourceAsStream("/assets/collapse-client/font/" + name + ".txt")) {
            if (in == null) throw new IllegalStateException("missing " + name);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.trim().split(" ");
                if (p[0].equals("H")) {
                    base = Integer.parseInt(p[2]);
                    aw = Integer.parseInt(p[3]);
                    ah = Integer.parseInt(p[4]);
                    capTop = Float.parseFloat(p[5].replace(',', '.'));
                    cellPad = Float.parseFloat(p[6]);
                } else if (p.length >= 7) {
                    Glyph g = new Glyph();
                    g.x = Integer.parseInt(p[1]); g.y = Integer.parseInt(p[2]);
                    g.w = Integer.parseInt(p[3]); g.h = Integer.parseInt(p[4]);
                    g.adv = Float.parseFloat(p[5].replace(',', '.'));
                    g.ox = Integer.parseInt(p[6]);
                    glyphs.put((char) Integer.parseInt(p[0]), g);
                }
            }
            ok = true;
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Font atlas failed: {}", name, t);
        }
    }

    public static void init() {
        MEDIUM = new Fonts("text_m");
        BOLD = new Fonts("text_b");
        HEAD = new Fonts("head");
    }

    private Glyph glyph(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) g = glyphs.get('?');
        return g;
    }

    public float width(String s, float size) {
        if (!ok) return MinecraftClient.getInstance().textRenderer.getWidth(s);
        float scale = size / base;
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = glyph(s.charAt(i));
            if (g != null) w += g.adv * scale;
        }
        return w;
    }

    /** Высота строки для вертикального выравнивания. */
    public float height(float size) { return size; }

    public void draw(DrawContext ctx, String s, float x, float y, float size, int color) {
        if (!ok) {
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, s, (int) x, (int) y, color);
            return;
        }
        // size = em в GUI-пикселях; атлас построен на base px
        float scale = size / base;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        m.translate(0, -(capTop + cellPad));
        float cx = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            Glyph g = glyph(ch);
            if (g == null) continue;
            if (ch != ' ') {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex,
                        Math.round(cx) + g.ox, 0, g.x, g.y, g.w, g.h, g.w, g.h, aw, ah, color);
            }
            cx += g.adv;
        }
        m.popMatrix();
    }

    public void drawCentered(DrawContext ctx, String s, float cx, float y, float size, int color) {
        draw(ctx, s, cx - width(s, size) / 2f, y, size, color);
    }

    public void drawRight(DrawContext ctx, String s, float rx, float y, float size, int color) {
        draw(ctx, s, rx - width(s, size), y, size, color);
    }

    /** Обрезка строки по ширине с многоточием. */
    public String fit(String s, float maxW, float size) {
        if (width(s, size) <= maxW) return s;
        while (s.length() > 1 && width(s + "…", size) > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
