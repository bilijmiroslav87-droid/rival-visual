package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.client.gui.DrawContext;

public class Coordinates extends Module {
    public Coordinates() {
        super("Coordinates", "Координаты на экране", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        String t = String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        ctx.drawTextWithShadow(mc.textRenderer, t, 4, h - 12, 0xFFFFFFFF);
    }
}
