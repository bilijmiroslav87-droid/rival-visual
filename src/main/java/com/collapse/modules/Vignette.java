package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.client.gui.DrawContext;

public class Vignette extends Module {
    private final NumSetting power = add(new NumSetting("Power", 140, 20, 230, 10));

    public Vignette() {
        super("Vignette", "Затемнение по краям экрана", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        int size = Math.min(w, h) / 3, steps = 12;
        for (int i = 0; i < steps; i++) {
            float t = 1 - i / (float) steps;
            int a = (int) (power.i() * t * t);
            int col = a << 24;
            int d = size * i / steps, d2 = size * (i + 1) / steps;
            ctx.fill(0, d, w, d2, col);
            ctx.fill(0, h - d2, w, h - d, col);
            ctx.fill(d, 0, d2, h, col);
            ctx.fill(w - d2, 0, w - d, h, col);
        }
    }
}
