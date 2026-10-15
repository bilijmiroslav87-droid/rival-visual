package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

public class FullStatus extends Module {
    public FullStatus() {
        super("Potions", "Активные эффекты на экране", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        float y = h / 2f - 30;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
            int sec = e.getDuration() / 20;
            String t = String.format("%d:%02d", sec / 60, sec % 60);
            float nw = Fonts.MEDIUM.width(name, 9f), tw = Fonts.MEDIUM.width(t, 9f);
            Draw.roundRect(ctx, 4, y, nw + tw + 22, 14, 6, com.collapse.hud.CollapseHud.GLASS);
            Draw.ring(ctx, 4, y, nw + tw + 22, 14, 6, 0x22FFFFFF);
            Draw.roundRect(ctx, 5, y + 2.5f, 2, 9, 1, Theme.accent());
            Fonts.MEDIUM.draw(ctx, name, 11, y + 3, 9f, 0xFFFFFFFF);
            Fonts.MEDIUM.draw(ctx, t, 11 + nw + 8, y + 3, 9f, 0xFFB0B0BC);
            y += 17;
        }
    }
}
