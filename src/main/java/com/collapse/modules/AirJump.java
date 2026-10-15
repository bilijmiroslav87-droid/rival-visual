package com.collapse.modules;

import com.collapse.Module;

public class AirJump extends Module {
    private int cd = 0;

    public AirJump() {
        super("AirJump", "Прыжок в воздухе", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (cd > 0) cd--;
        if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && cd == 0) {
            mc.player.jump();
            cd = 8;
        }
    }
}
