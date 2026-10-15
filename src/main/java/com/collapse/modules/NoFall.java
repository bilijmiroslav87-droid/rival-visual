package com.collapse.modules;

import com.collapse.Module;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Сбрасывает урон от падения", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.player.fallDistance > 2f) {
            mc.player.fallDistance = 0;
        }
    }
}
