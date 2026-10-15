package com.collapse.hud;

import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Notify {
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
        float y = h - 40;
        Iterator<N> it = LIST.iterator();
        while (it.hasNext()) {
            N n = it.next();
            if (System.currentTimeMillis() - n.born > 2400) n.a.set(0f);
            float t = Anim.ease(n.a.get());
            if (t < 0.01f && n.a.target() == 0f) { it.remove(); continue; }
            String state = n.on ? "включён" : "выключен";
            float nw = Fonts.BOLD.width(n.text, 9f), sw = Fonts.MEDIUM.width(state, 8f);
            float tw = 12 + 14 + nw + 6 + sw + 10;
            float x = w - (tw + 8) * t;
            int col = n.on ? Theme.accent() : 0xFFFF5470;
            Draw.shadow(ctx, x, y, tw, 20, 9, 8);
            Draw.roundRect(ctx, x, y, tw, 20, 9, Draw.alpha(0xEE0E0E14, t));
            Draw.ring(ctx, x, y, tw, 20, 9, Draw.alpha(Theme.withAlpha(col, 90), t));
            Draw.circle(ctx, x + 8, y + 6.5f, 7, Draw.alpha(col, t));
            Fonts.BOLD.draw(ctx, n.text, x + 22, y + 6, 9f, Draw.alpha(0xFFFFFFFF, t));
            Fonts.MEDIUM.draw(ctx, state, x + 22 + nw + 6, y + 6.5f, 8f, Draw.alpha(col, t));
            y -= 24;
        }
    }
}
