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
        String text; boolean on; long born = System.currentTimeMillis(); final Anim a = new Anim(0f, 12f);
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
            if (System.currentTimeMillis() - n.born > 2200) n.a.set(0f);
            float t = n.a.get();
            if (t < 0.01f && n.a.target() == 0f) { it.remove(); continue; }
            String s = n.text + (n.on ? "  ON" : "  OFF");
            float tw = Fonts.MEDIUM.width(s, 9.5f) + 22;
            float x = w - (tw + 8) * t - 4 + (1 - t) * 0;
            Draw.roundRect(ctx, w - (tw + 8) * t, y, tw, 18, 6, 0xE0141418);
            Draw.roundRect(ctx, w - (tw + 8) * t, y + 3, 3, 12, 1, n.on ? Theme.accent() : 0xFFFF4D5E);
            Fonts.MEDIUM.draw(ctx, s, w - (tw + 8) * t + 10, y + 5, 9.5f, 0xFFFFFFFF);
            y -= 22;
        }
    }
}
