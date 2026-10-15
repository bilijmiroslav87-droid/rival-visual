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
 * Свой шрифт Inter. Атлас PNG заранее сгенерирован при сборке (без AWT, чтобы работало на Android/FCL).
 * Рисуется обычным drawTexture с цветом, никаких шейдеров.
 */
public final class Fonts {
    public static Fonts MEDIUM;
    public static Fonts BOLD;

    private static final class Glyph {
        int x, y, w, h, adv;
    }

    private final Identifier texture;
    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private int lineHeight = 14;
    private int atlasW = 512, atlasH = 512;
    private final float baseSize = 28f;
    private boolean ok = false;

    private Fonts(String name) {
        this.texture = Identifier.of("collapse-client", "font/" + name + ".png");
        try (InputStream in = Fonts.class.getResourceAsStream("/assets/collapse-client/font/" + name + ".txt")) {
            if (in == null) throw new IllegalStateException("font meta missing: " + name);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.trim().split(" ");
                if (p.length == 0) continue;
                if (p[0].equals("H")) {
                    lineHeight = Integer.parseInt(p[1]);
                    atlasW = Integer.parseInt(p[3]);
                    atlasH = Integer.parseInt(p[4]);
                } else if (p.length >= 6) {
                    Glyph g = new Glyph();
                    g.x = Integer.parseInt(p[1]);
                    g.y = Integer.parseInt(p[2]);
                    g.w = Integer.parseInt(p[3]);
                    g.h = Integer.parseInt(p[4]);
                    g.adv = Integer.parseInt(p[5]);
                    glyphs.put((char) Integer.parseInt(p[0]), g);
                }
            }
            ok = true;
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Font load failed: {}", name, t);
        }
    }

    public static void init() {
        MEDIUM = new Fonts("inter_m");
        BOLD = new Fonts("inter_b");
    }

    /** Ширина строки при заданной высоте шрифта (в пикселях GUI). */
    public float width(String s, float size) {
        if (!ok) return MinecraftClient.getInstance().textRenderer.getWidth(s);
        float scale = size / baseSize;
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = glyphs.get(s.charAt(i));
            if (g == null) g = glyphs.get('?');
            if (g != null) w += g.adv * scale;
        }
        return w;
    }

    public float height(float size) { return size; }

    public void draw(DrawContext ctx, String s, float x, float y, float size, int color) {
        if (!ok) {
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, s, (int) x, (int) y, color);
            return;
        }
        float scale = size / baseSize;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        float cx = 0;
        int pad = 2;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = glyphs.get(s.charAt(i));
            if (g == null) g = glyphs.get('?');
            if (g == null) continue;
            if (s.charAt(i) != ' ') {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture,
                        Math.round(cx) - pad, 0, g.x, g.y, g.w, g.h, g.w, g.h, atlasW, atlasH, color);
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
