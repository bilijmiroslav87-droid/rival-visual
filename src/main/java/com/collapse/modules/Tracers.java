package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Projector;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class Tracers extends Module {
    private final NumSetting range = add(new NumSetting("Range", 100, 10, 250, 5));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", false));

    public Tracers() {
        super("Tracers", "Линии к игрокам", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.world == null || mc.player == null) return;
        float td = mc.getRenderTickCounter().getTickProgress(false);
        double r2 = range.get() * range.get();
        int sx = w / 2, sy = h / 2;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof net.minecraft.entity.LivingEntity)) continue;
            if (!(e instanceof PlayerEntity) && !mobs.get()) continue;
            if (e.squaredDistanceTo(mc.player) > r2) continue;
            float[] s = Projector.project(e.getLerpedPos(td).add(0, e.getHeight() / 2, 0), td);
            if (s == null) continue;
            line(ctx, sx, sy, (int) s[0], (int) s[1], Theme.accent());
        }
    }

    private static void line(DrawContext c, int x0, int y0, int x1, int y1, int col) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps == 0 || steps > 3000) return;
        for (int i = 0; i <= steps; i += 1) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            c.fill(x, y, x + 1, y + 1, col);
        }
    }
}
