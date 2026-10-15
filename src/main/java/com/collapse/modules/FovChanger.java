package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;

public class FovChanger extends Module {
    private final NumSetting fov = add(new NumSetting("FOV", 100, 30, 140, 1));
    private Integer prev = null;

    public FovChanger() {
        super("FOV", "Свой угол обзора", Category.VISUALS);
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
