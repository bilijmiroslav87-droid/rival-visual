package com.collapse.modules;

import com.collapse.Module;

public class Timer extends Module {
    public Timer() {
        super("Timer", "Ускоряет движение игрока", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.input.hasForwardMovement()) {
            var v = mc.player.getVelocity();
            mc.player.setVelocity(v.x * 1.08, v.y, v.z * 1.08);
        }
    }
}
