package com.collapse.modules;

import com.collapse.Module;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Автоматический спринт", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (!mc.player.isSprinting() && !mc.player.isSneaking() && mc.player.input.hasForwardMovement()) {
            mc.player.setSprinting(true);
        }
    }
}
