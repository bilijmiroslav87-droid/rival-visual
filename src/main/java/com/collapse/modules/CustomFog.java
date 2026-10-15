package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * CustomFog: цветная дымка поверх экрана (HUD-оверлей, без шейдеров,
 * поэтому работает и в мобильном рендере FCL).
 */
public class CustomFog extends Module {
    private final ModeSetting color = add(new ModeSetting("Color", "Theme", "Theme", "White", "Red", "Blue", "Purple", "Rainbow"));
    private final NumSetting density = add(new NumSetting("Density", 60, 5, 200, 5));

    public CustomFog() {
        super("CustomFog", "Цветной туман по краям экрана", Category.VISUALS);
    }

    private int base() {
        return switch (color.get()) {
            case "White" -> 0xFFFFFFFF;
            case "Red" -> 0xFFFF2A2A;
            case "Blue" -> 0xFF2A6BFF;
            case "Purple" -> 0xFF9B3DFF;
            case "Rainbow" -> Theme.rainbow(0);
            default -> Theme.accent();
        };
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        int c = base();
        int steps = 8;
        int size = density.i();
        for (int i = 0; i < steps; i++) {
            int a = (int) (110 * (1 - i / (float) steps));
            int col = Theme.withAlpha(c, a);
            int d = (int) (size * (i / (float) steps));
            int d2 = (int) (size * ((i + 1) / (float) steps));
            ctx.fill(0, d, w, d2, col);
            ctx.fill(0, h - d2, w, h - d, col);
            ctx.fill(d, 0, d2, h, col);
            ctx.fill(w - d2, 0, w - d, h, col);
        }
    }
}
