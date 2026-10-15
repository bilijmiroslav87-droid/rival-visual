package com.collapse.settings;

public abstract class Setting<T> {
    private final String name;
    protected T value;
    private final T def;

    protected Setting(String name, T def) {
        this.name = name;
        this.def = def;
        this.value = def;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T v) { this.value = v; }
    public T getDefault() { return def; }
}
