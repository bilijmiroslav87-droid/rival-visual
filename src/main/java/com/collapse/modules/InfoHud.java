package com.collapse.modules;

import com.collapse.Module;
import com.collapse.hud.CollapseHud;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Единый информационный HUD: координаты, FPS, пинг, скорость, время, эффекты в одной стеклянной панели.
 * Каждая строка включается отдельно в настройках модуля.
 */
public class InfoHud extends Module {
    private static final long START = System.currentTimeMillis();

    public final ModeSetting pos = add(new ModeSetting("Позиция", "Слева снизу", "Слева снизу", "Слева сверху", "Справа снизу"));
    public final BoolSetting coords = add(new BoolSetting("Координаты", true));
    public final BoolSetting fps = add(new BoolSetting("FPS", true));
    public final BoolSetting ping = add(new BoolSetting("Пинг", true));
    public final BoolSetting speed = add(new BoolSetting("Скорость", true));
    public final BoolSetting time = add(new BoolSetting("Время", true));
    public final BoolSetting session = add(new BoolSetting("Сессия", false));
    public final BoolSetting potions = add(new BoolSetting("Эффекты", true));

    public InfoHud() {
        super("HUD", "Всё в одном: координаты, FPS, пинг, скорость, время, эффекты", Category.VISUALS);
    }

    private record Row(String label, String value, int valueColor) {}

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        int acc = Theme.accent(), acc2 = Theme.accent2();
        List<Row> rows = new ArrayList<>();

        if (coords.get()) {
            rows.add(new Row("XYZ", (int) Math.floor(mc.player.getX()) + "  " + (int) Math.floor(mc.player.getY()) + "  " + (int) Math.floor(mc.player.getZ()), 0xFFFFFFFF));
        }
        if (fps.get()) {
            int f = mc.getCurrentFps();
            rows.add(new Row("FPS", String.valueOf(f), f >= 60 ? 0xFFFFFFFF : f >= 30 ? 0xFFFFC857 : 0xFFFF5470));
        }
        if (ping.get()) {
            int p = 0;
            if (mc.getNetworkHandler() != null) {
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) p = e.getLatency();
            }
            rows.add(new Row("PING", p + " ms", p < 80 ? 0xFFFFFFFF : p < 160 ? 0xFFFFC857 : 0xFFFF5470));
        }
        if (speed.get()) {
            double bps = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ) * 20;
            rows.add(new Row("SPEED", String.format(java.util.Locale.US, "%.1f b/s", bps), 0xFFFFFFFF));
        }
        if (time.get()) {
            rows.add(new Row("TIME", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), 0xFFFFFFFF));
        }
        if (session.get()) {
            long s = (System.currentTimeMillis() - START) / 1000;
            rows.add(new Row("SESSION", String.format(java.util.Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60), 0xFFFFFFFF));
        }
        if (potions.get()) {
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
                int sec = e.getDuration() / 20;
                rows.add(new Row(name, String.format("%d:%02d", sec / 60, sec % 60), 0xFFB8B8CC));
            }
        }
        if (rows.isEmpty()) return;

        float ls = 7f, vs = 8.5f, rowH = 12, padX = 9, padY = 7, gapMid = 14;
        float labelW = 0, valueW = 0;
        for (Row r : rows) {
            labelW = Math.max(labelW, Fonts.BOLD.width(r.label, ls));
            valueW = Math.max(valueW, Fonts.MEDIUM.width(r.value, vs));
        }
        float bw = padX * 2 + labelW + gapMid + valueW;
        float bh = padY * 2 + rows.size() * rowH;

        String p = pos.get();
        float x, y;
        if (p.equals("Справа снизу")) { x = w - bw - 6; y = h - bh - 6; }
        else if (p.equals("Слева сверху")) { x = 6; y = 34; }
        else { x = 6; y = h - bh - 6; }

        Draw.shadow(ctx, x, y, bw, bh, 9, 12);
        Draw.glow(ctx, x, y, bw, bh, 7, Draw.alpha(Theme.withAlpha(acc, 26), 1f));
        Draw.roundRect(ctx, x, y, bw, bh, 9, CollapseHud.GLASS);
        Draw.roundRect(ctx, x + 1, y + 1, bw - 2, Math.min(bh / 2f, 14), 8, 0x0CFFFFFF);
        Draw.ring(ctx, x, y, bw, bh, 9, 0x26FFFFFF);
        // вертикальная градиентная полоска слева
        Draw.roundGradientV(ctx, x + 3, y + 7, 2, bh - 14, 1, acc, acc2);

        float cy = y + padY;
        for (Row r : rows) {
            Fonts.BOLD.draw(ctx, r.label, x + padX + 2, cy + 2.5f, ls, Draw.alpha(acc, 1f));
            Fonts.MEDIUM.drawRight(ctx, r.value, x + bw - padX, cy + 1.5f, vs, r.valueColor);
            cy += rowH;
        }
    }
}
