package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.util.math.Vec3d;

public class Glide extends Module {
    public Glide() {
        super("Glide", "Медленное падение", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.player.isOnGround()) return;
        Vec3d v = mc.player.getVelocity();
        if (v.y < -0.1) mc.player.setVelocity(v.x, -0.1, v.z);
    }
}
