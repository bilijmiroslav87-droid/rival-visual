package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ColorSetting;
import com.collapse.settings.NumSetting;
import com.collapse.settings.WorldFx;

/** Настоящий туман мира (как в шейдерах): дистанция начала/конца и цвет. */
public class CustomFog extends Module {
    private final NumSetting start = add(new NumSetting("Начало (блоки)", 10, 0, 200, 1));
    private final NumSetting end = add(new NumSetting("Конец (блоки)", 80, 8, 400, 1));
    private final ColorSetting color = add(new ColorSetting("Цвет", 0xFFF65C52));
    private final BoolSetting rainbow = add(new BoolSetting("Радуга", false));

    public CustomFog() {
        super("CustomFog", "Настоящий туман в мире: цвет и дистанция", Category.VISUALS);
    }

    @Override
    public void onTick() {
        WorldFx.fogOn = true;
        WorldFx.fogStart = Math.min(start.f(), end.f() - 1);
        WorldFx.fogEnd = end.f();
        WorldFx.fogColor = color.get() & 0xFFFFFF;
        WorldFx.fogRainbow = rainbow.get();
    }

    @Override
    protected void onDisable() { WorldFx.fogOn = false; }
}
