package com.collapse.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Проекция мировых координат в экранные, на CPU. Никаких новых рендер-пайплайнов. */
public final class Projector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Projector() {}

    /** Возвращает {x, y} в GUI-координатах или null, если точка за камерой. */
    public static float[] project(Vec3d world, float tickDelta) {
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null || mc.getWindow() == null) return null;
        Vec3d rel = world.subtract(cam.getCameraPos());

        Matrix4f proj = mc.gameRenderer.getBasicProjectionMatrix(mc.options.getFov().getValue().floatValue());
        Matrix4f view = new Matrix4f().rotation(cam.getRotation().conjugate(new org.joml.Quaternionf()));
        Vector4f v = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1f);
        view.transform(v);
        proj.transform(v);
        if (v.w <= 0.05f) return null;

        float nx = v.x / v.w, ny = v.y / v.w;
        float sx = (nx * 0.5f + 0.5f) * mc.getWindow().getScaledWidth();
        float sy = (1f - (ny * 0.5f + 0.5f)) * mc.getWindow().getScaledHeight();
        return new float[]{sx, sy};
    }

    /** Экранный прямоугольник вокруг сущности: {minX, minY, maxX, maxY} или null. */
    public static float[] entityBox(net.minecraft.entity.Entity e, float td) {
        Vec3d p = e.getLerpedPos(td);
        double w = e.getWidth() / 2.0, h = e.getHeight();
        double[][] c = {
                {-w, 0, -w}, {w, 0, -w}, {-w, 0, w}, {w, 0, w},
                {-w, h, -w}, {w, h, -w}, {-w, h, w}, {w, h, w}};
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (double[] o : c) {
            float[] s = project(new Vec3d(p.x + o[0], p.y + o[1], p.z + o[2]), td);
            if (s == null) return null;
            minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
            minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
        }
        return new float[]{minX, minY, maxX, maxY};
    }
}
