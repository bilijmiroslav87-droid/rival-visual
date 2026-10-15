package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class Keystrokes extends Module {
    private final Anim[] a = {new Anim(0, 22), new Anim(0, 22), new Anim(0, 22), new Anim(0, 22), new Anim(0, 22)};

    public Keystrokes() {
        super("Keystrokes", "Показ нажатых клавиш", Category.VISUALS);
    }

    private void key(DrawContext ctx, int i, String l, float x, float y, float w, float h, boolean on) {
        a[i].set(on ? 1f : 0f);
        float t = a[i].get();
        int acc = Theme.accent();
        if (t > 0.02f) Draw.glow(ctx, x, y, w, h, 5, Draw.alpha(Theme.withAlpha(acc, 60), t));
        Draw.roundRect(ctx, x, y + t * 0.8f, w, h, 6, Theme.mix(0xCC0E0E15, acc, t * 0.92f));
        Draw.roundRect(ctx, x + 1, y + 1 + t * 0.8f, w - 2, h / 2f - 1, 5, Draw.alpha(0x10FFFFFF, 1f - t * 0.5f));
        Draw.ring(ctx, x, y + t * 0.8f, w, h, 6, Theme.mix(0x26FFFFFF, 0x66FFFFFF, t));
        Fonts.BOLD.drawCentered(ctx, l, x + w / 2, y + h / 2f - 3 + t * 0.8f, 8f, Theme.mix(0xFFE6E6F0, 0xFF0B0B10, t));
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        float x = 8, y = h / 2f - 24, k = 19, g = 3;
        key(ctx, 0, "W", x + k + g, y, k, k, com.collapse.util.Keys.forward());
        key(ctx, 1, "A", x, y + k + g, k, k, com.collapse.util.Keys.left());
        key(ctx, 2, "S", x + k + g, y + k + g, k, k, com.collapse.util.Keys.back());
        key(ctx, 3, "D", x + (k + g) * 2, y + k + g, k, k, com.collapse.util.Keys.right());
        key(ctx, 4, "SPACE", x, y + (k + g) * 2, k * 3 + g * 2, 14, com.collapse.util.Keys.jump());
    }
}
