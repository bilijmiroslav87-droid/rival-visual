package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

public class TargetHud extends Module {
    private final Anim hp = new Anim(1f, 10f);
    private final Anim show = new Anim(0f, 12f);
    private LivingEntity last;

    public TargetHud() {
        super("TargetHUD", "Карточка цели KillAura", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        LivingEntity t = KillAura.getTarget();
        if (t != null && t.isAlive()) {
            last = t;
            show.set(1f);
        } else {
            show.set(0f);
        }
        float vis = show.get();
        if (vis < 0.02f || last == null) return;

        float pw = 130, ph = 38;
        float x = w / 2f + 20, y = h / 2f + 20 + (1 - vis) * 8;
        int a = (int) (255 * vis);

        Draw.shadow(ctx, x, y, pw, ph, 6, 4);
        Draw.roundRect(ctx, x, y, pw, ph, 6, Draw.alpha(0xF0161620, vis));
        Draw.roundRect(ctx, x, y, 3, ph, 2, Draw.alpha(Theme.accent(), vis));

        Fonts.BOLD.draw(ctx, last.getName().getString(), x + 10, y + 6, 10.5f, (a << 24) | 0xFFFFFF);

        float frac = Math.max(0, Math.min(1, last.getHealth() / last.getMaxHealth()));
        hp.set(frac);
        float cur = hp.get();
        float bx = x + 10, by = y + 24, bw = pw - 20;
        Draw.roundRect(ctx, bx, by, bw, 5, 2, Draw.alpha(0xFF2A2A34, vis));
        Draw.roundRect(ctx, bx, by, Math.max(3, bw * cur), 5, 2, Draw.alpha(Theme.mix(0xFFFF3B3B, Theme.accent(), cur), vis));
        String hs = String.format("%.1f", last.getHealth());
        Fonts.MEDIUM.drawRight(ctx, hs, x + pw - 8, y + 7, 9f, Draw.alpha(0xFFB0B0BC, vis));
    }
}
