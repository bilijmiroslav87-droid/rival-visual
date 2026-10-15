package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.client.gui.DrawContext;

public class FpsInfo extends Module {
    public FpsInfo() {
        super("Info", "FPS и пинг", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        String t = mc.getCurrentFps() + " FPS  |  " + ping + " ms";
        ctx.drawTextWithShadow(mc.textRenderer, t, 4, h - 24, 0xFFFFFFFF);
    }
}
