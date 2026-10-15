package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class Keystrokes extends Module {
    private final Anim[] a = {new Anim(0, 20), new Anim(0, 20), new Anim(0, 20), new Anim(0, 20), new Anim(0, 20)};

    public Keystrokes() {
        super("Keystrokes", "Показ нажатых клавиш", Category.VISUALS);
    }

    private void key(DrawContext ctx, int i, String l, float x, float y, float w, boolean on) {
        a[i].set(on ? 1f : 0f);
        float t = a[i].get();
        Draw.roundRect(ctx, x, y, w, 18, 4, Theme.mix(0xCC16161E, Theme.accent(), t * 0.85f));
        Fonts.MEDIUM.drawCentered(ctx, l, x + w / 2, y + 4, 9.5f, 0xFFFFFFFF);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        var in = mc.player.input.playerInput;
        float x = 8, y = h / 2f - 20;
        key(ctx, 0, "W", x + 21, y, 18, in.forward());
        key(ctx, 1, "A", x, y + 21, 18, in.left());
        key(ctx, 2, "S", x + 21, y + 21, 18, in.backward());
        key(ctx, 3, "D", x + 42, y + 21, 18, in.right());
        key(ctx, 4, "SPACE", x, y + 42, 60, in.jump());
    }
}
