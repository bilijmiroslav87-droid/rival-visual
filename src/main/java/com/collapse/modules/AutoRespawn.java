package com.collapse.modules;

import com.collapse.Module;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Мгновенный респавн", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.player.isDead()) mc.player.requestRespawn();
    }
}
