package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class Crosshair extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", "Cross", "Cross", "Dot", "Circle"));
    private final NumSetting size = add(new NumSetting("Size", 5, 2, 15, 1));
    private final NumSetting gap = add(new NumSetting("Gap", 2, 0, 10, 1));
    private final BoolSetting dyn = add(new BoolSetting("Dynamic gap", true));

    public Crosshair() {
        super("Crosshair", "Свой прицел", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        int cx = w / 2, cy = h / 2, col = Theme.accent();
        int g = gap.i();
        if (dyn.get() && mc.player != null && (com.collapse.util.Keys.moving() || !mc.player.isOnGround())) g += 3;
        int s = size.i();
        if (style.is("Dot")) {
            ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
            return;
        }
        if (style.is("Circle")) {
            int r = s + 2;
            for (int a = 0; a < 360; a += 6) {
                int x = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * r);
                int y = cy + (int) Math.round(Math.sin(Math.toRadians(a)) * r);
                ctx.fill(x, y, x + 1, y + 1, col);
            }
            return;
        }
        ctx.fill(cx - g - s - 1, cy - 1, cx - g + 1, cy + 2, 0xB0000000);
        ctx.fill(cx + g, cy - 1, cx + g + s + 2, cy + 2, 0xB0000000);
        ctx.fill(cx - 1, cy - g - s - 1, cx + 2, cy - g + 1, 0xB0000000);
        ctx.fill(cx - 1, cy + g, cx + 2, cy + g + s + 2, 0xB0000000);
        ctx.fill(cx - g - s, cy, cx - g, cy + 1, col);
        ctx.fill(cx + g + 1, cy, cx + g + s + 1, cy + 1, col);
        ctx.fill(cx, cy - g - s, cx + 1, cy - g, col);
        ctx.fill(cx, cy + g + 1, cx + 1, cy + g + s + 1, col);
    }
}
