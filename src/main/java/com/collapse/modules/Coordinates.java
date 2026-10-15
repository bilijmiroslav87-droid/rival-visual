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
        String t = String.format("XYZ  %.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        float tw = Fonts.MEDIUM.width(t, 9.5f);
        Draw.roundRect(ctx, 4, h - 18, tw + 12, 14, 4, 0xB0101014);
        Fonts.MEDIUM.draw(ctx, t, 10, h - 15, 9.5f, Theme.accent());
    }
}
