package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class Coordinates extends Module {
    public Coordinates() {
        super("Coordinates", "Координаты на экране", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        String[] labels = {"X", "Y", "Z"};
        String[] vals = {
                String.valueOf((int) Math.floor(mc.player.getX())),
                String.valueOf((int) Math.floor(mc.player.getY())),
                String.valueOf((int) Math.floor(mc.player.getZ()))};
        float size = 8.5f, gap = 9;
        float total = 14;
        for (int i = 0; i < 3; i++) total += Fonts.BOLD.width(labels[i], size) + 4 + Fonts.BOLD.width(vals[i], size) + (i < 2 ? gap : 0);
        float x = 6, y = h - 18, hh = 14;
        y = h - 12 - hh;
        Draw.shadow(ctx, x, y, total, hh, 7, 8);
        Draw.roundRect(ctx, x, y, total, hh, 7, com.collapse.hud.CollapseHud.GLASS);
        Draw.ring(ctx, x, y, total, hh, 7, 0x26FFFFFF);
        Draw.gradientH(ctx, x + 6, y + hh - 1.4f, total - 12, 1.2f, Draw.alpha(Theme.accent(), 0.9f), Draw.alpha(Theme.accent2(), 0.9f));
        float cx = x + 7;
        for (int i = 0; i < 3; i++) {
            Fonts.BOLD.draw(ctx, labels[i], cx, y + 3.5f, size, Theme.accent());
            cx += Fonts.BOLD.width(labels[i], size) + 4;
            Fonts.MEDIUM.draw(ctx, vals[i], cx, y + 3.5f, size, 0xFFFFFFFF);
            cx += Fonts.BOLD.width(vals[i], size) + gap;
        }
    }
}
