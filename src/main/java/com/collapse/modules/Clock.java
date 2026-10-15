package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.settings.BoolSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Часы и время сессии в углу экрана. */
public class Clock extends Module {
    private final BoolSetting session = add(new BoolSetting("Время сессии", true));
    private final BoolSetting seconds = add(new BoolSetting("Секунды", false));
    private static final long START = System.currentTimeMillis();

    public Clock() {
        super("Clock", "Часы и время сессии", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern(seconds.get() ? "HH:mm:ss" : "HH:mm"));
        long s = (System.currentTimeMillis() - START) / 1000;
        String ses = String.format(java.util.Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        float size = 9f;
        float tw = Fonts.BOLD.width(time, size);
        float sw = session.get() ? Fonts.MEDIUM.width(ses, 8f) + 12 : 0;
        float total = 14 + tw + sw;
        float x = w / 2f - total / 2f, y = 6, hh = 16;
        Draw.shadow(ctx, x, y, total, hh, 8, 8);
        Draw.roundRect(ctx, x, y, total, hh, 8, 0xDD0E0E14);
        Draw.ring(ctx, x, y, total, hh, 8, 0x1FFFFFFF);
        Fonts.BOLD.draw(ctx, time, x + 7, y + 4, size, 0xFFFFFFFF);
        if (session.get()) {
            ctx.fill((int) (x + 7 + tw + 5), (int) y + 4, (int) (x + 7 + tw + 6), (int) (y + hh - 4), 0x33FFFFFF);
            Fonts.MEDIUM.draw(ctx, ses, x + 7 + tw + 11, y + 4.5f, 8f, Theme.accent());
        }
    }
}
