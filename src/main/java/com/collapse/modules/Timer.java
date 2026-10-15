package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;

public class Timer extends Module {
    private final NumSetting boost = add(new NumSetting("Boost", 1.06, 1.0, 1.5, 0.01));

    public Timer() {
        super("Timer", "Ускоряет движение игрока", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.input.hasForwardMovement()) return;
        var v = mc.player.getVelocity();
        mc.player.setVelocity(v.x * boost.get(), v.y, v.z * boost.get());
    }
}
