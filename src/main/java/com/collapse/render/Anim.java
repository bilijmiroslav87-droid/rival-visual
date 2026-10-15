package com.collapse.render;

/** Плавная интерполяция значения к цели (экспоненциальная). */
public final class Anim {
    private float value;
    private float target;
    private long last = System.nanoTime();
    private final float speed;

    public Anim(float start, float speed) {
        this.value = start;
        this.target = start;
        this.speed = speed;
    }

    public Anim(float start) { this(start, 14f); }

    public void set(float t) { this.target = t; }
    public void snap(float v) { this.value = v; this.target = v; }

    public float get() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        value += (target - value) * Math.min(1f, dt * speed);
        if (Math.abs(target - value) < 0.0005f) value = target;
        return value;
    }

    public float target() { return target; }

    public static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (float) Math.pow(1 - t, 3);
    }
}
