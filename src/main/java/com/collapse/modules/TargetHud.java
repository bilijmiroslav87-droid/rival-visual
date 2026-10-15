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
        float pw = 150, ph = 46;
        float x = w / 2f + 24, y = h / 2f + 22 + (1 - vis) * 10;

        Draw.shadow(ctx, x, y, pw, ph, 8, 12);
        Draw.glow(ctx, x, y, pw, ph, 7, Draw.alpha(Theme.withAlpha(acc, 28), vis));
        Draw.roundRect(ctx, x, y, pw, ph, 10, Draw.alpha(CollapseHud.GLASS, vis));
        Draw.roundRect(ctx, x + 1, y + 1, pw - 2, ph / 2f - 1, 9, Draw.alpha(0x0CFFFFFF, vis));
        Draw.ring(ctx, x, y, pw, ph, 10, Draw.alpha(0x26FFFFFF, vis));

        // аватар-плитка с инициалом
        String name = last.getName().getString();
        Draw.roundGradientV(ctx, x + 6, y + 6, 22, 22, 7, Draw.alpha(acc, vis), Draw.alpha(acc2, vis));
        String ini = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        Fonts.HEAD.drawCentered(ctx, ini, x + 17, y + 11.5f, 10f, Draw.alpha(0xFF0B0B10, vis));

        Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(name, pw - 80, 10f), x + 34, y + 7, 10f, Draw.alpha(0xFFFFFFFF, vis));
        double dist = mc.player != null ? mc.player.distanceTo(last) : 0;
        Fonts.MEDIUM.draw(ctx, String.format(java.util.Locale.US, "%.1f m", dist), x + 34, y + 19, 7.5f, Draw.alpha(0xFF9A9AAD, vis));

        float max = Math.max(1f, last.getMaxHealth());
        float frac = Math.max(0, Math.min(1, last.getHealth() / max));
        hp.set(frac); trail.set(frac);
        float cur = hp.get(), tr = Math.max(cur, trail.get());
        float bx = x + 8, by = y + ph - 14, bw = pw - 16;
        Draw.roundRect(ctx, bx, by, bw, 6, 3, Draw.alpha(0xFF23232E, vis));
        // «хвост» урона
        Draw.roundRect(ctx, bx, by, Math.max(4, bw * tr), 6, 3, Draw.alpha(0xFFFFFFFF, vis * 0.35f));
        int hc = Theme.mix(0xFFFF3B5C, acc, cur);
        Draw.roundRect(ctx, bx, by, Math.max(4, bw * cur), 6, 3, Draw.alpha(hc, vis));
        String hs = String.format(java.util.Locale.US, "%.1f", last.getHealth());
        Fonts.BOLD.drawRight(ctx, hs + " HP", x + pw - 8, y + 8, 8.5f, Draw.alpha(hc, vis));
        int armor = last.getArmor();
        if (armor > 0) Fonts.MEDIUM.drawRight(ctx, "броня " + armor, x + pw - 8, y + 19, 7.5f, Draw.alpha(0xFF9A9AAD, vis));
    }
}
