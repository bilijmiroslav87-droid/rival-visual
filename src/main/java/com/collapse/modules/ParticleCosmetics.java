package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * Косметика на частицах (как в Glass Menu): шляпа, крылья, нимб, след, всплеск при прыжке.
 * Работает в 1-м и 3-м лице и не зависит от слоя рендера модели игрока.
 */
public class ParticleCosmetics extends Module {
    public final ModeSetting hat = add(new ModeSetting("Шляпа", "Конус", "Нет", "Конус", "Ореол", "Конус и ореол", "Корона", "Рога"));
    public final ModeSetting wings = add(new ModeSetting("Крылья", "Ангел", "Нет", "Ангел", "Бабочка", "Дракон", "Кристалл", "Феникс"));
    public final BoolSetting trail = add(new BoolSetting("След", false));
    public final BoolSetting jump = add(new BoolSetting("Всплеск при прыжке", true));
    public final BoolSetting orbit = add(new BoolSetting("Орбиты", false));
    public final ModeSetting color = add(new ModeSetting("Цвет", "Тема", "Тема", "Градиент", "Белый", "Золото", "Красный", "Синий", "Фиолетовый", "Розовый", "Зелёный", "Радуга"));
    public final BoolSetting gradient = add(new BoolSetting("Градиент к белому", true));
    public final NumSetting size = add(new NumSetting("Размер", 1.0, 0.5, 2.0, 0.1));
    public final NumSetting density = add(new NumSetting("Плотность", 3, 1, 8, 1));
    public final NumSetting flap = add(new NumSetting("Взмах крыльев", 1.0, 0.0, 3.0, 0.1));

    private boolean wasGround = true;

    public ParticleCosmetics() {
        super("Aura", "Шляпа, крылья, след и всплеск на частицах. Видно с любого ракурса", Category.COSMETICS);
    }

    private int base() {
        switch (color.get()) {
            case "Белый": return 0xFFFFFF;
            case "Золото": return 0xFFC53D;
            case "Красный": return 0xFF4D6D;
            case "Синий": return 0x5B8CFF;
            case "Фиолетовый": return 0xA66BFF;
            case "Розовый": return 0xFF6BC1;
            case "Зелёный": return 0x2FE3A0;
            case "Радуга": {
                float h = (System.currentTimeMillis() % 4000L) / 4000f;
                return java.awt.Color.HSBtoRGB(h, 0.6f, 1f) & 0xFFFFFF;
            }
            case "Градиент": return Theme.mix(Theme.accent(), Theme.accent2(), (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 800.0))) & 0xFFFFFF;
            default: return Theme.accent() & 0xFFFFFF;
        }
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private int pick(int base, float t) { return gradient.get() ? lerp(base, 0xFFFFFF, t) : base; }

    private static void dust(ClientWorld w, int rgb, float scale, double x, double y, double z) {
        ParticleEffect e = new DustParticleEffect(rgb, scale);
        w.addParticleClient(e, x, y, z, 0, 0, 0);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        PlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        var rnd = w.random;
        int base = base();
        double sz = size.get();
        int dens = density.i();
        double time = w.getTime();

        // ---- шляпа ----
        double topY = p.getY() + p.getHeight() + 0.2;
        double spin = (w.getTime() % 628) * 0.04;
        switch (hat.get()) {
            case "Конус", "Конус и ореол" -> {
                double r0 = 0.45 * sz;
                for (int i = 0; i < dens + 1; i++) {
                    double ang = rnd.nextInt(8) * Math.PI / 4 + spin;
                    double t = rnd.nextDouble();
                    double rad = r0 * (1 - t);
                    dust(w, pick(base, (float) t), 0.9f, p.getX() + Math.cos(ang) * rad, topY + t * 0.55 * sz, p.getZ() + Math.sin(ang) * rad);
                }
            }
            default -> {}
        }
        if (hat.get().equals("Ореол") || hat.get().equals("Конус и ореол")) {
            for (int i = 0; i < dens; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                dust(w, base, 0.9f, p.getX() + Math.cos(ang) * 0.42 * sz, topY + 0.05, p.getZ() + Math.sin(ang) * 0.42 * sz);
            }
        }
        if (hat.get().equals("Корона")) {
            for (int i = 0; i < dens + 1; i++) {
                int spike = rnd.nextInt(6);
                double ang = spike * Math.PI / 3 + spin * 0.3;
                double t = rnd.nextDouble();
                dust(w, pick(base, (float) t), 0.9f, p.getX() + Math.cos(ang) * 0.36 * sz, topY + t * 0.35 * sz, p.getZ() + Math.sin(ang) * 0.36 * sz);
                double a2 = rnd.nextDouble() * Math.PI * 2;
                dust(w, base, 0.8f, p.getX() + Math.cos(a2) * 0.36 * sz, topY, p.getZ() + Math.sin(a2) * 0.36 * sz);
            }
        }
        if (hat.get().equals("Рога")) {
            for (int side = -1; side <= 1; side += 2) {
                double yaw = Math.toRadians(p.getYaw());
                double sx = Math.cos(yaw) * side, sz2 = Math.sin(yaw) * side;
                for (int i = 0; i < dens; i++) {
                    double t = rnd.nextDouble();
                    dust(w, pick(base, (float) t), 0.9f, p.getX() + sx * (0.2 + t * 0.25) * sz, topY - 0.05 + t * 0.5 * sz, p.getZ() + sz2 * (0.2 + t * 0.25) * sz);
                }
            }
        }

        // ---- крылья ----
        if (!wings.get().equals("Нет")) {
            double yaw = Math.toRadians(p.getYaw());
            double sideX = Math.cos(yaw), sideZ = Math.sin(yaw);
            double backX = Math.sin(yaw), backZ = -Math.cos(yaw);
            double fl = Math.sin(time * 0.2) * 0.35 * flap.get();
            double chestY = p.getY() + p.getHeight() * 0.68;
            if (p.isSneaking()) chestY -= 0.2;
            String kind = wings.get();
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < dens; i++) {
                    float frac = rnd.nextFloat();
                    double reach, lift;
                    switch (kind) {
                        case "Бабочка" -> { double a = frac * Math.PI; reach = 0.2 + Math.sin(a) * 0.9 * sz; lift = Math.cos(a) * 0.6 * sz + 0.1 + fl * frac; }
                        case "Дракон" -> { reach = (0.2 + frac * 1.1) * sz; lift = (0.3 + Math.abs(Math.sin(frac * 6)) * 0.4 * (1 - frac)) * sz + fl * frac; }
                        case "Кристалл" -> { reach = (0.2 + frac * 0.9) * sz; lift = (0.5 - Math.abs(frac - 0.35) * 1.2) * sz + fl * frac; }
                        case "Феникс" -> { reach = (0.2 + frac * 1.2) * sz; lift = (0.1 + frac * 0.7) * sz + fl * frac * 1.3; }
                        default -> { reach = (0.25 + frac * 0.9) * sz; lift = (0.15 + frac * 0.45) * sz + fl * frac; }
                    }
                    dust(w, pick(base, frac), 0.8f,
                            p.getX() + backX * 0.22 + sideX * side * reach, chestY + lift,
                            p.getZ() + backZ * 0.22 + sideZ * side * reach);
                }
            }
        }

        // ---- орбиты ----
        if (orbit.get()) {
            for (int k = 0; k < 3; k++) {
                double ang = time * 0.12 + k * Math.PI * 2 / 3;
                double r = 0.9 * sz;
                dust(w, pick(base, k / 3f), 1.0f, p.getX() + Math.cos(ang) * r, p.getY() + p.getHeight() * 0.5 + Math.sin(time * 0.1 + k) * 0.3, p.getZ() + Math.sin(ang) * r);
            }
        }

        // ---- след ----
        if (trail.get() && p.getVelocity().horizontalLengthSquared() > 0.003) {
            for (int i = 0; i < dens; i++) {
                dust(w, pick(base, rnd.nextFloat()), 0.9f, p.getX() + (rnd.nextDouble() - 0.5) * 0.4, p.getY() + 0.1 + rnd.nextDouble() * 0.2, p.getZ() + (rnd.nextDouble() - 0.5) * 0.4);
            }
        }

        // ---- всплеск при прыжке ----
        boolean ground = p.isOnGround();
        if (jump.get() && wasGround && !ground && p.getVelocity().y > 0) {
            for (int i = 0; i < 14; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                w.addParticleClient(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.05, p.getZ(), Math.cos(ang) * 0.09, 0.02, Math.sin(ang) * 0.09);
            }
        }
        wasGround = ground;
    }
}
