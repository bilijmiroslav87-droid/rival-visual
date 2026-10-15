package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.render.Projector;
import com.collapse.settings.NumSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;

public class ItemEsp extends Module {
    private final NumSetting range = add(new NumSetting("Range", 40, 5, 100, 5));

    public ItemEsp() {
        super("ItemESP", "Подписи у выпавших предметов", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.world == null || mc.player == null) return;
        float td = mc.getRenderTickCounter().getTickProgress(false);
        double r2 = range.get() * range.get();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity ie)) continue;
            if (e.squaredDistanceTo(mc.player) > r2) continue;
            float[] s = Projector.project(e.getLerpedPos(td).add(0, 0.4, 0), td);
            if (s == null) continue;
            String n = ie.getStack().getName().getString() + (ie.getStack().getCount() > 1 ? " x" + ie.getStack().getCount() : "");
            float tw = Fonts.MEDIUM.width(n, 8.5f);
            Draw.roundRect(ctx, s[0] - tw / 2 - 3, s[1] - 6, tw + 6, 11, 3, 0xB0101014);
            Fonts.MEDIUM.draw(ctx, n, s[0] - tw / 2, s[1] - 5, 8.5f, 0xFFFFE066);
        }
    }
}
