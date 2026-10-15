package com.collapse.modules;

import com.collapse.Module;
import com.collapse.hud.CollapseHud;
import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

/** Карточка цели KillAura: имя, HP с «хвостом» урона, броня, дистанция. */
public class TargetHud extends Module {
    private final Anim hp = new Anim(1f, 10f);
    private final Anim trail = new Anim(1f, 3.5f);
    private final Anim show = new Anim(0f, 12f);
    private LivingEntity last;

    public TargetHud() {
        super("TargetHUD", "Карточка цели KillAura", Category.VISUALS);
    }

    private long lastSeen = 0;

    /** Цель: тот, на кого смотришь (до 12 блоков), либо последний, кого ты бил. Не зависит от других модулей. */
    private LivingEntity pick() {
        if (mc.player == null || mc.world == null) return null;
        LivingEntity best = null;
        double bestDot = 0.985; // ~10 градусов
        net.minecraft.util.math.Vec3d eye = mc.player.getEyePos();
        net.minecraft.util.math.Vec3d look = mc.player.getRotationVec(1f);
        for (net.minecraft.entity.Entity e : mc.world.getEntities()) {
            if (!(e instanceof net.minecraft.entity.player.PlayerEntity p) || p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            net.minecraft.util.math.Vec3d to = new net.minecraft.util.math.Vec3d(p.getX(), p.getY() + p.getHeight() * 0.6, p.getZ()).subtract(eye);
            double d = to.length();
            if (d > 12 || d < 0.1) continue;
            double dot = to.normalize().dotProduct(look);
            if (dot > bestDot) { bestDot = dot; best = p; }
        }
        if (best != null) { lastSeen = System.currentTimeMillis(); return best; }
        // держим карточку ещё 1.5с после потери цели
        if (last != null && last.isAlive() && System.currentTimeMillis() - lastSeen < 1500) return last;
        return null;
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        LivingEntity t = pick();
        if (t != null && t.isAlive()) {
            if (last != t) { hp.snap(1f); trail.snap(1f); }
            last = t;
            show.set(1f);
        } else {
            show.set(0f);
        }
        float vis = Anim.ease(show.get());
        if (vis < 0.02f || last == null) return;

        int acc = Theme.accent(), acc2 = Theme.accent2();
        float pw = 142, ph = 44;
        float x = w / 2f + 22, y = h / 2f + 20 + (1 - vis) * 8;

        // подложка
        Draw.shadow(ctx, x, y, pw, ph, 8, 12);
        Draw.glow(ctx, x, y, pw, ph, 6, Draw.alpha(Theme.withAlpha(acc, 24), vis));
        Draw.roundRect(ctx, x, y, pw, ph, 11, Draw.alpha(0xE60B0B12, vis));
        Draw.ring(ctx, x, y, pw, ph, 11, Draw.alpha(0x22FFFFFF, vis));

        // голова скина с рамкой
        float fs = 30;
        float fx = x + 7, fy = y + 7;
        Draw.roundRect(ctx, fx - 1.5f, fy - 1.5f, fs + 3, fs + 3, 8, Draw.alpha(acc, vis * 0.9f));
        Draw.roundRect(ctx, fx, fy, fs, fs, 7, Draw.alpha(0xFF14141C, vis));
        boolean face = false;
        try {
            if (last instanceof net.minecraft.entity.player.PlayerEntity pl && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(pl.getUuid());
                if (entry != null) {
                    net.minecraft.client.gui.PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), Math.round(fx + 1), Math.round(fy + 1), Math.round(fs - 2));
                    face = true;
                }
            }
        } catch (Throwable ignored) {}
        if (!face) {
            String nm = last.getName().getString();
            Fonts.HEAD.drawCentered(ctx, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(), fx + fs / 2, fy + 8, 13f, Draw.alpha(acc, vis));
        }

        // имя и дистанция
        float tx = fx + fs + 9;
        String name = last.getName().getString();
        Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(name, pw - (tx - x) - 8, 9.5f), tx, y + 8, 9.5f, Draw.alpha(0xFFFFFFFF, vis));
        double dist = mc.player != null ? mc.player.distanceTo(last) : 0;
        int armor = last.getArmor();
        String sub = String.format(java.util.Locale.US, "%.1f m", dist) + (armor > 0 ? "  ·  " + armor + " arm" : "");
        Fonts.MEDIUM.draw(ctx, sub, tx, y + 20, 7.3f, Draw.alpha(0xFF8E8EA6, vis));

        // HP-бар
        float max = Math.max(1f, last.getMaxHealth());
        float frac = Math.max(0, Math.min(1, last.getHealth() / max));
        hp.set(frac); trail.set(frac);
        float cur = hp.get(), tr = Math.max(cur, trail.get());
        float bx = tx, by = y + ph - 11, bw = pw - (tx - x) - 8;
        Draw.roundRect(ctx, bx, by, bw, 5, 2.5f, Draw.alpha(0xFF1F1F2A, vis));
        Draw.roundRect(ctx, bx, by, Math.max(5, bw * tr), 5, 2.5f, Draw.alpha(0xFFFFFFFF, vis * 0.30f));
        int hc = Theme.mix(0xFFFF4466, acc, cur);
        Draw.gradientH(ctx, bx, by, Math.max(5, bw * cur), 5, Draw.alpha(hc, vis), Draw.alpha(Theme.mix(hc, acc2, 0.5f), vis));
        // золотые сердца поверх
        float abs = last.getAbsorptionAmount();
        if (abs > 0) Draw.roundRect(ctx, bx, by + 5.6f, Math.min(bw, bw * abs / max), 1.6f, 0.8f, Draw.alpha(0xFFFFC857, vis));
        String hs = String.format(java.util.Locale.US, "%.1f", last.getHealth());
        Fonts.BOLD.drawRight(ctx, hs, x + pw - 8, y + 8, 8.5f, Draw.alpha(hc, vis));
    }
}
