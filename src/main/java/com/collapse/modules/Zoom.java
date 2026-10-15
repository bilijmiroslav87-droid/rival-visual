package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;

public class Zoom extends Module {
    private final NumSetting fov = add(new NumSetting("Zoom FOV", 15, 5, 60, 1));
    private Integer prev = null;

    public Zoom() {
        super("Zoom", "Приближение (зажми C)", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) prev = mc.options.getFov().getValue();
    }

    @Override
    public void onTick() {
        if (mc.options != null) mc.options.getFov().setValue(fov.i());
    }

    @Override
    protected void onDisable() {
        if (mc.options != null && prev != null) mc.options.getFov().setValue(prev);
        prev = null;
    }
}
