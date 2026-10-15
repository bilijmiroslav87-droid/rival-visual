package com.collapse.settings;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean def) { super(name, def); }
    public void toggle() { value = !value; }
}
