package com.collapse.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Шрифты интерфейса через ванильный TTF-провайдер (font/*.json): Poppins и Frick, как в Glass Menu.
 * Размер задаётся масштабом матрицы, поэтому текст остаётся чётким (oversample 4 в json).
 */
public final class Fonts {
    public static Fonts MEDIUM, BOLD, HEAD;

    private static final float BASE = 9f; // «родной» размер шрифта в json

    private final Style style;
    private final float base;
    private final float yFix; // поправка по вертикали относительно прежней сетки
    private final java.util.Map<String, Float> cache = new java.util.HashMap<>();

    private Fonts(String fontId, float base, float yFix) {
        this.style = Style.EMPTY.withFont(new net.minecraft.text.StyleSpriteSource.Font(Identifier.of("collapse-client", fontId)));
        this.base = base;
        this.yFix = yFix;
    }

    public static void init() {
        MEDIUM = new Fonts("ui", 9f, -1.0f);
        BOLD = new Fonts("ui_medium", 10f, -1.0f);
        HEAD = new Fonts("ui_display", 13f, -1.5f);
    }

    /** Шрифт считается загруженным, если его ширина текста отличается от ванильной. */
    public boolean isOk() {
        try {
            float mine = tr().getWidth(text("Collapse WiP 123"));
            float vanilla = tr().getWidth("Collapse WiP 123");
            return mine > 0 && Math.abs(mine - vanilla) > 0.5f;
        } catch (Throwable t) { return false; }
    }
    public int glyphCount() { return isOk() ? 1 : 0; }

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    private Text text(String s) { return Text.literal(s).setStyle(style); }

    public float width(String s, float size) {
        if (s == null || s.isEmpty()) return 0;
        float w = tr().getWidth(text(s));
        return w * size / base;
    }

    public float height(float size) { return size; }

    public void draw(DrawContext ctx, String s, float x, float y, float size, int color) {
        if (s == null || s.isEmpty()) return;
        float scale = size / base;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y + yFix * scale + (size - base) * 0.0f);
        m.scale(scale, scale);
        ctx.drawText(tr(), text(s), 0, 0, color, false);
        m.popMatrix();
    }

    public void drawCentered(DrawContext ctx, String s, float cx, float y, float size, int color) {
        draw(ctx, s, cx - width(s, size) / 2f, y, size, color);
    }

    public void drawRight(DrawContext ctx, String s, float rx, float y, float size, int color) {
        draw(ctx, s, rx - width(s, size), y, size, color);
    }

    public String fit(String s, float maxW, float size) {
        if (width(s, size) <= maxW) return s;
        while (s.length() > 1 && width(s + "…", size) > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
