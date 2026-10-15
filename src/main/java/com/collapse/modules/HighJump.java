package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class HighJump extends Module {
    private final NumSetting power = add(new NumSetting("Power", 0.6, 0.42, 2.0, 0.02));

    public HighJump() {
        super("HighJump", "Высокий прыжок", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, power.get(), v.z);
        }
    }
}
