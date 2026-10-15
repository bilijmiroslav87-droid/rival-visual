package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class FpsInfo extends Module {
    public FpsInfo() {
        super("Info", "FPS, пинг и скорость", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        double bps = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ) * 20;
        String t = mc.getCurrentFps() + " FPS   " + ping + " ms   " + String.format("%.1f", bps) + " b/s";
        float tw = Fonts.MEDIUM.width(t, 9.5f);
        Draw.roundRect(ctx, 4, h - 34, tw + 12, 14, 4, 0xB0101014);
        Fonts.MEDIUM.draw(ctx, t, 10, h - 31, 9.5f, 0xFFFFFFFF);
    }
}
