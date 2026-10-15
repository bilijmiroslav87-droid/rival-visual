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
        String[] labels = {"FPS", "PING", "B/S"};
        String[] vals = {String.valueOf(mc.getCurrentFps()), ping + " ms", String.format(java.util.Locale.US, "%.1f", bps)};
        float size = 8.5f, lsize = 6.5f, gap = 10;
        float total = 14;
        for (int i = 0; i < 3; i++) total += Math.max(Fonts.BOLD.width(vals[i], size), Fonts.MEDIUM.width(labels[i], lsize)) + (i < 2 ? gap : 0);
        float x = 6, y = h - 40, hh = 26;
        Draw.shadow(ctx, x, y, total, hh, 8, 8);
        Draw.roundRect(ctx, x, y, total, hh, 8, 0xC8101018);
        Draw.ring(ctx, x, y, total, hh, 8, 0x22FFFFFF);
        float cx = x + 7;
        for (int i = 0; i < 3; i++) {
            float cw = Math.max(Fonts.BOLD.width(vals[i], size), Fonts.MEDIUM.width(labels[i], lsize));
            Fonts.BOLD.draw(ctx, vals[i], cx, y + 5, size, 0xFFFFFFFF);
            Fonts.MEDIUM.draw(ctx, labels[i], cx, y + 16, lsize, Theme.accent());
            cx += cw + gap;
        }
    }
}
