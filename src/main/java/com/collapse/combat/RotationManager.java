package com.collapse.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Серверные ротации в духе прогрессивных клиентов:
 *  - плавный поворот с ограничением скорости (а не мгновенный снап)
 *  - GCD-снап: углы кратны минимальному шагу мыши, как у живого игрока
 *  - небольшой шум и "ленивое" ведение цели
 * Игрок видит свою камеру как обычно, серверу уходят подменённые углы.
 */
public final class RotationManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random RNG = new Random();

    private static float yaw, pitch;
    private static boolean active = false;
    private static int holdTicks = 0;

    private RotationManager() {}

    public static boolean isActive() { return active; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    /** Шаг мыши в градусах при текущей чувствительности. */
    public static double gcd() {
        double s = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double f = s * s * s * 8.0;
        return f * 0.15;
    }

    public static float snap(float delta) {
        double g = gcd();
        if (g <= 0) return delta;
        return (float) (Math.round(delta / g) * g);
    }

    public static float[] calc(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float y = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float p = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{y, MathHelper.clamp(p, -90f, 90f)};
    }

    /** Точка на хитбоксе цели: чуть смещаем, чтобы не целиться строго в центр. */
    public static Vec3d aimPoint(LivingEntity t, float jitter) {
        double h = t.getHeight();
        double wob = jitter * 0.08;
        return new Vec3d(
                t.getX() + (RNG.nextDouble() - 0.5) * t.getWidth() * wob,
                t.getY() + h * 0.62 + (RNG.nextDouble() - 0.5) * h * wob,
                t.getZ() + (RNG.nextDouble() - 0.5) * t.getWidth() * wob);
    }

    public static void rotateTo(Entity target, float maxStep, float jitter) {
        if (mc.player == null) return;
        if (!active) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
        Vec3d eye = mc.player.getEyePos();
        Vec3d aim = target instanceof LivingEntity l ? aimPoint(l, jitter) : target.getEntityPos();
        float[] want = calc(eye, aim);

        float dy = MathHelper.wrapDegrees(want[0] - yaw);
        float dp = want[1] - pitch;

        // скорость зависит от расстояния до цели: далеко - быстрее, рядом - плавнее
        float k = MathHelper.clamp(Math.abs(dy) / 40f, 0.25f, 1f);
        float stepY = maxStep * k * (0.8f + RNG.nextFloat() * 0.4f);
        float stepP = maxStep * 0.6f * (0.8f + RNG.nextFloat() * 0.4f);

        dy = MathHelper.clamp(dy, -stepY, stepY);
        dp = MathHelper.clamp(dp, -stepP, stepP);

        yaw += snap(dy);
        pitch = MathHelper.clamp(pitch + snap(dp), -90f, 90f);
        active = true;
        holdTicks = 3;
    }

    public static void tick() {
        if (active && --holdTicks <= 0) {
            active = false;
        }
    }

    public static void reset() { active = false; holdTicks = 0; }

    /** Насколько текущая ротация смотрит на цель (в градусах ошибки). */
    public static double error(Entity target) {
        if (mc.player == null) return 180;
        float[] want = calc(mc.player.getEyePos(), target.getEntityPos().add(0, target.getHeight() * 0.62, 0));
        float cy = active ? yaw : mc.player.getYaw();
        float cp = active ? pitch : mc.player.getPitch();
        float dy = MathHelper.wrapDegrees(want[0] - cy);
        float dp = want[1] - cp;
        return Math.sqrt(dy * dy + dp * dp);
    }
}
