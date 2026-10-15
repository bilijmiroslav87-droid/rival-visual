package com.collapse.settings;

public class NumSetting extends Setting<Double> {
    private final double min, max, step;

    public NumSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min; this.max = max; this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public float f() { return value.floatValue(); }
    public int i() { return value.intValue(); }

    @Override
    public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) c = Math.round(c / step) * step;
        this.value = c;
    }

    public double getPercent() { return (value - min) / (max - min); }
}
