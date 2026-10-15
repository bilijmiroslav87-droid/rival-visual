package com.collapse.modules;

import com.collapse.Module;

public class NoRain extends Module {
    public NoRain() {
        super("NoRain", "Убирает дождь и грозу", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (mc.world != null) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    }
}
