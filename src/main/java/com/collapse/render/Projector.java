package com.collapse.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Проекция мира на экран на CPU. Проекцию собираем сами из FOV и размеров окна
 * (проверено офлайн-тестом), поворот берём из camera.getRotation().
 */
public final class Projector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Matrix4f PROJ = new Matrix4f();
    private static final Quaternionf INV = new Quaternionf();
    private static final Vector3f TMP = new Vector3f();
    private static final Vector4f V4 = new Vector4f();

    private Projector() {}

    public static float[] project(Vec3d world, float tickDelta) {
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null || mc.getWindow() == null) return null;
        int W = mc.getWindow().getScaledWidth(), H = mc.getWindow().getScaledHeight();
        if (W <= 0 || H <= 0) return null;

        float fov = mc.options.getFov().getValue().floatValue();
        PROJ.identity().perspective((float) Math.toRadians(fov), W / (float) H, 0.05f, 1024f);

        Vec3d rel = world.subtract(cam.getCameraPos());
        TMP.set((float) rel.x, (float) rel.y, (float) rel.z);
        cam.getRotation().conjugate(INV);
        INV.transform(TMP);
        V4.set(TMP.x, TMP.y, TMP.z, 1f);
        PROJ.transform(V4);
        if (V4.w <= 0.05f) return null;

        float sx = (V4.x / V4.w * 0.5f + 0.5f) * W;
        float sy = (1f - (V4.y / V4.w * 0.5f + 0.5f)) * H;
        return new float[]{sx, sy};
    }

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
