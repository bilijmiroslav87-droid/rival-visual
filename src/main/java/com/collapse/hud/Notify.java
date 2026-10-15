package com.collapse.hud;

import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Уведомления о включении/выключении модулей: стеклянные плашки с полоской таймера. */
public final class Notify {
    private static final long LIFE = 2600;

    private static final class N {
        String text; boolean on; long born = System.currentTimeMillis(); final Anim a = new Anim(0f, 11f);
    }
    private static final List<N> LIST = new ArrayList<>();

    private Notify() {}

    public static void push(String text, boolean on) {
        N n = new N(); n.text = text; n.on = on; n.a.set(1f);
        LIST.add(n);
        if (LIST.size() > 5) LIST.remove(0);
    }

    public static void render(DrawContext ctx, int w, int h) {
        float y = h - 44;
        Iterator<N> it = LIST.iterator();
        while (it.hasNext()) {
            N n = it.next();
            long age = System.currentTimeMillis() - n.born;
            if (age > LIFE) n.a.set(0f);
            float t = Anim.ease(n.a.get());
            if (t < 0.01f && n.a.target() == 0f) { it.remove(); continue; }
            String state = n.on ? "включён" : "выключен";
            float nw = Fonts.BOLD.width(n.text, 9f), sw = Fonts.MEDIUM.width(state, 8f);
            float tw = 12 + 18 + nw + 6 + sw + 10;
            float th = 22;
            float x = w - (tw + 8) * t;
            int col = n.on ? Theme.accent() : 0xFFFF5470;
            Draw.shadow(ctx, x, y, tw, th, 9, 10);
            Draw.glow(ctx, x, y, tw, th, 6, Draw.alpha(Theme.withAlpha(col, 34), t));
            Draw.roundRect(ctx, x, y, tw, th, 9, Draw.alpha(CollapseHud.GLASS, t));
            Draw.ring(ctx, x, y, tw, th, 9, Draw.alpha(Theme.withAlpha(col, 110), t));
            // значок состояния
            Draw.roundRect(ctx, x + 5, y + 5, 12, 12, 6, Draw.alpha(Theme.withAlpha(col, 60), t));
            Draw.circle(ctx, x + 8.5f, y + 8.5f, 5, Draw.alpha(col, t));
            Fonts.BOLD.draw(ctx, n.text, x + 24, y + 7, 9f, Draw.alpha(0xFFFFFFFF, t));
            Fonts.MEDIUM.draw(ctx, state, x + 24 + nw + 6, y + 7.5f, 8f, Draw.alpha(col, t));
            // полоска оставшегося времени
            float left = Math.max(0f, 1f - age / (float) LIFE);
            Draw.roundRect(ctx, x + 8, y + th - 3, (tw - 16) * left, 1.5f, 0.7f, Draw.alpha(col, t * 0.9f));
            y -= 26;
        }
    }
}
