package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class Crosshair extends Module {
    private final NumSetting size = add(new NumSetting("Size", 5, 2, 15, 1));
    private final NumSetting gap = add(new NumSetting("Gap", 2, 0, 10, 1));
    private final BoolSetting dot = add(new BoolSetting("Dot", true));

    public Crosshair() {
        super("Crosshair", "Свой прицел", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        int cx = w / 2, cy = h / 2;
        int s = size.i(), g = gap.i();
        int col = Theme.accent();
        ctx.fill(cx - g - s, cy, cx - g, cy + 1, col);
        ctx.fill(cx + g + 1, cy, cx + g + s + 1, cy + 1, col);
        ctx.fill(cx, cy - g - s, cx + 1, cy - g, col);
        ctx.fill(cx, cy + g + 1, cx + 1, cy + g + s + 1, col);
        if (dot.get()) ctx.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
    }
}
