package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.settings.ColorSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class CustomFog extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", 0xFF9B6BFF));
    private final NumSetting density = add(new NumSetting("Density", 70, 10, 220, 5));
    private final NumSetting alpha = add(new NumSetting("Alpha", 120, 20, 220, 5));

    public CustomFog() {
        super("CustomFog", "Цветной туман по экрану", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        int c = color.get();
        int steps = 10, size = density.i();
        for (int i = 0; i < steps; i++) {
            int a = (int) (alpha.i() * (1 - i / (float) steps) * (1 - i / (float) steps));
            int col = Theme.withAlpha(c, a);
            int d = size * i / steps, d2 = size * (i + 1) / steps;
            ctx.fill(0, d, w, d2, col);
            ctx.fill(0, h - d2, w, h - d, col);
            ctx.fill(d, 0, d2, h, col);
            ctx.fill(w - d2, 0, w - d, h, col);
        }
    }
}
