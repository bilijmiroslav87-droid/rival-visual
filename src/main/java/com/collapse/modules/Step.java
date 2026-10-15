package com.collapse.modules;

import com.collapse.Module;

public class Step extends Module {
    public Step() {
        super("Step", "Автоподъём на блоки", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.horizontalCollision && mc.player.isOnGround() && com.collapse.util.Keys.moving()) {
            mc.player.jump();
        }
    }
}
