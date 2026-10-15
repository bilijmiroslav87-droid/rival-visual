package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;

public class CustomTime extends Module {
    private final NumSetting time = add(new NumSetting("Time", 6000, 0, 24000, 250));

    public CustomTime() {
        super("CustomTime", "Своё время суток (клиентское)", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (mc.world != null) {
            long t = time.i();
            mc.world.setTime(t, t, false);
        }
    }
}
