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
 * Шрифт Inter. Несколько заранее отрисованных размеров (10/12/14/18), выбирается ближайший,
 * текстуры с blur=true (LINEAR), поэтому текст гладкий, а не пиксельный.
 */
public final class Fonts {
    public static Fonts MEDIUM;
    public static Fonts BOLD;

    private static final int[] SIZES = {10, 12, 14, 18};

    private static final class Glyph { int x, y, w, h, adv; }

    private static final class Atlas {
        Identifier tex;
        final Map<Character, Glyph> glyphs = new HashMap<>();
        int base, w = 512, h = 512;
        boolean ok;
    }

    private final Atlas[] atlases = new Atlas[SIZES.length];

    private Fonts(String prefix) {
        for (int i = 0; i < SIZES.length; i++) {
            Atlas a = new Atlas();
            a.base = SIZES[i] * 2;
            String name = prefix + "_" + SIZES[i];
            a.tex = Identifier.of("collapse-client", "font/" + name + ".png");
            try (InputStream in = Fonts.class.getResourceAsStream("/assets/collapse-client/font/" + name + ".txt")) {
                if (in == null) throw new IllegalStateException("missing " + name);
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    String[] p = line.trim().split(" ");
                    if (p[0].equals("H")) {
                        a.w = Integer.parseInt(p[3]);
                        a.h = Integer.parseInt(p[4]);
                    } else if (p.length >= 6) {
                        Glyph g = new Glyph();
                        g.x = Integer.parseInt(p[1]); g.y = Integer.parseInt(p[2]);
                        g.w = Integer.parseInt(p[3]); g.h = Integer.parseInt(p[4]);
                        g.adv = Integer.parseInt(p[5]);
                        a.glyphs.put((char) Integer.parseInt(p[0]), g);
                    }
                }
                a.ok = true;
            } catch (Throwable t) {
                CollapseClient.LOGGER.error("Font atlas failed: {}", name, t);
            }
            atlases[i] = a;
        }
    }

    public static void init() {
        MEDIUM = new Fonts("inter_m");
        BOLD = new Fonts("inter_b");
    }

    private Atlas pick(float size) {
        Atlas best = atlases[0];
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < SIZES.length; i++) {
            float d = Math.abs(SIZES[i] - size);
            if (d < bd && atlases[i].ok) { bd = d; best = atlases[i]; }
        }
        return best;
    }

    public float width(String s, float size) {
        Atlas a = pick(size);
        if (!a.ok) return MinecraftClient.getInstance().textRenderer.getWidth(s);
        float scale = size / a.base;
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = a.glyphs.get(s.charAt(i));
            if (g == null) g = a.glyphs.get('?');
            if (g != null) w += g.adv * scale;
        }
        return w;
    }

    public void draw(DrawContext ctx, String s, float x, float y, float size, int color) {
        Atlas a = pick(size);
        if (!a.ok) {
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, s, (int) x, (int) y, color);
            return;
        }
        float scale = size / a.base;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        float cx = 0;
        int pad = 2;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = a.glyphs.get(s.charAt(i));
            if (g == null) g = a.glyphs.get('?');
            if (g == null) continue;
            if (s.charAt(i) != ' ') {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, a.tex,
                        Math.round(cx) - pad, 0, g.x, g.y, g.w, g.h, g.w, g.h, a.w, a.h, color);
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
}
