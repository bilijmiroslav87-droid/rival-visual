package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    public Spider() {
        super("Spider", "Лазание по стенам", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.horizontalCollision) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.2, v.z);
        }
    }
}
