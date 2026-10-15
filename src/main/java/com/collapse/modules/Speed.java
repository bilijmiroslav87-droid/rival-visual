package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
    private final NumSetting mult = add(new NumSetting("Multiplier", 1.4, 1.0, 4.0, 0.1));

    public Speed() {
        super("Speed", "Ускорение на земле", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.isOnGround()) return;
        if (!mc.player.input.hasForwardMovement()) return;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x * mult.get(), v.y, v.z * mult.get());
    }
}
