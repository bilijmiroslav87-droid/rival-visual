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

        float pw = 150, ph = 46;
        float x = w / 2f + 24, y = h / 2f + 20 + (1 - vis) * 8;

        // матовая плашка как в Glass Menu
        Draw.roundRect(ctx, x, y, pw, ph, 8, Draw.alpha(0x16FFFFFF, vis));
        Draw.roundRect(ctx, x + 1, y + 1, pw - 2, ph - 2, 7, Draw.alpha(0xD00F0F14, vis));

        // голова скина
        float fs = 32, fx = x + 7, fy = y + 7;
        Draw.roundRect(ctx, fx - 1, fy - 1, fs + 2, fs + 2, 6, Draw.alpha(0x30FFFFFF, vis));
        boolean face = false;
        try {
            if (last instanceof net.minecraft.entity.player.PlayerEntity pl && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(pl.getUuid());
                if (entry != null) {
                    net.minecraft.client.gui.PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), Math.round(fx), Math.round(fy), Math.round(fs));
                    face = true;
                }
            }
        } catch (Throwable ignored) {}
        if (!face) {
            String nm = last.getName().getString();
            Draw.roundRect(ctx, fx, fy, fs, fs, 5, Draw.alpha(0x20FFFFFF, vis));
            Fonts.HEAD.drawCentered(ctx, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(), fx + fs / 2, fy + 9, 14f, Draw.alpha(0xFFF2F2F5, vis));
        }

        float tx = fx + fs + 10;
        String name = last.getName().getString();
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(name, pw - (tx - x) - 44, 10f), tx, y + 8, 10f, Draw.alpha(0xFFF2F2F5, vis));
        double dist = mc.player != null ? mc.player.distanceTo(last) : 0;
        int armor = last.getArmor();
        String sub = String.format(java.util.Locale.US, "%.1f m", dist) + (armor > 0 ? "   " + armor + " armor" : "");
        Fonts.MEDIUM.draw(ctx, sub, tx, y + 21, 7.5f, Draw.alpha(0xFF8A8F98, vis));

        float max = Math.max(1f, last.getMaxHealth());
        float frac = Math.max(0, Math.min(1, last.getHealth() / max));
        hp.set(frac); trail.set(frac);
        float cur = hp.get(), tr = Math.max(cur, trail.get());
        float bx = tx, by = y + ph - 11, bw = pw - (tx - x) - 10;
        Draw.roundRect(ctx, bx, by, bw, 4, 2, Draw.alpha(0x22FFFFFF, vis));
        Draw.roundRect(ctx, bx, by, Math.max(4, bw * tr), 4, 2, Draw.alpha(0x40FFFFFF, vis));
        int hc = cur > 0.6f ? 0xFF7CFFB2 : cur > 0.3f ? 0xFFFFD36E : 0xFFFF6F6F;
        Draw.roundRect(ctx, bx, by, Math.max(4, bw * cur), 4, 2, Draw.alpha(hc, vis));
        float abs = last.getAbsorptionAmount();
        if (abs > 0) Draw.roundRect(ctx, bx, by + 5, Math.min(bw, bw * abs / max), 1.5f, 0.75f, Draw.alpha(0xFFFFD36E, vis));
        Fonts.MEDIUM.drawRight(ctx, String.format(java.util.Locale.US, "%.1f", last.getHealth()), x + pw - 9, y + 8, 9f, Draw.alpha(hc, vis));
    }
}
