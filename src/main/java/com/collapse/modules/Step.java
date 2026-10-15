package com.collapse.modules;

import com.collapse.Module;

public class Step extends Module {
    public Step() {
        super("Step", "Запрыгивание на блоки без прыжка", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.horizontalCollision && mc.player.isOnGround() && mc.player.input.hasForwardMovement()) {
            mc.player.jump();
        }
    }
}
