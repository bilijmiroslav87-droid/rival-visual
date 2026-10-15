package com.collapse.settings;

public class ModeSetting extends Setting<String> {
    private final String[] modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = modes;
    }

    public String[] getModes() { return modes; }

    public void next() {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(value)) {
                value = modes[(i + 1) % modes.length];
                return;
            }
        }
        value = modes[0];
    }

    public boolean is(String m) { return value.equalsIgnoreCase(m); }
}
