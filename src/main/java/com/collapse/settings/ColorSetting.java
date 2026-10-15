package com.collapse.settings;

public class ColorSetting extends Setting<Integer> {
    private static final int[] PALETTE = {
        0xFF4A9EFF, 0xFF9B6BFF, 0xFFFF4D5E, 0xFF3DDC84, 0xFFFF6BC1,
        0xFFFF9A3D, 0xFFFFFFFF, 0xFFFFE066, 0xFF3DDCD0, 0xFF000000
    };

    public ColorSetting(String name, int def) { super(name, def); }

    public void next() {
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == value) { value = PALETTE[(i + 1) % PALETTE.length]; return; }
        }
        value = PALETTE[0];
    }
}
